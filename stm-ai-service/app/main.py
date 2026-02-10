import json
import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from app.database import db
from app.services.query_service import handle_user_request, execute_and_summarize, generate_dashboard_insights
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
    
    # ID utilisateur simulé (à remplacer par le token réel plus tard)
    user_id = "eb3a2803-29b1-4fa1-9b57-7f6b68ced78b"

    try:
        while True:
            try:
                data_str = await websocket.receive_text()
                
                if data_str == "__STOP__":
                    log.incoming("Signal STOP reçu.")
                    continue

                data = json.loads(data_str)

                if "action" in data:
                    action = data["action"]
                    log.incoming(f"Action reçue : {action}")
                    
                    if action == "CONFIRM":
                        sql_to_run = data.get("sql")
                        if sql_to_run:
                            await execute_and_summarize(websocket, sql_to_run)
                    
                    elif action == "ANALYZE_DASHBOARD":
                        user_stats = data.get("stats", {})
                        await generate_dashboard_insights(websocket, user_stats)
                        
                else:
                    prompt = data.get("prompt")
                    if prompt:
                        log.incoming(f"Prompt : '{prompt}'")
                        await handle_user_request(websocket, user_id, prompt)

            except json.JSONDecodeError:
                log.error("Message non-JSON reçu")
            except WebSocketDisconnect:
                break
            except Exception as e:
                log.error(f"Erreur : {e}")
                break

    except WebSocketDisconnect:
        log.warning("Client déconnecté")

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)