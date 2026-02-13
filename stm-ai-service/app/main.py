import asyncio
import json
import uvicorn
import os
import traceback  # <--- INDISPENSABLE POUR LE DEBUG
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
    try:
        model = Model(VOSK_MODEL_PATH)
        log.success("✅ Vosk chargé pour le streaming auto")
    except Exception as e:
        log.error(f"❌ Erreur chargement Vosk: {e}")
else:
    log.warning("⚠️ Modèle Vosk introuvable (dossier 'model' manquant)")

voice_service = VoiceService()

@app.on_event("startup")
async def startup():
    try:
        db.connect()
        log.success("✅ Base de données connectée")
    except Exception as e:
        log.error(f"❌ Erreur critique connexion DB: {e}")

class SmartWebSocket:
    def __init__(self, websocket: WebSocket):
        self._ws = websocket
        self.voice_mode_active = False 

    async def send_json(self, data: dict):
        try:
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
        except Exception as e:
            log.error(f"❌ Erreur send_json: {e}")

    async def accept(self): await self._ws.accept()
    
    async def receive(self): 
        # Protection contre la déconnexion pendant la lecture
        return await self._ws.receive()

@app.websocket("/ai/stream")
async def websocket_endpoint(websocket: WebSocket):
    # ⚡ OPTIMISÉ: Taille de buffer WebSocket pour réduire latence
    await websocket.accept()
    
    smart_ws = SmartWebSocket(websocket)
    
    # Configuration optimisée du recognizer pour réduire la latence
    rec = None
    if model:
        try:
            rec = KaldiRecognizer(model, 16000)
            rec.SetWords(True)
            rec.SetPartialWords(True)
            rec.SetMaxAlternatives(1)
        except Exception as e:
            log.error(f"❌ Erreur init KaldiRecognizer: {e}")
        
    last_partial_text = "" 
    current_user_id = "1" # Valeur par défaut

    try:
        while True:
            # Protection de la réception
            try:
                message = await smart_ws.receive()
            except WebSocketDisconnect:
                raise # On laisse remonter pour gérer la fermeture propre
            except Exception as e:
                log.error(f"❌ Erreur receive(): {e}")
                break

            # --- GESTION AUDIO (BYTES) ---
            if "bytes" in message and rec:
                smart_ws.voice_mode_active = True 
                if rec.AcceptWaveform(message["bytes"]):
                    res = json.loads(rec.Result())
                    text = res.get("text", "")
                    if text:
                        log.incoming(f"🎤 Voix : {text}")
                        last_partial_text = ""
                        # Try/Catch spécifique pour ne pas casser la boucle
                        try:
                            await handle_user_request(smart_ws, current_user_id, text)
                        except Exception as e:
                            log.error(f"❌ Erreur traitement voix: {e}")
                            log.error(traceback.format_exc())
                else:
                    partial_res = json.loads(rec.PartialResult())
                    partial_text = partial_res.get("partial", "")
                    if partial_text:
                        last_partial_text = partial_text

            # --- GESTION TEXTE (JSON) ---
            elif "text" in message:
                try:
                    data = json.loads(message["text"])
                except json.JSONDecodeError:
                    log.error(f"⚠️ JSON malformé reçu: {message['text']}")
                    continue # On ignore ce message et on passe au suivant

                # Mise à jour de l'ID utilisateur si présent
                if "userId" in data:
                    current_user_id = str(data["userId"])

                if "prompt" in data:
                    smart_ws.voice_mode_active = False # Désactive TTS si on tape
                    last_partial_text = ""
                    log.incoming(f"💬 Texte reçu: {data['prompt']}")
                    await handle_user_request(smart_ws, current_user_id, data["prompt"])

                elif data.get("action") == "CONFIRM":
                    log.info(f"✅ Action CONFIRM reçue pour SQL")
                    await execute_and_summarize(smart_ws, data.get("sql"))

                elif data.get("action") == "AUDIO_END":
                    if rec:
                        log.info("🔚 Signal fin audio")
                        final_res = json.loads(rec.FinalResult())
                        text = final_res.get("text", "")
                        
                        # Fallback partiel
                        if not text and last_partial_text:
                            text = last_partial_text
                            log.info(f"🔄 Fallback partiel utilisé: {text}")
                        
                        if text:
                            log.incoming(f"🎤 Voix (Final): {text}")
                            await handle_user_request(smart_ws, current_user_id, text)
                        else:
                            await websocket.send_json({"type": "AUDIO_END"})
                        
                        # Reset
                        last_partial_text = ""
                        rec = KaldiRecognizer(model, 16000)
                        rec.SetWords(True)
                        rec.SetPartialWords(True)
                        rec.SetMaxAlternatives(1)
                    else:
                        await websocket.send_json({"type": "AUDIO_END"})

                elif data.get("action") == "ANALYZE_DASHBOARD":
                    stats = data.get("stats", {})
                    log.incoming(f"📊 Demande d'analyse Dashboard")
                    try:
                        await generate_dashboard_insights(smart_ws._ws, stats)
                    except Exception as e:
                        log.error(f"❌ Erreur analyse dashboard: {e}")
                        # On pourrait renvoyer une erreur au client ici

    except WebSocketDisconnect:
        log.warning("🔌 Client déconnecté")
    
    except Exception as e:
        # C'EST ICI QUE LE CRASH SILENCIEUX EST ATTRAPÉ
        log.error(f"🔥 CRASH CRITIQUE WEBSOCKET: {str(e)}")
        log.error("👇 TRACEBACK COMPLET 👇")
        log.error(traceback.format_exc()) # Affiche tout le détail de l'erreur