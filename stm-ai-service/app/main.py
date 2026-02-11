import json
import uvicorn
import os
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from vosk import Model, KaldiRecognizer

from app.database import db
from app.services.query_service import handle_user_request, execute_and_summarize, generate_dashboard_insights
from app.services.voice_stream import VoiceService
from app.utils.logger import log

app = FastAPI()

# --- INITIALISATION VOSK ---
VOSK_MODEL_PATH = "model"
model = None
if os.path.exists(VOSK_MODEL_PATH):
    model = Model(VOSK_MODEL_PATH)
    log.success("Vosk chargé pour le streaming auto")

voice_service = VoiceService(default_voice="en")  # Peut être configuré depuis .env

@app.on_event("startup")
async def startup():
    db.connect()

class SmartWebSocket:
    def __init__(self, websocket: WebSocket):
        self._ws = websocket
        self.voice_mode_active = False 

    async def send_json(self, data: dict):
        # Envoi au chat JavaFX
        await self._ws.send_json(data)
        # Si message texte reçu via voix, on lance le TTS
        if self.voice_mode_active and "display_message" in data:
            text = data["display_message"]
            await voice_service.text_to_speech_stream(iter([text]), self._ws)

    async def accept(self): await self._ws.accept()
    async def receive(self): return await self._ws.receive()

@app.websocket("/ai/stream")
async def websocket_endpoint(websocket: WebSocket):
    # ⚡ OPTIMISÉ: Taille de buffer WebSocket pour réduire latence
    await websocket.accept()
    
    smart_ws = SmartWebSocket(websocket)
    
    # Configuration optimisée du recognizer pour réduire la latence
    if model:
        rec = KaldiRecognizer(model, 16000)
        # Activer les résultats partiels pour feedback plus rapide
        rec.SetWords(True)
        rec.SetPartialWords(True)
        # ⚡ Réduire le buffer pour réponse plus rapide
        rec.SetMaxAlternatives(1)  # Pas d'alternatives multiples
    else:
        rec = None
        
    current_user_id = "1" 

    try:
        while True:
            message = await smart_ws.receive()

            if "bytes" in message and rec:
                smart_ws.voice_mode_active = True 
                if rec.AcceptWaveform(message["bytes"]):
                    res = json.loads(rec.Result())
                    text = res.get("text", "")
                    if text:
                        log.incoming(f"🎤 Voix : {text}")
                        # Déclenche la logique métier + affichage chat + TTS auto
                        await handle_user_request(smart_ws, current_user_id, text)

            elif "text" in message:
                data = json.loads(message["text"])
                if "prompt" in data:
                    smart_ws.voice_mode_active = False # Désactive TTS si on tape
                    await handle_user_request(smart_ws, data.get("userId", "1"), data["prompt"])
                elif data.get("action") == "CONFIRM":
                    await execute_and_summarize(smart_ws, data.get("sql"))
                elif data.get("action") == "ANALYZE_DASHBOARD":
                    # Gestion de l'analyse du dashboard
                    stats = data.get("stats", {})
                    log.incoming(f"📊 Demande d'analyse Dashboard: {stats}")
                    await generate_dashboard_insights(smart_ws._ws, stats)

    except WebSocketDisconnect:
        log.warning("Client déconnecté")