### **Table : clients**

|**Champ**|**Type SQL**|**Description**|
|---|---|---|
|id|UUID|Clé primaire|
|name|VARCHAR(255)|Nom du client (entreprise, équipe, etc.)|
|type|ENUM('INDIVIDUAL', 'COMPANY')|Type de client|
|status|ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING')|Statut du compte|
|created_at|TIMESTAMP|Date de création|
|updated_at|TIMESTAMP|Dernière mise à jour|

### **Table : users**

|**Champ**|**Type SQL**|**Description**|
|---|---|---|
|id|UUID|PK|
|client_id|UUID|FK → clients.id|
|email|VARCHAR(255)|Unique|
|password_hash|TEXT|Hash sécurisé|
|first_name|VARCHAR(100)||
|last_name|VARCHAR(100)||
|role|ENUM('OWNER', 'ADMIN', 'MEMBER', 'SERVICE')|Rôle dans le client|
|status|ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING')|État du compte|
|last_login_at|TIMESTAMP||
|created_at|TIMESTAMP||
|updated_at|TIMESTAMP||

### **Table : user_preferences**

|**Champ**|**Type SQL**|**Description**|
|---|---|---|
|id|UUID|PK|
|user_id|UUID|FK → users.id|
|theme|ENUM('LIGHT', 'DARK', 'SYSTEM')|Thème préféré|
|language|VARCHAR(10)|Langue (fr, en, etc.)|
|notifications_enabled|BOOLEAN|Notifications activées|
|created_at|TIMESTAMP||
|updated_at|TIMESTAMP||

### **Table : user_sessions**

|**Champ**|**Type SQL**|**Description**|
|---|---|---|
|id|UUID|PK|
|user_id|UUID|FK → users.id|
|token_id|UUID|Identifiant du token (JWT / refresh)|
|ip_address|VARCHAR(45)|IPv4 / IPv6|
|user_agent|TEXT|Navigateur / device|
|created_at|TIMESTAMP|Date de création|
|expires_at|TIMESTAMP|Date d’expiration|

### **Relations**

```
clients (1)───(∞) users (1)───(1) user_preferences
                          │
                          └───(∞) user_sessions
```