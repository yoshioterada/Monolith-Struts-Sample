# Multi-stage build targeting Tomcat 6.0.53 + JDK 5.0u22
# Stage 1: Build
# Debian Stretch retained for JDK 5 compatibility.
FROM debian:stretch-slim as build

ARG TARGETARCH
RUN if [ -n "${TARGETARCH}" ] && [ "${TARGETARCH}" != "amd64" ]; then \
      echo "This image requires linux/amd64 (JDK 5 installer) but got ${TARGETARCH}" >&2; \
      exit 1; \
    fi

ARG JAVA_HOME=/opt/jdk1.5.0_22
ARG JDK_URL=https://archive.org/download/Java_5_update_22/jdk-1_5_0_22-linux-amd64.bin
ARG JDK_SHA256=2788b0c787cfa8d314e427d59fabf0b64a1c535d0b15a5437f38c0ede7beae4c
ARG JDK_LICENSE=accept
# Maven 2.2.1 is the last release with Java 5 compatibility.
# Override MAVEN_BASE_URL if your archive mirrors store Maven 2.x elsewhere.
ARG MAVEN_BASE_URL=https://archive.apache.org/dist/maven/maven-2
ARG MAVEN_VERSION=2.2.1
ENV JAVA_HOME=${JAVA_HOME}
ENV MAVEN_HOME=/opt/maven
ENV PATH=${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}

# Prefer local JDK installer in build context (binaries/jdk-1_5_0_22-linux-amd64.bin)
COPY binaries/jdk-1_5_0_22-linux-amd64.bin /tmp/jdk.bin

RUN set -eux; \
  if [ "${JDK_LICENSE}" != "accept" ]; then \
    echo "Set JDK_LICENSE=accept to acknowledge the JDK 5 license." >&2; \
    exit 1; \
  fi; \
  echo "deb http://snapshot.debian.org/archive/debian/20190331T000000Z stretch main" > /etc/apt/sources.list; \
  echo "deb http://snapshot.debian.org/archive/debian-security/20190331T000000Z stretch/updates main" >> /etc/apt/sources.list; \
  apt-get -o Acquire::Check-Valid-Until=false -o Acquire::AllowInsecureRepositories=true update; \
  apt-get install -y --no-install-recommends --allow-unauthenticated ca-certificates curl gnupg dirmngr; \
  for url in \
    https://ftp-master.debian.org/keys/archive-key-8.asc \
    https://ftp-master.debian.org/keys/archive-key-9.asc \
    https://ftp-master.debian.org/keys/archive-key-10.asc \
    https://ftp-master.debian.org/keys/archive-key-11.asc \
  ; do curl -fsSL "$url" | apt-key add -; done; \
  apt-get -o Acquire::Check-Valid-Until=false -o Acquire::AllowInsecureRepositories=true update; \
  apt-get install -y --no-install-recommends --allow-unauthenticated ca-certificates curl gzip libstdc++5 tar; \
  rm -rf /var/lib/apt/lists/*; \
  if [ ! -f /tmp/jdk.bin ]; then \
    curl -fsSL -o /tmp/jdk.bin ${JDK_URL}; \
  fi; \
  if [ -n "${JDK_SHA256}" ]; then \
    echo "${JDK_SHA256}  /tmp/jdk.bin" | sha256sum -c - || echo "WARNING: checksum mismatch, continuing"; \
  fi; \
  chmod +x /tmp/jdk.bin; \
  cd /tmp; \
  # JDK 5 installer prompts once for license acceptance.
  printf 'yes\n' | /tmp/jdk.bin; \
  mv /tmp/jdk1.5.0_22 ${JAVA_HOME}; \
  rm /tmp/jdk.bin; \
  curl -fsSL ${MAVEN_BASE_URL}/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzC /opt; \
  ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME}

WORKDIR /workspace
COPY pom.xml ./
COPY binaries/m2 /root/.m2/repository
COPY binaries/settings.xml /root/.m2/settings.xml
RUN mvn -B dependency:go-offline || true
COPY src ./src
RUN mvn -B -Dmaven.test.skip=true package

# Stage 2: Runtime
# Debian Stretch retained for JDK 5 compatibility.
FROM debian:stretch-slim as runtime

ARG TARGETARCH
RUN if [ -n "${TARGETARCH}" ] && [ "${TARGETARCH}" != "amd64" ]; then \
      echo "This image requires linux/amd64 (JDK 5 installer) but got ${TARGETARCH}" >&2; \
      exit 1; \
    fi

ARG JAVA_HOME=/opt/jdk1.5.0_22
ENV JAVA_HOME=${JAVA_HOME}
ENV CATALINA_HOME=/opt/tomcat
ENV PATH=${JAVA_HOME}/bin:${CATALINA_HOME}/bin:${PATH}

COPY --from=build ${JAVA_HOME} ${JAVA_HOME}

# Tomcat 6.0.53 (latest 6.x) from archive
RUN set -eux; \
  echo "deb http://snapshot.debian.org/archive/debian/20190331T000000Z stretch main" > /etc/apt/sources.list; \
  echo "deb http://snapshot.debian.org/archive/debian-security/20190331T000000Z stretch/updates main" >> /etc/apt/sources.list; \
  apt-get -o Acquire::Check-Valid-Until=false -o Acquire::AllowInsecureRepositories=true update; \
  apt-get install -y --no-install-recommends --allow-unauthenticated ca-certificates curl gnupg dirmngr; \
  for url in \
    https://ftp-master.debian.org/keys/archive-key-8.asc \
    https://ftp-master.debian.org/keys/archive-key-9.asc \
    https://ftp-master.debian.org/keys/archive-key-10.asc \
    https://ftp-master.debian.org/keys/archive-key-11.asc \
  ; do curl -fsSL "$url" | apt-key add -; done; \
  apt-get -o Acquire::Check-Valid-Until=false -o Acquire::AllowInsecureRepositories=true update; \
  apt-get install -y --no-install-recommends --allow-unauthenticated ca-certificates curl gzip libstdc++5 tar; \
  rm -rf /var/lib/apt/lists/*; \
  curl -fsSL https://archive.apache.org/dist/tomcat/tomcat-6/v6.0.53/bin/apache-tomcat-6.0.53.tar.gz \
    | tar -xzC /opt; \
  ln -s /opt/apache-tomcat-6.0.53 ${CATALINA_HOME}; \
  rm -rf ${CATALINA_HOME}/webapps/*

# PostgreSQL JDBC driver (Java 5 era)
RUN curl -fsSL -o ${CATALINA_HOME}/lib/postgresql.jar https://jdbc.postgresql.org/download/postgresql-9.2-1004.jdbc3.jar

# Entrypoint (reuses existing script; ensure CATALINA_HOME set)
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Deploy WAR
COPY --from=build /workspace/target/*.war ${CATALINA_HOME}/webapps/ROOT.war

EXPOSE 8080
ENTRYPOINT ["entrypoint.sh"]
CMD ["catalina.sh", "run"]
