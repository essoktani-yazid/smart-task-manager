import asyncio
import base64
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

class VoiceService:
    # Voix disponibles par langue
    VOICES = {
        "en": "en-US-AriaNeural",      # Anglais (féminin naturel)
        "en-male": "en-US-GuyNeural",  # Anglais (masculin)
        "fr": "fr-FR-DeniseNeural",    # Français (féminin)
        "fr-male": "fr-FR-HenriNeural", # Français (masculin)
        "es": "es-ES-ElviraNeural",    # Espagnol
        "de": "de-DE-KatjaNeural",     # Allemand
        "ar": "ar-SA-ZariyahNeural",   # Arabe
    }
    
    def __init__(self, default_voice="en"):
        self.sentence_pattern = re.compile(r'[.!?]+')
        self.current_voice = self.VOICES.get(default_voice, "en-US-AriaNeural")
        
    def set_voice(self, voice_key):
        """Change la voix TTS (langue)"""
        if voice_key in self.VOICES:
            self.current_voice = self.VOICES[voice_key]
            log.info(f"Voix TTS changée: {self.current_voice}")
        else:
            log.warning(f"Voix '{voice_key}' inconnue, utilisation par défaut")
    
    async def text_to_speech_stream(self, text_iterator, websocket, voice_override=None):
        """
        Prend un générateur de texte (LLM), détecte les phrases dynamiquement,
        et envoie l'audio PCM au client Java au fur et à mesure.
        
        Améliorations:
        - Détection intelligente de phrases (virgules, points-virgules)
        - Support des émojis
        - Gestion des erreurs robuste
        """
        buffer = ""
        sentence_count = 0
        
        try:
            # 1. On lit le flux du LLM mot par mot
            async for chunk in text_iterator:
                buffer += chunk
                
                # Détection avancée de fin de phrase (streaming progressif pour réduire la latence)
                # On cherche: . ? ! mais aussi parfois virgule après long texte
                if self._should_process_sentence(buffer):
                    # Extraire la phrase complète
                    sentence = self._extract_sentence(buffer)
                    if sentence:
                        sentence_count += 1
                        log.ai(f"📢 TTS Phrase {sentence_count}: {sentence[:50]}...")
                        # Lancer le TTS en parallèle sans bloquer la réception (améliore latence)
                        asyncio.create_task(self._process_sentence(websocket, sentence))
                        buffer = buffer[len(sentence):].lstrip()

            # Traiter le reste (s'il n'y a pas de point à la fin)
            if buffer.strip():
                sentence_count += 1
                log.ai(f"📢 TTS Phrase finale {sentence_count}: {buffer[:50]}...")
                await self._process_sentence(websocket, buffer)
                
            log.success(f"✅ TTS terminé - {sentence_count} phrases générées")
            
        except Exception as e:
            log.error(f"Erreur TTS Streaming: {e}")
        finally:
            # Signal de fin (toujours envoyé)
            await websocket.send_json({"type": "AUDIO_END"})
    
    def _should_process_sentence(self, text):
        """Détermine si on doit traiter le buffer comme une phrase"""
        # Fin de phrase classique
        if any(punct in text for punct in [". ", "? ", "! ", ".\n", "?\n", "!\n"]):
            return True
        # Phrase longue avec virgule (pour éviter trop d'attente)
        if len(text) > 120 and ", " in text:
            return True
        return False
    
    def _extract_sentence(self, text):
        """Extrait la première phrase complète du buffer"""
        # Chercher le premier délimiteur de phrase
        for delimiter in [". ", "? ", "! ", ".\n", "?\n", "!\n", ", "]:
            if delimiter in text:
                idx = text.index(delimiter)
                return text[:idx + len(delimiter)]
        return None

    async def _process_sentence(self, websocket, text, voice_override=None):
        """
        Convertit une phrase en audio et l'envoie au client.
        Utilise Edge TTS pour une synthèse vocale de qualité.
        Avec cache pour réduire la latence des phrases fréquentes.
        """
        try:
            # Nettoyage du texte (retirer les émojis qui peuvent poser problème)
            clean_text = self._clean_text_for_tts(text)
            if not clean_text.strip():
                return
            
            # Détection automatique de la langue (optionnel)
            selected_voice = voice_override or self.current_voice
            detected_lang = self._detect_language(clean_text)
            if detected_lang and detected_lang in self.VOICES:
                selected_voice = self.VOICES[detected_lang]
            
            # ⚡ CACHE SUPPRIMÉ (comme demandé par l'utilisateur)
            # 1. Génération TTS avec Edge
            communicate = edge_tts.Communicate(clean_text, selected_voice)
            mp3_data = b""
            
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    mp3_data += chunk["data"]

            if not mp3_data:
                log.warning("Aucune donnée audio générée pour: " + clean_text[:30])
                return

            # 2. ⚡ Conversion MP3 -> PCM RAW (Pour Java) en thread séparé
            pcm_data = await asyncio.to_thread(self._convert_to_pcm, mp3_data)

            # 3. Envoi au client en Base64
            if pcm_data:
                payload = base64.b64encode(pcm_data).decode('utf-8')
                await websocket.send_json({
                    "type": "AUDIO_CHUNK",
                    "data": payload
                })
        except Exception as e:
            log.error(f"Erreur _process_sentence: {e}")

    def _clean_text_for_tts(self, text):
        """Nettoie le texte pour TTS (retire emojis, caractères spéciaux)"""
        # Retirer les emojis communs
        emoji_pattern = re.compile("["
            u"\U0001F600-\U0001F64F"  # emoticons
            u"\U0001F300-\U0001F5FF"  # symboles & pictographes
            u"\U0001F680-\U0001F6FF"  # transport & symboles
            u"\U0001F1E0-\U0001F1FF"  # drapeaux
            u"\U00002702-\U000027B0"
            u"\U000024C2-\U0001F251"
            "]+", flags=re.UNICODE)
        
        cleaned = emoji_pattern.sub('', text)
        # Retirer les balises markdown simples
        cleaned = cleaned.replace('**', '').replace('`', '')
        return cleaned.strip()

    def _detect_language(self, text):
        """
        Détection simple de la langue basée sur des mots-clés.
        Pour une vraie app, utiliser langdetect ou une lib IA.
        """
        text_lower = text.lower()
        
        # Mots français communs
        french_words = ["bonjour", "merci", "tâche", "projet", "créer", "supprimer", "aujourd'hui"]
        if any(word in text_lower for word in french_words):
            return "fr"
        
        # Mots arabes (détection unicode)
        if any('\u0600' <= char <= '\u06FF' for char in text):
            return "ar"
        
        # Par défaut: anglais
        return "en"
    
    def _convert_to_pcm(self, mp3_bytes):
        """Convertit MP3 en PCM brut pour lecture Java"""
        try:
            audio = AudioSegment.from_file(io.BytesIO(mp3_bytes), format="mp3")
            audio = audio.set_frame_rate(JAVA_AUDIO_FORMAT["frame_rate"])
            audio = audio.set_channels(JAVA_AUDIO_FORMAT["channels"])
            audio = audio.set_sample_width(JAVA_AUDIO_FORMAT["sample_width"])
            return audio.raw_data
        except Exception as e:
            log.error(f"Erreur conversion MP3->PCM: {e}")
            return b""