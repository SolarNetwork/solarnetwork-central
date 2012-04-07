<form id="newParticipantGroupForm" method="POST">
	<%-- We just default to the current user but at in some cases we'll want to provide an editable dropdown. --%>
	<input type="hidden" name="participantGroup.locationId"/>
	
	<table class="dialogTable">
		<%--<tr><td class="dialogTableLabel">User: </td><td><sec:authentication property="principal.username"/></td></tr>--%>
	</table>

</form>

<jsp:include page="/WEB-INF/jsp/participants/groupCapabilityForm.jsp">
	<jsp:param name="formId" value="newParticipantGroupCapabilityForm"/>
</jsp:include>

<jsp:include page="/WEB-INF/jsp/participants/locationForm.jsp">
	<jsp:param name="formId" value="newParticipantGroupLocationForm"/>
</jsp:include>

<%-- Form used to store the group members for submission --%>
<form id="newParticipantGroupMembersForm" method="POST" style="display:none"></form>

<button type="button" id="createParticipantGroupButton"><fmt:message key="program.participantGroupDialog.create"/></button>

<div class="groupParticipantTabs">
	<ul>
		<li><a href="#newGroupMap">Map</a></li>
		<li><a href="#newGroupParticpants">Participants (<span class="groupParticipantCount">0</span>)</a></li>
	</ul>
	<div id="newGroupMap">
		<jsp:include page="/WEB-INF/jsp/participants/findParticipants.jsp">
			<jsp:param name="mapCanvasId" value="newGroupMapCanvis"/>
			<jsp:param name="mapTableId" value="newGroupMapTable"/>
		</jsp:include>
	</div>
	<div id="newGroupParticpants">
		<div class="participantList">
			<table id="newGroupParticipantTable" class="display groupTable">
				<tbody></tbody>
			</table>
		</div>
	</div>
</div>
