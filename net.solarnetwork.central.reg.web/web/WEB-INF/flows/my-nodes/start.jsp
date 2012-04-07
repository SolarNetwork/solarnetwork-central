<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<div class="intro">
	Hi there. You have ${fn:length(userNodesList)} nodes.
</div>
<div id="nodes">
	<c:if test="${fn:length(userNodesList) > 0}">
		<table>
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="user.node.created.label"/></th>
					<th><fmt:message key="user.node.name.label"/></th>
					<th><fmt:message key="user.node.description.label"/></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${userNodesList}" var="userNode">
					<tr>
						<td>${userNode.node.id}</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${userNode.node.created}"
									 pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
						<td>${userNode.name}</td>
						<td>${userNode.description}</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:if>
	<div class="button-group">
		<form:form>
			<input type="submit" name="_eventId_inviteNode" value="<fmt:message key='my-nodes.inviteNode'/>" />
		</form:form>
	</div>
</div>
<div class="intro">
	You have ${fn:length(pendingUserNodeConfirmationsList)} pending invitations.
</div>
<c:if test="${fn:length(pendingUserNodeConfirmationsList) > 0}">
	<div id="pending">
		<table>
			<thead>
				<tr>
					<th><fmt:message key="user.nodeconf.nodeId.label"/></th>
					<th><fmt:message key="user.nodeconf.created.label"/></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${pendingUserNodeConfirmationsList}" var="userNodeConf">
					<tr>
						<td>${userNodeConf.nodeId}</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${userNodeConf.created}"
									 pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</div>
</c:if>

