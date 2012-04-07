SolarNetwork.DRAS.ObserverHelper = function(config) {
	config = typeof config === 'object' ? config : {};

	var me = this;
	var eventContext = undefined;
	var programContext = undefined;
	var participantContext = undefined;
	
	this.init = function(config) {
		eventContext = typeof config.eventContext === 'string' ? config.eventContext : undefined;
		programContext = typeof config.programContext === 'string' ? config.programContext : undefined;
		participantContext = typeof config.participantContext === 'string' ? config.participantContext : undefined;
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
		
	this.loadProgramSelect = function (programSelectorId, callback) {
		$.getJSON(this.programUrl('/findPrograms.json'), function(data) {// TODO constrain programs to user
			SolarNetwork.DRAS.loadDropDown(programSelectorId, data.result);
			if (callback) {
				callback();
			}
		});
	};
	
	/**
	 * @param eventTableId The JQ id of the table to add the event to. e.g. '#eventTable'
	 * @param eventData The list of event objects to add to the table
	 */
	this.loadEventTable = function(eventTableId, eventData) {
		var table = $(eventTableId).dataTable({
			"bRetrieve" : true,
			"aaSorting": [[ 1, "desc" ]],
			"bJQueryUI": true, 
			"sPaginationType": "full_numbers"
		});
		table.fnClearTable();
		
		SolarNetwork.DRAS.addItemsToTable(eventTableId, eventData, 'event');
	};

	this.loadProgramEventsTable = function (eventTableId, programId) {
		SolarNetwork.debug('Loading events for program %d', programId);
		if (programId) {
			$.getJSON(this.eventUrl('/findEvents.json?simpleFilter.programId='+programId), function(data) {
				me.loadEventTable(eventTableId, data.result);
			});
		} else {
			$.getJSON(this.eventUrl('/findEvents.json'), function(data) {
				me.loadEventTable(eventTableId, data.result);
			});
		}
	};

	this.loadHomePage = function (eventTableId, eventMapCanvasId) {
		var map = mapHelper.initialise(eventMapCanvasId);

		/* TODO: This should only the upcoming/recent events, not all events.
		 */
		$.getJSON(this.eventUrl('/findEvents.json'), function(data) {
			if ( SolarNetwork.isNonEmptyArray(data.result) ) {
				me.loadEventTable(eventTableId, data.result);
				mapHelper.addEventsToMap(map, data.result, '#mapParticipantInfo');
			}
		});

	};

	/**
	 * This loads the participants and groups for an event into their respective tables.
	 * 
	 * @param participantsTableId The JQ id of the participants table in the dialog to populate.
	 * @param groupTableId The JQ id of the groups table in the dialog to populate.
	 * @param mapTableId The JQ id of the map table in the dialog to populate.
	 * @param eventId The id of the event to load the members for.
	 */
	this.loadEventMembersTables = function(participantsTableId, groupTableId, mapTableId, eventId) {
		SolarNetwork.debug('Loading participants for event %s into %s', eventId, participantsTableId);
		SolarNetwork.debug('Loading groups for event %s into %s', eventId, groupTableId);

		// Clear the tables
		SolarNetwork.DRAS.initEventParticipantTable(participantsTableId);
		SolarNetwork.DRAS.initEventGroupTable(groupTableId);
		SolarNetwork.DRAS.initEventMapTable(participantsTableId, groupTableId, mapTableId);
		
		$('.eventParticipantCount').html(0);
		$('.eventGroupCount').html(0);

		if (eventId) {
			$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.eventId='+eventId), function(data) {

				// Load the participant data
				if (data.result) {
					SolarNetwork.DRAS.addItemsToTable(participantsTableId, data.result, 'participant');
					$('.eventParticipantCount').html(data.result.length);
					
					// Load the map table
					SolarNetwork.DRAS.addItemsToTable(mapTableId, data.result, 'participantMap');
				}
			});
			$.getJSON(this.participantUrl('/findParticipantGroups.json?simpleFilter.includeCapability=true&simpleFilter.eventId='+eventId), function(data) {
				// Load the group data
				if (data.result) {
					SolarNetwork.DRAS.addItemsToTable(groupTableId, data.result, 'group');
					$('.eventGroupCount').html(data.result.length);

					// Load the map table
					SolarNetwork.DRAS.addItemsToTable(mapTableId, data.result, 'groupMap');
				}
			});
		}
	};

	/**
	 * Sets up the tabs for displaying the participants and groups in an event and loads the data.
	 * 
	 * @param eventDialogId The JQ id of the event dialog.
	 * @param mapCanvasId The DOM id of the map div.
	 * @param eventId The id of the event to load
	 * @return an object with the map and latLngBounds that were used when dispaying the map.
	 */
	this.loadEventDialogTabs = function(eventDialogId, mapCanvasId, eventId, participantsTableId, groupTableId, mapTableId) {
		// Setup the participant tabs
		$('.eventParticipantTabs').tabs({selected: 0});

		// Load the participant and group tables
		this.loadEventMembersTables(participantsTableId, groupTableId, mapTableId, eventId);

		// Setup and display the map
		var map = mapHelper.initialise(mapCanvasId);

		// Store the map on the dialog so that it can be used
		$(eventDialogId).data("map", map);

		// Display the event members on the map
		var latLngBounds = new GLatLngBounds();
		if (eventId) {
			$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.eventId='+eventId), function(data) {
				mapHelper.addMembersToEventMap('Participant', data.result, map, latLngBounds);
			});
			$.getJSON(this.participantUrl('/findParticipantGroups.json?simpleFilter.includeCapability=true&simpleFilter.eventId='+eventId), function(data) {
				mapHelper.addMembersToEventMap('Group', data.result, map, latLngBounds);
			});
		}
	};
		
	this.init(config);
};

var observerHelper = new SolarNetwork.DRAS.ObserverHelper({programContext : "/solardras/u/pro", eventContext : "/solardras/u/event", participantContext : "/solardras/u/part"});

//Override to display edit event dialog
SolarNetwork.DRAS.showEventDialog = function() {
	alert("Read only dialog to be implemented");// TODO replace with read only dialog
};

SolarNetwork.DRAS.setupHomePage = function() {
	observerHelper.loadHomePage('#eventTable', 'eventMapCanvis');
};

SolarNetwork.DRAS.setupUsersPage = function(userTable) {
	observerHelper.setupUsersPage(userTable);
};

