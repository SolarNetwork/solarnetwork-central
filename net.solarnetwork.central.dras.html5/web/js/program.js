SolarNetwork.DRAS.ProgramHelper = function(config) {
	config = typeof config === 'object' ? config : {};

	var me = this;
	var programContext = undefined;
	var participantContext = undefined;
	var locationContext = undefined;
	var i18n = undefined;
	
	var init = function(cfg) {
		programContext = typeof cfg.programContext === 'string' ? cfg.programContext : undefined;
		participantContext = typeof cfg.participantContext === 'string' ? cfg.participantContext : undefined;
		locationContext = typeof cfg.locationContext === 'string' ? cfg.locationContext : undefined;
		i18n = typeof cfg.i18n === 'object' ? cfg.i18n : undefined;
	};
		
	this.programUrl = function(path) {
		return programContext + path;
	};
		
	this.participantUrl = function(path) {
		return participantContext + path;
	};
		
	this.locationUrl = function(path) {
		return locationContext + path;
	};
	
	this.populateProgram = function(programFormId, program) {
		SolarNetwork.debug('populating program %s into form %s', program.id, programFormId);

		$(programFormId +' input[name="program.id"]').val(program.id);
		$(programFormId +' input[name="program.name"]').val(program.name);
		$(programFormId +' .programCreated').html(program.created);
	};
	
	this.populateLocation = function(locationFormId, location) {
		SolarNetwork.debug('populating location %s for into form %s', location.id, locationFormId);
		$(locationFormId +' input[name="id"]').val(location.id);
		$(locationFormId +' input[name="name"]').val(location.name);
		$(locationFormId +' input[name="icp"]').val(location.icp);
		$(locationFormId +' input[name="gxp"]').val(location.gxp);
		$(locationFormId +' input[name="latitude"]').val(location.latitude);
		$(locationFormId +' input[name="longitude"]').val(location.longitude);
		$(locationFormId +' input[name="locality"]').val(location.locality);
		$(locationFormId +' input[name="stateOrProvince"]').val(location.stateOrProvince);
		$(locationFormId +' input[name="region"]').val(location.region);
		$(locationFormId +' input[name="postalCode"]').val(location.postalCode);
		$(locationFormId +' input[name="country"]').val(location.country);
	};
	
	this.populateParticipant = function(participantFormId, participantLocationFormId, participantCapabilityFormId, participantInfo) {
		SolarNetwork.debug('populating participant %s into form %s', participantInfo.participant.id, participantFormId);
		
		$(participantFormId +' input[name="participant.id"]').val(participantInfo.participant.id);
		$(participantFormId +' input[name="participant.locationId"]').val(participantInfo.participant.locationId);
		$(participantFormId +' input[name="participant.userId"]').val(participantInfo.participant.userId);
		$(participantFormId +' .participantCreated').html(participantInfo.participant.created);
		// Maybe we should look up and display user name.
		
		$(participantCapabilityFormId +' input[name="participant.id"]').val(participantInfo.participant.id);
		if (participantInfo.capability) {
			$(participantCapabilityFormId +' input[name="capability.shedCapacityWattHours"]').val(participantInfo.capability.shedCapacityWattHours);
			$(participantCapabilityFormId +' input[name="capability.shedCapacityWatts"]').val(participantInfo.capability.shedCapacityWatts);
			$(participantCapabilityFormId +' input[name="capability.generationCapacityWatts"]').val(participantInfo.capability.generationCapacityWatts);
		}
		
		this.populateLocation(participantLocationFormId, participantInfo.location);
	};
	
	this.populateParticipantGroup = function(participantGroupFormId, participantGroupLocationFormId, participantGroupCapabilityFormId, participantGroupInfo) {
		SolarNetwork.debug('populating participant %s into form %s', participantGroupInfo.id, participantGroupFormId);
		
		$(participantGroupFormId +' input[name="participantGroup.id"]').val(participantGroupInfo.id);
		$(participantGroupFormId +' input[name="participantGroup.name"]').val(participantGroupInfo.name);
		$(participantGroupFormId +' input[name="participantGroup.locationId"]').val(participantGroupInfo.locationId);
		$(participantGroupFormId +' .participantGroupCreated').html(participantGroupInfo.created);
		// Maybe we should look up and display user name.
		
		// FIXME ideally we want the location passed in as part of a participantGroupInfo that has participantGroup property as well.
		$.getJSON(this.locationUrl('/findLocations.json?simpleFilter.ids='+participantGroupInfo.locationId), function(data) {
			SolarNetwork.debug('Loading location %s for group %s', data.result[0].id, participantGroupInfo.id);
			SolarNetwork.debug(data.result[0]);
			me.populateLocation(participantGroupLocationFormId, data.result[0]);
		});
		
		// Load the capabilities
		$(participantGroupCapabilityFormId + ' input[name="participantGroup.id"]').val(participantGroupInfo.id);
		if (participantGroupInfo.capability) {
			$(participantGroupCapabilityFormId +' input[name="capability.shedCapacityWattHours"]').val(participantGroupInfo.capability.shedCapacityWattHours);
			$(participantGroupCapabilityFormId +' input[name="capability.shedCapacityWatts"]').val(participantGroupInfo.capability.shedCapacityWatts);
			$(participantGroupCapabilityFormId +' input[name="capability.generationCapacityWatts"]').val(participantGroupInfo.capability.generationCapacityWatts);
		}
	};
	
	/**
	 * @param programTableId The JQ id of the table to add the program to. e.g. '#programTable'
	 * @param programData The list of program objects to add to the table
	 */
	this.initProgramTable = function(programTableId, programData) {
		var table = $(programTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI": true, 
			"sPaginationType": "full_numbers",
			"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
					//$(nRow).addClass('gradeA');
					return nRow;
					}	
		});
		table.fnClearTable();
		
		SolarNetwork.DRAS.addItemsToTable(programTableId, programData, 'program');
	};

	this.loadProgramTable = function (programTableId, callback) {
		$.getJSON(this.programUrl('/findPrograms.json'), function(data) {
			me.initProgramTable(programTableId, data.result);
			if (callback) {
				callback();
			}
		});
	};

	this.loadProgram = function(programId) {
		SolarNetwork.debug('Loading details for program: %d', programId);

		$.getJSON(this.programUrl('/program.json?programId='+programId), function(data) {
			me.populateProgram('#editProgramForm', data.result);
		});
		
		this.loadProgramParticipantsTable('#participantTable', programId);
		this.loadProgramParticipantGroupsTable('#participantGroupsTable', programId);
	};
	
	this.loadProgramParticipantsTable = function(participantsTableId, programId) {
		SolarNetwork.debug('Loading participants for program %d', programId);
		SolarNetwork.DRAS.initEventParticipantTable(participantsTableId);
		$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.programId='+programId), function(data) {
			SolarNetwork.DRAS.addItemsToTable(participantsTableId, data.result, 'programParticipant');
		});
	};
	
	this.loadProgramParticipantGroupsTable = function(participantGroupsTableId, programId) {
		SolarNetwork.debug('Loading groups for program %d', programId);
		SolarNetwork.DRAS.initEventGroupTable(participantGroupsTableId);
		/* Since groups can't be assigned to programs then there's no point in filtering by program &simpleFilter.programId='+programId*/
		$.getJSON(this.participantUrl('/findParticipantGroups.json?simpleFilter.includeCapability=true'), function(data) {
			SolarNetwork.DRAS.addItemsToTable(participantGroupsTableId, data.result, 'programGroup');
		});
	};
	
	this.setupProgramsPage = function(programTableId, currentUserId) {
		
		$('#newProgramButton').click(function() {
			SolarNetwork.DRAS.showCreatePanel('#newProgramPanel', '#editProgramPanel');
		});
		
		programHelper.loadProgramTable('#programTable', function(){
			// Call the link on the first entry to load the program details
			SolarNetwork.debug('Loading program: ' + $('#programTable td:first a').attr('href'));
			eval($('#programTable td:first a').attr('href'));
		});
		
		// Setup the new Program form
		$("#newProgramForm").validate();
		$('#newProgramForm').ajaxForm({
			url : this.programUrl('/admin/addProgram.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				if ( !$("#newProgramForm").valid() ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Created new Program (%s): %s', status, data);
				SolarNetwork.DRAS.addItemToTable(programTableId, data.result, 'program');
				me.loadProgram(data.result.id);
				SolarNetwork.DRAS.showEditPanel('#newProgramPanel', '#editProgramPanel');
				$('#newProgramForm')[0].reset();
			}
		});
		
		// Setup the edit Program form
		$("#editProgramForm").validate();
		$('#editProgramForm').ajaxForm({
			url : this.programUrl('/admin/saveProgram.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				if ( !$("#editProgramForm").valid() ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Edited existing Program (%s): %s', status, data);
				
				// TODO Would be a lot nicer to only refresh relevant row rather than entire table
				programHelper.loadProgramTable('#programTable');
				
				$('#editProgramForm')[0].reset();
				me.loadProgram(data.result.id);
				SolarNetwork.DRAS.showEditPanel('#newProgramPanel', '#editProgramPanel');
			}
		});
		
		// Setup the dialog for creating participants
		this.setupNewParticipantDialog(currentUserId);
		
		// Setup the dialog for creating groups
		this.setupNewParticipantGroupDialog(currentUserId);
		
		// Set up the dialog for editing participants
		this.setupEditParticipantDialog();
		
		// Set up the dialog for editing groups
		this.setupEditParticipantGroupDialog();
	};
	
	this.setupNewParticipantDialog = function(currentUserId) {
		
		// Set up the button to show the new participant dialog
		$('#newParticipantButton').click(function() {
			// Set the current user as the owner
			$('#newParticipantDialog input[name="participant.userId"]').val(currentUserId);
			
			// Display the dialog
			$('#newParticipantDialog').dialog({"modal" :true, "draggable": false, "title" : "Create Participant", "resizable" : false, "minWidth": 1000, "minHeight": 600});
		});
		
		// Set up the new participant forms
		$('#newParticipantLocationForm').ajaxForm({
			url : this.locationUrl('/admin/addLocation.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				var form1Valid = $("#newParticipantForm").valid();
				var form2Valid = $("#newParticipantLocationForm").valid();
				if ( !(form1Valid && form2Valid) ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Participant location %s created, now creating participant', data.result.id);
				$('#newParticipantForm input[name="participant.locationId"]').val(data.result.id);
				// Submit the new participant form now that we've created the location
				$('#newParticipantForm').submit();
			}
		});
		$('#newParticipantForm').ajaxForm({
			url : this.participantUrl('/admin/addParticipant.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Created participant %s', data.result.id);
				// Assign participant to current program
				var programId = $('#editProgramForm input[name="program.id"]').val();
				SolarNetwork.debug('Adding participant %s to program %s', data.result.id, programId);
				$.post(me.programUrl('/admin/assignParticipantMembers.json'),
						{mode : 'Append', parentId : programId, group : data.result.id},
						'json'
				);
						
				
				$.getJSON(me.participantUrl('/capableParticipant.json?participantId=' + data.result.id), function(data) {
					SolarNetwork.DRAS.addItemToTable('#participantTable', data.result, 'programParticipant');
				});
				
				// Save capabilities
				$('#newParticipantCapabilityForm  input[name="participant.id"]').val(data.result.id);
				$('#newParticipantCapabilityForm').submit();
				
				$('#newParticipantDialog').dialog('close');
				$('#newParticipantLocationForm')[0].reset();
				$('#newParticipantForm')[0].reset();
			}
		});
		$('#newParticipantCapabilityForm').ajaxForm({
			url : this.participantUrl('/admin/saveParticipantCapability.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			}
		});
		
		// Set up the create participant button
		$('#createParticipantButton').click(function()  {
			// Create the location
			$('#newParticipantLocationForm').submit();
		});
	};
	
	this.showEditParticipantDialog = function(participantId) {
		$.getJSON(this.participantUrl('/capableParticipant.json?participantId=' + participantId), function(data) {
			// Populate the dialog with the participant details
			me.populateParticipant('#editParticipantForm','#editParticipantLocationForm', '#editParticipantCapabilityForm', data.result);
	
			// Display the dialog
			$('#editParticipantDialog').dialog({"modal" :true, "draggable": false, "title" : "Edit Participant", "resizable" : false, "minWidth": 1000, "minHeight": 600});
		});
	};
	
	this.setupEditParticipantDialog = function() {
		
		// Set up the edit participant forms
		$('#editParticipantLocationForm').ajaxForm({
			url : this.locationUrl('/admin/saveLocation.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				var form1Valid = $("#editParticipantForm").valid();
				var form2Valid = $("#editParticipantLocationForm").valid();
				if ( !(form1Valid && form2Valid) ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Participant location %s updated, now updating participant', data.result.id);
				$('#editParticipantForm input[name="participant.locationId"]').val(data.result.id);
				// Submit the edit participant form now that we've created the location
				$('#editParticipantForm').submit();
			}
		});
		$('#editParticipantForm').ajaxForm({
			url : this.participantUrl('/admin/saveParticipant.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Edited participant %s', data.result.id);
				// FIXME Update just the row rather than the entire table
				me.loadProgramParticipantsTable('#participantTable', $('#editProgramForm input[name="program.id"]').val());
				
				$('#editParticipantCapabilityForm').submit();
				
				$('#editParticipantDialog').dialog('close');
				$('#editParticipantLocationForm')[0].reset();
				$('#editParticipantForm')[0].reset();
			}
		});
		$('#editParticipantCapabilityForm').ajaxForm({
			url : this.participantUrl('/admin/saveParticipantCapability.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			}
		});
		
		// Set up the create participant button
		$('#editParticipantButton').click(function()  {
			// Update the location which in turn will update the participant
			$('#editParticipantLocationForm').submit();
		});
	};

	this.setupNewParticipantGroupDialog = function(currentUserId) {
		
		// Set up the button to show the new participantGroup dialog
		$('#newParticipantGroupButton').click(function() {
			// Set the current user as the owner
			$('#newParticipantGroupDialog input[name="participantGroup.userId"]').val(currentUserId);
			
			// Display the dialog
			$('#newParticipantGroupDialog').dialog({"modal" :true, "draggable": false, "title" : "Create Group", "resizable" : false, "minWidth": 1000, "minHeight": 600});

			// Set up the map tabs
			me.loadGroupDialogTabs('#newParticipantGroupDialog', 'newGroupMapCanvis', undefined, '#newGroupParticipantTable', '#newGroupMapTable');
		});
		
		$("#newParticipantGroupDialog .findParticipantsButton").click(function() {
			var searchCriteria = {
				programId : $('#editProgramForm input[name="program.id"]').val()
			};
			me.findParticipants(searchCriteria, $('#newParticipantGroupDialog').data("map"), '#newGroupParticipantTable', '#newGroupMapTable');
		});
		
		// Set up the new participantGroup forms
		$('#newParticipantGroupLocationForm').ajaxForm({
			url : this.locationUrl('/admin/addLocation.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				var form1Valid = $("#newParticipantGroupForm").valid();
				var form2Valid = $("#newParticipantGroupLocationForm").valid();
				if ( !(form1Valid && form2Valid) ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Group location %s created, now creating group', data.result.id);
				$('#newParticipantGroupForm input[name="participantGroup.locationId"]').val(data.result.id);
				// Submit the new group form now that we've created the location
				$('#newParticipantGroupForm').submit();
			}
		});
		$('#newParticipantGroupForm').ajaxForm({
			url : this.participantUrl('/admin/addParticipantGroup.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Created group %s', data.result.id);
				// We are not assigning groups to programs
				
				// Save the capabilities
				$('#newParticipantGroupCapabilityForm  input[name="participantGroup.id"]').val(data.result.id);
				$('#newParticipantGroupCapabilityForm').submit();
				
				// Assign participants to groups
				me.addParticipantsToGroup('#newParticipantGroupMembersForm', '#newGroupParticipantTable', data.result.id);
				
				$('#newParticipantGroupDialog').dialog('close');
				$('#newParticipantGroupForm')[0].reset();
				$('#newParticipantGroupLocationForm')[0].reset();
			}
		});
		$('#newParticipantGroupCapabilityForm').ajaxForm({
			url : this.participantUrl('/admin/saveParticipantGroupCapability.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {

//				$.getJSON(me.participantUrl('/participantGroup.json?participantGroupId=' + data.result.id), function(data) {
//					SolarNetwork.DRAS.addItemToTable('#participantGroupTable', data.result, 'programGroup');
//				});
				// FIXME use something like above to add to table rather than refreshing the entire thing
				me.loadProgramParticipantGroupsTable('#participantGroupsTable');
			}
		});
		$('#newParticipantGroupMembersForm').ajaxForm({
			url : this.participantUrl('/admin/assignParticipantGroupMembers.json'),
			dataType : 'json',
			traditional : true,
			success : function(data, status) {
			}
		});
		
		// Set up the create group button
		$('#createParticipantGroupButton').click(function()  {
			// Create the location
			$('#newParticipantGroupLocationForm').submit();
		});
	};
	
	this.showEditParticipantGroupDialog = function(participantGroupId) {
		$.getJSON(this.participantUrl('/participantGroup.json?participantGroupId=' + participantGroupId), function(data) {
			// Populate the dialog with the participantGroup details
			me.populateParticipantGroup('#editParticipantGroupForm','#editParticipantGroupLocationForm', '#editParticipantGroupCapabilityForm', data.result);
	
			// Display the dialog
			$('#editParticipantGroupDialog').dialog({"modal" :true, "draggable": false, "title" : "Edit Group", "resizable" : false, "minWidth": 1000, "minHeight": 600});

			// Set up the map tabs
			me.loadGroupDialogTabs('#editParticipantGroupDialog', 'editGroupMapCanvis', participantGroupId, '#editGroupParticipantTable', '#editGroupMapTable'); 
		});
	};

	this.setupEditParticipantGroupDialog = function() {
		
		$("#editParticipantGroupDialog .findParticipantsButton").click(function() {
			var searchCriteria = {
				programId : $('#editProgramForm input[name="program.id"]').val()
			};
			me.findParticipants(searchCriteria, $('#editParticipantGroupDialog').data("map"), '#editGroupParticipantTable', '#editGroupMapTable');
		});
		
		// Set up the edit participantGroup forms
		$('#editParticipantGroupLocationForm').ajaxForm({
			url : this.locationUrl('/admin/saveLocation.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				var form1Valid = $("#editParticipantGroupForm").valid();
				var form2Valid = $("#editParticipantGroupLocationForm").valid();
				if ( !(form1Valid && form2Valid) ) {
					return false;
				}
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Group location %s edited, now editing group', data.result.id);
				
				// Submit the edit group form now that we've created the location
				$('#editParticipantGroupForm').submit();
			}
		});
		$('#editParticipantGroupForm').ajaxForm({
			url : this.participantUrl('/admin/saveParticipantGroup.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Edited group %s', data.result.id);
				
				// Save the capabilities
				$('#editParticipantGroupCapabilityForm').submit();
				
				// Assign participants to groups
				me.addParticipantsToGroup('#editParticipantGroupMembersForm', '#editGroupParticipantTable', data.result.id);
				
				$('#editParticipantGroupDialog').dialog('close');
				$('#editParticipantGroupForm')[0].reset();
				$('#editParticipantGroupLocationForm')[0].reset();
			}
		});
		$('#editParticipantGroupCapabilityForm').ajaxForm({
			url : this.participantUrl('/admin/saveParticipantGroupCapability.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				// FIXME Update just the row rather than refreshing the entire thing
				me.loadProgramParticipantGroupsTable('#participantGroupsTable');
			}
		});
		$('#editParticipantGroupMembersForm').ajaxForm({
			url : this.participantUrl('/admin/assignParticipantGroupMembers.json'),
			dataType : 'json',
			traditional : true,
			success : function(data, status) {
			}
		});
		
		// Set up the create group button
		$('#editParticipantGroupButton').click(function()  {
			// Edit the location which will in turn call editing the group
			$('#editParticipantGroupLocationForm').submit();
		});
	};
	
	/**
	 * This populates the form with  
	 * newParticipantGroupMembersForm
	 */
	this.addParticipantsToGroup = function(newParticipantGroupMembersFormId, participantTableId, groupId) {
		$(newParticipantGroupMembersFormId).html('<input type="hidden" name="mode" value="Replace"><input type="hidden" name="parentId" value="' + groupId + '">');// Clear the contents of the form
		$(participantTableId + " .participantRow").each(function(index) {
			SolarNetwork.debug('Adding participant %s to group %s', $(this).attr('participantId'), groupId);
			$(newParticipantGroupMembersFormId).append('<input type="hidden" name="group" value="' + $(this).attr('participantId') + '">');
		});
		$(newParticipantGroupMembersFormId).submit();
	};
	
	/**
	 * Sets up the tabs for displaying the participants loads the data.
	 * 
	 * @param groupDialogId The JQ id of the group dialog.
	 * @param mapCanvasId The DOM id of the map div.
	 * @param groupId The id of the group to load
	 * @return an object with the map and latLngBounds that were used when dispaying the map.
	 */
	this.loadGroupDialogTabs = function(groupDialogId, mapCanvasId, groupId, participantsTableId, mapTableId) {
		// Setup the participant tabs
		$('.groupParticipantTabs').tabs({selected: 0});

		// Clear the tables
		this.initGroupParticipantTable(participantsTableId);
		this.initGroupMapTable(participantsTableId, mapTableId);

		// Setup and display the map
		var map = mapHelper.initialise(mapCanvasId);

		// Store the map on the dialog so that it can be used
		$(groupDialogId).data("map", map);

		// Display the group members on the map
		var latLngBounds = new GLatLngBounds();
		if (groupId) {
			$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.groupId='+groupId), function(data) {
				SolarNetwork.debug('Loading participants for group %s into %s', groupId, participantsTableId);
				
				// Load the participant data
				SolarNetwork.DRAS.addItemsToTable(participantsTableId, data.result, 'programParticipant');
				$('.groupParticipantCount').html($(participantsTableId + ' .dataTables_empty')[0] ? 0 : $(participantsTableId + ' tr').length - 1);

				SolarNetwork.DRAS.addItemsToTable(mapTableId, data.result, 'groupParticipantMap');
				mapHelper.addMembersToEventMap('Participant', data.result, map, latLngBounds);
			});
		}
	};
	
	/**
	 * @param groupMapTableId The JQ id of the table to add the group to. e.g. '#groupTable'
	 */
	this.initGroupMapTable = function(participantsTableId, groupMapTableId) {
		SolarNetwork.debug('Loading group map table %s',  groupMapTableId);
		var table = $(groupMapTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI" : true,
			"bAutoWidth" : false,
			"aoColumns": [{ bVisible : false}, 
			              { sWidth : "50%", sTitle : i18n.msg('name') }, 
			              { sWidth : "25%", sTitle : i18n.msg("capability.shedCapacityWatts") }, 
			              { sWidth : "25%", sTitle : i18n.msg("capability.shedCapacityWattHours") }, 
			              { sWidth : "10%" }],
			"fnRowCallback": function (nRow, aData, iDisplayIndex) {
					return me.groupMapTableRowCallback(participantsTableId, nRow, aData, iDisplayIndex);
				}	
				
		});
		table.fnClearTable();
	};
	
	this.groupMapTableRowCallback = function(participantsTableId, nRow, aData, iDisplayIndex) {
		// Check to see if the item is selected or not
		var participantsData = $(participantsTableId).dataTable().fnGetData();
		
		var selected = false;
		for (var i = 0; i < participantsData.length; i++ ) {
			if (participantsData[i][0] == aData[0]) {
				selected = true;
				break;
			}
		}
		// Update the check box
		if (selected) {
			nRow.childNodes[3].innerHTML = '<input type="checkbox" checked="checked" onclick="programHelper.toggleGroupMember(\'' + participantsTableId + '\', \'' + aData[0] + '\', this.checked)"></input>';
		} else {
			nRow.childNodes[3].innerHTML = '<input type="checkbox" onclick="programHelper.toggleGroupMember(\'' + participantsTableId + '\', \'' + aData[0] + '\', this.checked)"></input>';
		}
		return nRow;
	};
	
	this.toggleGroupMember = function(participantsTableId, id, enabled) {
		SolarNetwork.debug('toggleGroupMember item %s enabled=%s', id, enabled);

		$.getJSON(this.participantUrl('/capableParticipant.json?participantId=' + id), function(data) {
			if (enabled) {
				SolarNetwork.DRAS.addItemToTable(participantsTableId, data.result, 'programParticipant');
			} else {
				//var rowToRemove = $(participantsTableId + ' .participant' + id)[0];
				var rowToRemove = $(participantsTableId + ' tr[participantId="' + id + '"]')[0];
				$(participantsTableId).dataTable().fnDeleteRow(rowToRemove);
			}
			$('.groupParticipantCount').html($(participantsTableId + ' .dataTables_empty')[0] ? 0 : $(participantsTableId + ' tr').length - 1);
		});
	};
	
	/**
	 * @param groupMemberTableId The JQ id of the table to add the group to. e.g. '#participantTable'
	 */
	this.initGroupParticipantTable = function(groupMemberTableId) {
		SolarNetwork.debug('Loading group member table %s', groupMemberTableId);
		var table = $(groupMemberTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI": true, 
			"bAutoWidth" : false,
			"aoColumns":  [{ bVisible : false}, 
			               { sTitle : i18n.msg('name') }, 
				           { sTitle : i18n.msg("capability.shedCapacityWatts") }, 
				           { sTitle : i18n.msg("capability.shedCapacityWattHours") }],
			"fnRowCallback": function (nRow, aData, iDisplayIndex) {
				// We add the id or participant as a class of the row so we can identify it later.
				$(nRow).attr('participantId', aData[0]);
				$(nRow).addClass('participantRow');
				return nRow;
			}
		});
		table.fnClearTable();
	};
	
	/**
	 * 
	 * @param searchCriteria Map object containing the search criteria.
	 * @param map The GMap2 to add the participants to.
	 */
	this.findParticipants = function(searchCriteria, map, participantsTableId, mapTableId) {
		SolarNetwork.debug("findParticipants");

		var latLngBounds = new GLatLngBounds();
		
		// Reset the table for the search results
		$(mapTableId).dataTable().fnClearTable();
		this.initGroupMapTable(participantsTableId, mapTableId);

		// TODO loads participants based on searchCriteria
		$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.programId='+searchCriteria.programId), function(participantData) {
			SolarNetwork.DRAS.addItemsToTable(mapTableId, participantData.result, 'groupParticipantMap');
			mapHelper.addMembersToEventMap('Participant', participantData.result, map, latLngBounds);
		});

		
	};
	
	init(config);
	
};

SolarNetwork.DRAS.showProgramPanel = function(programId) {
	SolarNetwork.DRAS.showEditPanel('#newProgramPanel', '#editProgramPanel');
	programHelper.loadProgram(programId);
};

SolarNetwork.DRAS.showParticipantDialog = function(participantId) {
	programHelper.showEditParticipantDialog(participantId);
};

SolarNetwork.DRAS.showParticipantGroupDialog = function(participantGroupId) {
	programHelper.showEditParticipantGroupDialog(participantGroupId);
};
