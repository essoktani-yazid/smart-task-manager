import json
from fastapi import WebSocketDisconnect
from app.database import db
from app.services.ai_service import stream_llm_response
from app.rag import prompts
from app.utils.logger import log

# --- UTILITAIRES ---
def clean_llm_json(response: str) -> str:
    """
    Nettoie la réponse LLM pour extraction JSON.
    Retire les balises markdown ```json et ``` si présentes.
    
    Args:
        response: La réponse brute du LLM
        
    Returns:
        JSON nettoyé sans balises markdown
    """
    cleaned = response.strip()
    
    # Retirer balises markdown ```json
    if cleaned.startswith("```json"):
        cleaned = cleaned[7:]  # Enlever ```json
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3]  # Enlever ```
        cleaned = cleaned.strip()
    # Retirer balises markdown ```
    elif cleaned.startswith("```"):
        cleaned = cleaned[3:]  # Enlever ```
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3]  # Enlever ```
        cleaned = cleaned.strip()
    
    return cleaned

# --- UTILITAIRES D'ENVOI SÉCURISÉ ---
async def safe_send(websocket, data: dict):
    """
    Envoie des données au client WebSocket de manière sécurisée.
    Retourne False si le client est déconnecté, True sinon.
    """
    try:
        await websocket.send_json(data)
        return True
    except (WebSocketDisconnect, RuntimeError):
        log.warning("Client déconnecté pendant l'envoi.")
        return False

async def send_status(websocket, message):
    """Envoie un message de statut intermédiaire (pour le spinner)."""
    return await safe_send(websocket, {"status": message})

