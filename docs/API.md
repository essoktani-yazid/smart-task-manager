# Référence API — Smart Task Manager

> Documentation exhaustive des endpoints HTTP REST et des flux temps réel WebSockets exposés par le serveur central (`stm-server`) et le microservice d'intelligence artificielle (`stm-ai-service`).

- **URL de base (Développement local) :** `http://localhost:8080/api`
- **URL de base (Environnement hébergé) :** `http://52.224.243.167:8080/api`
- **Serveur WebSocket Notifications :** `ws://localhost:8887` *(ou `ws://52.224.243.167:8887`)*
- **Microservice IA (WebSocket Streaming) :** `ws://localhost:8000/ai/stream` *(ou `ws://52.224.243.167:8000/ai/stream`)*
- **Format d'échange :** `application/json; charset=utf-8`

---

## 1. Authentification & Sécurité (`/api/auth`)

### Inscription d'un nouvel utilisateur
- **URL :** `/auth/register`
- **Méthode :** `POST`

#### Corps de la requête
```json
{
  "username": "yazid_dev",
  "email": "yazid@example.com",
  "password": "MotDePasseSecurise123",
  "firstName": "Yazid",
  "lastName": "Essoktani"
}
```

#### Réponse de succès (`201 Created`)
```json
{
  "id": "d712fa65-c503-43e8-a370-825dd8481d38",
  "username": "yazid_dev",
  "email": "yazid@example.com",
  "firstName": "Yazid",
  "lastName": "Essoktani",
  "isActive": true
}
```

---

### Connexion utilisateur
- **URL :** `/auth/login`
- **Méthode :** `POST`

#### Corps de la requête
```json
{
  "username": "yazid_dev",
  "password": "MotDePasseSecurise123"
}
```

#### Réponse de succès (`200 OK`)
```json
{
  "id": "d712fa65-c503-43e8-a370-825dd8481d38",
  "username": "yazid_dev",
  "email": "yazid@example.com",
  "firstName": "Yazid",
  "lastName": "Essoktani",
  "googleCalendarEnabled": true
}
```

---

### Vérification de mot de passe (Opérations sensibles)
- **URL :** `/auth/verify-password`
- **Méthode :** `POST`

#### Corps de la requête
```json
{
  "userId": "d712fa65-c503-43e8-a370-825dd8481d38",
  "password": "MotDePasseSecurise123"
}
```

#### Réponse de succès (`200 OK`)
```json
{
  "valid": true
}
```

---

## 2. Gestion des Tâches (`/api/tasks`)

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/tasks` | Récupère la liste de toutes les tâches |
| `GET` | `/tasks/{id}` | Récupère le détail d'une tâche par son UUID |
| `GET` | `/tasks/{userId}/user` | Récupère toutes les tâches d'un utilisateur |
| `GET` | `/tasks/sub/{parentId}` | Récupère toutes les sous-tâches d'une tâche parente |
| `GET` | `/tasks/project/{projectId}` | Récupère les tâches associées à un projet |
| `POST` | `/tasks` | Crée une nouvelle tâche (ou sous-tâche avec `parentTaskId`) |
| `PUT` | `/tasks/{id}` | Met à jour une tâche existante |
| `DELETE` | `/tasks/{id}` | Supprime une tâche et ses sous-tâches en cascade |

### Création d'une tâche (`POST /tasks`)

#### Corps de la requête
```json
{
  "title": "Concevoir le pipeline CI/CD",
  "description": "Automatiser la génération des installeurs EXE et DEB via GitHub Actions",
  "priority": "HIGH",
  "status": "IN_PROGRESS",
  "dueDate": "2026-10-31T17:00:00",
  "userId": "d712fa65-c503-43e8-a370-825dd8481d38",
  "projectId": "8b5d3210-90fe-4e56-9122-38bfa7991201",
  "recurrenceType": "NONE"
}
```

#### Réponse de succès (`201 Created`)
```json
{
  "id": "c194a8f9-715a-4efb-861e-128f731420b9",
  "title": "Concevoir le pipeline CI/CD",
  "priority": "HIGH",
  "status": "IN_PROGRESS",
  "createdAt": "2026-10-25T15:30:00",
  "userId": "d712fa65-c503-43e8-a370-825dd8481d38"
}
```

---

## 3. Gestion des Projets (`/api/projects`)

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/projects` | Récupère la totalité des projets |
| `GET` | `/projects/{id}` | Récupère les détails d'un projet |
| `GET` | `/projects/{userId}/user` | Récupère les projets créés par un utilisateur |
| `GET` | `/projects/{projectId}/teams` | Récupère les équipes affectées au projet |
| `POST` | `/projects` | Crée un nouveau projet |
| `POST` | `/projects/{projectId}/teams/{teamId}` | Associe une équipe à un projet |
| `PUT` | `/projects/{id}` | Met à jour le nom, la description ou la couleur du projet |
| `DELETE` | `/projects/{id}` | Supprime un projet |
| `DELETE` | `/projects/{projectId}/teams/{teamId}` | Dissocie une équipe d'un projet |

