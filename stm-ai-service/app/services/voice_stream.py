import asyncio
import base64
import edge_tts
from pydub import AudioSegment
import io

# Format Audio accepté par Java (javax.sound.sampled)
# PCM Signed 16-bit, 24kHz, Mono, Little Endian
JAVA_AUDIO_FORMAT = {
    "frame_rate": 24000,
    "channels": 1,
    "sample_width": 2
}

class VoiceService:
    async def text_to_speech_stream(self, text_iterator, websocket):
        """
        Prend un générateur de texte (LLM), détecte les phrases,
        et envoie l'audio PCM au client Java au fur et à mesure.
        """
        buffer = ""
        
        # 1. On lit le flux du LLM mot par mot
        async for chunk in text_iterator:
            buffer += chunk
            
            # Détection simple de fin de phrase (. ? !)
            if any(punct in buffer for punct in [".", "?", "!"]):
                # On a une phrase ! On traite.
                await self._process_sentence(websocket, buffer)
                buffer = "" # Reset buffer

        # Traiter le reste (s'il n'y a pas de point à la fin)
        if buffer.strip():
            await self._process_sentence(websocket, buffer)
            
        # Signal de fin
        await websocket.send_json({"type": "AUDIO_END"})

    async def _process_sentence(self, websocket, text):
        # 2. Génération Audio (Edge-TTS - Rapide & Gratuit)
        communicate = edge_tts.Communicate(text, "fr-FR-VivienneNeural")
        mp3_data = b""
        
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                mp3_data += chunk["data"]

        if not mp3_data: return

        # 3. Conversion MP3 -> PCM RAW (Pour Java)
        # C'est l'étape clé pour la rapidité côté client
        pcm_data = await asyncio.to_thread(self._convert_to_pcm, mp3_data)

        # 4. Envoi au client
        payload = base64.b64encode(pcm_data).decode('utf-8')
        await websocket.send_json({
            "type": "AUDIO_CHUNK",
            "data": payload
        })

    def _convert_to_pcm(self, mp3_bytes):
        try:
            audio = AudioSegment.from_file(io.BytesIO(mp3_bytes), format="mp3")
            audio = audio.set_frame_rate(JAVA_AUDIO_FORMAT["frame_rate"])
            audio = audio.set_channels(JAVA_AUDIO_FORMAT["channels"])
            audio = audio.set_sample_width(JAVA_AUDIO_FORMAT["sample_width"])
            return audio.raw_data
        except Exception as e:
            print(f"Erreur conversion: {e}")
            return b""