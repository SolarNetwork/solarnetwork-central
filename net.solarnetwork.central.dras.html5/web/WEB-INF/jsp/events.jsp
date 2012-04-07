<h1><fmt:message key="events.title"/></h1>
	
<div class="panel ui-corner-all">
	
	<div class="dras-toolbar fg-toolbar ui-toolbar ui-widget-header ui-corner-all ui-helper-clearfix">
		<div class="programSelector">Program: <select id="eventProgramSelector"></select></div>
		<div class="eventCreator"><button class="ui-button-text newEventButton" id="newEventButton">New Event</button></div>
	</div>
	 
	
	<table id="eventTable" class="display">
		<thead><tr><th>Event</th><th>Date</th></tr></thead>
		<tbody></tbody>
	</table>
	
	<div id="createEventDialog" style="display:none">
		<jsp:include page="/WEB-INF/jsp/events/createEventDialog.jsp"></jsp:include>
	</div>
	
	<div id="editEventDialog" style="display:none">
		<jsp:include page="/WEB-INF/jsp/events/editEventDialog.jsp"></jsp:include>
	</div>
</div>

<div id='mapParticipantInfo' style="display:none"></div>

<script>
var i18n = new SolarNetwork.DRAS.Messages({
	context : SolarNetwork.DRAS.Config.contextMessage,
	contextUi : "<c:url value='/u/msg'/>",
	callback : function() {
			handleLoaded();
		}
	});

var operatorHelper = new SolarNetwork.DRAS.OperatorHelper({
	programContext : "/solardras/u/pro", 
	eventContext : "/solardras/u/event", 
	observerHelper : observerHelper, 
	participantContext : "/solardras/u/part",
	i18n : i18n
	});
	
var loaded = 0;
var handleLoaded = function() {
	loaded++;
	if ( loaded > 1 ) {
		SolarNetwork.DRAS.setupEventsPage();
	}
};
$(document).ready(function() {
	handleLoaded();
});
</script>