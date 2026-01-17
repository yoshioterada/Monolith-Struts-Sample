<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<h2 class="page-title">カート</h2>
<logic:empty name="cartItems">
  <div class="card">カートは空です。</div>
</logic:empty>
<logic:notEmpty name="cartItems">
  <div class="card table-responsive">
  <table>
    <tr>
      <th>商品名</th>
      <th>数量</th>
      <th>単価</th>
    </tr>
    <logic:iterate id="item" name="cartItems">
      <tr>
        <td><bean:write name="item" property="productName" filter="true"/></td>
        <td><bean:write name="item" property="quantity" filter="true"/></td>
        <td><bean:write name="item" property="unitPrice" filter="true"/></td>
      </tr>
    </logic:iterate>
  </table>
  </div>
  <div class="card cart-summary">
    <p>小計: <strong><bean:write name="cartSubtotal" filter="true"/></strong></p>
  <logic:present name="coupon">
    <p>クーポン: <bean:write name="coupon" property="code" filter="true"/></p>
    <p>割引額: <bean:write name="discountAmount" filter="true"/></p>
  </logic:present>
  </div>
</logic:notEmpty>

<h3>クーポン適用</h3>
<html:form action="/coupon/apply.do" method="post">
  <html:text property="code" size="20"/>
  <html:token/>
  <html:submit value="適用"/>
</html:form>

<p><html:link page="/checkout.do" styleClass="btn">チェックアウトへ</html:link></p>