# --- LOGIQUE PRINCIPALE ---
async def handle_user_request(websocket, user_id, user_message):
    log.info("--- Début du traitement ---")
    
    # 1. ANALYSE DE L'INTENTION
    # On envoie un statut pour activer le spinner côté client
    if not await send_status(websocket, "Analyzing user intent..."): return
    
    # Appel au LLM pour obtenir le JSON (Operation + SQL)
    system_prompt = prompts.get_system_prompt_sql(user_id)
    # IMPORTANT: return_as_string=True pour parser le JSON
    ai_res = await stream_llm_response(user_message, system_prompt, return_as_string=True)
    
    try:
        # Nettoyer la réponse si elle contient des balises markdown
        cleaned_res = clean_llm_json(ai_res)
        data = json.loads(cleaned_res)
        op = data["operation_type"]
        query = data["sql_query"]
        log.ai(f"Opération : {op} | SQL : {query}")
    except Exception as e:
        log.error(f"Erreur parsing JSON IA : {e}")
        log.error(f"Réponse reçue : {ai_res}")
        await safe_send(websocket, {"display_message": "❌ Error analyzing request. Please try again.", "requires_confirmation": False})
        return

    # 2. TRAITEMENT SELON LE TYPE D'OPÉRATION
    
    # =========================================================================
    # CAS 1 : LECTURE (READ)
    # =========================================================================
    if op == "READ":
        if not await send_status(websocket, "Reading database..."): return
        
        try:
            # Exécution directe
            rows = db.execute(query).fetchall()
            log.db(f"Lignes récupérées : {len(rows)}")
        except Exception as e:
            await safe_send(websocket, {"display_message": f"❌ Erreur SQL: {e}", "requires_confirmation": False})
            return

        if not await send_status(websocket, f"Found {len(rows)} items. Summarizing..."): return
        
        # Génération du résumé naturel via le LLM (en string complète)
        final_text = await stream_llm_response(
            prompts.get_final_answer_prompt(user_message, rows), 
            "You are a helpful assistant.",
            return_as_string=True
        )
        
        await safe_send(websocket, {"display_message": final_text, "requires_confirmation": False})

    # =========================================================================
    # CAS 2 : CRÉATION (CREATE)
    # =========================================================================
    elif op == "CREATE":
        if not await send_status(websocket, "Preparing creation request..."): return
        
        # On demande à l'IA de formuler une confirmation propre
        confirm_text = await stream_llm_response(
            prompts.get_confirmation_prompt(query), 
            "Assistant de confirmation.",
            return_as_string=True
        )
        
        await safe_send(websocket, {
            "display_message": confirm_text,
            "requires_confirmation": True, # Boutons visibles
            "sql_to_execute": query,
            "operation_type": "CREATE"
        })

    # =========================================================================
    # CAS 3 : MODIFICATION / SUPPRESSION (UPDATE / DELETE)
    # =========================================================================
    elif op in ["UPDATE", "DELETE"]:
        if not await send_status(websocket, f"Calculating impact for {op}..."): return
        
        # BLOC DE SÉCURITÉ GLOBAL (Empêche le spinner infini)
        try:
            # A. Construction de la requête de prévisualisation (SELECT)
            preview_query = query.replace("DELETE FROM", "SELECT * FROM").replace("UPDATE", "SELECT * FROM")
            
            # Gestion des UPDATE complexes (ex: UPDATE tasks SET ... WHERE ...)
            if "SET" in preview_query:
                try:
                    table_part = "tasks" if "tasks" in query else "sub_tasks"
                    if "WHERE" in query.upper():
                        idx = query.upper().find("WHERE")
                        where_part = query[idx:]
                        preview_query = f"SELECT * FROM {table_part} {where_part}"
                    else:
                        # Sécurité si pas de WHERE : on limite pour voir un échantillon
                        preview_query = f"SELECT * FROM {table_part} LIMIT 5"
                except Exception as parsing_error:
                    log.error(f"Erreur parsing preview: {parsing_error}")
                    preview_query = query # Fallback (risqué mais rare)

            # B. Exécution de la simulation
            rows = []
            count = 0
            try:
                cursor = db.execute(preview_query)
                if cursor:
                    rows = cursor.fetchall()
                    count = len(rows)
            except Exception as sql_error:
                log.error(f"Erreur SQL Simulation: {sql_error}")
                rows = []
                count = 0

            # C. Logique de réponse selon le nombre de résultats

            # --- SOUS-CAS : AUCUN RÉSULTAT (0) ---
            if count == 0:
                log.ai("0 résultat trouvé. Demande d'explication à l'IA...")
                
                no_result_msg = await stream_llm_response(
                    prompts.get_no_results_prompt(user_message, query),
                    "You are a helpful assistant.",
                    return_as_string=True
                )

                # IMPORTANT : requires_confirmation=False pour ne pas afficher les boutons
                await safe_send(websocket, {
                    "display_message": no_result_msg,
                    "requires_confirmation": False, 
                    "sql_to_execute": None
                })
            
            # --- SOUS-CAS : RÉSULTATS TROUVÉS (> 0) ---
            else:
                # 1. Préparation des exemples
                examples_list = []
                for row in rows[:3]: 
                    # Gestion robuste du titre
                    title = row.get('title') or row.get('description') or f"Task #{row.get('id')}"
                    examples_list.append(f"• {title}")
                
                examples_str = "\n".join(examples_list)
                if len(rows) > 3:
                    examples_str += f"\n• ... and {len(rows)-3} others."

                # 2. Tentative de génération par l'IA avec Fallback (Sécurité)
                log.ai(f"Génération avertissement pour {count} items...")
                
                warning_msg = ""
                try:
                    # On essaie d'appeler l'IA
                    warning_msg = await stream_llm_response(
                        prompts.get_impact_warning_prompt(user_message, count, examples_str, op), # On passe 'op'
                        "Security Assistant",
                        return_as_string=True
                    )
                except Exception as llm_error:
                    # SI L'IA PLANTE (KeyError 'choices', Timeout, etc.)
                    log.error(f"⚠️ L'IA n'a pas pu générer l'avertissement : {llm_error}")
                    # => On utilise un message standard de secours
                    warning_msg = f"⚠️ **Attention** : This action will affect **{count}** task(s).\n\n"
                    warning_msg += f"Examples:\n{examples_str}\n\n"
                    warning_msg += "**System Fallback:** Could not verify with AI, please confirm carefully.\n"
                    warning_msg += "Do you want to proceed?"

                # 3. Envoi du message (IA ou Secours)
                await safe_send(websocket, {
                    "display_message": warning_msg,
                    "requires_confirmation": True, 
                    "sql_to_execute": query,
                    "operation_type": op
                })

        except Exception as critical_error:
            # Filet de sécurité ultime : Si tout plante, on prévient le client
            log.error(f"CRASH CRITIQUE DANS UPDATE/DELETE: {critical_error}")
            await safe_send(websocket, {
                "display_message": f"❌ An internal error occurred.\nDetails: {str(critical_error)}",
                "requires_confirmation": False
            })
    
    log.info("--- Fin du traitement ---")


