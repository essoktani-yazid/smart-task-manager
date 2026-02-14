<div align="center">
  <!-- En-tête créatif -->
  <img src="https://capsule-render.vercel.app/api?type=waving&color=00B4D8&height=150&section=header&text=Smart+Task+Manager&fontSize=50&fontAlignY=35&fontColor=ffffff" width="100%"/>

  <!-- Animation texte ciblant "Ce que fait le projet" et "Pourquoi c'est utile" -->
  <img src="https://readme-typing-svg.herokuapp.com?font=JetBrains+Mono&weight=700&size=20&duration=3000&pause=1000&color=00B4D8&center=true&vCenter=true&width=600&lines=Gestion+Intelligente+de+T%C3%A2ches;Assistant+IA+%26+Commandes+Vocales;Collaboration+d'%C3%89quipe+%26+Temps+R%C3%A9el;Synchronisation+Google+Calendar" alt="Typing SVG" />

  <br/>

  <!-- Badges recommandés par GitHub -->
  <p>
    <img src="https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge" alt="Version">
    <img src="https://img.shields.io/github/license/essoktani-yazid/smart-task-manager?style=for-the-badge" alt="License">
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
    <img src="https://img.shields.io/badge/JavaFX-21-FF6F00?style=for-the-badge" alt="JavaFX">
    <img src="https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python 3.11">
    <img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white" alt="FastAPI">
    <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  </p>
</div>

---

<!-- 1. CE QUE FAIT LE PROJET -->
## À propos du projet

**Smart Task Manager** est une solution applicative distribuée et intelligente conçue pour optimiser la productivité individuelle et la gestion de projet collaborative. Elle permet aux équipes et aux professionnels de piloter leurs flux de travail (tableaux Kanban, dépendances de tâches, suivi temporel), d'interagir avec un **assistant IA vocal et textuel** capable d'exécuter des actions métier directement en langage naturel, et de synchroniser leurs réunions en temps réel avec **Google Calendar** sans friction administrative.

