#!/usr/bin/env sh
set -e

CATALINA_HOME=${CATALINA_HOME:-/usr/local/tomcat}
CONTEXT_DIR="$CATALINA_HOME/conf/Catalina/localhost"
mkdir -p "$CONTEXT_DIR"

DB_HOST=${DB_HOST:-db}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-skishop}
DB_USER=${DB_USER:-skishop}
DB_PASSWORD=${DB_PASSWORD:-skishop}

cat > "$CONTEXT_DIR/ROOT.xml" <<EOF
<Context>
  <Resource name="jdbc/skishop"
            auth="Container"
            type="javax.sql.DataSource"
            maxTotal="20"
            maxIdle="5"
            maxWaitMillis="10000"
            username="${DB_USER}"
            password="${DB_PASSWORD}"
            driverClassName="org.postgresql.Driver"
            url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" />
</Context>
EOF

echo "[entrypoint] Generated ROOT.xml for jdbc/skishop -> jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
exec "$@"
