# spring-cloud-gateway

Ce microservice est la **Passerelle API (API Gateway)** du système. Il constitue l'unique point d'entrée pour l'application cliente Flutter (`client_ui`) et se charge de rediriger les requêtes vers les microservices appropriés tout en assurant la sécurité globale de l'écosystème.

## ⚙️ Rôle et Fonctionnalités

- **Routage Unique** : Expose un seul point d'accès pour le client (Port `8765`), évitant ainsi d'avoir à exposer individuellement chaque microservice.
- **Routage Dynamique** : Utilise le registre d'adresses d'Eureka pour rediriger les requêtes vers les microservices actifs via du Load Balancing (`lb://`).
- **Filtrage de Sécurité (JWT)** : Intercepte les requêtes pour vérifier la validité du jeton JWT présent dans l'en-tête `Authorization: Bearer <token>` via un filtre personnalisé (`AuthenticationFilter`).
- **Propagation du Contexte Utilisateur** : Extrait l'identifiant utilisateur, le numéro de téléphone et le rôle du token JWT validé et les injecte sous forme d'en-têtes HTTP pour les services aval :
  - `X-User-Id`
  - `X-User-Phone`
  - `X-User-Role`

---

## 🔌 Configuration du Service

- **Port par défaut** : `8765`
- **Technologie** : Spring Cloud Gateway, Reactive Web (WebFlux), JWT (JSON Web Tokens)
- **Dépendance Eureka** : Découverte dynamique activée

### Règles de Routage (extraites des configurations) :

| Préfixe de Chemin | Service de Destination | Sécurisé par JWT |
| :--- | :--- | :---: |
| `/auth/**` | `authentication-service` | Non (public) |
| `/users/client/register` | `user-service` | Non (public) |
| `/users/**` | `user-service` | Oui |
| `/transactions/**` | `wallet-service` | Oui |
| `/accounts/**` | `wallet-service` | Oui |
| `/pricing/**` | `pricing-service` | Oui |
| `/tracking/**` | `tracking-service` | Oui |
| `/personnalisation/**` | `personnalisation-service` | Oui |

---

## 🔒 Le Filtre d'Authentification (`AuthenticationFilter`)

Le filtre `AuthenticationFilter` hérite de `AbstractGatewayFilterFactory`. Son fonctionnement est le suivant :

1. **Vérification de la route** : Si le chemin demandé fait partie des chemins publics (définis dans `RouteValidator`), la requête passe directement sans validation.
2. **Extraction du Token** : Recherche de l'en-tête `Authorization`. Si absent ou ne commençant pas par `Bearer `, la requête est rejetée avec une erreur `401 Unauthorized`.
3. **Validation du Token** : Le token est décodé et validé par `JwtUtil` en utilisant la clé secrète configurée.
4. **Enrichissement de la requête** : Si le token est valide, le filtre extrait les informations d'identité (sub/phone, role, userId) et reconstruit la requête en ajoutant les en-têtes `X-User-Id`, `X-User-Phone` et `X-User-Role` avant de la transmettre au microservice aval.
5. **Gestion d'erreur** : En cas de token expiré ou invalide, une erreur `401 Unauthorized` est retournée.

---

## 🚀 Démarrage

### Mode Local
Lancez le service avec Maven :
```bash
mvn spring-boot:run
```

### Mode Docker
Ce service est lancé automatiquement par le Docker Compose global du backend. Il dépend de la bonne santé du `eureka-server`.
