import mysql.connector
from app.core.config import settings
from app.utils.logger import log

class Database:
    def __init__(self):
        self.config = {
            'host': settings.DB_HOST,
            'user': settings.DB_USER,
            'password': settings.DB_PASSWORD,
            'database': settings.DB_NAME
        }
        self.connection = None

    def connect(self):
        try:
            self.connection = mysql.connector.connect(**self.config)
            log.success(f"Connecté à MySQL : {settings.DB_NAME}")
        except Exception as e:
            log.error(f"Erreur connexion MySQL : {e}")

    def execute(self, query):
        if not self.connection or not self.connection.is_connected():
            log.info("Reconnexion à la DB...")
            self.connect()
        
        log.db(f"Exécution SQL : {query}")
        cursor = self.connection.cursor(dictionary=True)
        try:
            cursor.execute(query)
            if query.strip().upper().startswith(("INSERT", "UPDATE", "DELETE")):
                self.connection.commit()
                log.db("Commit effectué")
            return cursor
        except Exception as e:
            log.error(f"Erreur SQL : {e}")
            raise e

db = Database()