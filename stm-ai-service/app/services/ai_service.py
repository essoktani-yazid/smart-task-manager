import httpx
import json
from app.core.config import settings

async def call_llm(prompt, system_content, is_json=True):
    headers = {
        "Authorization": f"Bearer {settings.OPENROUTER_API_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "model": "mistralai/mistral-7b-instruct",
        "messages": [
            {"role": "system", "content": system_content},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.1
    }
    
    if is_json:
        payload["response_format"] = {"type": "json_object"}

    async with httpx.AsyncClient(timeout=60.0) as client:
        try:
            response = await client.post(settings.OPENROUTER_URL, headers=headers, json=payload)
            response.raise_for_status()
            res_json = response.json()
            
            if 'choices' not in res_json:
                print(f"Error: Unexpected API response: {res_json}", flush=True)
                raise ValueError(f"API Error: {res_json.get('error', 'Unknown error')}")
                
            return res_json['choices'][0]['message']['content']
        except httpx.HTTPStatusError as e:
            print(f"HTTP Error: {e.response.text}", flush=True)
            raise
        except Exception as e:
            print(f"Request Error: {str(e)}", flush=True)
            raise