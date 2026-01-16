# SkiShop Monolith (Struts 1.2.9)

Struts 1.2.9 monolithic EC sample packaged as a WAR (Java 5-era syntax with a
Maven compiler target of 1.5 in `pom.xml`).

## Requirements
- Java 5 compatible runtime (Tomcat 6.0.53 + JDK6 recommended)
- Maven (for build/test)
- PostgreSQL 9.2 (schema in `src/main/resources/db/schema.sql`)

## Build & Test
```sh
mvn -B test
mvn -B package
```

The WAR is generated at `target/skishop-monolith.war`.

## Configuration
`app.properties` is bundled in `src/main/resources` and copied to `WEB-INF/classes`.
It supports `${TOKEN}` placeholders resolved from system properties or environment variables
(for example `DB_PASSWORD`). JNDI configuration example lives in
`src/main/webapp/META-INF/context.xml` (Tomcat 6-style pool attributes).

## Operations / Release
See [docs/ops.md](docs/ops.md) for Tomcat 6 deployment steps, environment variables/context
settings, backup/restore notes, and WAR versioning/release notes.
