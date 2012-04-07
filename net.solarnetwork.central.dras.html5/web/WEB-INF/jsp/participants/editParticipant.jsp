<form id="editParticipantForm" method="POST">
	<%-- We just default to the current user but at in some cases we'll want to provide an editable dropdown. --%>
	<input type="hidden" name="participant.userId"/>
	<input type="hidden" name="participant.locationId"/>
	<input type="hidden" name="participant.id"/>
	
	<table class="dialogTable">
		<%-- TODO look up and display user name --%>
		<tr><td class="dialogTableLabel">Created: </td><td class="participantCreated"></td></tr>
	</table>
	
</form>

<jsp:include page="/WEB-INF/jsp/participants/participantCapabilityForm.jsp">
	<jsp:param name="formId" value="editParticipantCapabilityForm"/>
</jsp:include>

<jsp:include page="/WEB-INF/jsp/participants/locationForm.jsp">
	<jsp:param name="formId" value="editParticipantLocationForm"/>
</jsp:include>


<button type="button" id="editParticipantButton"><fmt:message key="program.participantDialog.edit"/></button>
