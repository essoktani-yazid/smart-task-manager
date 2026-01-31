from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
import requests
import json
import datetime
import os
from dotenv import load_dotenv
import uvicorn
import asyncio

# Charger les variables d'environnement (API KEY)
load_dotenv()

app = FastAPI()

# Configuration OpenRouter
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

class TaskInput(BaseModel):
    text: str

def process_task_with_ai(text: str) -> dict:
    """
    Fonction helper pour traiter le texte avec l'IA.
    Retourne un dict avec le résultat ou l'erreur.
    """
    # Vérification de sécurité
    if not OPENROUTER_API_KEY:
        return {
            "title": text,
            "priority": "MEDIUM",
            "due_date": None,
            "error": "API Key manquante. Vérifiez votre fichier .env"
        }

    current_date = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")

    # 1. Construction du Prompt Système + User
    system_prompt = f"""
    Tu es un assistant personnel expert en productivité. Nous sommes le {current_date}.
    Ta tâche est d'extraire des informations structurées depuis une demande utilisateur.
    
    Réponds UNIQUEMENT avec un objet JSON valide suivant cette structure, sans texte autour :
    {{
        "title": "Titre nettoyé de la tâche",
        "priority": "HIGH" | "MEDIUM" | "LOW",
        "due_date": "YYYY-MM-DD HH:MM:SS",  <-- Format strict demandé
        "description": "Courte description ou contexte supplémentaire"
    }}
    """

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": text}
    ]

    # 2. Configuration des Headers
    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://smarttaskmanager.com", 
        "X-Title": "SmartTask AI"
    }

    # 3. Payload
    payload = {
        "model": "mistralai/mistral-7b-instruct",
        "temperature": 0.1,
        "top_p": 0.9,
        "messages": messages,
        "stream": False,
        "response_format": { "type": "json_object" }
    }

    try:
        print(f"📡 Envoi à OpenRouter (Mistral)...")
        response = requests.post(OPENROUTER_URL, headers=headers, json=payload)
        response.raise_for_status()
        
        data = response.json()
        ai_content = data['choices'][0]['message']['content']
        task_data = json.loads(ai_content)
        
        return task_data

    except json.JSONDecodeError:
        print(f"❌ Erreur: L'IA n'a pas renvoyé du JSON valide.\nReçu: {ai_content}")
        return {
            "title": text,
            "priority": "MEDIUM",
            "due_date": None,
            "error": "Invalid JSON format from AI"
        }
    except Exception as e:
        print(f"❌ Erreur API: {str(e)}")
        return {
            "title": text,
            "priority": "MEDIUM",
            "due_date": None,
            "error": str(e)
        }

@app.post("/parse")
def parse_task_mistral(input: TaskInput):
    result = process_task_with_ai(input.text)
    if "error" in result and "API Key manquante" in result.get("error", ""):
         raise HTTPException(status_code=500, detail=result["error"])
    return result

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            # Attendre un message texte du client
            data = await websocket.receive_text()
            print(f"📩 Reçu via WebSocket: {data}")
            
            # Traiter (ici on fait un appel synchrone, attention bloquant)
            # Pour être propre en async, on devrait exécuter process_task_with_ai dans un threadpool
            # Mais pour simplifier ici, on l'appelle directement ou via to_thread
            
            response_data = await asyncio.to_thread(process_task_with_ai, data)
            
            # Renvoyer la réponse JSON
            await websocket.send_json(response_data)
            print(f"📤 Réponse envoyée via WebSocket")
            
    except WebSocketDisconnect:
        print("🔌 Client déconnecté")

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=5000)
