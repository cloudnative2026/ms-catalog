# ms-catalog

Spring Boot / Java 21 catalog service for Pedidos360. PostgreSQL is used for now.

## Run locally

Install Java 21 and start Docker Desktop, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Spring Boot starts the existing PostgreSQL Compose service and discovers its mapped port.
Liquibase creates the products table; Hibernate validates the schema.
The existing development database credentials and Entra issuer/audience are retained.
Do not reuse the Compose example credentials for deployment.

API: http://localhost:8080/api/catalog/products
Swagger: http://localhost:8080/swagger-ui/index.html

Swagger documentation is public; API operations require an Entra **access token**
with the configured issuer and audience. Use Swagger's Authorize button.
The token's roles claim must contain Admin or Operador (case-sensitive).
The Spring application name does not have to match the Entra registration name.

## Endpoints

| Method | Path | Roles |
| --- | --- | --- |
| GET | /api/catalog/products | Admin, Operador |
| GET | /api/catalog/products/{id} | Admin, Operador |
| POST | /api/catalog/products | Admin |
| PUT | /api/catalog/products/{id} | Admin |
| DELETE | /api/catalog/products/{id} | Admin |
| PATCH | /api/catalog/products/{id}/stock | Admin, Operador |

POST and PUT body:

```json
{"name":"Coffee","description":"250 g","price":4990,"stock":20,"imageUrl":null,"active":true}
```

POST returns 201 and Location, DELETE returns 204, missing products return 404,
invalid bodies return 400, and conflicting concurrent updates return 409.
DELETE permanently deletes a product. To retain it, PUT with active=false.
Listing includes both active and inactive products for catalog administration.

PATCH body: `{"stock":15}`. This **replaces** stock with 15.
It is not an atomic stock decrement/reservation API for orders.
Order acceptance and idempotent stock deduction remain a separate integration step.
Optimistic locking prevents simultaneous updates from silently overwriting one another.

Local browser origins: http://localhost:5500 and http://localhost:5173.

## Tests

```powershell
.\mvnw.cmd test
```

Tests use H2 in PostgreSQL compatibility mode and mocked JWT decoding.
They do not require Docker or contact Entra. They verify the migration, CRUD,
validation and authorization, but do not replace a live PostgreSQL/Entra smoke test.