> [!NOTE]
> Pour une vue détaillée des choix de conception et des schémas système, consultez la [documentation d'architecture](docs/ARCHITECTURE.md).

<!-- 2. POURQUOI LE PROJET EST UTILE -->
## Pourquoi utiliser ce projet ?

Smart Task Manager répond aux problématiques de dispersion des outils et de lenteur dans l'organisation quotidienne des équipes en apportant des avantages concrets :

- **Assistant IA Contextuel & Mode Vocal :** Traduction instantanée d'instructions orales ou écrites en requêtes de données sécurisées (`NL-to-SQL`), propulsé par un modèle LLM (Mistral 7B via OpenRouter), streaming WebSocket et synthèse/reconnaissance vocale intégrée (Vosk & Edge-TTS).
- **Collaboration d'Équipe & Gestion Kanban Avancée :** Gestion des rôles au sein des équipes (Owner, Admin, Member), assignation de projets collectifs, suivi visuel par colonnes de statut, gestion des sous-tâches, des dépendances de tâches et du temps de travail effectif (*time tracking*).
- **Synchronisation Google Calendar & Réunions :** Synchronisation bidirectionnelle native avec l'API Google Calendar v3 via OAuth2, génération de liens de réunion (Google Meet / Zoom) et envoi automatisé d'invitations e-mail.
- **Architecture Distribuée & Temps Réel :** Séparation claire entre le client riche JavaFX, le serveur central Java (API REST + WebSocket pour les notifications instantanées) et le microservice d'intelligence artificielle en Python FastAPI.
- **Multi-plateforme & Installateurs Autonomes :** Packaging automatisé via `jpackage` fournissant un installateur Windows (`.exe`) et un paquet Linux (`.deb`).

### Stack Technique

| Couche | Technologies |
| :--- | :--- |
| **Frontend / UI Desktop** | `Java 21`, `JavaFX 21`, `MaterialFX`, `ControlsFX`, `CSS3` |
| **Backend & API** | `Java 21`, `HttpServer (com.sun.net.httpserver)`, `Java-WebSocket`, `JDBC`, `Gson` |
| **Microservice IA & Audio** | `Python 3.11`, `FastAPI`, `Uvicorn`, `WebSockets`, `OpenRouter API (Mistral 7B)`, `Vosk`, `Edge-TTS`, `pydub`, `FFmpeg` |
| **Données & Persistance** | `MySQL 8.0` (Schéma relationnel 16 tables, index d'optimisation, contraintes d'intégrité) |
| **Services & Intégrations** | `Google Calendar API v3` (OAuth2 Jetty), `JavaMail (SMTP)`, `OpenRouter` |
| **DevOps & Packaging** | `Maven Multi-Module`, `Docker`, `jpackage (EXE / DEB)`, `GitHub Actions CI/CD` |

<!-- 3. COMMENT DÉMARRER -->
## Pour commencer

Suivez ces instructions pour déployer et exécuter une instance locale du projet pour le développement et les tests.

### Prérequis

Assurez-vous d'avoir installé les outils suivants sur votre environnement :

- `Java JDK 21+` (ex: Eclipse Temurin ou OpenJDK 21)
- `Apache Maven 3.9+`
- `Python 3.11+`
- `MySQL Server 8.0+`
- `FFmpeg` (requis pour le décodage et l'encodage audio du microservice IA)

### Installation

1. **Cloner le dépôt**
   ```bash
   git clone https://github.com/essoktani-yazid/smart-task-manager.git
   cd smart-task-manager
   ```

2. **Configurer la base de données MySQL**
   Créez la base de données et les tables en exécutant le script SQL fourni :
   ```bash
   mysql -u root -p < stm-server/src/main/resources/schema.sql
   ```
   *(Assurez-vous que les identifiants de connexion correspondent à votre environnement dans `stm-server/src/main/resources/database.properties` et `stm-ai-service/.env`)*.

3. **Lancer le serveur backend Java (`stm-server`)**
   ```bash
   mvn clean compile exec:java -pl stm-server
   ```
   Le serveur API démarre sur le port `8080` (`http://localhost:8080/api`) et le serveur WebSocket de notifications sur le port `8887`.

4. **Lancer le microservice IA (`stm-ai-service`)**
   ```bash
   cd stm-ai-service
   python -m venv venv
   source venv/bin/activate  # Sous Windows : .\venv\Scripts\activate
   pip install -r requirements.txt
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```

5. **Lancer l'application cliente JavaFX (`stm-client`)**
   Dans une nouvelle fenêtre de terminal à la racine du projet :
   ```bash
   mvn javafx:run -pl stm-client
   ```

### Exemple d'utilisation

Une fois le serveur démarré, les services REST et WebSockets sont opérationnels. Voici un exemple de requête HTTP pour créer une tâche via l'API :

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finaliser la présentation du projet",
    "description": "Préparer les diapositives pour la démonstration",
    "priority": "HIGH",
    "status": "TODO",
    "dueDate": "2026-10-30T14:00:00",
    "userId": "d712fa65-c503-43e8-a370-825dd8481d38"
  }'
```

*Pour découvrir l'ensemble des endpoints et paramètres disponibles, consultez la [Référence API complète](docs/API.md).*

### Structure du projet

```text
.
├── stm-common/             # Module partagé : Modèles de données (Task, User, Team, Event...), Adapters
├── stm-server/             # Serveur HTTP REST et WebSocket Java (Handlers, DAOs, JDBC, Notifications)
│   └── src/main/resources/ # Scripts SQL (schema.sql, create_teams_tables.sql)
├── stm-client/             # Application cliente de bureau JavaFX 21 (Vues FXML, Contrôleurs, Services)
│   └── src/main/resources/ # Fichiers FXML, feuilles CSS, icônes et images
├── stm-ai-service/         # Microservice IA Python (FastAPI, WebSockets, RAG / NL-to-SQL, Mode Vocal)
├── docs/                   # Documentation technique détaillée
│   ├── assets/             # Captures d'écran, démonstrations et schémas d'architecture
│   ├── ARCHITECTURE.md     # Conception système, flux de données et modèle relationnel
│   └── API.md              # Documentation exhaustive des routes REST et protocoles WebSocket
├── .github/
│   ├── workflows/          # CI/CD GitHub Actions (Génération automatique des installateurs EXE et DEB)
│   └── prompts/            # Directives et prompts du projet
├── pom.xml                 # POM parent Maven multi-module
├── Procfile                # Configuration de déploiement Cloud (Railway)
├── LICENCE                 # Licence logicielle MIT
└── README.md               # Documentation principale
```

<!-- 4. AIDE & SUPPORT -->
## Aide et Support

Vous avez une question sur l'architecture ou l'implémentation du projet ?
- **Documentation :** Consultez les fichiers techniques présents dans le dossier [`docs/`](docs/).
- **Contact direct :** N'hésitez pas à me contacter directement via LinkedIn ou mon portfolio.

<!-- 5. CONTRIBUTION & AUTEUR -->
## Contribuer

Les contributions et propositions d'améliorations sont les bienvenues !
1. **Forkez** le projet
2. Créez votre branche de fonctionnalité (`git checkout -b feature/NouvelleFonctionnalite`)
3. Commitez vos changements (`git commit -m 'feat: Ajout de NouvelleFonctionnalite'`)
4. Pushez vers la branche (`git push origin feature/NouvelleFonctionnalite`)
5. Ouvrez une **Pull Request**

## Équipe de développement

Projet académique réalisé dans le cadre du **Master SDIA** (Systèmes Distribués et Intelligence Artificielle) à l'**ENSET Mohammedia**, sous la supervision du **Pr. Loubna Aminou**.

Conçu et développé en équipe par :

- **[Yazid ESSOKTANI](https://github.com/essoktani-yazid)** : Architecture distribuée 3-tiers, Microservice IA (NL-to-SQL, WebSocket, Mode vocal Vosk/TTS), Dashboard KPI, CI/CD multi-plateforme (`jpackage` pour Windows EXE & Linux DEB) et déploiement cloud.
- **[Rayane TOKO](https://github.com/RyanTk03)** (`ryntk`) : Module Collaboration & Équipes (création, gestion des membres et attribution des rôles), liaison Équipes-Projets, serveur de notifications temps réel WebSocket (`NotificationWebSocketServer`).
- **[Amine BOUSSAID](https://github.com/aminebsd)** (`aminebsd`) : Module Calendrier interactif & Événements, synchronisation bidirectionnelle Google Calendar API v3 (OAuth2), gestion des visioconférences (Meet/Zoom) et notifications e-mail.
- **[Loubna MAHRACH](https://github.com/loubnamahrach)** (`loubnamahrach`) : Modélisation initiale de la base de données relationnelle, module Authentification (Login & Inscription), interface et gestion du Profil utilisateur (sécurité et changement de mot de passe).

## Auteur

<div align="center">

**Yazid ESSOKTANI**  
*Full-Stack Web & AI Developer*

[![Portfolio](https://img.shields.io/badge/Portfolio-essoktani.tech-00B4D8?style=for-the-badge&logo=google-chrome&logoColor=white)](https://www.essoktani.tech/)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Yazid_ESSOKTANI-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/yazid-essoktani-337549251)
[![GitHub](https://img.shields.io/badge/GitHub-@essoktani--yazid-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/essoktani-yazid)

</div>

---

## Licence

Distribué sous la licence **MIT**. Consultez le fichier [`LICENCE`](LICENCE) pour plus d'informations.
