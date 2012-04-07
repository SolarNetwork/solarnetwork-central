<form id="newParticipantForm" method="POST">
	<%-- We just default to the current user but at in some cases we'll want to provide an editable dropdown. --%>
	<input type="hidden" name="participant.userId"/>
	<input type="hidden" name="participant.locationId"/>
	
	<table class="dialogTable">
		<tr><td class="dialogTableLabel">User: </td><td><sec:authentication property="principal.username"/></td></tr>
	</table>

</form>

<jsp:include page="/WEB-INF/jsp/participants/participantCapabilityForm.jsp">
	<jsp:param name="formId" value="newParticipantCapabilityForm"/>
</jsp:include>

<jsp:include page="/WEB-INF/jsp/participants/locationForm.jsp">
	<jsp:param name="formId" value="newParticipantLocationForm"/>
</jsp:include>


<button type="button" id="createParticipantButton"><fmt:message key="program.participantDialog.create"/></button>
