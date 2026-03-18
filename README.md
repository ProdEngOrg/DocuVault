# DocuVault - SaaS Document Management System

## Team
- **Team Name:** Cloud 9
- **Members:**
  - Enache-Preoteasa David - Identity & Workspace Manager (delegare spații virtuale, implementare sistem de distribuire a documentelor cu permisiuni de acces).
  - Bunescu Robert - Document Operations Core & API Design & Versioning on those files(Dezvoltare operații CRUD pentru documente).

## Project Description

DocuVault este o aplicație de tip Software-as-a-Service (SaaS) care expune un API RESTful pentru gestionarea sigură și eficientă a documentelor în cloud. Sistemul oferă utilizatorilor spații de lucru virtuale izolate, unde aceștia pot crea, citi, actualiza și șterge documente, având control deplin asupra datelor proprii.

O componentă centrală a logicii de business o reprezintă sistemul automat de versionare. Spre deosebire de un sistem de stocare simplu, atunci când un utilizator actualizează conținutul sau metadatele unui document, DocuVault nu suprascrie informația veche, ci generează automat o nouă versiune a fișierului, păstrând istoricul complet al modificărilor pentru trasabilitate și recuperare.

Arhitectura proiectului este modulară, separând responsabilitățile de izolare, operațiunile de bază pe fișiere (CRUD) și motorul de versionare. Acest grad de decuplare, susținut de o bază de date NoSQL persistentă (MongoDB), permite testarea riguroasă a regulilor de acces și a fluxurilor de date.

### Key Features
- **Isolated Virtual Workspaces:** Delegarea de spații virtuale unice pentru fiecare utilizator, garantând izolarea datelor. Funcționalitatea de distribuire a documentelor cu permisiuni de acces.
- **Document Management & Export:** Operațiuni complete de CRUD pe fișierele virtuale stocate în baza de date, inclusiv funcționalitatea de a exporta/descărca fișierul pe device-ul clientului. Răspunsuri API cu link-uri hypermedia pentru a facilita navigarea dinamică între resurse (ex: link-uri directe către versiunile anterioare ale unui document returnat).
- **Automated Simple Versioning:** Urmărirea istorică a modificărilor prin crearea automată de noi versiuni (v1, v2, v3) la fiecare update al unui document.

### Technical Stack
- **Backend:** Spring Boot (Java 21)
- **Database:** MongoDB
- **API:** RESTful/HATEOAS
- **Testing:** JUnit, Mockito, Cucumber
- **Monitoring:** Prometheus, Grafana
- **Deployment:** Docker

---

## API Documentation

Base URL: `http://localhost:8080`

### Documents (`/api/documents`)

All mutating document endpoints require the `X-User-Id` header for ownership and permission checks.

| Method | Endpoint | Headers | Description | Request Body |
|--------|----------|---------|-------------|--------------|
| `POST` | `/api/documents` | `X-User-Id` | Create a new document (v1) | `{ "title", "content", "workspaceId", "viewers", "editors" }` |
| `GET` | `/api/documents/{groupId}` | — | Get latest version by group ID | — |
| `PUT` | `/api/documents/{groupId}` | `X-User-Id` | Update document (creates new version) | `{ "title", "content", "viewers", "editors" }` |
| `PUT` | `/api/documents/add-viewer` | `X-User-Id` | Add viewer to document | `{ "userId", "documentGroupId" }` |
| `DELETE` | `/api/documents/{groupId}` | `X-User-Id` | Delete all versions (owner only) | — |
| `GET` | `/api/documents/workspace/{workspaceId}` | — | Get all documents in a workspace | — |
| `GET` | `/api/documents/owner/{ownerId}` | — | Get all documents by owner | — |

### Document access control

**Owner:** Delete File, Add viewers & editors, Edit file, View file

**Workspace Member:** Edit file, View file

**Editor:** Edit file, View file

**Viewer:** View file

### Workspaces (`api/workspaces`)

