import asyncio
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

voice_service = VoiceService()

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
        if "display_message" in data:
            log.info(f"📤 [FLOW] display_message envoyé | voice_mode_active={self.voice_mode_active}")
        # Si message texte reçu via voix, on lance le TTS
        if self.voice_mode_active and "display_message" in data:
            log.info("🔊 [FLOW] Lancement TTS")
            await asyncio.sleep(0.05)
            text = data["display_message"]
            async def async_text_gen():
                yield text
            await voice_service.text_to_speech_stream(async_text_gen(), self._ws)
        elif "display_message" in data:
            log.info("⏭️ [FLOW] TTS ignoré (voice_mode_inactif)")

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
    last_partial_text = ""  # 🆕 Garder la dernière transcription partielle

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
                        last_partial_text = ""  # Reset après traitement
                        await handle_user_request(smart_ws, current_user_id, text)
                    else:
                        log.warning("⚠️ Vosk a traité le waveform mais aucun texte transcrit")
                else:
                    # Partial result (phrase non terminée)
                    partial_res = json.loads(rec.PartialResult())
                    partial_text = partial_res.get("partial", "")
                    if partial_text:
                        last_partial_text = partial_text  # 🆕 Sauvegarder
                        log.info(f"🔄 Transcription partielle: {partial_text}")

            elif "text" in message:
                data = json.loads(message["text"])
                if "prompt" in data:
                    smart_ws.voice_mode_active = False # Désactive TTS si on tape
                    last_partial_text = ""
                    await handle_user_request(smart_ws, data.get("userId", "1"), data["prompt"])
                elif data.get("action") == "CONFIRM":
                    await execute_and_summarize(smart_ws, data.get("sql"))
                elif data.get("action") == "AUDIO_END":
                    # Signal de fin d'audio - Forcer Vosk à finaliser
                    if rec:
                        log.info("🔚 Signal fin audio reçu - Appel FinalResult()")
                        final_res = json.loads(rec.FinalResult())
                        text = final_res.get("text", "")
                        
                        # 🆕 Fallback: utiliser la dernière transcription partielle si FinalResult est vide
                        if not text and last_partial_text:
                            text = last_partial_text
                            log.info(f"🔄 Utilisation du fallback partiel: {text}")
                        
                        if text:
                            log.incoming(f"🎤 Voix (Final): {text}")
                            try:
                                await handle_user_request(smart_ws, current_user_id, text)
                            except Exception as e:
                                log.error(f"❌ Erreur handle_user_request (vocal): {e}")
                                # Garantir que le client reçoit AUDIO_END même en cas d'erreur
                                await websocket.send_json({"type": "AUDIO_END"})
                        else:
                            log.warning("⚠️ Aucune parole détectée - Renvoi AUDIO_END au client")
                            await websocket.send_json({"type": "AUDIO_END"})
                        
                        # Reset pour la prochaine phrase
                        last_partial_text = ""
                        rec = KaldiRecognizer(model, 16000)
                        rec.SetWords(True)
                        rec.SetPartialWords(True)
                        rec.SetMaxAlternatives(1)
                    else:
                        log.warning("⚠️ Vosk non chargé - Renvoi AUDIO_END au client")
                        await websocket.send_json({"type": "AUDIO_END"})
                elif data.get("action") == "ANALYZE_DASHBOARD":
                    # Gestion de l'analyse du dashboard
                    stats = data.get("stats", {})
                    log.incoming(f"📊 Demande d'analyse Dashboard: {stats}")
                    await generate_dashboard_insights(smart_ws._ws, stats)

    except WebSocketDisconnect:
        log.warning("Client déconnecté")
    except RuntimeError as e:
        log.warning(f"WebSocket fermé: {e}")