<form id="newEventForm" method="POST">
	<input type="hidden" name="event.programId" value="${param.programId}" id="programId"/>
	
	<table>
		<tr><td>Name: </td><td><input type="text" name="event.name" class="required" value=""></input></td></tr>
		<tr><td>Notification Date: </td><td><input type="text" name="notificationDate" class="datetimepicker required" value=""></input></td></tr>
		<tr><td>Event Date: </td><td><input type="text" name="eventDate" class="datetimepicker required" value=""></input></td></tr>
		<tr><td>End Date: </td><td><input type="text" name="endDate" class="datetimepicker required" value=""></input></td></tr>
		<%--<tr><td>Duration: </td><td><input type="text" name="eventDuration" class="timepicker required" value=""></input></td></tr> --%>
		<tr><td></td><td><input type="submit" value="Create"></td></tr>
	</table>
	
</form>

<%-- Form used to store the group members for submission --%>
<form id="createEventMembersForm" method="POST" style="display:none"></form>
	
<div class="eventParticipantTabs">
	<ul>
		<li><a href="#createEventMap">Map</a></li>
		<li><a href="#createEventParticpants">Participants (<span class="eventParticipantCount">0</span>)</a></li>
		<li><a href="#createEventGroups">Groups (<span class="eventGroupCount">0</span>)</a></li>
	</ul>
	<div id="createEventMap">
		<jsp:include page="/WEB-INF/jsp/participants/findParticipants.jsp">
			<jsp:param name="mapCanvasId" value="createEventMapCanvis"/>
			<jsp:param name="mapTableId" value="createEventMapTable"/>
		</jsp:include>
	</div>
	<div id="createEventParticpants">
		<div class="participantList">
			<table id="createEventParticipantTable" class="display eventTable">
				<tbody></tbody>
			</table>
		</div>
	</div>
	<div id="createEventGroups">
		<div class="groupList">
			<table id="createEventGroupTable" class="display eventTable">
				<tbody></tbody>
			</table>
		</div>
	</div>
	
</div>

