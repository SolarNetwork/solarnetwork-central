SolarNetwork.DRAS = {

	/**
	 * @param tableId The JQ id of the table to add the item to. e.g. '#groupTable'
	 * @param items The list of items to add to the table
	 * @param itemTemplate The key that identifies that table row template to use when adding the item to the table.
	 */
	addItemsToTable : function(tableId, items, itemTemplate) {
		if (SolarNetwork.isArray(items)) {
			$.each(items, function(idx, item) {
				SolarNetwork.DRAS.addItemToTable(tableId, item, itemTemplate);
			});
		}
	},
	
	roleDisplayName : function(role) {
		// TODO: DRAS needs to be an object, with a reference to i18n object
		var name = I18N.msg('role.'+role);
		return (name === undefined ? role : name);
	},
	
	/**
	 * Return a watt value formatted as a String.
	 * 
	 * @param watts the watt value
	 * @returns MW formatted string
	 */
	formatWatts : function(watts) {
		// display in MW
		return Number(watts / 1000).toFixed(1);
	},
	
	/**
	 * Return a watt-hour value formatted as a String.
	 * 
	 * @param wattHours the watt-hour value
	 * @returns MWh formatted string
	 */
	formatWattHours : function(wattHours) {
		// display in MW
		return Number(wattHours / 1000).toFixed(1);
	},
	
	/**
	 * @param tableId The JQ id of the table to add the item to. e.g. '#groupTable'
	 * @param item The item object to add to the table
	 * @param itemTemplate The key that identifies that table row template to use when adding the item to the table.
	 */
	addItemToTable : function(tableId, item, itemTemplate) {
		if (itemTemplate === 'group') {
			$(tableId).dataTable().fnAddData( [item.id,
			                                   item.location ? item.location.name : item.id, // FIXME API doesn't return location in all places that we want it
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : '')
			                                   ]);
		} else if (itemTemplate === 'programGroup') {
			$(tableId).dataTable().fnAddData( [item.id,
			                                   '<a href="javascript:SolarNetwork.DRAS.showParticipantGroupDialog(\'' + item.id + '\')">' + item.location.name + '</a>',
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : '')
			                                   ]);
		} else if (itemTemplate === 'participant') {
			$(tableId).dataTable().fnAddData( [item.id,
			                                   item.location.name,
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : '')
			                                   ]);
		} else if (itemTemplate === 'programParticipant') {
			$(tableId).dataTable().fnAddData( [item.id,
			                                   '<a href="javascript:SolarNetwork.DRAS.showParticipantDialog(\'' + item.id + '\')">' + item.location.name + '</a>',
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : '')
			                                   ]);
		} else if (itemTemplate === 'groupMap') {
			$(tableId).dataTable().fnAddData( [item.id ,
			                                   'group',
			                                   I18N.msg('participantGroup') + ': ' + item.location.name, 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : ''), 
			                                   '<input type="checkbox" disabled="disabled"></input>'] );
		} else if (itemTemplate === 'participantMap') {
			$(tableId).dataTable().fnAddData( [item.id ,
			                                   'participant',
			                                   I18N.msg('participant') + ': ' + item.location.name,
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : ''), 
			                                   '<input type="checkbox" disabled="disabled"></input>'] );
		} else if (itemTemplate === 'groupParticipantMap') {
			$(tableId).dataTable().fnAddData( [item.id ,
			                                   I18N.msg('participant') + ': ' + item.location.name, 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWatts(item.capability.shedCapacityWatts) : ''), 
			                                   (item.capability !== undefined ? SolarNetwork.DRAS.formatWattHours(item.capability.shedCapacityWattHours) : ''), 
			                                   '<input type="checkbox" disabled="disabled"></input>'] );
		} else if (itemTemplate === 'event') {
			$(tableId).dataTable().fnAddData( ['<a href="javascript:SolarNetwork.DRAS.showEventDialog(\'' + item.id + '\')">' + item.name + '</a>', item.eventDate] );
		} else if (itemTemplate === 'program') {
			$(tableId).dataTable().fnAddData( ['<a href="javascript:SolarNetwork.DRAS.showProgramPanel(\''+item.id+'\')">' + item.name + '</a>'] );
		} else if (itemTemplate === 'user') {
			$(tableId).dataTable().fnAddData( ['<a href="javascript:SolarNetwork.DRAS.showUserPanel(\''+item.id+'\')">' + item.username + '</a>', 
			                                   item.displayName, 
			                                   (SolarNetwork.isArray(item.roleNames) ? item.roleNames.map(function(val){
			                                	   return SolarNetwork.DRAS.roleDisplayName(val);
			                                   }).join(', ') : '')
			                                   ] );
		}
		
	},
	
	/**
	 * @param selectId The JQ id of the select input.
	 * @param items The list of items to add to the drop down, this expects them to have an id and a name property.
	 */
	loadDropDown : function(selectId, items) {
		$.each(items, function(idx, item) {
			$(selectId).append('<option value="' + item.id + '">' + item.name + '</option>');
		});
	},
	
	showEditPanel : function(createPanelId, editPanelId) {
		$(createPanelId).hide();
		$(editPanelId).show("slide");
	},
	
	showCreatePanel : function(createPanelId, editPanelId) {
		$(editPanelId).hide();
		$(createPanelId).show("slide", { direction: "right" });
	},
	
	convertTimeToPeriod : function(time) {
		var timePieces = time.split(':');
		return 'PT' + timePieces[0] + 'H' + timePieces[1] + 'M';
	},

	
	convertPeriodToTime : function(period) {
		return period;// ~TODO
	}
	
};