/**
 * @require d3 3.0
 * @require queue 1.0
 */
(function() {
'use strict';

var nodeUrlHelperFunctions;
var locationUrlHelperFunctions;

if ( sn === undefined ) {
	sn = {};
}

/**
 * @namespace the SolarNetwork Datum namespace
 * @require d3 3.0
 * @require queue 1.0
 * @require solarnetwork-d3 0.0.6
 */
sn.datum = {};

/**
 * A node-specific URL utility object.
 * 
 * @class
 * @constructor
 * @param {Number} node The node ID to use.
 * @param {Object} configuration The configuration options to use.
 * @returns {sn.datum.nodeUrlHelper}
 */
sn.datum.nodeUrlHelper = function(node, configuration) {
	var that = {
		version : '1.1.0'
	};
	
	var nodeId = node;
	
	var config = sn.util.copy(configuration, {
		host : 'data.solarnetwork.net',
		tls : true,
		path : '/solarquery',
		secureQuery : false
	});
	
	/**
	 * Get a URL for just the SolarNet host, without any path.
	 *
	 * @returns {String} the URL to the SolarNet host
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function hostURL() {
		return ('http' +(config.tls === true ? 's' : '') +'://' +config.host);
	}
	
	/**
	 * Get a URL for the SolarNet host and the base API path, e.g. <code>/solarquery/api/v1/sec</code>.
	 *
	 * @returns {String} the URL to the SolarNet base API path
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function baseURL() {
		return (hostURL() +config.path +'/api/v1/' +(config.secureQuery === true ? 'sec' : 'pub'));
	}
	
	/**
	 * Get a URL for the "reportable interval" for this node, optionally limited to a specific source ID.
	 *
	 * @param {Array} sourceIds An array of source IDs to limit query to. If not provided then all available 
	 *                sources will be returned.
	 * @returns {String} the URL to find the reportable interval
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function reportableIntervalURL(sourceIds) {
		var url = (baseURL() +'/range/interval?nodeId=' +nodeId);
		if ( Array.isArray(sourceIds) ) {
			url += '&' + sourceIds.map(function(e) { return 'sourceIds='+encodeURIComponent(e); }).join('&')
		}
		return url;
	}
	
	/**
	 * Get a available source IDs for this node, optionally limited to a date range.
	 *
	 * @param {Date} startDate An optional start date to limit the results to.
	 * @param {Date} endDate An optional end date to limit the results to.
	 * @returns {String} the URL to find the available source
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function availableSourcesURL(startDate, endDate) {
		var url = (baseURL() +'/range/sources?nodeId=' +nodeId);
		if ( startDate !== undefined ) {
			url += '&start=' +encodeURIComponent(sn.dateFormat(startDate));
		}
		if ( endDate !== undefined ) {
			url += '&end=' +encodeURIComponent(sn.dateFormat(endDate));
		}
		return url;
	}
	
	/**
	 * Generate a SolarNet {@code /datum/list} URL.
	 * 
	 * @param {Date} startDate The starting date for the query, or <em>null</em> to omit
	 * @param {Date} endDate The ending date for the query, or <em>null</em> to omit
	 * @param {String|Number} agg A supported aggregate type (e.g. Hour, Day, etc) or a minute precision Number
	 * @param {Array} sourceIds Array of source IDs to limit query to
	 * @param {Object} pagination An optional pagination object, with <code>offset</code> and <code>max</code> properties.
	 * @return {String} the URL to perform the list with
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function dateTimeListURL(startDate, endDate, agg, sourceIds, pagination) {
		var url = (baseURL() +'/datum/list?nodeId=' +nodeId);
		if ( startDate ) {
			url += '&startDate=' +encodeURIComponent(sn.dateTimeFormatURL(startDate));
		}
		if ( endDate ) {
			url += '&endDate=' +encodeURIComponent(sn.dateTimeFormatURL(endDate));
		}
		if ( agg ) {
			url += '&aggregate=' + encodeURIComponent(agg);
		}
		if ( Array.isArray(sourceIds) ) {
			url += '&' + sourceIds.map(function(e) { return 'sourceIds='+encodeURIComponent(e); }).join('&')
		}
		if ( pagination !== undefined ) {
			if ( pagination.max > 0 ) {
				url += '&max=' + encodeURIComponent(pagination.max);
			}
			if ( pagination.offset > 0 ) {
				url += '&offset=' + encodeURIComponent(pagination.offset);
			}
		}
		return url;
	}
		
	/**
	 * Generate a SolarNet {@code /datum/mostRecent} URL.
	 * 
	 * @param {Array} sourceIds Array of source IDs to limit query to
	 * @return {String} the URL to perform the most recent query with
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function mostRecentURL(sourceIds) {
		var url = (baseURL() + '/datum/mostRecent?nodeId=' + nodeId);
		if ( Array.isArray(sourceIds) ) {
			url += '&' + sourceIds.map(function(e) { return 'sourceIds='+encodeURIComponent(e); }).join('&')
		}
		return url;
	}
	
	/**
	 * Get or set the node ID to use.
	 * 
	 * @param {String} [value] the node ID to use
	 * @return when used as a getter, the node ID, otherwise this object
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function nodeID(value) {
		if ( !arguments.length ) return nodeId;
		nodeId = value;
		return that;
	}
	
	/**
	 * Get a description of this helper object.
	 *
	 * @return {String} The description of this object.
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function keyDescription() {
		return ('node ' +nodeId);
	}
	
	// setup core properties
	Object.defineProperties(that, {
		secureQuery				: { get : function() { return (config.secureQuery === true); }, enumerable : true },
		keyDescription			: { value : keyDescription },
		nodeId					: { get : function() { return nodeId; }, enumerable : true },
		nodeID					: { value : nodeID },
		hostURL					: { value : hostURL },
		baseURL					: { value : baseURL },
		reportableIntervalURL 	: { value : reportableIntervalURL },
		availableSourcesURL		: { value : availableSourcesURL },
		dateTimeListURL			: { value : dateTimeListURL },
		mostRecentURL			: { value : mostRecentURL }
	});
	
	// allow plug-ins to supply URL helper methods, as long as they don't override built-in ones
	(function() {
		if ( Array.isArray(nodeUrlHelperFunctions) ) {
			nodeUrlHelperFunctions.forEach(function(helper) {
				if ( that.hasOwnProperty(helper.name) === false ) {
					Object.defineProperty(that, helper.name, { value : function() {
						return helper.func.apply(that, arguments);
					} });
				}
			});
		}
	}());

	return that;
};

/**
 * Register a custom function to generate URLs with {@link sn.datum.nodeUrlHelper}.
 * 
 * @param {String} name The name to give the custom function. By convention the function
 *                      names should end with 'URL'.
 * @param {Function} func The function to add to sn.datum.nodeUrlHelper instances.
 */
sn.datum.registerNodeUrlHelperFunction = function(name, func) {
	if ( typeof func !== 'function' ) {
		return;
	}
	if ( nodeUrlHelperFunctions === undefined ) {
		nodeUrlHelperFunctions = [];
	}
	name = name.replace(/[^0-9a-zA-Z_]/, '');
	nodeUrlHelperFunctions.push({name : name, func : func});
};

/**
 * A location-specific URL utility object.
 * 
 * @class
 * @constructor
 * @param {Number} location The location ID to use.
 * @param {Object} configuration The configuration options to use.
 * @returns {sn.datum.locationUrlHelper}
 */
sn.datum.locationUrlHelper = function(location, configuration) {
	var that = {
		version : '1.0.0'
	};
	
	var locationId = location;
	
	var config = sn.util.copy(configuration, {
		host : 'data.solarnetwork.net',
		tls : true,
		path : '/solarquery',
		secureQuery : false
	});
	
	/**
	 * Get a URL for just the SolarNet host, without any path.
	 *
	 * @returns {String} the URL to the SolarNet host
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function hostURL() {
		return ('http' +(config.tls === true ? 's' : '') +'://' +config.host);
	}
	
	/**
	 * Get a URL for the SolarNet host and the base API path, e.g. <code>/solarquery/api/v1/sec</code>.
	 *
	 * @returns {String} the URL to the SolarNet base API path
	 * @memberOf sn.datum.locationUrlHelper
	 */
	function baseURL() {
		return (hostURL() +config.path +'/api/v1/' +(config.secureQuery === true ? 'sec' : 'pub'));
	}
	
	/**
	 * Get a URL for the "reportable interval" for this location, optionally limited to a specific source ID.
	 *
	 * @param {Array} sourceIds An array of source IDs to limit query to. If not provided then all available 
	 *                sources will be returned.
	 * @returns {String} the URL to find the reportable interval
	 * @memberOf sn.datum.locationUrlHelper
	 */
	function reportableIntervalURL(sourceIds) {
		var url = (baseURL() +'/location/datum/interval?locationId=' +locationId);
		if ( Array.isArray(sourceIds) ) {
			url += '&' + sourceIds.map(function(e) { return 'sourceIds='+encodeURIComponent(e); }).join('&')
		}
		return url;
	}
	
	/**
	 * Get a available source IDs for this location, optionally limited to a date range.
	 *
	 * @param {Date} startDate An optional start date to limit the results to.
	 * @param {Date} endDate An optional end date to limit the results to.
	 * @returns {String} the URL to find the available source
	 * @memberOf sn.datum.locationUrlHelper
	 */
	function availableSourcesURL(startDate, endDate) {
		var url = (baseURL() +'/location/datum/sources?locationId=' +locationId);
		if ( startDate !== undefined ) {
			url += '&start=' +encodeURIComponent(sn.dateFormat(startDate));
		}
		if ( endDate !== undefined ) {
			url += '&end=' +encodeURIComponent(sn.dateFormat(endDate));
		}
		return url;
	}
	
	/**
	 * Generate a SolarNet {@code /datum/list} URL.
	 * 
	 * @param {Date} startDate The starting date for the query, or <em>null</em> to omit
	 * @param {Date} endDate The ending date for the query, or <em>null</em> to omit
	 * @param {String|Number} agg A supported aggregate type (e.g. Hour, Day, etc) or a minute precision Number
	 * @param {Array} sourceIds Array of source IDs to limit query to
	 * @param {Object} pagination An optional pagination object, with <code>offset</code> and <code>max</code> properties.
	 * @return {String} the URL to perform the list with
	 * @memberOf sn.datum.locationUrlHelper
	 */
	function dateTimeListURL(startDate, endDate, agg, sourceIds, pagination) {
		var url = (baseURL() +'/location/datum/list?locationId=' +locationId);
		if ( startDate ) {
			url += '&startDate=' +encodeURIComponent(sn.dateTimeFormatURL(startDate));
		}
		if ( endDate ) {
			url += '&endDate=' +encodeURIComponent(sn.dateTimeFormatURL(endDate));
		}
		if ( agg ) {
			url += '&aggregate=' + encodeURIComponent(agg);
		}
		if ( Array.isArray(sourceIds) ) {
			url += '&' + sourceIds.map(function(e) { return 'sourceIds='+encodeURIComponent(e); }).join('&')
		}
		if ( pagination !== undefined ) {
			if ( pagination.max > 0 ) {
				url += '&max=' + encodeURIComponent(pagination.max);
			}
			if ( pagination.offset > 0 ) {
				url += '&offset=' + encodeURIComponent(pagination.offset);
			}
		}
		return url;
	}
		
	/**
	 * Generate a SolarNet {@code /datum/mostRecent} URL.
	 * 
	 * @param {Array} sourceIds Array of source IDs to limit query to
	 * @return {String} the URL to perform the most recent query with
	 * @memberOf sn.datum.locationUrlHelper
	 */
	function mostRecentURL(sourceIds) {
		var url = (baseURL() + '/location/datum/mostRecent?locationId=' + locationId);
		if ( Array.isArray(sourceIds) ) {
			url += '&' + sourceIds.map(function(e) { return 'sourceIds='+encodeURIComponent(e); }).join('&')
		}
		return url;
	}
	
	/**
	 * Get or set the location ID to use.
	 * 
	 * @param {String} [value] the location ID to use
	 * @return when used as a getter, the location ID, otherwise this object
	 * @memberOf sn.datum.locationUrlHelper
	 */
	function locationID(value) {
		if ( !arguments.length ) return locationId;
		locationId = value;
		return that;
	}
	
	/**
	 * Get a description of this helper object.
	 *
	 * @return {String} The description of this object.
	 * @memberOf sn.datum.nodeUrlHelper
	 */
	function keyDescription() {
		return ('node ' +nodeId);
	}
	
	// setup core properties
	Object.defineProperties(that, {
		keyDescription			: { value : keyDescription },
		locationId				: { get : function() { return locationId; }, enumerable : true },
		locationID				: { value : locationID },
		hostURL					: { value : hostURL },
		baseURL					: { value : baseURL },
		reportableIntervalURL 	: { value : reportableIntervalURL },
		availableSourcesURL		: { value : availableSourcesURL },
		dateTimeListURL			: { value : dateTimeListURL },
		mostRecentURL			: { value : mostRecentURL }
	});
	
	// allow plug-ins to supply URL helper methods, as long as they don't override built-in ones
	(function() {
		if ( Array.isArray(locationUrlHelperFunctions) ) {
			locationUrlHelperFunctions.forEach(function(helper) {
				if ( that.hasOwnProperty(helper.name) === false ) {
					Object.defineProperty(that, helper.name, { value : function() {
						return helper.func.apply(that, arguments);
					} });
				}
			});
		}
	}());

	return that;
};

/**
 * Register a custom function to generate URLs with {@link sn.datum.locationUrlHelper}.
 * 
 * @param {String} name The name to give the custom function. By convention the function
 *                      names should end with 'URL'.
 * @param {Function} func The function to add to sn.datum.locationUrlHelper instances.
 */
sn.datum.registerLocationUrlHelperFunction = function(name, func) {
	if ( typeof func !== 'function' ) {
		return;
	}
	if ( locationUrlHelperFunctions === undefined ) {
		locationUrlHelperFunctions = [];
	}
	name = name.replace(/[^0-9a-zA-Z_]/, '');
	locationUrlHelperFunctions.push({name : name, func : func});
};

/**
 * Call the {@code availableSourcesURL} web service and invoke a callback function with the results.
 * 
 * <p>The callback function will be passed an error object and the array of sources.
 * 
 * @param {sn.datum.nodeUrlHelper} urlHelper A {@link sn.datum.nodeUrlHelper} or 
                                             {@link sn.datum.locationUrlHelper} object.
 * @param {Function} callback A callback function which will be passed an error object
 *                            and the result array.
 */
sn.datum.availableSources = function(urlHelper, callback) {
	if ( !(urlHelper && urlHelper.availableSourcesURL && callback) ) {
		return;
	}
	var url = urlHelper.availableSourcesURL();
	d3.json(url, function(error, json) {
		var sources;
		if ( error ) {
			callback(error);
		} else if ( !json ) {
			callback('No data returned from ' +url);
		} else if ( json.success !== true ) {
			callback(json.message ? json.message : 'Query not successful.');
		} else {
			sources = (Array.isArray(json.data) ? json.data.sort() : []);
			sources.sort();
			callback(null, sources);
		}
	});
};

/**
 * Call the {@code reportableIntervalURL} web service for a set of source IDs and
 * invoke a callback function with the results.
 * 
 * <p>The callback function will be passed the same 'data' object returned
 * by the {@code reportableIntervalURL} endpoint, but the start/end dates will be
 * a combination of the earliest available and latest available results for
 * every different node ID provided.
 * 
 * @param {Array} sourceSets An array of objects, each with a {@code sourceIds} array 
 *                property and a {@code nodeUrlHelper} {@code sn.datum.nodeUrlHelper}
 *                or {@code locationUrlHelper} {@code sn.datum.locationUrlHelper}
 *                propery.
 * @param {Function} [callback] A callback function which will be passed the result object.
 */
sn.datum.availableDataRange = function(sourceSets, callback) {
	var q = queue(),
		helpers = [];
	
	// submit all queries to our queue
	(function() {
		var i,
			url,
			urlHelper;
		for ( i = 0; i < sourceSets.length; i += 1 ) {
			if ( sourceSets[i].nodeUrlHelper ) {
				urlHelper = sourceSets[i].nodeUrlHelper;
			} else if ( sourceSets[i].locationUrlHelper ) {
				urlHelper = sourceSets[i].locationUrlHelper;
			} else {
				urlHelper = sourceSets[i].urlHelper;
			}
			if ( urlHelper && urlHelper.reportableIntervalURL ) {
				helpers.push(urlHelper);
				url = urlHelper.reportableIntervalURL(sourceSets[i].sourceIds);
				q.defer(d3.json, url);
			}
		}
	}());
	
	function extractReportableInterval(results) {
		var result, 
			i = 0,
			repInterval;
		for ( i = 0; i < results.length; i += 1 ) {
			repInterval = results[i];
			if ( repInterval.data === undefined || repInterval.data.endDate === undefined ) {
				sn.log('No data available for {0} sources {1}', 
					helpers[i].keyDescription(), sourceSets[i].sourceIds.join(','));
				continue;
			}
			repInterval = repInterval.data;
			if ( result === undefined ) {
				result = repInterval;
			} else {
				// merge start/end dates
				// note we don't copy the time zone... this breaks when the tz are different!
				if ( repInterval.endDateMillis > result.endDateMillis ) {
					result.endDateMillis = repInterval.endDateMillis;
					result.endDate = repInterval.endDate;
				}
				if ( repInterval.startDateMillis < result.startDateMillis ) {
					result.startDateMillis = repInterval.startDateMillis;
					result.startDate = repInterval.startDate;
				}
			}
		}
		return result;
	}
	
	q.awaitAll(function(error, results) {
		if ( error ) {
			sn.log('Error requesting available data range: ' +error);
			return;
		}
		var intervalObj = extractReportableInterval(results);
		if ( intervalObj.startDateMillis !== undefined ) {
			intervalObj.sDate = new Date(intervalObj.startDateMillis);
			//intervalObj.sLocalDate = sn.dateTimeFormatLocal.parse(intervalObj.startDate);
		}
		if ( intervalObj.endDateMillis !== undefined ) {
			intervalObj.eDate = new Date(intervalObj.endDateMillis);
		}

		if ( typeof callback === 'function' ) {
			callback(intervalObj);
		}
	});
};

/**
 * Get a query range appropriate for using with {@link sn.datum.loader}. Returns an object
 * with <code>start</code> and <code>end</code> Date properties, using the given <code>endDate</code>
 * parameter as the basis for calculating the start as an offset, based on the given <code>aggregate</code>
 * level.
 * 
 * @param {string} aggregate - the aggregate level
 * @param {object} aggregateTimeCount - either a Number or an Object with Number properties named 
 *                 <code>numXs</code> where <code>X</code> is the aggregate level, representing
 *                 the number of aggregate time units to include in the query
 * @param {Date} endDate - the end date
 * @returns {Object}
 * @since 0.0.4
 */
sn.datum.loaderQueryRange = function(aggregate, aggregateTimeCount, endDate) {
	var end,
		start,
		timeUnit,
		timeCount,
		precision;
	
	function exclusiveEndDate(time, date) {
		var result = time.utc.ceil(date);
		if ( result.getTime() === date.getTime() ) {
			// already on exact aggregate, so round up to next
			result = time.offset(result, 1);
		}
		return result;
	}
	
	function timeCountValue(propName) {
		var result;
		if ( isNaN(Number(aggregateTimeCount)) ) {
			if ( aggregateTimeCount[propName] !== undefined ) {
				result = Number(aggregateTimeCount[propName]);
			} else {
				result = 1;
			}
		} else {
			result = aggregateTimeCount;
		}
		if ( typeof result !== 'number' ) {
			result = 1;
		}
		return result;
	}
	
	function precisionValue(agg) {
		var result = 10;
		if ( aggregate.search(/^Five/) === 0 ) {
			result = 5;
		} else if ( aggregate.search(/^Fifteen/) === 0 ) {
			result = 15;
		}
		return result;
	}
	
	if ( aggregate.search(/Minute$/) >= 0 ) {
		timeCount = timeCountValue('numHours');
		timeUnit = 'hour';
		end = exclusiveEndDate(d3.time.minute, endDate);
		precision = precisionValue(aggregate);
		end.setUTCMinutes((end.getUTCMinutes() + precision - (end.getUTCMinutes() % precision)), 0, 0);
		start = d3.time.hour.utc.offset(end, -timeCount);
	} else if ( aggregate === 'Month' ) {
		timeCount = timeCountValue('numYears');
		timeUnit = 'year';
		end = exclusiveEndDate(d3.time.month, endDate);
		start = d3.time.year.utc.offset(d3.time.month.utc.floor(endDate), -timeCount);
	} else if ( aggregate === 'Day' ) {
		timeCount = timeCountValue('numMonths');
		timeUnit = 'month';
		end = exclusiveEndDate(d3.time.day, endDate);
		start = d3.time.month.utc.offset(d3.time.day.utc.floor(endDate), -timeCount);
	} else {
		// assume Hour
		timeCount = timeCountValue('numDays');
		timeUnit = 'day';
		end = exclusiveEndDate(d3.time.hour, endDate);
		start = d3.time.day.utc.offset(d3.time.hour.utc.floor(end), -timeCount);
	}
	return {
		start : start, 
		end : end, 
		timeUnit : timeUnit, 
		timeCount : timeCount
	};
};

/**
 * Load data for a set of source IDs, date range, and aggregate level using the 
 * {@code dateTimeListURL} endpoint. This object is designed 
 * to be used once per query. After creating the object and configuring an asynchronous
 * callback function with {@link #callback(function)}, call {@link #load()} to start
 * loading the data. The callback function will be called once all data has been loaded.
 * 
 * @class
 * @param {string[]} sourceIds - array of source IDs to load data for
 * @param {function} urlHelper - a {@link sn.nodeUrlHelper} or {@link sn.locationUrlHelper}
 * @param {date} start - the start date, or {@code null}
 * @param {date} end - the end date, or {@code null}
 * @param {string} aggregate - aggregate level
 * @returns {sn.datum.loader}
 */
sn.datum.loader = function(sourceIds, urlHelper, start, end, aggregate) {
	
	var that = { version : '1.0.0' };

	var finishedCallback;
	var urlParameters;

	var state = 0; // keys are source IDs, values are 1:loading, 2:done
	var results;
	
	function requestCompletionHandler(error) {
		state = 2; // done
		
		// check if we're all done loading, and if so call our callback function
		if ( finishedCallback ) {
			finishedCallback.call(that, error, results);
		}
	}

	function loadData(offset) {
		var pagination = {},
			key,
			url,
			dataExtractor,
			offsetExtractor;
		if ( offset ) {
			pagination.offset = offset;
		}
		url = urlHelper.dateTimeListURL(start, end, aggregate, sourceIds, pagination);
		if ( urlParameters ) {
			(function() {
				var tmp = sn.encodeURLQueryTerms(urlParameters);
				if ( tmp.length ) {
					url += '&' + tmp;
				}
			}());
		}
		dataExtractor = function(json) {
			if ( json.success !== true || json.data === undefined || Array.isArray(json.data.results) !== true ) {
				return undefined;
			}
			return json.data.results;
		};
		offsetExtractor = function(json) { 
			return (json.data.returnedResultCount + json.data.startingOffset < json.data.totalResults 
					? (json.data.returnedResultCount + json.data.startingOffset)
					: 0);
		};
		d3.json(url, function(error, json) {
			var dataArray,
				nextOffset;
			if ( error ) {
				sn.log('Error requesting data for {0}: {2}', urlHelper.keyDescription(), error);
				return;
			}
			dataArray = dataExtractor(json);
			if ( dataArray === undefined ) {
				sn.log('No data available for {0}', urlHelper.keyDescription());
				requestCompletionHandler(error);
				return;
			}

			if ( results === undefined ) {
				results = dataArray;
			} else {
				results = results.concat(dataArray);
			}
			
			// see if we need to load more results
			nextOffset = offsetExtractor(json);
			if ( nextOffset > 0 ) {
				loadData(nextOffset);
			} else {
				requestCompletionHandler(error);
			}
		});
	}
	
	/**
	 * Get or set the callback function, invoked after all data has been loaded. The callback
	 * function will be passed two arguments: an error and the results.
	 * 
	 * @param {function} [value] the callback function to use
	 * @return when used as a getter, the current callback function, otherwise this object
	 * @memberOf sn.datum.loader
	 */
	that.callback = function(value) {
		if ( !arguments.length ) { return finishedCallback; }
		if ( typeof value === 'function' ) {
			finishedCallback = value;
		}
		return that;
	};
	
	/**
	 * Get or set additional URL parameters. The parameters are set as object properties.
	 * If a property value is an array, multiple parameters for that property will be added.
	 * 
	 * @param {object} [value] the URL parameters to include with the JSON request
	 * @return when used as a getter, the URL parameters, otherwise this object
	 * @memberOf sn.datum.loader
	 */
	that.urlParameters = function(value) {
		if ( !arguments.length ) return urlParameters;
		if ( typeof value === 'object' ) {
			urlParameters = value;
		}
		return that;
	};

	/**
	 * Initiate loading the data. As an alternative to configuring the callback function via
	 * the {@link #callback(value)} method, a callback function can be passed as an argument
	 * to this function. This allows this function to be passed to <code>queue.defer</code>,
	 * for example.
	 * 
	 * @param {function} [callback] a callback function to use
	 * @return this object
	 * @memberOf sn.datum.loader
	 */
	that.load = function(callback) {
		// to support queue use, allow callback to be passed directly to this function
		if ( typeof callback === 'function' ) {
			finishedCallback = callback;
		}
		state = 1;
		loadData();
		return that;
	};

	return that;
};

/**
 * Load data from multiple {@link sn.datum.loader} objects, invoking a callback function
 * after all data has been loaded. Call {@link #load()} to start loading the data.
 * 
 * @class
 * @param {sn.datum.loader[]} loaders - array of {@link sn.datum.loader} objects
 * @returns {sn.datum.multiLoader}
 */
sn.datum.multiLoader = function(loaders) {
	var that = {
			version : '1.0.0'
	};

	var finishedCallback,
		q = queue();
		
	/**
	 * Get or set the callback function, invoked after all data has been loaded. The callback
	 * function will be passed two arguments: an error and an array of result arrays returned
	 * from {@link sn.datum.loader#load()} on each supplied loader.
	 * 
	 * @param {function} [value] the callback function to use
	 * @return when used as a getter, the current callback function, otherwise this object
	 * @memberOf sn.datum.multiLoader
	 */
	that.callback = function(value) {
		if ( !arguments.length ) { return finishedCallback; }
		if ( typeof value === 'function' ) {
			finishedCallback = value;
		}
		return that;
	};
	
	/**
	 * Initiate loading the data. This will call {@link sn.datum.loader#load()} on each
	 * supplied loader, in parallel.
	 * 
	 * @memberOf sn.datum.multiLoader
	 */
	that.load = function() {
		loaders.forEach(function(e) {
			q.defer(e.load);
		});
		q.awaitAll(function(error, results) {
			if ( finishedCallback ) {
				finishedCallback.call(that, error, results);
			}
		});
		return that;
	};

	return that;
};

/**
 * Get a Date object for a datum. This function will return the first available date according
 * to the first available property found according to these rules:
 * 
 * <ol>
 * <li><code>date</code> - assumed to be a Date object already</li>
 * <li><code>localDate</code> - a string in <b>yyyy-MM-dd</b> form, optionally with a String
 *     <code>localTime</code> property for an associated time in <b>HH:mm</b> form.</li>
 * <li><code>created</code> - a string in <b>yyyy-MM-dd HH:mm:ss.SSS'Z'</b> form.</li>
 * </ul>
 *
 * @param {Object} d The datum to get the Date for.
 * @returns {Date} The found Date, or <em>null</em> if not available
 */
sn.datum.datumDate = function(d) {
	if ( d ) {
		if ( d.date ) {
			return d.date;
		} else if ( d.localDate ) {
			return sn.dateTimeFormat.parse(d.localDate +(d.localTime ? ' ' +d.localTime : ' 00:00'));
		} else if ( d.created ) {
			return sn.timestampFormat.parse(d.created);
		}
	}
	return null;
};

}());
