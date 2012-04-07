<div class="leftPanel"><div class="panel ui-corner-all">
	<div class="flatPanel">
		<input type="text" class="eventMemberSearch"/><button class="findParticipantsButton" type="button">Find Participants</button>
	</div>
	
	<div id="${param.mapCanvasId}" class="editEventMap"></div>
	
	<%--<div class="ui-widget">
		<div style="margin-top: 20px; padding: 0 .7em;" class="ui-state-highlight ui-corner-all"> 
			<p><span style="float: left; margin-right: .3em;" class="ui-icon ui-icon-info"></span>
			Group participants are not displayed on this map</p>
		</div>
	</div> --%>
	
</div></div>

<div class="rightPanel"><div class="panel ui-corner-all">

<div class="groupList">
	<table id="${param.mapTableId}" class="display eventTable">
		<tbody></tbody>
	</table>
</div>

</div></div>

<div class="clear"></div>
