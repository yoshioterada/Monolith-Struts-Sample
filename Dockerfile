# Multi-stage build targeting Tomcat 6 + JDK7 (compatible with -source/-target 1.5)
# Stage 1: Build
FROM azul/zulu-openjdk:7 as build

ARG MAVEN_VERSION=3.2.5
ENV MAVEN_HOME=/opt/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

RUN set -eux; \
  curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzC /opt; \
  ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME}

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B dependency:go-offline || true
COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: Runtime
FROM azul/zulu-openjdk:7-jre as runtime

ENV CATALINA_HOME=/opt/tomcat
ENV PATH=${CATALINA_HOME}/bin:${PATH}

# Tomcat 6.0.53 (latest 6.x) from archive
RUN set -eux; \
  curl -fsSL https://archive.apache.org/dist/tomcat/tomcat-6/v6.0.53/bin/apache-tomcat-6.0.53.tar.gz \
    | tar -xzC /opt; \
  ln -s /opt/apache-tomcat-6.0.53 ${CATALINA_HOME}; \
  rm -rf ${CATALINA_HOME}/webapps/*

# PostgreSQL JDBC driver (JRE6+/JRE7 compatible)
RUN curl -fsSL -o ${CATALINA_HOME}/lib/postgresql.jar https://jdbc.postgresql.org/download/postgresql-9.4.1212.jar

# Entrypoint (reuses existing script; ensure CATALINA_HOME set)
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Deploy WAR
COPY --from=build /workspace/target/*.war ${CATALINA_HOME}/webapps/ROOT.war

EXPOSE 8080
ENTRYPOINT ["entrypoint.sh"]
CMD ["catalina.sh", "run"]
