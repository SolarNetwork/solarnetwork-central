SolarNetwork.DRAS.OperatorHelper = function(config) {
	config = typeof config === 'object' ? config : {};

	var me = this;
	var eventContext = undefined;
	var programContext = undefined;
	var participantContext = undefined;
	var observerHelper = undefined;
	var i18n = undefined;
	
	 var init = function(cfg) {
		eventContext = typeof cfg.eventContext === 'string' ? cfg.eventContext : undefined;
		programContext = typeof cfg.programContext === 'string' ? cfg.programContext : undefined;
		participantContext = typeof cfg.participantContext === 'string' ? cfg.participantContext : undefined;
		i18n = typeof cfg.i18n === 'object' ? cfg.i18n : undefined;
		observerHelper = cfg.observerHelper;
	};
		
	this.eventUrl = function(path) {
		return eventContext + path;
	};
		
	this.programUrl = function(path) {
		return programContext + path;
	};
		
	this.participantUrl = function(path) {
		return participantContext + path;
	};

	this.populateEvent = function(eventFormId, event) {
		SolarNetwork.debug('populating event %s', event);
		
		$(eventFormId +' input[name="event.programId"]').val(event.programId);
		$(eventFormId +' input[name="event.id"]').val(event.id);
		$(eventFormId +' input[name="eventCreated"]').html(event.created);
		$(eventFormId +' input[name="event.name"]').val(event.name);
		$(eventFormId +' input[name="eventDate"]').val(event.eventDate);
		$(eventFormId +' input[name="endDate"]').val(event.endDate);
		$(eventFormId +' input[name="notificationDate"]').val(event.notificationDate);
	};
	
	this.showEditEventDialog = function(eventId) {
		// Load the event into the form
		$.getJSON(this.eventUrl('/event.json?eventId='+eventId), function(data) {
			me.populateEvent('#editEventForm', data.result);
		});
		
		// Display the dialog
		$('#editEventDialog input[name="event.programId"]').val($("#eventProgramSelector option:selected").attr('value'));
		$('#editEventDialog').dialog({"modal" :true, "draggable": false, "title" : "Edit Event: " + eventId, "resizable" : false, "minWidth": 1000, "minHeight": 600});

		// Load the event member tabs
		observerHelper.loadEventDialogTabs("#editEventDialog", 'editEventMapCanvis', eventId, '#editEventParticipantTable', '#editEventGroupTable', '#editEventMapTable');
	};
	
	this.showCreateEventDialog = function() {
		// Display the dialog
		$('#createEventDialog #programId').val($("#eventProgramSelector option:selected").attr('value'));
		$('#createEventDialog').dialog({"modal" :true, "draggable": false, "title" : "New Event", "resizable" : false, "minWidth": 1000, "minHeight": 600});

		// Load the event member tabs
		observerHelper.loadEventDialogTabs("#createEventDialog", 'createEventMapCanvis', undefined, '#createEventParticipantTable', '#createEventGroupTable', '#createEventMapTable');
	};
	
	this.setupEditControls = function() {
		// Some generic dialog setup shared by the dialogs
		//2011-01-01T12:00:00.000%2B13:00
		$(".datetimepicker" ).datetimepicker({
			dateFormat: 'yy-mm-dd',
			timeFormat: 'hh:mm:ss.000+13:00',// FIXME this timezone thing is a hack
			separator: 'T'
		});
		$('.timepicker').timepicker({
					timeFormat: 'hh:mm',
					hourGrid: 3,
					minuteGrid: 10
		});
	};
	
	this.setupEventsPage = function() {
		$('#eventProgramSelector').change(function() {
			observerHelper.loadProgramEventsTable('#eventTable', $("#eventProgramSelector option:selected").attr('value'));
		});
		// Display the new event dialog when the button is clicked
		$('.newEventButton').click(function() {
			me.showCreateEventDialog();
		});
		observerHelper.loadProgramSelect('#eventProgramSelector', function() {
			observerHelper.loadProgramEventsTable('#eventTable', $("#eventProgramSelector option:selected").attr('value'));
		});
		this.setupCreateEventDialog('#createEventDialog', '#eventTable');
		this.setupEditEventDialog('#editEventDialog', '#eventTable');
		
		this.setupEditControls();
	};
	
	/**
	 * Sets up the tabs for displaying the participants and groups in an event and loads the data.
	 * 
	 * @param eventDialogId The JQ id of the event dialog.
	 * @param mapCanvasId The DOM id of the map div.
	 * @param eventId The id of the event to load
	 */
	this.setupEventDialogTabs = function(eventDialogId, participantsTableId, groupTableId, mapTableId) {
		
		$(eventDialogId + " .findParticipantsButton").click(function() {
			// TODO pass in search critera
			var searchCriteria = { programId : $("#eventProgramSelector option:selected").attr('value') };
			
			// We expect loadEventDialogTabs to have stored the map on the dialog
			me.findParticipants(searchCriteria, $(eventDialogId).data("map"), eventDialogId, participantsTableId, groupTableId, mapTableId);
		});
		
		// Set up the tables
		this.initEventParticipantTable(participantsTableId);
		this.initEventGroupTable(groupTableId);
		this.initEventMapTable(participantsTableId, groupTableId, mapTableId);
	};
	
	this.setupEditEventDialog = function(dialogId, eventTableId) {
		$('#editEventForm').validate();
		$('#editEventForm').ajaxForm({
			url : this.eventUrl('/admin/saveEvent.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				if ( !$("#editEventForm").valid() ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Edited Event (%s): %s', status, data.result.id);
				
				// TODO Would be a lot nicer to only refresh relevant row rather than entire table
				observerHelper.loadProgramEventsTable('#eventTable', $("#eventProgramSelector option:selected").attr('value'));
				
				// Add the members to the event
				me.addMembersToEvent('#editEventMembersForm', '#editEventParticipantTable', '#editEventGroupTable', data.result.id);
				
				$(dialogId).dialog('close');
				$('#editEventForm')[0].reset();
			}
		});
		$('#editEventMembersForm').ajaxForm({
			url : this.eventUrl('/admin/assignMembers.json'),
			dataType : 'json',
			traditional : true
		});
		
		// Set up the tabs
		this.setupEventDialogTabs("#editEventDialog", '#editEventParticipantTable', '#editEventGroupTable', '#editEventMapTable');
	};
	
	this.setupCreateEventDialog = function(dialogId, eventTableId) {
		
		$("#newEventForm").validate();
		$('#newEventForm').ajaxForm({
			url : this.eventUrl('/admin/addEvent.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				if ( !$("#newEventForm").valid() ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Created new Event (%s): %s', status, data.result.id);
				SolarNetwork.DRAS.addItemToTable(eventTableId, data.result, 'event');
				
				// Add the members to the event
				me.addMembersToEvent('#createEventMembersForm', '#createEventParticipantTable', '#createEventGroupTable', data.result.id);
				
				$(dialogId).dialog('close');
				$('#newEventForm')[0].reset();
			}
		});
		$('#createEventMembersForm').ajaxForm({
			url : this.eventUrl('/admin/assignMembers.json'),
			dataType : 'json',
			traditional : true
		});
		
		// Set up the tabs
		this.setupEventDialogTabs("#createEventDialog", '#createEventParticipantTable', '#createEventGroupTable', '#createEventMapTable');
	};
	
	/**
	 * 
	 * @param searchCriteria Map object containing the search criteria.
	 * @param map The GMap2 to add the participants to.
	 */
	this.findParticipants = function(searchCriteria, map, eventDialogId, participantsTableId, groupTableId, mapTableId) {
		SolarNetwork.debug("findParticipants");

		var latLngBounds = new GLatLngBounds();
		
		// Reset the table for the search results
		$(mapTableId).dataTable().fnClearTable();
		this.initEventMapTable(participantsTableId, groupTableId, mapTableId);

		if (searchCriteria.programId) {
			// TODO loads groups based on criteriaMap, for now we just get all groups
			// Groups are currently not linked to programs: &simpleFilter.programId=' + searchCriteria.programId
			$.getJSON(this.participantUrl('/findParticipantGroups.json?simpleFilter.includeCapability=true'), function(data) {
				mapHelper.addMembersToEventMap('Group', data.result, map, latLngBounds);
				SolarNetwork.DRAS.addItemsToTable(mapTableId, data.result, 'groupMap');
			});
			
			// TODO loads participants based on criteriaMap
			$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.programId=' + searchCriteria.programId), function(participantData) {
				mapHelper.addMembersToEventMap('Participant', participantData.result, map, latLngBounds);
				SolarNetwork.DRAS.addItemsToTable(mapTableId, participantData.result, 'participantMap');
			});
		} else {
			// TODO add call for home page when we don't have the program context
			// TODO loads groups based on criteriaMap, for now we just get all groups
			$.getJSON(this.participantUrl('/findParticipantGroups.json?simpleFilter.includeCapability=true'), function(data) {
				mapHelper.addMembersToEventMap('Group', data.result, map, latLngBounds);
				SolarNetwork.DRAS.addItemsToTable(mapTableId, data.result, 'groupMap');
			});
			
			// TODO loads participants based on criteriaMap
			$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true'), function(participantData) {
				mapHelper.addMembersToEventMap('Participant', participantData.result, map, latLngBounds);
				SolarNetwork.DRAS.addItemsToTable(mapTableId, participantData.result, 'participantMap');
			});
		}
		
	};
	
	/**
	 * This populates the form with the cu 
	 * newParticipantGroupMembersForm
	 */
	this.addMembersToEvent = function(eventMembersFormId, participantTableId, groupTableId, eventId) {
		$(eventMembersFormId).html('<input type="hidden" name="p.mode" value="Replace"><input type="hidden" name="g.mode" value="Replace"><input type="hidden" name="event.id" value="' + eventId + '">');// Clear the contents of the form
		
		$(participantTableId + " .participantRow").each(function(index) {
			SolarNetwork.debug('Adding participant %s to event %s', $(this).attr('participantId'), eventId);
			$(eventMembersFormId).append('<input type="hidden" name="p.group" value="' + $(this).attr('participantId') + '">');
		});
		
		$(groupTableId + " .groupRow").each(function(index) {
			SolarNetwork.debug('Adding group %s to event %s', $(this).attr('groupId'), eventId);
			$(eventMembersFormId).append('<input type="hidden" name="g.group" value="' + $(this).attr('groupId') + '">');
		});
		$(eventMembersFormId).submit();
	};
	
	this.toggleEventParticipant = function(participantsTableId, id, enabled) {
		SolarNetwork.debug('toggleEventParticipant item %s enabled=%s', id, enabled);
		$.getJSON(this.participantUrl('/capableParticipant.json?participantId=' + id), function(data) {
			if (enabled) {
				SolarNetwork.DRAS.addItemToTable(participantsTableId, data.result, 'participant');
			} else {
				var rowToRemove = $(participantsTableId + ' tr[participantId="' + id + '"]')[0];
				$(participantsTableId).dataTable().fnDeleteRow(rowToRemove);
			}
			$('.eventParticipantCount').html($(participantsTableId + ' .dataTables_empty')[0] ? 0 : $(participantsTableId + ' tr').length - 1);
		});
	};
	
	this.toggleEventGroup = function(groupsTableId, id, enabled) {
		SolarNetwork.debug('toggleEventGroup item %s enabled=%s', id, enabled);
		$.getJSON(this.participantUrl('/participantGroup.json?participantGroupId=' + id), function(data) {
			if (enabled) {
				SolarNetwork.DRAS.addItemToTable(groupsTableId, data.result, 'group');
			} else {
				var rowToRemove = $(groupsTableId + ' tr[groupId="' + id + '"]')[0];
				$(groupsTableId).dataTable().fnDeleteRow(rowToRemove);
			}
			$('.eventGroupCount').html($(groupsTableId + ' .dataTables_empty')[0] ? 0 : $(groupsTableId + ' tr').length - 1);
		});
	};
	
	/**
	 * @param eventMapTableId The JQ id of the table to add the group to. e.g. '#groupTable'
	 */
	this.initEventMapTable = function(participantsTableId, groupTableId, eventMapTableId) {
		SolarNetwork.debug('Loading event map table %s',  eventMapTableId);
		var table = $(eventMapTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI" : true,
			"bAutoWidth" : false,
			"aoColumns": [{ bVisible : false},
			              { bVisible : false},
			              { sWidth: "50%", sTitle : i18n.msg('name') }, 
			              { sWidth: "25%", sTitle : i18n.msg("capability.shedCapacityWatts") }, 
			              { sWidth: "25%", sTitle : i18n.msg("capability.shedCapacityWattHours") },
			              { sWidth: "25%" }],
			"fnRowCallback": function (nRow, aData, iDisplayIndex) {
					return me.eventMapTableRowCallback(participantsTableId, groupTableId, nRow, aData, iDisplayIndex);
				}	
				
		});
		table.fnClearTable();
	};
	
	/**
	 * @param eventMemberTableId The JQ id of the table to add the group to. e.g. '#groupTable'
	 */
	this.initEventParticipantTable = function(eventMemberTableId) {
		SolarNetwork.debug('Loading event participant table %s', eventMemberTableId);
		var table = $(eventMemberTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI": true, 
			"bAutoWidth" : false,
			"aoColumns": [{ bVisible : false}, 
			              { sTitle : i18n.msg('name') }, 
			              { sTitle : i18n.msg("capability.shedCapacityWatts") }, 
			              { sTitle : i18n.msg("capability.shedCapacityWattHours") }],
			  			"fnRowCallback": function (nRow, aData, iDisplayIndex) {
							// We add the id or participant as an attribute of the row so we can identify it later.
							$(nRow).attr('participantId', aData[0]);
							$(nRow).addClass('participantRow');
							return nRow;
						}
		});
		table.fnClearTable();
	};
	
	/**
	 * @param eventMemberTableId The JQ id of the table to add the group to. e.g. '#groupTable'
	 */
	this.initEventGroupTable = function(eventMemberTableId) {
		SolarNetwork.debug('Loading event group table %s', eventMemberTableId);
		var table = $(eventMemberTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI": true, 
			"bAutoWidth" : false,
			"aoColumns": [{ bVisible : false}, 
			              { sTitle : i18n.msg('name') }, 
			              { sTitle : i18n.msg("capability.shedCapacityWatts") }, 
			              { sTitle : i18n.msg("capability.shedCapacityWattHours") }],
			  			"fnRowCallback": function (nRow, aData, iDisplayIndex) {
							// We add the id or group as an attribute of the row so we can identify it later.
							$(nRow).attr('groupId', aData[0]);
							$(nRow).addClass('groupRow');
							return nRow;
						}
		});
		table.fnClearTable();
	};
	
	this.eventMapTableRowCallback = function(participantsTableId, groupTableId, nRow, aData, iDisplayIndex) {
		// Check to see if the item is selected or not
		var participantsData = $(participantsTableId).dataTable().fnGetData();
		var groupsData = $(groupTableId).dataTable().fnGetData();
		
		var selected = false;
		if (aData[1] == 'group') {
			for (var i = 0; i < groupsData.length; i++ ) {
				if (groupsData[i][0] == aData[0]) { 
					selected = true;
					break;
				}
			}

			// Update the check box
			if (selected) {
				nRow.childNodes[3].innerHTML = '<input type="checkbox" checked="checked" onclick="operatorHelper.toggleEventGroup(\'' + groupTableId + '\', \'' + aData[0] + '\', this.checked)"></input>';
			} else {
				nRow.childNodes[3].innerHTML = '<input type="checkbox" onclick="operatorHelper.toggleEventGroup(\'' + groupTableId + '\', \'' + aData[0] + '\', this.checked)"></input>';
			}
		} else if (aData[1] == 'participant') {
			for (var i = 0; i < participantsData.length; i++ ) {
				if (participantsData[i][0] == aData[0]) {
					selected = true;
					break;
				}
			}
			
			// Update the check box
			if (selected) {
				nRow.childNodes[3].innerHTML = '<input type="checkbox" checked="checked" onclick="operatorHelper.toggleEventParticipant(\'' + participantsTableId + '\', \'' + aData[0] + '\', this.checked)"></input>';
			} else {
				nRow.childNodes[3].innerHTML = '<input type="checkbox" onclick="operatorHelper.toggleEventParticipant(\'' + participantsTableId+ '\', \'' + aData[0] + '\', this.checked)"></input>';
			}
		}
		return nRow;
	};
	
	init(config);
};

// Override to display edit event dialog
SolarNetwork.DRAS.showEventDialog = function(eventId) {
	operatorHelper.showEditEventDialog(eventId);
};

//Override to display edit event dialog
SolarNetwork.DRAS.setupHomePage = function() {
	observerHelper.loadHomePage('#eventTable', 'eventMapCanvis');
	operatorHelper.setupEditEventDialog('#editEventDialog', '#eventTable');
	operatorHelper.setupEditControls();
};

SolarNetwork.DRAS.initEventMapTable = function(participantsTableId, groupTableId, eventMapTableId) {
	operatorHelper.initEventMapTable(participantsTableId, groupTableId, eventMapTableId);
};

SolarNetwork.DRAS.initEventParticipantTable = function(eventMemberTableId) {
	operatorHelper.initEventParticipantTable(eventMemberTableId);
};

SolarNetwork.DRAS.initEventGroupTable = function(eventMemberTableId) {
	operatorHelper.initEventGroupTable(eventMemberTableId);
};

SolarNetwork.DRAS.setupEventsPage = function() {
	operatorHelper.setupEventsPage();
};