async def execute_and_summarize(websocket, sql_query):
    """
    Exécute le SQL confirmé, vérifie le résultat, et utilise l'IA pour la réponse finale.
    """
    log.info(f"--- Exécution confirmée : {sql_query} ---")
    
    # 1. Feedback visuel : Exécution en cours
    if not await send_status(websocket, "Executing operation..."): return

    execution_result = ""
    is_success = True

    # 2. Exécution réelle en base de données
    try:
        # On utilise db.execute qui retourne un curseur (via SQLAlchemy ou connecteur brut)
        cursor = db.execute(sql_query)
        # On tente de récupérer le nombre de lignes affectées (rowcount)
        row_count = cursor.rowcount if hasattr(cursor, 'rowcount') else "unknown"
        
        execution_result = f"Success. Rows affected: {row_count}"
        log.db(f"Exécution OK. Rows: {row_count}")
        
    except Exception as e:
        is_success = False
        execution_result = f"Error: {str(e)}"
        log.error(f"Erreur SQL execution: {e}")

    # 3. Feedback visuel : Analyse du résultat
    if not await send_status(websocket, "Analyzing result..."): return

    # 4. Appel au LLM pour générer la réponse post-action
    # On crée un prompt 'ad-hoc' ou on l'ajoute dans prompts.py
    summary_prompt = (
        f"The user ordered an operation. Here is the SQL executed: \"{sql_query}\".\n"
        f"Here is the database execution result: \"{execution_result}\".\n"
        "Generate a short, natural language response confirming the action to the user."
        "If it failed, apologize and explain why."
    )

    final_message = await stream_llm_response(
        summary_prompt, 
        "You are a helpful task manager assistant.",
        return_as_string=True
    )

    # 5. Envoi de la réponse finale au client
    # Le client affichera le message et arrêtera le spinner
    await safe_send(websocket, {
        "display_message": final_message,
        "requires_confirmation": False
    })

async def generate_dashboard_insights(websocket, user_stats):
    """
    Génère l'analyse pour le Dashboard (Smart Header).
    """
    try:
        # 1. Préparer le prompt
        stats_str = json.dumps(user_stats)
        prompt_text = prompts.get_dashboard_insight_prompt(stats_str)
        
        log.ai(f"📊 Génération insights dashboard...")
        log.ai(f"Stats: {stats_str}")
        
        # 2. Appel au LLM pour obtenir le JSON d'insights
        content = await stream_llm_response(
            prompt=prompt_text,
            system_content="Productivity Coach",
            return_as_string=True  # On veut un JSON complet
        )
        
        log.ai(f"Réponse LLM brute: {content[:100]}...")
        
        # 2.5 Nettoyer les balises markdown si présentes
        cleaned_content = clean_llm_json(content)
        
        log.ai(f"JSON nettoyé: {cleaned_content[:100]}...")
        
        # 2.6 Valider que c'est du JSON valide avant d'envoyer
        try:
            json.loads(cleaned_content)  # Test de validation
            log.success(f"✅ JSON d'insights valide généré")
        except json.JSONDecodeError as json_err:
            log.error(f"❌ JSON invalide après nettoyage: {json_err}")
            log.error(f"Contenu: {cleaned_content}")
            raise
        
        # 3. Envoi au client Java
        await websocket.send_text(cleaned_content)
        log.success(f"📤 Insights envoyés au dashboard")
        
    except Exception as e:
        log.error(f"Erreur lors de la génération d'insights : {e}")
        import traceback
        traceback.print_exc()
        
        # Fallback avec valeurs par défaut
        error_json = {
            "mood": "🤖",
            "title": "Dashboard Ready",
            "message": "Your productivity data is being analyzed. Check back soon!",
            "theme_color": "#6366F1",
            "action_label": None
        }
        await websocket.send_text(json.dumps(error_json))