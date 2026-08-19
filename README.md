# one-user-management

API REST peste `User`, pe aceeași structură ca `academy-hub-api`: pachet per feature, `models` / `repository` / `dtos` / `exceptions` / `services` + `services/interfaces`, plus stratul nou `controllers`.

## Pornire

```bash
mvn spring-boot:run
```

MySQL trebuie să ruleze pe `localhost:3306`. Baza se creează singură (`createDatabaseIfNotExist=true`).

La prima pornire, `DataSeeder` bagă 5 useri **doar dacă tabela e goală**. Nu mai trebuie să inserezi nimic de mână.

Ca să vezi și demo-ul de query-uri din consolă (fostul `CatalogRunner`):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

## Atenție la prima rulare

`id` a devenit `UUID` (era `String`). `ddl-auto: update` **nu schimbă tipul unei coloane care există deja**, deci pe o bază veche vei primi erori. O dată, înainte de prima pornire:

```sql
DROP TABLE IF EXISTS users;
```

## Structura

```
ro/mycode/user_management/
├── UserManagementApplication.java
├── DataSeeder.java              @Order(1) — populează baza dacă e goală
├── CatalogRunner.java           @Order(2) @Profile("demo") — demo query-uri în consolă
├── web/
│   ├── GlobalExceptionHandler.java   @RestControllerAdvice — excepție → status HTTP
│   └── ApiError.java                 formatul unic de eroare
└── users/
    ├── controllers/UserController.java
    ├── dtos/                    record-uri Request/Response + UserSummary (proiecție)
    ├── exceptions/              EmailAlreadyUsed, EmailNotFound, UserIdNotFound, NoUsersFound
    ├── models/User.java
    ├── repository/UserRepository.java
    └── services/
        ├── UserCommandServiceImpl.java   scrieri  (@Transactional)
        ├── UserQueryServiceImpl.java     citiri   (@Transactional(readOnly = true))
        └── interfaces/                   contractele, cu @Valid pe parametri
```

Regula de separare: **command scrie, query citește.** Controllerul nu vorbește niciodată direct cu repository-ul, iar entitatea `User` nu iese niciodată pe HTTP — pleacă doar DTO-uri.

## Endpoint-uri

Toate sub `/api/users`.

### Scrieri

| Metodă | Path | Status | Ce face |
|---|---|---|---|
| `POST` | `/api/users` | `201` + header `Location` | creează user |
| `PUT` | `/api/users/{id}` | `200` | update parțial — trimiți doar câmpurile schimbate |
| `PUT` | `/api/users/{id}/password` | `200` | schimbă parola prin `@Modifying` |
| `DELETE` | `/api/users/{id}` | `200` | șterge, întoarce ce a șters |

### Citiri

| Path | Query params | Ce demonstrează |
|---|---|---|
| `/api/users` | — | listă simplă |
| `/api/users/{id}` | — | `@PathVariable UUID` |
| `/api/users/search` | `name`, `minAge`, `page`, `size`, `sort` | params opționale + `Pageable` |
| `/api/users/by-email` | `email` | param obligatoriu, validat cu `@Email` |
| `/api/users/by-name` | `firstName`, `lastName` | doi parametri deodată |
| `/api/users/by-last-name` | `contains` | fragment, ignore case |
| `/api/users/by-emails` | `emails` | `List<String>` — CSV sau param repetat |
| `/api/users/by-domain` | `domain` | `defaultValue = "@gmail.com"` |
| `/api/users/older-than` | `age` | param obligatoriu de tip `int` |
| `/api/users/between-ages` | `minAge`, `maxAge` | interval |
| `/api/users/adults` | `minAge` | `defaultValue = "18"` |
| `/api/users/from-age` | `age`, `page`, `size`, `sort` | paginare + sortare din URL |
| `/api/users/top3-by-age` | — | `findTop3By...` |
| `/api/users/summaries` | `lastName` | proiecție pe interfață |
| `/api/users/exists` | `email` | răspuns scalar |
| `/api/users/count` | `youngerThan` | `count` derivat |
| `/api/users/average-age` | — | agregare |

## Exemple

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Rares","lastName":"Dumitru","email":"rares.dumitru@gmail.com","password":"parola999","age":31}'
```

```bash
curl 'http://localhost:8080/api/users/search?name=ste&minAge=18&page=0&size=10&sort=lastName,asc'
```

```bash
curl 'http://localhost:8080/api/users/by-emails?emails=cristian.tudor@gmail.com,maria.stere@gmail.com'
curl 'http://localhost:8080/api/users/by-emails?emails=cristian.tudor@gmail.com&emails=maria.stere@gmail.com'
```

```bash
curl -X PUT http://localhost:8080/api/users/<id> \
  -H 'Content-Type: application/json' -d '{"age":33}'
```

## Erori

Orice excepție iese în același format, produs de `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-08-19T18:51:48.922403Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "details": [
    "age: Age must be greater than zero",
    "email: Email must be a valid address",
    "firstName: First name is required",
    "password: Password must be at least 8 characters"
  ]
}
```

| Situație | Status |
|---|---|
| body invalid / query param invalid / lipsă / de tip greșit / UUID stricat / JSON stricat | `400` |
| id sau email inexistent, rută inexistentă | `404` |
| email deja folosit | `409` |

`details[]` listează **toate** câmpurile picate, nu doar primul.

## Postman

`postman/one-user-management.postman_collection.json` — 36 de request-uri, grupate în: Command, Query, Query params, Erori.

Import în Postman → rulează întâi **`GET all users`**: scriptul lui salvează primul id în variabila `{{userId}}`, pe care o folosesc toate request-urile cu `/{id}`. Variabila `baseUrl` e `http://localhost:8080`.

Fiecare request are un test pe statusul așteptat, deci poți rula toată colecția cu Collection Runner.
