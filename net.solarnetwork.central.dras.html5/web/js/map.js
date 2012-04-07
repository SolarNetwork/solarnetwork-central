/**
 * SolarNetwork DRAS Map API.
 * 
 * @class SolarNetwork DRAS Map API
 * @constructor
 * @param {Object} config configuration parameters
 * @param {Number} config.nodeId the ID of the node to use
 */
SolarNetwork.DRAS.MapHelper = function(config) {
	config = typeof config === 'object' ? config : {};
	
	var me = this;
	
	var defaultZoomLevel = undefined;
	var minZoomLevel = undefined;
	var latitude = undefined;
	var longitude = undefined;

	var participantContext = undefined;
		
	this.participantUrl = function(path) {
		return participantContext + path;
	};
	
	this.init = function(cfg) {
		latitude = typeof cfg.lat === 'number' ? cfg.lat : undefined;
		longitude = typeof cfg.long === 'number' ? cfg.long : undefined;
		defaultZoomLevel = typeof cfg.defaultZoom === 'number' ? cfg.defaultZoom : undefined;
		minZoomLevel = typeof cfg.minZoom == 'number' ? cfg.minZoom : undefined;
		participantContext = typeof config.participantContext === 'string' ? config.participantContext : undefined;
	};
		
	/**
	 * Sets up the map canvas.
	 * 
	 * @param canvasId DOM id of the div being used for the map.
	 */
	this.initialise = function(canvasId) {
      if (GBrowserIsCompatible()) {
        var map = new GMap2(document.getElementById(canvasId));
        map.setCenter(new GLatLng(latitude, longitude), defaultZoomLevel);
        map.setUIToDefault();
        return map;
      }
    };
    
    this.getBoundsZoomLevel = function(map, latLngBounds) {
    	var zoom = map.getBoundsZoomLevel(latLngBounds);
    	if (zoom > minZoomLevel) {
    		return minZoomLevel;
    	}
    	return zoom;
    };
    
    this.addEventMemberMapMarker = function(map, latLngBounds, location, markerOptions, markerInfoId) {
    	if (!location) {
    		SolarNetwork.debug('Can\'t map marker as no location supplied %s', markerInfoId);
    		return;
    	}
		SolarNetwork.debug('Adding marker to map %s,%s', location.latitude, location.longitude);
    	var point = new GLatLng(location.latitude, location.longitude);
    	latLngBounds.extend(point);
    	var marker = new GMarker(point, markerOptions);
    	marker.bindInfoWindowTabs(
    			[new GInfoWindowTab('Participant', document.getElementById(markerInfoId + 'details'))]);
    	map.addOverlay(marker);

		map.setCenter(latLngBounds.getCenter(), this.getBoundsZoomLevel(map, latLngBounds));
    };
    
    this.addParticipantEventMapMarker = function(map, latLngBounds, location, markerOptions, markerInfoId) {
		SolarNetwork.debug('Adding marker to map %s,%s', location.latitude, location.longitude);
    	var point = new GLatLng(location.latitude, location.longitude);
    	latLngBounds.extend(point);
    	var marker = new GMarker(point, markerOptions);
    	marker.bindInfoWindowTabs(
    			[new GInfoWindowTab('Participant', document.getElementById(markerInfoId + 'details')),
    			 new GInfoWindowTab('Events', document.getElementById(markerInfoId + 'events'))]);
    	map.addOverlay(marker);

		map.setCenter(latLngBounds.getCenter(), this.getBoundsZoomLevel(map, latLngBounds));
    };
    
    /**
     * Adds the info tab for an event member. This expects a div with the id 'mapParticipantInfo' to be present on the page.
     * 
     * @param type The name of the member type being added, Group or Participant
     * @param member The participant/group that is being added.
     * @param memberInfoDivId The DOM id of the div used to store the participant/group info.
     */
    this.addEventMemberInfo = function(type, member, memberInfoDivId) {
    	var mapParticipantInfo = $('#mapParticipantInfo #' + memberInfoDivId + 'details');
    	if (!mapParticipantInfo.length > 0) {
    		$('#mapParticipantInfo').append('<div id =' + memberInfoDivId+ 'details>' +
    				'<h4>' + this.getMemberName(type, member) + '</h4>' +
    				'<tr><td>Region:</td><td>' + member.location.region + '</td></tr>' +
    				'<tr><td>Country:</td><td>' + member.location.country + '</td></tr></table>');
    	}
    };
    
    /**
     * Adds an event to the info tab for a participant/group. This expects a div with the id 'mapParticipantInfo' to be present on the page.
     * 
     * @param type The name of the member type being added, Group or Participant
     * @param event The event item that is being added.
     * @param member The participant/group that the event is being added to.
     * @param participantInfoDivId The DOM id of the div used to store the participant/group info.
     * @return true if the member is new to the map.
     */
    this.addEventToMemberInfo = function(type, event, member, memberInfoDivId) {
    	var isNew = false;
    	var mapParticipantInfo = $('#mapParticipantInfo #' + memberInfoDivId + 'details');
    	if (!mapParticipantInfo.length > 0) {
    		isNew = true;// This is the first time we have seen this event member
    		$('#mapParticipantInfo').append('<div id =' + memberInfoDivId+ 'details>' +
    				'<h4>' + this.getMemberName(type, member) +'</h4>' +
    				'<tr><td>Region:</td><td>' + member.location.region + '</td></tr>' +
    				'<tr><td>Country:</td><td>' + member.location.country + '</td></tr></table>');

    		$('#mapParticipantInfo').append('<div id =' + memberInfoDivId+ 'events>' +
    		'Recent/Upcoming Events:<ul class="mapParticipantInfo"></ul></div>');
    	}
    	$('#mapParticipantInfo #' + memberInfoDivId + 'events ul').append('<li>' + event.name + '</li>');
    	return isNew;
    };
	
    this.addParticipantsToMap = function(map, latLngBounds, event, participants) {
		if ( !SolarNetwork.isArray(participants) ) {
			return;
		}
		SolarNetwork.debug('Adding participants to map %s', map);
		SolarNetwork.debug(participants);
		for ( var i = 0; i < participants.length; i++ ) {
			var participantInfoDivId = 'pPI' + participants[i].id;
			var isNew = this.addEventToMemberInfo('Participant', event, participants[i], participantInfoDivId);
			
			if (isNew) {
				this.addParticipantEventMapMarker(map, latLngBounds, participants[i].location,
					{"title" : this.getMemberName('Participant', participants[i]), "draggable" : false, "maxWidth" : 300},
					participantInfoDivId);
			}
		}
	};
	
	this.addGroupsToMap = function(map, latLngBounds, event, groups) {
		if ( !SolarNetwork.isArray(groups) ) {
			return;
		}
		SolarNetwork.debug('Adding groups to map %s', map);
		SolarNetwork.debug(groups);
		for ( var i = 0; i < groups.length; i++ ) {
			var groupInfoDivId = 'gPI' + groups[i].id;
			var isNew = this.addEventToMemberInfo('Group', event, groups[i], groupInfoDivId);
			
			if (isNew) {
				this.addParticipantEventMapMarker(map, latLngBounds, groups[i].location,
					{"title" : this.getMemberName('Group', groups[i]), "draggable" : false, "maxWidth" : 300},
					groupInfoDivId);
			}
		}
	};
	
	this.addMembersToEventMap = function(type, members, map, latLngBounds) {
		if (!SolarNetwork.isArray(members)) {
			return;
		}
		for (var i = 0; i < members.length; i++ ) {
			var participantInfoDivId = type + 'PI' + members[i].id;
			this.addEventMemberInfo(type, members[i], participantInfoDivId);
			this.addEventMemberMapMarker(map, latLngBounds, members[i].location, 
					{"title" : this.getMemberName(type, members[i]), "draggable" : false, "maxWidth" : 300}, 
					participantInfoDivId);
		}
	};
	
	this.getMemberName = function(type, member) {
		return type + ': ' + member.location.name;
	};
    
	/**
	 * 
	 * @param map The google map object to add the events to.
	 * @param events The event objects to add the map (from /solardras/obs/events.json).
	 * @param mapParticipantInfoDivId JQ id of the dive used to store the pariticipant info.
	 */
	this.addEventsToMap = function(map, events, mapParticipantInfoDivId) {
		if ( !SolarNetwork.isArray(events) ) {
			return;
		}
		
		var latLngBounds = new GLatLngBounds();
		for ( var i = 0; i < events.length; i++ ) {
			this.addEventToMap(map, latLngBounds, events[i]);
		}
    };

    this.addEventToMap = function(map, latLngBounds, event) {
		SolarNetwork.debug('Loading participants for event %d to map %s', event.id, map);

		$.getJSON(this.participantUrl('/findParticipants.json?simpleFilter.includeCapability=true&simpleFilter.eventId='+event.id), function(data) {
			me.addParticipantsToMap(map, latLngBounds, event, data.result);
		});
		$.getJSON(this.participantUrl('/findParticipantGroups.json?simpleFilter.includeCapability=true&simpleFilter.eventId='+event.id), function(data) {
			me.addGroupsToMap(map, latLngBounds, event, data.result);
		});
	};

	
	this.init(config);
};

var mapHelper = new SolarNetwork.DRAS.MapHelper({lat : -40.930115, long : 174.067383, defaultZoom : 5, minZoom : 10, participantContext : "/solardras/u/part"});
