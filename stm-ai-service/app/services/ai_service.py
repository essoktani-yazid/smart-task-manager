import httpx
import json
from app.core.config import settings
from app.utils.logger import log

async def stream_llm_response(prompt, system_content="You are a helpful assistant.", return_as_string=False):
    """
    Générateur qui connecte le LLM (OpenRouter) et renvoie les morceaux de texte (chunks)
    dès qu'ils arrivent, sans attendre la fin.
    
    Args:
        prompt: La question utilisateur
        system_content: Le prompt système
        return_as_string: Si True, retourne la réponse complète en string, sinon en générateur
    
    Returns:
        Generator[str] ou str selon return_as_string
    """
    headers = {
        "Authorization": f"Bearer {settings.OPENROUTER_API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://smarttask.app",
    }
    
    payload = {
        "model": "mistralai/mistral-7b-instruct",
        "messages": [
            {"role": "system", "content": system_content},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.7,
        "stream": True,  # <--- CRUCIAL : Active le mode Streaming SSE
        # ⚡ OPTIMISATIONS PERFORMANCE
        "max_tokens": 1500,  # Limite pour éviter réponses trop longues
        "top_p": 0.9,  # Réduit l'échantillonnage pour plus de vitesse
    }

    async def _stream_chunks():
        """Générateur interne pour les chunks"""
        try:
            # ⚡ OPTIMISÉ: Timeout réduit 60s -> 30s, HTTP/2 activé
            async with httpx.AsyncClient(timeout=30.0, http2=True) as client:
                async with client.stream("POST", settings.OPENROUTER_URL, headers=headers, json=payload) as response:
                    async for line in response.aiter_lines():
                        if line.startswith("data: "):
                            line_content = line[6:]  # Enlever "data: "
                            if line_content.strip() == "[DONE]":
                                break
                            try:
                                chunk = json.loads(line_content)
                                # Extraction du contenu selon le format standard OpenAI/OpenRouter
                                if "choices" in chunk and len(chunk["choices"]) > 0:
                                    delta = chunk["choices"][0].get("delta", {})
                                    if "content" in delta:
                                        yield delta["content"]
                            except json.JSONDecodeError:
                                continue
        except Exception as e:
            log.error(f"Erreur LLM Stream: {e}")
            yield ""  # Yield vide en cas d'erreur pour éviter le crash
    
    # Si on veut une string complète (pour JSON parsing par exemple)
    if return_as_string:
        full_text = ""
        async for chunk in _stream_chunks():
            full_text += chunk
        return full_text
    else:
        # Sinon, on retourne le générateur
        return _stream_chunks()