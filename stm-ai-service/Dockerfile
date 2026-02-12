FROM python:3.11-slim

WORKDIR /app

# INSTALLATION DE FFMPEG (CRITIQUE pour l'audio)
RUN apt-get update && apt-get install -y ffmpeg gcc && rm -rf /var/lib/apt/lists/*

COPY stm-ai-service/ .
RUN pip install --no-cache-dir -r requirements.txt

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]