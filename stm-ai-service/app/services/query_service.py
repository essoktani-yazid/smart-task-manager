import json
from fastapi import WebSocketDisconnect
from app.database import db
from app.services.ai_service import call_llm
from app.rag import prompts
from app.utils.logger import log

# --- UTILITAIRES D'ENVOI SÉCURISÉ ---
async def safe_send(websocket, data: dict):
    """Envoie des données au client. Retourne False si le client est parti."""
    try:
        await websocket.send_json(data)
        return True
    except (WebSocketDisconnect, RuntimeError):
        log.warning("Client déconnecté pendant l'envoi.")
        return False

async def send_status(websocket, message):
    """Envoie un message de statut intermédiaire."""
    return await safe_send(websocket, {"status": message})

# --- LOGIQUE PRINCIPALE ---
async def handle_user_request(websocket, user_id, user_message):
    log.info("--- Début du traitement ---")
    
    # 1. ANALYSE DE L'INTENTION
    if not await send_status(websocket, "Analyzing user intent..."): return
    
    system_prompt = prompts.get_system_prompt_sql(user_id)
    ai_res = await call_llm(user_message, system_prompt)
    
    try:
        data = json.loads(ai_res)
        op = data["operation_type"]
        query = data["sql_query"]
        log.ai(f"Opération : {op} | SQL : {query}")
    except Exception as e:
        log.error(f"Erreur parsing JSON IA : {e}")
        await send_status(websocket, "Error analyzing request.")
        return

    # 2. BRANCHEMENT SELON L'OPÉRATION
    
    # === CAS 1 : LECTURE (READ) ===
    # On exécute tout de suite et on résume
    if op == "READ":
        if not await send_status(websocket, "Reading database..."): return
        
        try:
            rows = db.execute(query).fetchall()
            log.db(f"Lignes récupérées : {len(rows)}")
        except Exception as e:
            await safe_send(websocket, {"display_message": f"❌ Erreur SQL: {e}", "requires_confirmation": False})
            return

        if not await send_status(websocket, f"Found {len(rows)} items. Summarizing..."): return
        
        # Génération de la réponse naturelle
        final_text = await call_llm(
            prompts.get_final_answer_prompt(user_message, rows), 
            "You are a helpful assistant.", 
            is_json=False
        )
        
        await safe_send(websocket, {"display_message": final_text, "requires_confirmation": False})

    # === CAS 2 : CRÉATION (CREATE) ===
    # On ne crée PAS. On demande confirmation.
    elif op == "CREATE":
        if not await send_status(websocket, "Preparing creation request..."): return
        
        confirm_text = await call_llm(
            prompts.get_confirmation_prompt(query), 
            "Assistant de confirmation.", 
            is_json=False
        )
        
        await safe_send(websocket, {
            "display_message": confirm_text,
            "requires_confirmation": True,
            "sql_to_execute": query,
            "operation_type": "CREATE"
        })

    # === CAS 3 : MODIFICATION / SUPPRESSION (UPDATE / DELETE) ===
    # On simule l'impact avec un SELECT et on demande confirmation.
    elif op in ["UPDATE", "DELETE"]:
        if not await send_status(websocket, f"Calculating impact for {op}..."): return
        
        # Transformation de la requête destructive en requête de visualisation (SELECT)
        # Ceci est une implémentation simplifiée. Pour la prod, utiliser un parser SQL.
        preview_query = query.replace("DELETE FROM", "SELECT * FROM").replace("UPDATE", "SELECT * FROM")
        
        # Nettoyage sommaire si UPDATE contient SET ...
        if "SET" in preview_query:
            # On coupe après le nom de la table et on garde le WHERE
            # Ex: UPDATE tasks SET status='DONE' WHERE id=1 -> SELECT * FROM tasks WHERE id=1
            try:
                table_part = "tasks" if "tasks" in query else "sub_tasks"
                if "WHERE" in query.upper():
                    where_part = query[query.upper().find("WHERE"):]
                    preview_query = f"SELECT * FROM {table_part} {where_part}"
                else:
                    # Sécurité : Pas de WHERE = Pas d'action massive sans avertissement
                    preview_query = f"SELECT * FROM {table_part} LIMIT 5" 
            except:
                preview_query = query # Fallback risqué mais nécessaire pour debug

        try:
            rows = db.execute(preview_query).fetchall()
            count = len(rows)
        except:
            rows = []
            count = "?"

        # Construction du message d'avertissement
        msg = f"⚠️ **Attention** : Cette action va impacter **{count}** élément(s).\n\n"
        if rows:
            msg += "Exemples concernés :\n"
            for row in rows[:3]: # On n'affiche que les 3 premiers
                msg += f"- {row.get('title', 'Element')}\n"
            if len(rows) > 3:
                msg += f"... et {len(rows)-3} autres.\n"
        
        msg += "\nVoulez-vous vraiment continuer ?"

        await safe_send(websocket, {
            "display_message": msg,
            "requires_confirmation": True,
            "sql_to_execute": query,
            "operation_type": op
        })
    
    log.info("--- Fin du traitement ---")