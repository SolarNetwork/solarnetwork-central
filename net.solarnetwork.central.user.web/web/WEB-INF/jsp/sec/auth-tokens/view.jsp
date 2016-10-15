<%--
	Input parameters:
	
		userAuthTokens - collection of "user" UserAuthToken objects
		dataAuthTokens - collection of "data" UserAuthToken objects
		userNodes      - collection of UserNode objects
 --%>
<p class="intro">
	<fmt:message key='auth-tokens.intro'/>
</p>

<section id="user-auth-tokens">
<h2>
	<fmt:message key='auth-tokens.user.title'/>
	<a href="#create-user-auth-token" class="btn btn-primary pull-right" data-toggle="modal" 
		title="<fmt:message key='auth-tokens.action.create'/>">
		<i class="glyphicon glyphicon-plus"></i>
	</a>
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
					<th><fmt:message key='auth-tokens.label.created'/></th>
					<th><fmt:message key='auth-tokens.label.status'/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${userAuthTokens}" var="token">
				<tr>
					<td class="monospace"><c:out value='${token.authToken}'/></td>
					<td>
						<joda:dateTimeZone value="GMT">
							<joda:format value="${token.created}"
								 pattern="dd MMM yyyy"/> GMT
						</joda:dateTimeZone>
					</td>
					<td>
						<span class="label label-${token.status eq 'Active' ? 'success' : 'warning'}">
							<fmt:message key='auth-tokens.label.status.${token.status}'/>
						</span>
					</td>
					<td>
						<form class="action-user-token">
							<c:set var="tokenId"><c:out value='${token.authToken}'/></c:set>
							<input type="hidden" name="id" value="${tokenId}"/>
							<button type="button" class="btn btn-small btn-default user-token-change-status" 
								data-status="${token.status}"
								data-action="<c:url value='/u/sec/auth-tokens/changeStatus'/>">
								<fmt:message key="auth-tokens.action.${token.status eq 'Active' ? 'disable' : 'enable'}"/>
							</button>
							<button type="button" class="btn btn-small btn-default user-token-delete" title="<fmt:message key='auth-tokens.action.delete'/>">
								<i class="glyphicon glyphicon-trash"></i>
							</button>
							<sec:csrfInput/>
						</form>
					</td>
				</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:otherwise>
</c:choose>
</section>

<c:if test="${not empty userNodes}">
	<section id="data-auth-tokens">
	<h2>
		<fmt:message key='auth-tokens.data.title'/>
		<a href="#create-data-auth-token" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='auth-tokens.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p>
		<fmt:message key='auth-tokens.data.intro'/>
	</p>
	<c:choose>
		<c:when test="${fn:length(dataAuthTokens) eq 0}">
			<fmt:message key='auth-tokens.data.none'/>
		</c:when>
		<c:otherwise>
			<table class="table">
				<thead>
					<tr>
						<th><fmt:message key='auth-tokens.label.token'/></th>
						<th><fmt:message key='auth-tokens.label.nodes'/></th>
						<th><fmt:message key='auth-tokens.label.created'/></th>
						<th><fmt:message key='auth-tokens.label.status'/></th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${dataAuthTokens}" var="token">
					<tr>
						<td class="monospace"><c:out value='${token.authToken}'/></td>
						<td>
							<c:forEach items="${token.nodeIds}" var="nodeId" varStatus="nodeIdStatus">
								${nodeId}<c:if test="${not nodeIdStatus.last}">, </c:if>
							</c:forEach>
						</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${token.created}"
									 pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
						<td>
							<span class="label label-${token.status eq 'Active' ? 'success' : 'warning'}">
								<fmt:message key='auth-tokens.label.status.${token.status}'/>
							</span>
						</td>
						<td>
							<form class="action-data-token">
								<c:set var="tokenId"><c:out value='${token.authToken}'/></c:set>
								<input type="hidden" name="id" value="${tokenId}"/>
								<button type="button" class="btn btn-small btn-default data-token-change-status" 
									data-status="${token.status}"
									data-action="<c:url value='/u/sec/auth-tokens/changeStatus'/>">
									<fmt:message key="auth-tokens.action.${token.status eq 'Active' ? 'disable' : 'enable'}"/>
								</button>
								<button type="button" class="btn btn-small btn-default data-token-delete" title="<fmt:message key='auth-tokens.action.delete'/>">
									<i class="glyphicon glyphicon-trash"></i>
								</button>
								<sec:csrfInput/>
							</form>
						</td>
					</tr>
					</c:forEach>
				</tbody>
			</table>
		</c:otherwise>
	</c:choose>
	</section>
</c:if>

