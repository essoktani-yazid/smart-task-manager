import json
import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from app.database import db
from app.services.query_service import handle_user_request
from app.utils.logger import log

app = FastAPI()

@app.on_event("startup")
async def startup():
    log.info("Démarrage du serveur STM AI...")
    db.connect()

@app.websocket("/ai/stream")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    log.success("Client WebSocket connecté")
    
    user_id = "eb3a2803-29b1-4fa1-9b57-7f6b68ced78b"

    try:
        while True:
            try:
                # 1. Attente du message
                data_str = await websocket.receive_text()
                
                # 2. Gestion du signal STOP brut
                if data_str == "__STOP__":
                    log.incoming("Signal STOP reçu. Annulation du traitement en cours.")
                    continue

                # 3. Parsing du message JSON
                data = json.loads(data_str)

                # 4. Routing
                if "action" in data:
                    # --- GESTION DES CONFIRMATIONS (Boutons UI) ---
                    action = data["action"]
                    log.incoming(f"Action reçue : {action}")
                    
                    if action == "CONFIRM":
                        sql = data.get("sql")
                        if sql:
                            log.db(f"Exécution SQL confirmée : {sql}")
                            db.execute(sql)
                            await websocket.send_json({
                                "display_message": "✅ Action effectuée avec succès !",
                                "requires_confirmation": False
                            })
                    elif action == "CANCEL":
                        await websocket.send_json({
                            "display_message": "❌ Action annulée.",
                            "requires_confirmation": False
                        })
                
                else:
                    # --- FLUX NORMAL (Question utilisateur) ---
                    prompt = data.get("prompt")
                    if prompt:
                        log.incoming(f"Prompt : '{prompt}'")
                        await handle_user_request(websocket, user_id, prompt)

            except json.JSONDecodeError:
                log.error("Message reçu non-JSON")
            except WebSocketDisconnect:
                log.warning("Client déconnecté (boucle)")
                break
            except Exception as e:
                log.error(f"Erreur inattendue : {e}")
                break

    except WebSocketDisconnect:
        log.warning("Client déconnecté proprement")

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)