<h1><fmt:message key="home.events.title"/></h1>

<div class="leftPanel"><div class="panel ui-corner-all">
	<div id="eventMapCanvis" class="eventMap"></div>
	<div id='mapParticipantInfo' style="display:none"></div>
</div></div>

<div class="rightPanel"><div class="panel ui-corner-all">
	<table id="eventTable" class="display">
		<thead><tr><th>Event</th><th>Date</th></tr></thead>
		<tbody></tbody>
	</table>
</div></div>

<div id="editEventDialog" style="display:none">
	<jsp:include page="/WEB-INF/jsp/events/editEventDialog.jsp"></jsp:include>
</div>

<div class="clear"></div>

<script type="text/javascript">var i18n = new SolarNetwork.DRAS.Messages({
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
		SolarNetwork.DRAS.setupHomePage();
	}
};
$(document).ready(function() {
	handleLoaded();
} );
</script>