| Method | Path | Headers | Description | Request Body |
|-|-|-|-|-|
| GET | `/statistics/{id}` | — | Get workspace statistics | — |
| POST | — | — | Create workspace | `{ "name", "userId" }` |
| POST | `/add-user` | — | Add user to workspace | `{ "userId", "workspaceId" }` |

## Contributing

All team members follow trunk-based development:
1. Create feature branch from `main`
2. Make changes and commit with clear messages
3. Create PR and request review
4. Address feedback
5. Merge after approval

# Prerequisites

For using Github Codespaces, no prerequisites are mandatory.
Follow the [./PREREQUISITES.md](./PREREQUISITES.md) instructions to configure a local virtual machine with Ubuntu, Docker, IntelliJ.

# Access the code

* Fork the code GitHub repository under your Organization
  * https://github.com/UNIBUC-PROD-ENGINEERING/service
* Clone the code repository:
  * git@github.com:YOUR_ORG_NAME/service.git

# Run code in Github Codespaces

* Make sure that the Github repository is forked under your account / Organization
* Create a new Codespace from your forked repository
* Wait for the Codespace to be up and running
* Make sure that Docker service has been started
    * ```docker ps``` should return no error
* For running / debugging directly in Visual Studio Code
  * Start the MongoDB related services
    * ```./start_mongo_only.sh```
  * Build and run the Spring Boot service
    * ```./gradlew build```
    * ```./gradlew bootRun```
* For running all services in Docker:
    * Build the Docker image of the prod-eng service
        * ```make build```
    * Start all the service containers
        * ```./start.sh```
* Use [requests.http](requests.http) to test API endpoints
* Navigation between methods (e.g. 'Go to Definition') may require:
  * ```./gradlew build``` 

NOTE: for a live demo, please check out [this youtube video](https://youtu.be/-9ePlxz03kg)

# Run/debug code in IntelliJ
* Build the code
    * IntelliJ will build it automatically
    * If you want to build it from command line and also run unit tests, run: ```./gradlew build```
* Create an IntelliJ run configuration for a Jar application
    * Add in the configuration the JAR path to the build folder `./build/libs/prod-eng-0.0.1-SNAPSHOT.jar`
* Start the MongoDB container using Docker Compose
    * ```./start_mongo_only.sh```
* Run/debug your IntelliJ run configuration
* Open in your browser:
    * http://localhost:8080/api/users

# Deploy and run the code locally as Docker instance

* Build the Docker image of the prod-eng service
    * ```make build```
* Start all the containers
    * ```./start.sh```

* Verify that all containers started, by running
  ```
  service git:(master) ✗  $ docker ps
  CONTAINER ID   IMAGE             COMMAND                  CREATED         STATUS         PORTS                                                                                          NAMES
  d0a14d57ade1   jenkins/jenkins   "/usr/bin/tini -- /u…"   5 seconds ago   Up 4 seconds   0.0.0.0:50000->50000/tcp, [::]:50000->50000/tcp, 0.0.0.0:8082->8080/tcp, [::]:8082->8080/tcp   service-jenkins-1
  d9465565ebc9   mongo-express     "/sbin/tini -- /dock…"   5 seconds ago   Up 4 seconds   0.0.0.0:8090->8081/tcp, [::]:8090->8081/tcp                                                    service-mongo-admin-ui-1
  304c29bb39ea   mongo:6.0.20      "docker-entrypoint.s…"   5 seconds ago   Up 4 seconds   0.0.0.0:27017->27017/tcp, [::]:27017->27017/tcp                                                service-mongo-1
  a74b4cb2fb58   prod-eng-img      "java -jar /prod-eng…"   5 seconds ago   Up 4 seconds   0.0.0.0:5005->5005/tcp, [::]:5005->5005/tcp, 0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp       service-prod-eng-1
  ```
* Open in your browser:
    * http://localhost:8080/api/users
* You can test other API endpoints using [requests.http](requests.http)
* You can access the MongoDB Admin UI at:
  * http://localhost:8090
  * default credentials: username `unibuc`, password `adobe`
  * database `test` contains application entities
