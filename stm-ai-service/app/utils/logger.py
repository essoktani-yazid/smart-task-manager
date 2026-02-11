class Logger:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

    @staticmethod
    def info(msg):
        print(f"{Logger.CYAN}ℹ️  INFO    | {msg}{Logger.ENDC}")

    @staticmethod
    def success(msg):
        print(f"{Logger.GREEN}✅ SUCCESS | {msg}{Logger.ENDC}")
    
    # --- AJOUTEZ CETTE MÉTHODE ---
    @staticmethod
    def warning(msg):
        print(f"{Logger.WARNING}⚠️ WARNING | {msg}{Logger.ENDC}")
    # -----------------------------

    @staticmethod
    def incoming(msg):
        print(f"{Logger.BLUE}📥 RECEIVE | {msg}{Logger.ENDC}")

    @staticmethod
    def outgoing(msg):
        print(f"{Logger.BLUE}📤 SEND    | {msg}{Logger.ENDC}")

    @staticmethod
    def ai(msg):
        print(f"{Logger.HEADER}🤖 AI      | {msg}{Logger.ENDC}")

    @staticmethod
    def db(msg):
        print(f"{Logger.WARNING}🗄️  DB      | {msg}{Logger.ENDC}")

    @staticmethod
    def error(msg):
        print(f"{Logger.FAIL}❌ ERROR   | {msg}{Logger.ENDC}")

log = Logger()