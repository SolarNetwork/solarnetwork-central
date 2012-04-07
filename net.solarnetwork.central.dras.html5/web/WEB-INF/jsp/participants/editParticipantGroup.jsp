<form id="editParticipantGroupForm" method="POST">
	<%-- We just default to the current user but at in some cases we'll want to provide an editable dropdown. --%>
	<input type="hidden" name="participantGroup.userId"/>
	<input type="hidden" name="participantGroup.locationId"/>
	<input type="hidden" name="participantGroup.id"/>
	
	<table class="dialogTable">
		<tr><td class="dialogTableLabel">Created: </td><td class="participantGroupCreated"></td></tr>
	</table>

</form>

<jsp:include page="/WEB-INF/jsp/participants/groupCapabilityForm.jsp">
	<jsp:param name="formId" value="editParticipantGroupCapabilityForm"/>
</jsp:include>

<jsp:include page="/WEB-INF/jsp/participants/locationForm.jsp">
	<jsp:param name="formId" value="editParticipantGroupLocationForm"/>
</jsp:include>

<%-- Form used to store the group members for submission --%>
<form id="editParticipantGroupMembersForm" method="POST" style="display:none"></form>

<button type="button" id="editParticipantGroupButton"><fmt:message key="program.participantGroupDialog.edit"/></button>

<div class="groupParticipantTabs">
	<ul>
		<li><a href="#editGroupMap">Map</a></li>
		<li><a href="#editGroupParticpants">Participants (<span class="groupParticipantCount">0</span>)</a></li>
	</ul>
	<div id="editGroupMap">
		<jsp:include page="/WEB-INF/jsp/participants/findParticipants.jsp">
			<jsp:param name="mapCanvasId" value="editGroupMapCanvis"/>
			<jsp:param name="mapTableId" value="editGroupMapTable"/>
		</jsp:include>
	</div>
	<div id="editGroupParticpants">
		<div class="participantList">
			<table id="editGroupParticipantTable" class="display groupTable">
				<tbody></tbody>
			</table>
		</div>
	</div>
</div>
