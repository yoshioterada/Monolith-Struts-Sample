# Operations & Release Guide

This guide focuses on Tomcat 6.0.53 because the project ships Dockerfiles for that
version (`./Dockerfile`, `./Dockerfile.tomcat6`).

## Deployment (Tomcat 6)
1. Build the WAR: `mvn -B package`.
2. Apply database schema: `src/main/resources/db/schema.sql` (optional seed data: `data.sql`).
3. Create a context descriptor (example: `$CATALINA_HOME/conf/Catalina/localhost/skishop-monolith.xml`).
   ```xml
   <Context>
     <Resource name="jdbc/skishop" auth="Container" type="javax.sql.DataSource"
               maxActive="20" maxIdle="5" maxWait="10000"
               username="${db.username}" password="${db.password}"
               driverClassName="org.postgresql.Driver"
               url="${db.url}"/>
   </Context>
   ```
4. Provide the `db.*` properties via `CATALINA_OPTS` system properties or by using literal values
   in the context XML (Tomcat expands `${...}` from system properties).
5. Copy `target/skishop-monolith.war` into `$CATALINA_HOME/webapps/` and start Tomcat.

## Context & Environment Configuration
The application tries JNDI (`java:comp/env/jdbc/skishop`) first, then falls back to
`WEB-INF/classes/app.properties`.

`app.properties` keys (defaults in `src/main/resources/app.properties`, used when falling back
to `DataSourceFactory`):
- `db.url`, `db.username`, `db.password`, `db.driver`
- `db.pool.maxActive`, `db.pool.maxIdle`, `db.pool.maxWait` (Tomcat 6-style naming)
- `app.timezone`
- `smtp.host`, `smtp.port`, `smtp.username`, `smtp.password`, `mail.from`

For JNDI pool settings, follow the Tomcat 6 context example above.

`app.properties` placeholder resolution uses `${TOKEN}` (system properties first, then
environment variables). The same `${db.url}`/`${db.username}`/`${db.password}` placeholders can
also be used in the Tomcat context XML via system properties.
Default usage:
- `DB_PASSWORD`: injects the database password into `db.password`.

Docker entrypoint variables (from `docker/entrypoint.sh` in this repo):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- Tomcat 6 pool settings: `DB_POOL_MAX_ACTIVE`, `DB_POOL_MAX_IDLE`, `DB_POOL_MAX_WAIT`

## Backup & Restore (PostgreSQL)
Backup:
```sh
pg_dump -Fc -f skishop-YYYYMMDD.dump -h <host> -U <db_user> <db_name>
```

Restore (stop the app first):
```sh
pg_restore --clean --if-exists -h <host> -U <db_user> -d <db_name> skishop-YYYYMMDD.dump
```

Docker example:
```sh
docker compose exec db pg_dump -Fc -U skishop skishop > skishop-YYYYMMDD.dump
docker compose exec -T db pg_restore --clean --if-exists -U skishop -d skishop < skishop-YYYYMMDD.dump
```

## Runbook (Operations)
- **Deploy verification**: after Tomcat start (typically within 1-2 minutes, or when
  `Server startup in <ms>` appears), confirm `catalina.out` has no startup errors such as
  `ClassNotFoundException`, `OutOfMemoryError`, or DB connection failures, then access `/login.do`
  or `/home.do` to confirm HTTP 200 responses with the expected JSP content (login form or home
  page) rendered.
- **Thread dump** (Java 5+ compatible):
  1. Identify the Tomcat JVM PID (`pgrep -f 'org.apache.catalina.startup.Bootstrap'` preferred;
     `jps -l` also works when available in your Java install).
  2. Capture with `jstack <pid>` (preferred). Use `kill -3 <pid>` only if needed; `SIGQUIT` writes
     a thread dump to `catalina.out` in standard JVMs, so validate the behavior in non-production
     if your runtime is customized.
  3. Attach the timestamped dump to incident notes.
- **Log rotation**:
  1. Check `log4j.properties` in the deployed app (`WEB-INF/classes/log4j.properties`, or inspect
     the WAR with `jar tf`/`jar xf`) for the RollingFileAppender configuration (appender name may
     vary), including the log file path (default `logs/app.log`) and configured
     `MaxFileSize`/`MaxBackupIndex` values.
  2. Run `ls -la logs/` to confirm `app.log`, `app.log.1`, ... are present after rotation.
  3. Use `stat logs/app.log` (or `ls -lh logs/app.log`) to confirm the active log stays below
     the size threshold.

## WAR Versioning & Release Notes
- Version is sourced from `pom.xml` (`<version>`), with `finalName` set to `skishop-monolith`.
- For versioned artifacts, rename the WAR after build (e.g. `skishop-monolith-1.0.1.war`) or
  adjust `finalName` during release packaging.
- Keep the deployed file name consistent with the desired context path.

Release checklist:
1. Update `pom.xml` version.
2. Update the release notes below.
3. Run `mvn -B test` and `mvn -B package`.
4. Publish the WAR and tag the release.

### Release Notes
Template (capture user-visible changes, data migrations, and ops impacts):
- Summary of changes
- Database schema/data changes (scripts, backfill steps)
- Configuration changes (new/removed settings)
- Validation performed (tests, smoke checks)

#### v1.0.0 (TBD - set on release)
- Initial release of the monolithic Struts application.
