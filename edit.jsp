<%@ taglib uri="standard-common" prefix="c" %>
<%@ taglib uri="standard-format" prefix="fmt" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="sanchez-authorization" prefix="auth"%>
<%@ taglib uri="sanchez-widget" prefix="widget"%>
<%@ taglib uri="fisglobal-token" prefix="token" %>
<%@ taglib uri="standard-function" prefix="fn" %>
<c:set var="currencyFormat">
	<fmt:message key="format.currency.noSymbols" />
</c:set>
<c:set var="frequencyLabel">
	<fmt:message key="label.frequency.${requestScope.pendingTransferForm.EFTPAY_FREQUENCY}" />
</c:set>

<html:form method="post" action="pendingTransfers.do">
	<html:hidden property="method" value="edit" />
	<html:hidden property="page" value="1" />
	<html:hidden property="EFTPAY_EFD" />
	<html:hidden property="EFTPAY_FREQUENCY" />
	<html:hidden property="EFTPAY_EFTTYPE" />
	<html:hidden property="EFTPAY_SEQ" />
	<html:hidden property="EFTPAY_CID" />
	<html:hidden property="EFTPAY_RECACCT" />
	<html:hidden property="CTBLEFTTYPE_STBLEFT" />
	<table width="100%" cellspacing="0">
		<tr><td><fmt:message key="text.transfer.pending.update.1" /></td></tr>
		<tr>
			<td>
				<table width="100%" cellspacing="2">
					
					<widget:display labelKey="label.date" contextField="EFTPAY_EFD" dataClass="data" dataWidth="65%" value="${requestScope.pendingTransferForm.EFTPAY_EFD}" />
					
                    <widget:text labelKey="label.amount" property="EFTPAY_AMOUNT" dataClass="data" size="18" maxlength="20" value="${requestScope.pendingTransferForm.EFTPAY_AMOUNT}" />
                                        
					<widget:display labelKey="label.fromAccount" contextField="EFTPAY_CID" dataClass="data" value="${requestScope.pendingTransferForm.EFTPAY_CID}" />
					
					<c:choose>
						<c:when test="${empty requestScope.pendingTransferForm.accountType}">
							<widget:display labelKey="label.toAccount" contextField="EFTPAY_RECACCT" dataClass="data" value="${requestScope.pendingTransferForm.toBankName} - ${requestScope.pendingTransferForm.EFTPAY_RECACCT}" />
						</c:when>
						<c:otherwise>
							<widget:display labelKey="label.toAccount" contextField="EFTPAY_RECACCT" dataClass="data" value="${requestScope.pendingTransferForm.toBankName} - ${requestScope.pendingTransferForm.EFTPAY_RECACCT} - ${requestScope.pendingTransferForm.accountType}" />
						</c:otherwise>
					</c:choose>
								
					<c:choose>
						<c:when test="${fn:startsWith(frequencyLabel, '??')}">
							<widget:display labelKey="label.frequency" contextField="EFTPAY_FREQUENCY" dataClass="data" value="${requestScope.pendingTransferForm.EFTPAY_FREQUENCY}" />
						</c:when>
						<c:otherwise>
							<widget:display labelKey="label.frequency" contextField="EFTPAY_FREQUENCY" dataClass="data" value="${frequencyLabel}" />
						</c:otherwise>
					</c:choose>
				</table>
			</td>
		</tr>
		<tr>
			<td>
				<div class="buttonWrapper">
				<input type="submit" name="submit" value="<fmt:message key="label.submit" />">
				<input type="button" onclick="location.href='pendingTransfers.do?<c:out value="${token:getTokenParameter(pageContext.request, pageContext.session)}"/>';" value="<fmt:message key="label.cancel" />">
				</div>
			</td>
		</tr>

	</table>
</html:form>