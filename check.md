# Monolith Struts Compliance Checklist

- [x] Java 1.5 language constraints observed (no annotations/lambdas/try-with-resources; String utilities avoid Java 6 APIs).
- [x] Maven compiler configured for source/target 1.5 with Java 5-era dependencies (Struts 1.2.9, log4j 1.2.17, commons-dbcp/pool/dbutils, JavaMail 1.4.x, JDBC3 PostgreSQL driver).
- [x] Layered package structure present (web/action/form, service, dao, domain/dto/common).
- [x] Struts XML configuration defines form beans, action mappings, validator plugin, Tiles plugin, and global exception handling.
- [x] Tiles base layout and JSPs use Struts taglibs (`html`, `bean`, `logic`) with shared header/footer/messages.
- [x] AuthRequestProcessor enforces session/role checks and CSRF token validation; login resets session.
- [x] Validator rules cover core forms (login/register/password reset/cart/checkout/coupon/address/admin).
- [x] DAO template and DataSourceLocator/Factory use JDBC with DBCP and app.properties/JNDI configuration.
- [x] Security measures in place (password hashing with salt/iterations, HttpOnly cart cookie, tokenized POSTs).
- [x] Docker runtime targets Tomcat 6.0.53 with a Java 5-compatible JDK and PostgreSQL 9.2 configuration.
