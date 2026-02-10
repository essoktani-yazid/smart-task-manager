FROM python:3.11-slim

WORKDIR /app

# On copie tout le dossier du service IA dans le conteneur
COPY stm-ai-service/ . 

# Maintenant on est dans /app qui contient requirements.txt et le dossier app/
RUN pip install --no-cache-dir -r requirements.txt

# Commande de démarrage
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]