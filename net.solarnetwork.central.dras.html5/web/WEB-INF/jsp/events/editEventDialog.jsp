<form id="editEventForm" method="POST">
	<input type="hidden" name="event.programId"/>
	<input type="hidden" name="event.id"/>
	
	<table>
		<tr><td>Created: </td><td id="eventCreated"></td></tr>
		<tr><td>Name: </td><td><input type="text" name="event.name" class="required" value=""></input></td></tr>
		<tr><td>Notification Date: </td><td><input type="text" name="notificationDate" class="datetimepicker required" value=""></input></td></tr>
		<tr><td>Event Date: </td><td><input type="text" name="eventDate" class="datetimepicker required" value=""></input></td></tr>
		<tr><td>End Date: </td><td><input type="text" name="endDate" class="datetimepicker required" value=""></input></td></tr>
		<tr><td></td><td><input id="updateEventButton" type="submit" value="Save"></td></tr>
	</table>
</form>

<%-- Form used to store the group members for submission --%>
<form id="editEventMembersForm" method="POST" style="display:none"></form>
	
<div class="eventParticipantTabs">
	<ul>
		<li><a href="#editEventMap">Map</a></li>
		<li><a href="#editEventParticpants">Participants (<span class="eventParticipantCount">0</span>)</a></li>
		<li><a href="#editEventGroups">Groups (<span class="eventGroupCount">0</span>)</a></li>
	</ul>
	<div id="editEventMap">
		<jsp:include page="/WEB-INF/jsp/participants/findParticipants.jsp">
			<jsp:param name="mapCanvasId" value="editEventMapCanvis"/>
			<jsp:param name="mapTableId" value="editEventMapTable"/>
		</jsp:include>
	</div>
	<div id="editEventParticpants">
		<div class="participantList">
			<table id="editEventParticipantTable" class="display eventTable">
				<tbody></tbody>
			</table>
		</div>
	</div>
	<div id="editEventGroups">
		<div class="groupList">
			<table id="editEventGroupTable" class="display eventTable">
				<tbody></tbody>
			</table>
		</div>
	</div>		
</div>
