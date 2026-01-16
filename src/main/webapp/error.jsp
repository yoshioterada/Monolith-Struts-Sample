<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<!DOCTYPE html>
<html>
  <head>
    <title><bean:message key="app.title"/></title>
  </head>
  <body>
    <h2><bean:message key="error.global"/></h2>
    <html:errors/>
    <p><html:link page="/home.do">Back to Home</html:link></p>
  </body>
</html>
