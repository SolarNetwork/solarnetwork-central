<c:if test="${not empty token.policy}">
	<dl>
		<c:if test="${not empty token.policy.notAfter}">
			<dt><fmt:message key='auth-tokens.label.notAfter'/></dt>
			<dd>
				<joda:dateTimeZone value="UTC">
					<joda:format value="${token.policy.notAfter}" pattern="dd MMM yyyy"/> UTC
				</joda:dateTimeZone>
			</dd>
		</c:if>
		<c:if test="${not empty token.policy.refreshAllowed and token.policy.refreshAllowed}">
			<dt><fmt:message key='auth-tokens.label.refreshAllowed'/></dt>
			<dd>
				<fmt:message key='auth-tokens-policy-refreshAllowed.true.label'/>
			</dd>
		</c:if>
		<c:if test="${fn:length(token.policy.nodeIds) gt 0}">
			<dt><fmt:message key='auth-tokens.label.nodes'/></dt>
			<dd>
				<c:forEach items="${token.policy.nodeIds}" var="nodeId" varStatus="nodeIdStatus">
					${nodeId}<c:if test="${not nodeIdStatus.last}">, </c:if>
				</c:forEach>
			</dd>
		</c:if>
		<c:if test="${fn:length(token.policy.sourceIds) gt 0}">
			<dt><fmt:message key='auth-tokens.label.sources'/></dt>
			<dd>
				<c:forEach items="${token.policy.sourceIds}" var="sourceId" varStatus="sourceIdStatus">
					${sourceId}<c:if test="${not sourceIdStatus.last}">, </c:if>
				</c:forEach>
			</dd>
		</c:if>
		<c:if test="${not empty token.policy.minAggregation}">
			<dt><fmt:message key='auth-tokens.label.minAggregation'/></dt>
			<dd>
				<fmt:message key='aggregation.${token.policy.minAggregation}.label'/>
			</dd>
		</c:if>
		<c:if test="${fn:length(token.policy.nodeMetadataPaths) gt 0}">
			<dt><fmt:message key='auth-tokens.label.nodeMetadataPaths'/></dt>
			<dd>
				<c:forEach items="${token.policy.nodeMetadataPaths}" var="nodeMetadataPath" varStatus="nodeMetadataPathStatus">
					${nodeMetadataPath}<c:if test="${not nodeMetadataPathStatus.last}">, </c:if>
				</c:forEach>
			</dd>
		</c:if>
		<c:if test="${fn:length(token.policy.userMetadataPaths) gt 0}">
			<dt><fmt:message key='auth-tokens.label.userMetadataPaths'/></dt>
			<dd>
				<c:forEach items="${token.policy.userMetadataPaths}" var="userMetadataPath" varStatus="userMetadataPathStatus">
					${userMetadataPath}<c:if test="${not userMetadataPathStatus.last}">, </c:if>
				</c:forEach>
			</dd>
		</c:if>
		<c:if test="${fn:length(token.policy.apiPaths) gt 0}">
			<dt><fmt:message key='auth-tokens.label.apiPaths'/></dt>
			<dd>
				<c:forEach items="${token.policy.apiPaths}" var="apiPath" varStatus="apiPathStatus">
					${apiPath}<c:if test="${not apiPathStatus.last}">, </c:if>
				</c:forEach>
			</dd>
		</c:if>
	</dl>
</c:if>
