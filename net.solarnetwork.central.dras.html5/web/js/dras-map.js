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
	var elementId = undefined;
	
	var center = undefined;
	var zoomLevel = undefined;
	var maxZoomLevel = undefined;
	var map = undefined;
	
	var init = function(cfg) {
		elementId = typeof cfg.elementId === 'string' ? cfg.elementId : undefined;
		var latitude = typeof cfg.lat === 'number' ? cfg.lat : undefined;
		var longitude = typeof cfg.long === 'number' ? cfg.long : undefined;
		zoomLevel = typeof cfg.zoom === 'number' ? cfg.zoom : undefined;
		maxZoomLevel = typeof cfg.maxZoom == 'number' ? cfg.maxZoom : undefined;
		
		if ( latitude !== undefined && longitude !== undefined 
				&& GBrowserIsCompatible() ) {
			center = new GLatLng(latitude, longitude);
			map = new GMap2(document.getElementById(elementId));
			map.setCenter(center, zoom);
			map.setUIToDefault();
		}
	};
	
	/**
	 * Get the Google Map object.
	 *
	 * @return {Object} the GMap2 object
	 */
	this.getMap = function() {
		return map;
	};
	
	/**
	 * Get the map bounds zoom level, constrained to the configured maxZoomLevel.
	 * 
	 * @param {Object} latLongBounds the GLatLngBounds object
	 * @return {Number} the zoom level
	 */
	this.getBoundsZoomLevel = function(latLongBounds) {
		var currZoomLevel = map.getBoundsZoomLevel(latLongBounds);
    	return (maxZoomLevel !== undefined && currZoomLevel > maxZoomLevel 
    		? maxZoomLevel 
    		: currZoomLevel);
	};
	
	/**
	 * Add an event member marker to the map.
	 * 
	 * @param {Object} location the location object
	 * @param {Number} location.latitude the latitude
	 * @param {Number} location.longitude the longitude
	 * @param {Object} latLngBounds the GLatLngBounds
	 * @param {Object} markerOptions options to pass to GMarker
	 * @param {Object} markerInfoId the ID to assign to the marker
	 */
	 this.addEventMemberMapMarker = function(location, latLngBounds, markerOptions, markerInfoId) {
		SolarNetwork.debug('Adding marker to map %s,%s', location.latitude, location.longitude);
		var point = new GLatLng(location.latitude, location.longitude);
		latLngBounds.extend(point);
		var marker = new GMarker(point, markerOptions);
		marker.bindInfoWindowTabs([
			new GInfoWindowTab('Participant', document.getElementById(markerInfoId + 'details'))
			]);
		map.addOverlay(marker);
		
		map.setCenter(latLngBounds.getCenter(), this.getBoundsZoomLevel(latLngBounds));
	};
    
	init(config);
};
