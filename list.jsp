<%@ taglib uri="standard-common" prefix="c" %>
<%@ taglib uri="standard-format" prefix="fmt" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="sanchez-authorization" prefix="auth" %>
<%@ taglib uri="sanchez-widget" prefix="widget"%>
<%@ taglib uri="standard-function" prefix="fn" %>

<c:set var="currencyFormat" scope="request">
	<fmt:message key="format.currency" />
</c:set>
<c:set var="currencyFormatNoSymbols" scope="request">
	<fmt:message key="format.currency.noSymbols" />
</c:set>
<c:set var="smallDateFormat" scope="request">
	<fmt:message key="format.date.small" />
</c:set>

<script type="text/javascript">
	function submitForm(frm, action) {
		var numSeq = getRadioElementValue(frm.name, "transferIndex");
		if (numSeq != null) {
			frm.method.value = action;
			if (action == "delete") {
				return confirm("<fmt:message key="messages.transfer.deleteConfirm.1" />");
			}
			return true;
		} else {
			if (action == "editForm") {
				alert("<fmt:message key="messages.transfer.selectEdit" />");
			} else if (action == "delete") {
				alert("<fmt:message key="messages.transfer.selectDelete" />");				
			}
			return false;
		}
	}
</script>

<html:form action="pendingTransfers.do" method="post">
	<html:hidden property="method" value="" />
	<table class="contentTable" cellspacing="0">
		<tr><td><fmt:message key="text.transfer.pending.list.1" /></td></tr>
		<tr>
			<td>
				<table class="ledgerScrollable" cellspacing="0" id="pending-transfers-list" data-scroll-height="335px" data-empty-table-message="<fmt:message key="label.noPendingTransfers" />">
					<thead>
						<tr>
							<th width="5%" scope="col"><fmt:message key="label.action" /></th>
							<th class="sort-title-numeric" width="10%" scope="col"><fmt:message key="label.date" /></th>
							<th class="right sort-title-numeric" width="15%" scope="col"><fmt:message key="label.amount" /></th>
							<th width="30%" scope="col"><fmt:message key="label.fromAccount" /></th>
							<th width="30%" scope="col"><fmt:message key="label.toAccount" /></th>
							<th width="10%" scope="col"><fmt:message key="label.frequency" /></th>
						</tr>
					</thead>
					<tbody>
						<c:set var="contextVar" value="EFTPAY_EFD,EFTPAY_AMOUNT,EFTPAY_CID,EFTPAY_RECACCT,EFTPAY_FREQUENCY"/>
						<c:forEach var="transfer" items="${sessionScope.user.pendingTransfers}" varStatus="transferStatus">
							<c:set var="protectionRow" value="${transfer.protectionRow}" scope="page" />
							<c:if test="${auth:checkFieldListAuth(agent,protectionRow, contextVar)}">
								<tr>
									<td>
										<c:if test="${auth:target(agent, 'pendingTransfers.do?method=editForm', PERMISSION_TYPE_FULL)}">
											<c:set var="hasEditAuthorization" value="true" />
										</c:if>
										<c:if test="${auth:target(agent, 'pendingTransfers.do?method=delete', PERMISSION_TYPE_FULL)}">
											<c:set var="hasDeleteAuthorization" value="true" />
										</c:if>
										<c:if test="${transfer.status == 'A' && (hasEditAuthorization == 'true' || hasDeleteAuthorization == 'true')}">
											<input type="radio" name="transferIndex" value="${transferStatus.index}">
										</c:if>
									</td>
									<c:choose>
										<c:when test="${auth:checkFieldAuth(agent,protectionRow, 'EFTPAY_EFD')}">
											<widget:display value="${transfer.startDate}" suppressLabelTD="true" suppressOuterTR="true" renderSort="true" dateFormatKey="format.date.small"/>
										</c:when>
										<c:otherwise>
											<td></td>
								    </c:otherwise>
									</c:choose>
									<c:choose>
										<c:when test="${auth:checkFieldAuth(agent,protectionRow, 'EFTPAY_AMOUNT')}">
											<widget:display dataClass="right" value="${transfer.amount}" suppressLabelTD="true" suppressOuterTR="true" renderSort="true" numberFormatKey="format.currency"/>
										</c:when>
										<c:otherwise>
											<td></td>
								    </c:otherwise>
									</c:choose>
									<c:choose>
										<c:when test="${auth:checkFieldAuth(agent,protectionRow, 'EFTPAY_CID')}">
											<widget:display value="${transfer.fromAccountNumber}" suppressLabelTD="true" suppressOuterTR="true" />
										</c:when>
										<c:otherwise>
											<td></td>
								    </c:otherwise>
									</c:choose>
									<c:choose>
										<c:when test="${auth:checkFieldAuth(agent,protectionRow, 'EFTPAY_RECACCT')}">
											<widget:display value="${transfer.toAccountNumber}" suppressLabelTD="true" suppressOuterTR="true" />
										</c:when>
										<c:otherwise>
											<td></td>
								    </c:otherwise>
									</c:choose>
									<c:choose>
										<c:when test="${auth:checkFieldAuth(agent,protectionRow, 'EFTPAY_FREQUENCY')}">
											<fmt:message var="frequencyLabel" key="label.frequency.${transfer.frequency}" />
											<c:choose>
												<c:when test="${fn:startsWith(frequencyLabel, '??')}">
													<widget:display value="${transfer.frequency}" suppressOuterTR="true" suppressLabelTD="true"/>
												</c:when>
												<c:otherwise>
													<widget:display value="${frequencyLabel}" suppressOuterTR="true" suppressLabelTD="true"/>
												</c:otherwise>
											</c:choose>
										</c:when>
										<c:otherwise>
											<td></td>
								    	</c:otherwise>
									</c:choose>
								</tr>
							</c:if>
						</c:forEach>
					</tbody>
				</table>
			</td>
		</tr>
		<c:if test="${!empty sessionScope.user.pendingTransfers}">
			<tr>
				<td>
					<div class="buttonWrapper">
						<c:if test="${hasEditAuthorization}">
							<input type="submit" name="edit" onClick="return submitForm(this.form, 'editForm');" value="<fmt:message key="label.edit" />">
						</c:if>
						<c:if test="${hasDeleteAuthorization}">
							<input type="submit" name="delete" onClick="return submitForm(this.form, 'delete');" value="<fmt:message key="label.delete" />">
						</c:if>
					</div>
				</td>
			</tr>
		</c:if>
	</table>
</html:form>