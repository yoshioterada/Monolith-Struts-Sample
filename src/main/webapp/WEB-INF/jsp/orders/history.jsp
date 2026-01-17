<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<h2>注文履歴</h2>
<logic:empty name="orders">
  <p>注文履歴がありません。</p>
</logic:empty>
<logic:notEmpty name="orders">
  <table border="1">
    <tr>
      <th>注文番号</th>
      <th>状態</th>
      <th>合計</th>
      <th>操作</th>
    </tr>
    <logic:iterate id="order" name="orders">
      <bean:define id="orderId" name="order" property="id" type="java.lang.String"/>
      <tr>
        <td><bean:write name="order" property="orderNumber" filter="true"/></td>
        <td><bean:write name="order" property="status" filter="true"/></td>
        <td><bean:write name="order" property="totalAmount" filter="true"/></td>
        <td>
          <html:link page="/orders/detail.do" paramId="orderId" paramName="order" paramProperty="id">詳細</html:link>
          <br/>
          <form action="/orders/cancel.do" method="post">
            <input type="hidden" name="orderId" value="<bean:write name='order' property='id' filter='true'/>"/>
            <html:token/>
            <button type="submit">キャンセル</button>
          </form>
          <form action="/orders/return.do" method="post">
            <input type="hidden" name="orderId" value="<bean:write name='order' property='id' filter='true'/>"/>
            <html:token/>
            <button type="submit">返品</button>
          </form>
        </td>
      </tr>
    </logic:iterate>
  </table>
</logic:notEmpty>
