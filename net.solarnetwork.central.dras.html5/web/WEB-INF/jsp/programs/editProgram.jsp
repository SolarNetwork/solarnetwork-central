<div class="ui-widget-header ui-corner-all">
	Edit Program
</div>

<form id="editProgramForm" method="POST">
	<input type="hidden" name="program.id"/>
	
	<table>
		<tr><td>Created: </td><td class="programCreated"></td></tr>
		<tr><td>Name: </td><td><input type="text" name="program.name" class="required"/></td></tr>
		
		<tr><td></td><td><input id="saveProgram" type="submit" value="Save"></td></tr>
	</table>
	
	<div><h4 class="title">Participants</h4><button id="newParticipantButton" type="button" class="titleButton">New Participant</button><div class="clear"></div></div>
		
	<div class="participantList">
		<table id="participantTable" class="display eventTable">
			<tbody></tbody>
		</table>
	</div>
	
	<div><h4 class="title">Groups</h4><button id="newParticipantGroupButton" type="button" class="titleButton">New Group</button><div class="clear"></div></div>
		
	<div class="participantList">
		<table id="participantGroupsTable" class="display eventTable">
			<tbody></tbody>
		</table>
	</div>
	
</form>