<form id="create-user-auth-token" class="modal fade" action="<c:url value='/u/sec/auth-tokens/generateUser'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='auth-tokens.user.create.title'/></h4>
		 	</div>
		 	<div class="modal-body">
		 		<p class="before"><fmt:message key='auth-tokens.user.create.intro'/></p>
		 		<div class="after">
		 			<p><fmt:message key='auth-tokens.created.intro'/></p>
			 		<table class="table">
			 			<thead>
			 				<tr>
			 					<th><fmt:message key='auth-tokens.label.token'/></th>
			 					<th class="text-danger"><fmt:message key='auth-tokens.label.secret'/></th>
			 				</tr>
			 			</thead>
			 			<tbody>
			 				<tr>
			 					<td class="monospace result-token"></td>
			 					<td class="monospace result-secret text-danger"></td>
			 				</tr>
			 			</tbody>
			 		</table>
			 		<div class="alert alert-danger">
			 			<strong><fmt:message key='auth-tokens.created.reiterate.title'/></strong>
			 			<fmt:message key='auth-tokens.created.reiterate'/>
			 		</div>
		 		</div>
		 	</div>
		 	<div class="modal-footer">
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<button type="submit" class="btn btn-primary before">
		 			<fmt:message key='auth-tokens.action.create'/>
		 		</button>
		 	</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<form id="delete-user-auth-token" class="modal fade" action="<c:url value='/u/sec/auth-tokens/delete'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='auth-tokens.user.delete.title'/></h4>
		 	</div>
		 	<div class="modal-body">
		 		<p><fmt:message key='auth-tokens.user.delete.intro'/></p>
		 		<div class="row">
		 			<label class="col-sm-3">
		 				<fmt:message key='auth-tokens.label.token'/>
		 			</label>
		 			<div class="col-sm-9 container-token monospace"></div>
		 		</div>
		 	</div>
		 	<div class="modal-footer">
		 		<input type="hidden" name="id" value=""/>
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<button type="submit" class="btn btn-danger">
		 			<i class="glyphicon glyphicon-trash"></i>
		 			<fmt:message key='auth-tokens.action.delete'/>
		 		</button>
		 	</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<c:if test="${not empty userNodes}">
	<form id="create-data-auth-token" class="modal fade" action="<c:url value='/u/sec/auth-tokens/generateData'/>" method="post">
		<div class="modal-dialog">
			<div class="modal-content">
			 	<div class="modal-header">
			 		<button type="button" class="close" data-dismiss="modal">&times;</button>
			 		<h4 class="modal-title"><fmt:message key='auth-tokens.data.create.title'/></h4>
			 	</div>
			 	<div class="modal-body">
			 		<p class="before"><fmt:message key='auth-tokens.data.create.intro'/></p>
			 		<div id="create-data-auth-token-policy-nodeids" class="before toggle-buttons">
			 			<c:forEach items="${userNodes}" var="userNode" varStatus="status">
			 				<button type="button" class="toggle btn btn-sm btn-default" data-node-id="${userNode.node.id}">${userNode.node.id}<c:if test="${fn:length(userNode.name) gt 0}"> - ${userNode.name}</c:if></button>
			 			</c:forEach>
			 		</div>
			 		<div class="before">
			 			<label for="create-data-auth-token-policy-sourceids"><fmt:message key='auth-tokens.policy.sourceIds.label'/></label>
			 			<textarea id="create-data-auth-token-policy-sourceids" class="form-control" name="sourceIds" rows="2" 
			 				placeholder="<fmt:message key='auth-tokens.policy.sourceIds.placeholder'/>"></textarea>
			 			<div id="create-data-auth-token-policy-sourceids-hint" class="toggle-buttons"></div>
			 		</div>
			 		<div class="after">
			 			<p><fmt:message key='auth-tokens.created.intro'/></p>
				 		<table class="table">
				 			<thead>
				 				<tr>
				 					<th><fmt:message key='auth-tokens.label.token'/></th>
				 					<th class="text-danger"><fmt:message key='auth-tokens.label.secret'/></th>
				 				</tr>
				 			</thead>
				 			<tbody>
				 				<tr>
				 					<td class="monospace result-token"></td>
				 					<td class="monospace result-secret text-danger"></td>
				 				</tr>
				 			</tbody>
				 		</table>
				 		<div class="alert alert-danger">
				 			<strong><fmt:message key='auth-tokens.created.reiterate.title'/></strong>
				 			<fmt:message key='auth-tokens.created.reiterate'/>
				 		</div>
			 		</div>
			 	</div>
			 	<div class="modal-footer">
			 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
			 		<button type="submit" class="btn btn-primary before">
			 			<fmt:message key='auth-tokens.action.create'/>
			 		</button>
			 	</div>
			</div>
		</div>
		<sec:csrfInput/>
	</form>
	
	<form id="delete-data-auth-token" class="modal fade" action="<c:url value='/u/sec/auth-tokens/delete'/>" method="post">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal">&times;</button>
					<h4 class="modal-title"><fmt:message key='auth-tokens.data.delete.title'/></h4>
				</div>
				<div class="modal-body">
					<p><fmt:message key='auth-tokens.data.delete.intro'/></p>
					<div class="row">
						<label class="col-sm-3">
							<fmt:message key='auth-tokens.label.token'/>
						</label>
						<div class="col-sm-9 container-token monospace"></div>
					</div>
				</div>
				<div class="modal-footer">
					<input type="hidden" name="id" value=""/>
					<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
					<button type="submit" class="btn btn-danger">
						<i class="glyphicon glyphicon-trash"></i>
						<fmt:message key='auth-tokens.action.delete'/>
					</button>
				</div>
			</div>
		</div>
		<sec:csrfInput/>
	</form>
</c:if>
