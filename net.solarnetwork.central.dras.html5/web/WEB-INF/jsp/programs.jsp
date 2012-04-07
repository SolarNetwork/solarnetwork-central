<div>
<h1 class="title"><fmt:message key="programs.title"/></h1><button id="newProgramButton" type="button" class="titleButton"><fmt:message key="program.new"/></button>
<div class="clear"></div>
</div>

<div class="leftPanel"><div class="panel ui-corner-all">
	<div class="programList">
		<table id="programTable" class="display">
			<thead><tr><th><fmt:message key="program.table.name"/></th></tr></thead>
			<tbody></tbody>
		</table>
	</div>
</div></div>

<div class="rightPanel"><div class="panel ui-corner-all">

	<div id="newProgramPanel" class="programDetails">
		<jsp:include page="programs/newProgram.jsp"></jsp:include>
	</div>

	<div id="editProgramPanel" class="programDetails" style="display:none">
		<jsp:include page="programs/editProgram.jsp"></jsp:include>
	</div>
</div></div>

<div class="clear"></div>

<div id="newParticipantDialog" style="display:none">
	<jsp:include page="/WEB-INF/jsp/participants/newParticipant.jsp"></jsp:include>
</div>

<div id="editParticipantDialog" style="display:none">
	<jsp:include page="/WEB-INF/jsp/participants/editParticipant.jsp"></jsp:include>
</div>

<div id="newParticipantGroupDialog" style="display:none">
	<jsp:include page="/WEB-INF/jsp/participants/newParticipantGroup.jsp"></jsp:include>
</div>
	
<div id="editParticipantGroupDialog" style="display:none">
	<jsp:include page="/WEB-INF/jsp/participants/editParticipantGroup.jsp"></jsp:include>
</div>

<div id='mapParticipantInfo' style="display:none"></div>

<script type="text/javascript">

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


var programHelper = new SolarNetwork.DRAS.ProgramHelper({
	programContext : "/solardras/u/pro", 
	participantContext: "/solardras/u/part", 
	locationContext :"/solardras/u/loc",
	i18n : i18n
	});

var loaded = 0;
var handleLoaded = function() {
	loaded++;
	if ( loaded > 1 ) {
		// A better way of getting the user id would be good, ideally load and store when they log in
		$.getJSON(userHelper.userUrl('/findUsers.json?simpleFilter.uniqueId=<sec:authentication property="principal.username"/>'), function(data) {
			programHelper.setupProgramsPage('#programTable', data.result[0].id);
		});
	}
};
$(document).ready(function() {
	handleLoaded();
});

</script>