<p class="intro">
	<fmt:message key='auth-tokens.intro'/>
</p>

<h2>
	<fmt:message key='auth-tokens.user.title'/>
	<span class="pull-right">
		<a href="#create-user-auth-token" class="btn btn-primary" data-toggle="modal" 
			title="<fmt:message key='auth-tokens.action.create'/>">
			<i class="icon-plus icon-white"></i>
		</a>
	</span>
</h2>
<p>
	<fmt:message key='auth-tokens.user.intro'/>
</p>
<c:choose>
	<c:when test="${fn:length(userAuthTokens) eq 0}">
		<fmt:message key='auth-tokens.user.none'/>
	</c:when>
	<c:otherwise>
		<table class="table">
			<thead>
				<tr>
					<th><fmt:message key='auth-tokens.label.token'/></th>
					<th><fmt:message key='auth-tokens.label.secret'/></th>
					<th><fmt:message key='auth-tokens.label.created'/></th>
					<th><fmt:message key='auth-tokens.label.status'/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${userAuthTokens}" var="token">
				<tr>
					<td><c:out value='${token.authToken}'/></td>
					<td>${token.authSecret}</td>
					<td>
						<joda:dateTimeZone value="GMT">
							<joda:format value="${token.created}"
								 pattern="dd MMM yyyy"/> GMT
						</joda:dateTimeZone>
					</td>
					<td>
						<span class="label label-${token.status.value eq 'Active' ? 'success' : 'warning'}">
							<fmt:message key='auth-tokens.label.status.${token.status}'/>
						</span>
					</td>
					<td>
						<form class="action-user-token">
							<input type="hidden" name="id" value="${token.authToken}"/>
							<button type="button" class="btn btn-small user-token-change-status">
								<fmt:message key="auth-tokens.action.${token.status.value eq 'Active' ? 'disable' : 'enable'}"/>
							</button>
							<button type="button" class="btn btn-small user-token-delete" title="<fmt:message key='auth-tokens.action.delete'/>">
								<i class="icon-trash"></i>
							</button>
						</form>
					</td>
				</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:otherwise>
</c:choose>

<form id="create-user-auth-token" class="modal hide fade" action="<c:url value='/u/sec/auth-tokens/generateUser'/>" method="post">
 	<div class="modal-header">
 		<button type="button" class="close" data-dismiss="modal">&times;</button>
 		<h3><fmt:message key='auth-tokens.user.create.title'/></h3>
 	</div>
 	<div class="modal-body">
 		<p><fmt:message key='auth-tokens.user.create.intro'/></p>		
 	</div>
 	<div class="modal-footer">
 		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
 		<button type="submit" class="btn btn-primary">
 			<fmt:message key='auth-tokens.action.create'/>
 		</button>
 	</div>
</form>

<form id="delete-user-auth-token" class="modal hide fade" action="<c:url value='/u/sec/auth-tokens/deleteUser'/>" method="post">
 	<div class="modal-header">
 		<button type="button" class="close" data-dismiss="modal">&times;</button>
 		<h3><fmt:message key='auth-tokens.user.delete.title'/></h3>
 	</div>
 	<div class="modal-body">
 		<p><fmt:message key='auth-tokens.user.delete.intro'/></p>
 		<div class="row">
 			<label class="span2">
 				<fmt:message key='auth-tokens.label.token'/>
 			</label>
 			<div class="span3 container-token"></div>
 		</div>
 	</div>
 	<div class="modal-footer">
 		<input type="hidden" name="id" value=""/>
 		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
 		<button type="submit" class="btn btn-danger">
 			<i class="icon-trash icon-white"></i>
 			<fmt:message key='auth-tokens.action.delete'/>
 		</button>
 	</div>
</form>