---

## 4. Gestion des Équipes & Membres (`/api/teams`)

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/teams?userId={userId}` | Liste les équipes dont l'utilisateur est membre |
| `GET` | `/teams/{id}` | Récupère les informations d'une équipe |
| `POST` | `/teams` | Crée une nouvelle équipe |
| `PUT` | `/teams/{id}` | Met à jour une équipe |
| `DELETE` | `/teams/{id}` | Supprime une équipe |
| `GET` | `/teams/{id}/members` | Liste les membres de l'équipe avec leurs rôles |
| `POST` | `/teams/{id}/members` | Ajoute un collaborateur dans l'équipe |
| `DELETE` | `/teams/{id}/members?userId={userId}`| Retire un collaborateur de l'équipe |

#### Exemple d'ajout d'un membre (`POST /teams/{id}/members`)
```json
{
  "userId": "b96bb367-508a-4bd2-8f77-51f3c842c799",
  "role": "MEMBER"
}
```

---

## 5. Calendrier & Événements (`/api/events`)

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/events` | Liste tous les événements de calendrier |
| `GET` | `/events/{id}` | Détails d'un événement par son ID |
| `GET` | `/events/user/{userId}` | Événements visibles par l'utilisateur |
| `GET` | `/events/creator/{userId}` | Événements créés par l'utilisateur |
| `GET` | `/events/shared/{userId}` | Événements partagés avec l'utilisateur |
| `GET` | `/events/date/{YYYY-MM-DD}` | Événements programmés pour une date précise |
| `GET` | `/events/range?start={}&end={}` | Événements compris dans une plage temporelle |
| `POST` | `/events` | Planifie un nouvel événement |
| `PUT` | `/events/{id}` | Met à jour les détails ou l'horaire d'un événement |
| `DELETE` | `/events/{id}` | Supprime un événement |

#### Exemple de création d'événement (`POST /events`)
```json
{
  "title": "Réunion de cadrage Sprint 4",
  "description": "Revue des US et validation du Kanban",
  "eventDate": "2026-10-28",
  "startTime": "14:00:00",
  "endTime": "15:30:00",
  "priority": "HIGH",
  "creatorUserId": "d712fa65-c503-43e8-a370-825dd8481d38",
  "hasMeetingLink": true,
  "meetingLink": "https://meet.google.com/abc-defg-hij",
  "meetingPlatform": "GOOGLE_MEET",
  "visibility": "PUBLIC"
}
```

---

## 6. Dépendances, Commentaires, Tags & Suivi de Temps

### Dépendances entre tâches (`/api/dependencies`)
- `GET /dependencies/successor/{taskId}` : Tâches prérequises avant de démarrer `taskId`.
- `GET /dependencies/predecessor/{taskId}` : Tâches bloquées par `taskId`.
- `POST /dependencies` : Créer une dépendance (`predecessorId`, `successorId`, `dependencyType`).
- `DELETE /dependencies/{id}` : Supprimer une dépendance.

### Commentaires & Fichiers joints (`/api/comments` & `/api/attachments`)
- `GET /comments/task/{taskId}` : Fil de discussion rattaché à une tâche.
- `POST /comments` : Ajout d'un commentaire (`taskId`, `userId`, `content`).
- `GET /attachments/task/{taskId}` : Liste des documents joints à une tâche.
- `POST /attachments` : Téléversement des métadonnées d'une pièce jointe.

