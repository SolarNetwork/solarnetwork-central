<%-- This expects the id of the form to be passed in as a param. --%>

<form id="${param.formId}" method="POST">
	<%-- Find location will store the resulting ID here, also used in edit mode to store the location id. --%>
	<input name="participant.id" type="hidden">
	
	<div class="leftPanel">
		<table class="dialogTable">
			<tr><td class="dialogTableLabel"><fmt:message key="capability.shedCapacityWatts"/>: </td><td><input name="capability.shedCapacityWatts" type="text" class=" required"></td></tr>
		</table>
	</div>
	
	<div class="rightPanel">
		<table class="dialogTable">
			<tr><td class="dialogTableLabel"><fmt:message key="capability.shedCapacityWattHours"/>: </td><td><input name="capability.shedCapacityWattHours" type="text" class=" required"></td></tr>
		</table>
	</div>
	
	<div class="clear"></div>
</form>
