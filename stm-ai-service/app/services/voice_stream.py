import asyncio
import edge_tts
from pydub import AudioSegment
import io
import re
from app.utils.logger import log

# Format Audio accepté par Java (javax.sound.sampled)
# PCM Signed 16-bit, 24kHz, Mono, Little Endian
JAVA_AUDIO_FORMAT = {
    "frame_rate": 24000,
    "channels": 1,
    "sample_width": 2
}

# Taille max par message WebSocket binaire
CHUNK_SIZE_PCM = 32 * 1024 

class VoiceService:
    # ⚡ Voix unique forcée : Christopher (US English)
    VOICE = "en-US-ChristopherNeural"
    
    def __init__(self):
        self.sentence_pattern = re.compile(r'[.!?]+')
        self.current_voice = self.VOICE
        log.info(f"🎤 VoiceService initialisé sur : {self.current_voice}")
        
    def set_voice(self, voice_key):
        """Ignore les demandes de changement de voix."""
        log.warning(f"Tentative de changement de voix vers '{voice_key}' ignorée. Verrouillé sur Christopher.")
    
    async def text_to_speech_stream(self, text_iterator, websocket, voice_override=None):
        """Génère l'audio et l'envoie en BINAIRE au client."""
        buffer = ""
        sentence_count = 0
        
        try:
            # 1. Lecture du flux LLM
            async for chunk in text_iterator:
                buffer += chunk
                
                if self._should_process_sentence(buffer):
                    sentence = self._extract_sentence(buffer)
                    if sentence:
                        sentence_count += 1
                        log.ai(f"📢 TTS Phrase {sentence_count}: {sentence[:50]}...")
                        # On attend l'envoi complet avant de passer à la suite
                        await self._process_sentence(websocket, sentence)
                        buffer = buffer[len(sentence):].lstrip()

            # 2. Traitement du reste du buffer
            if buffer.strip():
                sentence_count += 1
                log.ai(f"📢 TTS Phrase finale {sentence_count}: {buffer[:50]}...")
                await self._process_sentence(websocket, buffer)
                
            log.success(f"✅ TTS terminé - {sentence_count} phrases envoyées")
            
        except Exception as e:
            log.error(f"Erreur TTS Streaming: {e}")
        finally:
            # 3. Signal de fin
            log.info("📤 [FLOW] Envoi AUDIO_END")
            await websocket.send_json({"type": "AUDIO_END"})
    
    def _should_process_sentence(self, text):
        if any(punct in text for punct in [". ", "? ", "! ", ".\n", "?\n", "!\n"]):
            return True
        if len(text) > 120 and ", " in text:
            return True
        return False
    
    def _extract_sentence(self, text):
        for delimiter in [". ", "? ", "! ", ".\n", "?\n", "!\n", ", "]:
            if delimiter in text:
                idx = text.index(delimiter)
                return text[:idx + len(delimiter)]
        return None

    async def _process_sentence(self, websocket, text, voice_override=None):
        """Convertit texte -> MP3 -> PCM et envoie en BINAIRE"""
        try:
            clean_text = self._clean_text_for_tts(text)
            if not clean_text.strip():
                return
            
            # Utilisation directe de la voix constante
            communicate = edge_tts.Communicate(clean_text, self.VOICE)
            mp3_data = b""
            
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    mp3_data += chunk["data"]

            if not mp3_data:
                return

            # Conversion MP3 -> PCM (Thread séparé)
            pcm_data = await asyncio.to_thread(self._convert_to_pcm, mp3_data)

            # Envoi BINAIRE
            if pcm_data:
                offset = 0
                chunk_count = 0
                while offset < len(pcm_data):
                    chunk = pcm_data[offset:offset + CHUNK_SIZE_PCM]
                    await websocket.send_bytes(chunk)
                    offset += len(chunk)
                    chunk_count += 1
                log.info(f"📤 [FLOW] AUDIO binaire envoyé: {chunk_count} paquets")
                    
        except Exception as e:
            log.error(f"Erreur _process_sentence: {e}")

    def _clean_text_for_tts(self, text):
        emoji_pattern = re.compile("["
            u"\U0001F600-\U0001F64F"
            u"\U0001F300-\U0001F5FF"
            u"\U0001F680-\U0001F6FF"
            u"\U0001F1E0-\U0001F1FF"
            u"\U00002702-\U000027B0"
            u"\U000024C2-\U0001F251"
            "]+", flags=re.UNICODE)
        
        cleaned = emoji_pattern.sub('', text)
        cleaned = cleaned.replace('**', '').replace('`', '')
        return cleaned.strip()
    
    def _convert_to_pcm(self, mp3_bytes):
        try:
            audio = AudioSegment.from_file(io.BytesIO(mp3_bytes), format="mp3")
            audio = audio.set_frame_rate(JAVA_AUDIO_FORMAT["frame_rate"])
            audio = audio.set_channels(JAVA_AUDIO_FORMAT["channels"])
            audio = audio.set_sample_width(JAVA_AUDIO_FORMAT["sample_width"])
            return audio.raw_data
        except Exception as e:
            log.error(f"Erreur conversion MP3->PCM: {e}")
            return b""