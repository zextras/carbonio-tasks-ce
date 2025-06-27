# Run tasks locally with Docker

This minimal setup includes all necessary dependencies without mocks (with Consul being the only exception).

Steps:
    1. `mvn clean install -DskipTests=true`
    2. `cd docker/minimal`
    3. `docker compose up --build`
    4. Browse tasks on `http://localhost:9000/`, backend accessible on `http://localhost:20002`
    5. Login using `test@demo.zextras.io`/`password`

Possible configs for tasks:
  - CARBONIO_TASKS_HOST
  - CARBONIO_TASKS_PORT
  - CARBONIO_POSTGRES_HOST
  - CARBONIO_POSTGRES_PORT
  - CARBONIO_USER_MANAGEMENT_HOST
  - CARBONIO_USER_MANAGEMENT_PORT