### Suivi temporel (`/api/timetracking`)
- `GET /timetracking/task/{taskId}` : Journal des heures passées sur la tâche.
- `GET /timetracking/{userId}/user` : Historique des sessions de l'utilisateur.
- `POST /timetracking` : Début d'une session de chronométrage (`taskId`, `userId`, `startTime`).
- `PUT /timetracking` : Clôture de la session (`endTime`, `durationMs`, `notes`).

---

## 7. Système de Notifications Temps Réel

### REST API (`/api/notifications`)
- `GET /notifications/user/{userId}` : Historique des notifications d'un utilisateur.
- `GET /notifications/user/{userId}/unread` : Notifications non lues.
- `PUT /notifications/{id}/read` : Marquer une notification comme lue.
- `PUT /notifications/read-all` : Tout marquer comme lu (`{"userId": "..."}`).
- `DELETE /notifications/{id}` : Supprimer une notification.

### WebSocket Notifications (`ws://localhost:8887`)
1. **Handshake d'authentification :** Dès l'ouverture du socket, le client envoie :
   ```text
   AUTH:d712fa65-c503-43e8-a370-825dd8481d38
   ```
2. **Réception d'événements :** Le serveur pousse automatiquement les notifications au format JSON :
   ```json
   {
     "type": "NOTIFICATION",
     "payload": {
       "id": "notif_98765",
       "userId": "d712fa65-c503-43e8-a370-825dd8481d38",
       "type": "TASK_ASSIGNED",
       "title": "Nouvelle tâche assignée",
       "message": "Vous avez été assigné à la tâche 'Finaliser la CI/CD'",
       "isRead": false,
       "createdAt": "2026-10-25T15:32:00"
     }
   }
   ```

---

## 8. Microservice IA (`ws://localhost:8000/ai/stream`)

Le service IA écoute sur une connexion WebSocket bidirectionnelle et orchestre les interactions textuelles et vocales :

### Requête en langage naturel
```json
{
  "userId": "d712fa65-c503-43e8-a370-825dd8481d38",
  "prompt": "Quelles sont mes tâches urgentes prévues pour aujourd'hui ?"
}
```

### Confirmation d'opération en écriture (NL-to-SQL)
Lorsque l'intention requiert une modification de la base (INSERT / UPDATE / DELETE), le service retourne un objet de confirmation :
```json
{
  "requires_confirmation": true,
  "sql": "INSERT INTO tasks (title, priority, status, due_date, user_id) VALUES ('Corriger le bug de login', 'URGENT', 'TODO', NOW(), 'd712fa65-c503-43e8-a370-825dd8481d38')",
  "display_message": "Je m'apprête à créer la tâche urgente **Corriger le bug de login** assignée pour aujourd'hui. Confirmez-vous cette action ?"
}
```
Le client confirme en renvoyant :
```json
{
  "action": "CONFIRM",
  "sql": "INSERT INTO tasks (title, priority, status, due_date, user_id) VALUES (...)"
}
```

### Analyse prédictive du tableau de bord
```json
{
  "action": "ANALYZE_DASHBOARD",
  "stats": {
    "totalTasks": 24,
    "completedTasks": 18,
    "blockedTasks": 2,
    "urgentPending": 4
  }
}
```

---

## 9. Codes de statut et gestion des erreurs

| Code HTTP | Libellé | Cause typique |
| :--- | :--- | :--- |
| `200 OK` | Succès | Requête traitée avec succès. |
| `201 Created` | Ressource créée | Nouvelle entité créée (Tâche, Projet, Équipe, Compte). |
| `400 Bad Request` | Requête invalide | Identifiant manquant, corps JSON invalide ou nom d'utilisateur déjà pris. |
| `401 Unauthorized` | Non authentifié | Mot de passe incorrect ou session non valide. |
| `404 Not Found` | Introuvable | Ressource inexistante ou route URL erronée. |
| `405 Method Not Allowed` | Méthode non permise | Méthode HTTP non supportée sur cet endpoint. |
| `500 Internal Error` | Erreur serveur | Exception SQL, erreur de connexion MySQL ou crash inattendu. |

---
[⬅ Retour au README principal](../README.md)
