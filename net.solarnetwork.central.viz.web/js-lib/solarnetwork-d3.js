/*global colorbrewer,console,d3,queue */
(function() {
'use strict';
/**
 * @namespace the SolarNetwork namespace
 * @require d3 3.0
 * @require queue 1.0
 */
var sn = {
	version : '0.0.7',
	
	/**
	 * @namespace the SolarNetwork chart namespace.
	 */
	chart : {},
	
	config : {
		debug : false,
		host : 'data.solarnetwork.net',
		tls : (function() {
			return (window !== undefined 
				&& window.location.protocol !== undefined 
				&& window.location.protocol.toLowerCase().indexOf('https') === 0 ? true : false);
		}()),
		path : '/solarquery',
		solarUserPath : '/solaruser',
		secureQuery : false
	},
	
	colors : {
		steelblue: ['#356287', '#4682b4', '#6B9BC3', '#89AFCF', '#A1BFD9', '#B5CDE1', '#DAE6F0'],
		triplets : [
			'#3182bd', '#6baed6', '#9ecae1', 
			'#e6550d', '#fd8d3c', '#fdae6b', 
			'#31a354', '#74c476', '#a1d99b', 
			'#756bb1', '#9e9ac8', '#bcbddc', 
			'#843c39', '#ad494a', '#d6616b', 
			'#8c6d31', '#bd9e39', '#e7ba52', 
			'#7b4173', '#a55194', '#ce6dbd'
			]
	},
	
	seasonColors : ['#5c8726', '#e9a712', '#762123', '#80a3b7'],
	
	env : {},
		
	setDefaultEnv : function(defaults) {
		var prop;
		for ( prop in defaults ) {
			if ( defaults.hasOwnProperty(prop) ) {
				if ( sn.env[prop] === undefined ) {
					sn.env[prop] = defaults[prop];
				}
			}
		}
	},
	
	setEnv : function(env) {
		var prop;
		for ( prop in env ) {
			if ( env.hasOwnProperty(prop) ) {
				sn.env[prop] = env[prop];
			}
		}
	},

	runtime : {},
	
	dateTimeFormat : d3.time.format.utc("%Y-%m-%d %H:%M"),

	timestampFormat : d3.time.format.utc("%Y-%m-%d %H:%M:%S.%LZ"),

	dateTimeFormatLocal : d3.time.format("%Y-%m-%d %H:%M"),

	dateTimeFormatURL : d3.time.format.utc("%Y-%m-%dT%H:%M"),
	
	dateFormat : d3.time.format.utc("%Y-%m-%d"),
	
	// fmt(string, args...): helper to be able to use placeholders even on iOS, where console.log doesn't support them
	fmt : function() {
		if ( !arguments.length ) {
			return;
		}
		var i = 0,
			formatted = arguments[i],
			regexp,
			replaceValue;
		for ( i = 1; i < arguments.length; i += 1 ) {
			regexp = new RegExp('\\{'+(i-1)+'\\}', 'gi');
			replaceValue = arguments[i];
			if ( replaceValue instanceof Date ) {
				replaceValue = (replaceValue.getUTCHours() === 0 && replaceValue.getMinutes() === 0 
					? sn.dateFormat(replaceValue) : sn.dateTimeFormat(replaceValue));
			}
			formatted = formatted.replace(regexp, replaceValue);
		}
		return formatted;
	},
	
	log : function() {
		if ( sn.config.debug === true && console !== undefined ) {
			console.log(sn.fmt.apply(this, arguments));
		}
	},
	
	/**
	 * Register a node URL helper function for the given name.
	 */
	registerNodeUrlHelper : function(name, helper) {
		if ( sn.env.nodeUrlHelpers === undefined ) {
			sn.env.nodeUrlHelpers = {};
		}
		sn.env.nodeUrlHelpers[name] = helper;
	},
	
	counter : function() {
		var c = 0;
		var obj = function() {
			return c;
		};
		obj.incrementAndGet = function() {
			c += 1;
			return c;
		};
		return obj;
	},
	
	/**
	 * Return an array of colors for a set of unique keys, where the returned
	 * array also contains associative properties for all key values to thier
	 * corresponding color value.
	 * 
	 * <p>This is designed so the set of keys always map to the same color, 
	 * even across charts where not all sources may be present.</p>
	 */
	colorMap : function(fillColors, keys) {
		var colorRange = d3.scale.ordinal().range(fillColors);
		var colorData = keys.map(function(el, i) { return {source:el, color:colorRange(i)}; });
		
		// also provide a mapping of sources to corresponding colors
		var i, len, sourceName;
		for ( i = 0, len = colorData.length; i < len; i += 1 ) {
			// a source value might actually be a number string, which JavaScript will treat 
			// as an array index so only set non-numbers here
			sourceName = colorData[i].source;
			if ( sourceName === '' ) {
				// default to Main if source not provided
				sourceName = 'Main';
			}
			if ( isNaN(Number(sourceName)) ) {
				colorData[sourceName] = colorData[i].color;
			}
		}
		
		return colorData;
	},
	
	/**
	 * Use the configured runtime color map to turn a source into a color.
	 * 
	 * The {@code sn.runtime.colorData} property must be set to a color map object
	 * as returned by {@link sn.colorMap}.
	 * 
	 * @param {object} d the data element, expected to contain a {@code source} property
	 * @returns {string} color value
	 */
	colorFn : function(d) {
		var s = Number(d.source);
		if ( isNaN(s) ) {
			return sn.runtime.colorData[d.source];
		}
		return sn.runtime.colorData.reduce(function(c, obj) {
			return (obj.source === d.source ? obj.color : c);
		}, sn.runtime.colorData[0].color);
	}
};

/**
 * Parse the query portion of a URL string, and return a parameter object for the
 * parsed key/value pairs.
 * 
 * <p>Multiple parameters of the same name will be stored as an array on the returned object.</p>
 * 
 * @param {String} search the query portion of the URL, which may optionally include 
 *                        the leading '?' character
 * @return {Object} the parsed query parameters, as a parameter object
 */
sn.parseURLQueryTerms = function(search) {
	var params = {};
	var pairs;
	var pair;
	var i, len, k, v;
	if ( search !== undefined && search.length > 0 ) {
		// remove any leading ? character
		if ( search.match(/^\?/) ) {
			search = search.substring(1);
		}
		pairs = search.split('&');
		for ( i = 0, len = pairs.length; i < len; i++ ) {
			pair = pairs[i].split('=', 2);
			if ( pair.length === 2 ) {
				k = decodeURIComponent(pair[0]);
				v = decodeURIComponent(pair[1]);
				if ( params[k] ) {
					if ( !Array.isArray(params[k]) ) {
						params[k] = [params[k]]; // turn into array;
					}
					params[k].push(v);
				} else {
					params[k] = v;
				}
			}
		}
	}
	return params;
};

/**
 * Encode the properties of an object as a URL query string.
 * 
 * <p>If an object property has an array value, multiple URL parameters will be encoded for that property.</p>
 * 
 * @param {Object} an object to encode as URL parameters
 * @return {String} the encoded query parameters
 */
sn.encodeURLQueryTerms = function(parameters) {
	var result = '',
		prop,
		val,
		i,
		len;
	function handleValue(k, v) {
		if ( result.length ) {
			result += '&';
		}
		result += encodeURIComponent(k) + '=' + encodeURIComponent(v);
	}
	if ( parameters ) {
		for ( prop in parameters ) {
			if ( parameters.hasOwnProperty(prop) ) {
				val = parameters[prop];
				if ( Array.isArray(val) ) {
					for ( i = 0, len = val.length; i < len; i++ ) {
						handleValue(prop, val[i]);
					}
				} else {
					handleValue(prop, val);
				}
			}
		}
	}
	return result;
};

/**
 * Comparator function that sorts Objects with {@code date} properties
 * of Date objects in ascending order.
 * 
 * @param {Object} left the left object with a {@code date} Date property
 * @param {Object} right the right object with a {@code date} Date property
 * @return {@code -1} if left is less than right, {@code 1} if greater,
 *         and {@code 0} if equal
 */
sn.datePropAscending = function(left, right) {
	var a = left.date.getTime();
	var b = right.date.getTime(); 
	return (a < b ? -1 : a > b ? 1 : 0);
};

/**
 * Comparator function that sorts Date objects in ascending order.
 * 
 * @param {Date} left the left Date
 * @param {Date} right the right Date
 * @return {@code -1} if left is less than right, {@code 1} if greater,
 *         and {@code 0} if equal
 */
sn.dateAscending = function(left, right) {
	var a = left.getTime();
	var b = right.getTime(); 
	return (a < b ? -1 : a > b ? 1 : 0);
};

/**
 * Take SolarNetwork raw JSON data result and return a d3-friendly normalized array of data.
 * The 'sources' parameter can be either undefined or an empty Array, which will be populated
 * with the list of found {@code sourceId} values from the raw JSON data. 
 * 
 * The {@code rawData} is organized like this:
 * 
 * <pre>
 * [
 * 	{
 * 		"localDate" : "2011-12-02",
 * 		"localTime" : "12:00",
 * 		"sourceId" : "Main",
 * 		"wattHours" : 470.0,
 * 		"watts" : 592
 * 	},
 * 	{
 * 		"localDate" : "2011-12-02",
 * 		"localTime" : "12:00",
 * 		"sourceId" : "Secondary",
 * 		"wattHours" : 312.0,
 * 		"watts" : 123
 * 	}
 * 
 * ]
 * </pre>
 * 
 * Returned data sample format:
 * <pre>
 * [
 * 		{
 * 			date       : Date(2011-12-02 12:00),
 * 			Main       : { watts: 592, wattHours: 470 },
 * 			Secondary  : { watts: 123, wattHours: 312 },
 * 			_aggregate : { wattHoursTotal: 782 }
 * 		}
 * ]
 * </pre>
 * 
 * @param {object[]} rawData the raw source data
 * @param {string[]} [sources] if defined, then this array will be populated with the unique
 *                             set of {@code sourceId} values found in the data
 */
sn.powerPerSourceArray = function(rawData, sources) {
	var filteredData = {};
	var sourceMap = (sources === undefined ? undefined : {});
	if ( !Array.isArray(rawData) ) {
		return filteredData;
	}
	var i, len;
	var el, dateStr, d, sourceName;
	for ( i = 0, len = rawData.length; i < len; i += 1 ) {
		el = rawData[i];
		dateStr = el.localDate +' ' +el.localTime;
		d = filteredData[dateStr];
		if ( d === undefined ) {
			d = {date:sn.dateTimeFormat.parse(dateStr)};
			filteredData[dateStr] = d;
		}
		
		// if there is no data for the allotted sample, watts === -1, so don't treat
		// that sample as a valid source ID
		sourceName = el.sourceId;
		if ( sourceName === undefined || sourceName === '' ) {
			// default to Main if source not provided
			sourceName = 'Main';
		}
		if ( el.watts !== -1 && sourceName !== 'date' && sourceName.charAt(0) !== '_' ) {
			if ( sourceMap !== undefined && sourceMap[sourceName] === undefined ) {
				sources.push(sourceName);
				sourceMap[sourceName] = 1;
			}
			d[sourceName] = {watts:el.watts, wattHours:el.wattHours};
			if ( el.wattHours > 0 ) {
				if ( d['_aggregate'] === undefined ) {
					d['_aggregate'] = {wattHoursTotal: el.wattHours};
				} else {
					d['_aggregate'].wattHoursTotal += el.wattHours;
				}
			}
		}
	}
	
	if ( sources !== undefined ) {
		// sort sources
		sources.sort();
	}
	
	var prop;
	var a = [];
	for ( prop in filteredData ) {
		if ( filteredData.hasOwnProperty(prop) ) {
			a.push(filteredData[prop]);
		}
	}
	return a.sort(sn.datePropAscending);
};

/**
 * Call the {@code reportableInterval} and {@code availableSources} web services
 * and post a {@code snAvailableDataRange} event with the associated data.
 * 
 * <p>The event will contain a 'data' object property with the following
 * properties:</p>
 * 
 * <dl>
 *   <dt>data.reportableInterval</dt>
 *   <dd>The reportable interval for the given dataTypes. This tells you the
 *   earliest and latest dates data is available for.</dd>
 * 
 *   <dt>data.availableSources</dt>
 *   <dd>A sorted array of available source IDs for the first data type with 
 *   any sources available for the reportable interval. This tells you all the possible 
 *   sources available in the data set.</dd>
 *   
 *   <dt>data.availableSourcesMap</dt>
 *   <dd>An object whose properties are the data types passed on the {@code dataTypes}
 *   argument, and their associated value the sorted array of available sources for
 *   that data type over the reportable interval. This tells you all the possible sources
 *   for every data type, rather than just the first data type.</dd>
 * </dl>
 * 
 * A function can be passed for the {@code helper} argument, if different helpers are
 * needed for different data sets. This might be useful if you'd like to pull Power data
 * from one node but Consumption from another, for example. It will be called first
 * without any arguments and should return a {@code sn.nodeUrlHelper} instance to use
 * for the {@link sn.nodeUrlHelper#reportableInterval()} method. Then, for each data
 * type passed in {@code dataTypes} the function will be called again with the <em>data
 * type value</em> and <em>array index</em> as parameters.
 * 
 * @param {sn.nodeUrlHelper|function} helper a URL helper instance, or a function that returns one
 * @param {string[]} dataTypes array of string data types, e.g. 'Power' or 'Consumption'
 * @param {function} [callback] an optional callback; if provided no event will be generated. The
 *                              function will be passed the same object as passed on the event's 
 *                              data property
 */
sn.availableDataRange = function(helper, dataTypes, callback) {
	var urlHelperFn = helper;
	if ( urlHelperFn.reportableInterval !== undefined ) {
		// just turn into a function that returns helper
		urlHelperFn = function() { return helper; };
	}
	
	// if nodeId same for all data types, we can issue a single query, otherwise one query per node ID
	var numRangeQueries = 0,
		lastNodeId,
		q = queue(),
		sourcesRequests = [],
		urlHelper;
	
	dataTypes.forEach(function(e, i) {
		urlHelper = urlHelperFn(e, i);
		if ( urlHelper.nodeId() !== lastNodeId ) {
			q.defer(d3.json, urlHelper.reportableInterval(dataTypes));
			lastNodeId = urlHelper.nodeId();
			numRangeQueries += 1;
		}
		sourcesRequests.push(urlHelperFn(e, i).availableSources(e));
	});
	sourcesRequests.forEach(function(e) {
		q.defer(d3.json, e);
	});
	
	function extractReportableInterval(results) {
		var result, 
			i = 0,
			repInterval;
		for ( i = 0; i < numRangeQueries; i += 1 ) {
			repInterval = results[i];
			if ( repInterval.data === undefined || repInterval.data.endDate === undefined ) {
				sn.log('No data available for node {0}', urlHelperFn(dataTypes[i], i).nodeId());
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
		// turn start/end date strings into actual Date objects;
		// NOTE: we use the date strings here, rather than the available *DateMillis values, because the date strings
		//       are formatted in the node's local time zone, which allows the chart to display the data in OTHER
		//       time zones as if it were also in the node's local time zone.
		var intervalObj = extractReportableInterval(results);// repInterval.data;
		if ( intervalObj.startDate !== undefined ) {
			intervalObj.sDate = sn.dateTimeFormat.parse(intervalObj.startDate);
			intervalObj.sLocalDate = sn.dateTimeFormatLocal.parse(intervalObj.startDate);
		}
		if ( intervalObj.endDate !== undefined ) {
			intervalObj.eDate = sn.dateTimeFormat.parse(intervalObj.endDate);
			intervalObj.eLocalDate = sn.dateTimeFormatLocal.parse(intervalObj.endDate);
		}

		var evt = document.createEvent('Event');
		evt.initEvent('snAvailableDataRange', true, true);
		evt.data = {
				reportableInterval : intervalObj,
				availableSourcesMap : {} // mapping of data type -> sources
		};

		// now extract sources, which start at index numRangeQueries
		var i, len = results.length;
		var response, sourceList;
		function sourceMapper(el) {
			// for historic purposes, the empty source ID is mapped to 'Main'
			return (el === '' ? 'Main' : el);
		}
		function removeDuplicates(el, i, me) {
		    return me.indexOf(el) === i;
		};
		for ( i = numRangeQueries; i < len; i += 1 ) {
			response = results[i];
			if ( response.success !== true || Array.isArray(response.data) !== true || response.data.length < 1 ) {
				sn.log('No sources available for node {0} data type {1}', urlHelperFn(dataTypes[i - numRangeQueries], i - numRangeQueries).nodeId(), dataTypes[i - numRangeQueries]);
				continue;
			}
			sourceList = response.data.map(sourceMapper).filter(removeDuplicates);
			sourceList.sort();
			if ( evt.data.availableSources === undefined ) {
				// add as "default" set of sources, for the first data type
				evt.data.availableSources = sourceList;
			}
			evt.data.availableSourcesMap[dataTypes[i-numRangeQueries]] = sourceList;
		}
		if ( typeof callback === 'function' ) {
			callback(evt.data);
		} else {
			document.dispatchEvent(evt);
		}
	});
};

sn.colorDataLegendTable = function(containerSelector, colorData, clickHandler, labelRenderer) {
	// add labels based on available sources
	var table = d3.select(containerSelector).selectAll('table').data([0]);
	table.enter().append('table').append('tbody');
	
	var labelTableRows = table.select('tbody').selectAll('tr').data(colorData);
	
	var newLabelTableRows = labelTableRows.enter().append('tr');
	
	labelTableRows.exit().remove();
			
	if ( clickHandler ) {
		// attach the event handler for 'click', and add the 'clickable' class
		// so can be styled appropriately (e.g. cursor: pointer)
		newLabelTableRows.on('click', clickHandler).classed('clickable', true);
	}
	
	if ( labelRenderer === undefined ) {
		// default way to render labels is just a text node
		labelRenderer = function(s) {
			s.text(Object);
		};
	}	
	var swatches = labelTableRows.selectAll('td.swatch')
		.data(function(d) { return [d.color]; })
			.style('background-color', Object);
	swatches.enter().append('td')
				.attr('class', 'swatch')
				.style('background-color', Object);
	swatches.exit().remove();
			
	var descriptions = labelTableRows.selectAll('td.desc')
		.data(function(d) { return [(d.source === '' ? 'Main' : d.source)]; })
			.call(labelRenderer);
	descriptions.enter().append('td')
			.attr('class', 'desc')
			.call(labelRenderer);
	descriptions.exit().remove();
};

/**
 * A node-specific URL utility object.
 * 
 * @class
 * @constructor
 * @param nodeId {Number} the node ID to use
 * @returns {sn.nodeUrlHelper}
 */
sn.nodeUrlHelper = function(nodeId) {
	var hostURL = function() {
		return ('http' +(sn.config.tls === true ? 's' : '') +'://' +sn.config.host);
	};
	var baseURL = function() {
		return (hostURL() +sn.config.path +'/api/v1/' +(sn.config.secureQuery === true ? 'sec' : 'pub'));
	};
	var helper = { 
		
		nodeId : function() { return nodeId; },
		
		hostURL : hostURL,
		
		baseURL : baseURL,
		
		reportableInterval : function(types) {
			var t = (Array.isArray(types) && types.length > 0 ? types : ['Power']);
			var url = (baseURL() +'/range/interval?nodeId=' +nodeId
					+ '&' +t.map(function(e) { return 'types='+encodeURIComponent(e); }).join('&'));
			return url;
		},
		
		availableSources : function(type, startDate, endDate) {
			var url = (baseURL() +'/range/sources?nodeId=' +nodeId
						+ '&type=' +encodeURIComponent(type !== undefined ? type : 'Power'));
			if ( startDate !== undefined ) {
				url += '&start=' +encodeURIComponent(sn.dateFormat(startDate));
			}
			if ( endDate !== undefined ) {
				url += '&end=' +encodeURIComponent(sn.dateFormat(endDate));
			}
			return url;
		},
		
		/**
		 * Generate a SolarNet {@code /datum/query} URL.
		 * 
		 * @param type {String} a single supported datum type, or an Array of datum types, to query for
		 * @param startDate {Date} the starting date for the query, or <em>null</em> to omit
		 * @param endDate {Date} the ending date for the query, or <em>null</em> to omit
		 * @param agg {String} a supported aggregate type
		 * @return {String} a URL string
		 */
		dateTimeQuery : function(type, startDate, endDate, agg, opts) {
			var types = (Array.isArray(type) ? type : [type]);
			types.sort();
			var eDate = (opts !== undefined && opts.exclusiveEndDate === true ? d3.time.second.utc.offset(endDate, -1) : endDate);
			var dataURL = (baseURL() +'/datum/query?nodeId=' +nodeId 
                    		+'&type=' +encodeURIComponent(type.toLowerCase()));
			if ( startDate ) {
				dataURL += '&startDate=' +encodeURIComponent(sn.dateTimeFormatURL(startDate));
			}
			if ( endDate ) {
				dataURL += '&endDate=' +encodeURIComponent(sn.dateTimeFormatURL(eDate));
			}
			var aggNum = Number(agg);
			if ( !isNaN(agg) ) {
				dataURL += '&precision=' +aggNum.toFixed(0);
			} else if ( typeof agg === 'string' && agg.length > 0 ) {
				dataURL += '&aggregate=' + encodeURIComponent(agg);
			}
			if ( opts !== undefined ) {
				if ( Array.isArray(opts.sourceIds) ) {
					opts.sourceIds.forEach(function(e) {
						dataURL += '&sourceIds=' + encodeURIComponent(e);
					});
				}
			}
			return dataURL;
		},
		
		/**
		 * Generate a SolarNet {@code /datum/list} URL.
		 * 
		 * @param type {String} a single supported datum type, or an Array of datum types, to query for
		 * @param startDate {Date} the starting date for the query, or <em>null</em> to omit
		 * @param endDate {Date} the ending date for the query, or <em>null</em> to omit
		 * @param agg {String} a supported aggregate type
		 * @return {String} a URL string
		 */
		dateTimeList : function(type, startDate, endDate, agg, opts) {
			var types, 
				eDate = (opts !== undefined && opts.exclusiveEndDate === true && endDate ? d3.time.second.utc.offset(endDate, -1) : endDate), 
				dataURL = baseURL() +'/datum/list?nodeId=' +nodeId;
			if ( type ) {
				types = (Array.isArray(type) ? type : [type]);
				types.sort();
				types.forEach(function(e) {
            		dataURL += '&type=' +encodeURIComponent(e.toLowerCase());
				});
            }
			if ( startDate ) {
				dataURL += '&startDate=' +encodeURIComponent(sn.dateTimeFormatURL(startDate));
			}
			if ( eDate ) {
				dataURL += '&endDate=' +encodeURIComponent(sn.dateTimeFormatURL(eDate));
			}
			if ( typeof agg === 'string' && agg.length > 0 ) {
				dataURL += '&aggregate=' + encodeURIComponent(agg);
			}
			if ( opts !== undefined ) {
				if ( Array.isArray(opts.sourceIds) ) {
					opts.sourceIds.forEach(function(e) {
						dataURL += '&sourceIds=' + encodeURIComponent(e);
					});
				}
				if ( opts.offset > 0 ) {
					dataURL += '&offset=' + encodeURIComponent(opts.offset);
				}
			}
			return dataURL;
		},
		
		mostRecentQuery : function(type) {
			type = (type === undefined ? 'power' : type.toLowerCase());
			var url;
			if ( type === 'weather' ) {
				url = (baseURL() + '/weather/recent?nodeId=');
			} else {
				url = (baseURL() + '/datum/mostRecent?nodeId=');
			}
			url += nodeId;
			if ( type !== 'weather' ) {
				url += '&type=' + encodeURIComponent(type);
			}
			return url;
		},
		
		nodeDashboard : function(source) {
			return ('http://' +sn.config.host +'/solarviz/node-dashboard.do?nodeId=' +nodeId
				 +(source === undefined ? '' : '&consumptionSourceId='+source));
		}
	};
	
	// this is a stand-alone function so we correctly capture the 'prop' name in the loop below
	function setupProxy(prop) {
		helper[prop] = function() {
			return sn.env.nodeUrlHelpers[prop].apply(helper, arguments);
		};
	}
	
	// allow plug-ins to supply URL helper methods, as long as they don't override built-in ones
	(function() {
		var prop,
			helpers = sn.env.nodeUrlHelpers;
		if ( helpers !== undefined ) {
			for ( prop in helpers ) {
				if ( !helpers.hasOwnProperty(prop) || helper[prop] !== undefined || typeof helpers[prop] !== 'function' ) {
					continue;
				}
				setupProxy(prop);
			}
		}
	}());
	
	return helper;
};

/**
 * A configuration utility object.
 * 
 * For any properties passed on {@code initialMap}, getter/setter accessors will be defined
 * on the returned {@code sn.Configuration} instance, so you can use normal JavaScript
 * accessor methods to get/set those values. You can always get/set arbitrary values using
 * the {@link #value(key, newValue)} function.
 * 
 * @class
 * @constructor
 * @param {Object} initialMap the initial properties to store (optional)
 * @returns {sn.Configuration}
 */
sn.Configuration = function(initialMap) {
	this.map = {};
	if ( initialMap !== undefined ) {
		var me = this;
		(function() {
			var createGetter = function(prop) { return function() { return me.map[prop]; }; };
			var createSetter = function(prop) { return function(value) { me.map[prop] = value; }; };
			var prop;
			for ( prop in initialMap ) {
				if ( initialMap.hasOwnProperty(prop) && !me.hasOwnProperty(prop) ) {
					Object.defineProperty(me, prop, {
						enumerable : true,
						configurable : true,
						get : createGetter(prop),
						set : createSetter(prop)
					});
				}
				me.map[prop] = initialMap[prop];
			}
		}());
	}
};
sn.Configuration.prototype = {
	/**
	 * Test if a key is enabled, via the {@link #toggle} function.
	 * 
	 * @param {String} key the key to test
	 * @returns {Boolean} <em>true</em> if the key is enabled
	 */
	enabled : function(key) {
		if ( key === undefined ) {
			return false;
		}
		return (this.map[key] !== undefined);
	},

	/**
	 * Set or toggle the enabled status of a given key.
	 * 
	 * <p>If the <em>enabled</em> parameter is not passed, then the enabled
	 * status will be toggled to its opposite value.</p>
	 * 
	 * @param {String} key they key to set
	 * @param {Boolean} enabled the optional enabled value to set
	 * @returns {sn.Configuration} this object to allow method chaining
	 */
	toggle : function(key, enabled) {
		var val = enabled;
		if ( key === undefined ) {
			return this;
		}
		if ( val === undefined ) {
			// in 1-argument mode, toggle current value
			val = (this.map[key] === undefined);
		}
		return this.value(key, (val === true ? true : null));
	},
	
	/**
	 * Get or set a configuration value.
	 * 
	 * @param {String} key The key to get or set the value for 
	 * @param [newValue] If defined, the new value to set for the given {@code key}.
	 *                   If {@code null} then the value will be removed.
	 * @returns If called as a getter, the associated value for the given {@code key},
	 * otherwise this object.
	 */
	value : function(key, newValue) {
		var me = this;
		if ( arguments.length === 1 ) {
			return this.map[key];
		}
		if ( newValue === null ) {
			delete this.map[key];
			if ( this.hasOwnProperty(key) ) {
				delete this[key];
			}
		} else {
			this.map[key] = newValue;
			if ( !this.hasOwnProperty(key) ) {
				Object.defineProperty(this, key, {
					enumerable : true,
					configurable : true,
					get : function() { return me.map[key]; },
					set : function(value) { me.map[key] = value; }
				});
			}
		}
		return this;
	}
};

/**
 * Utility object for generating "layer" data returned from the
 * {@link sn.powerPerSourceArray} function, suitable for using with 
 * stacked charts.
 * 
 * <p>The returned object is a function, and calling the function causes 
 * a new layer data set to be calculated from the associated data array.
 * The layer data set is a 2D array, the first dimension representing 
 * individual layers and the second dimension the datum values for the
 * associated layer. The datum values are objects with <strong>x</strong>,
 * <strong>y</strong>, and <strong>y0</strong> (the stack offset value).</p>
 * 
 * <p>The returned array also has some properties defined on it:</p>
 * 
 * <dl>
 * <dt>domainX</dt><dd>A 2-element array with the minimum and maximum dates of the 
 * data set. This can be passed to the <code>d3.domain()</code> function for the 
 * <strong>x</strong> domain.</dd>
 * <dt>maxY</dt><dd>The maximum overall <strong>y</strong> coordinate value, across
 * all layers. This can be passed as the maximum value to the <code>d3.domain()</code>
 * function for the <strong>y</strong> domain.</dd>
 * </dl>
 * 
 * <p>A {@link sn.Configuration} object can be used to toggle different layers on or 
 * off in the generated data, by rendering all <strong>y</strong> coordinate values as
 * <strong>0</strong> for disabled layers. This allows the data to transition nicely
 * when toggling layer visibility.</p>
 * 
 * @param {string[]} keyValueSet  array of all possible key values, so that a stable
 *                                set of layer data can be generated
 * @param {string} valueProperty  the name of the property that contains the values to
 *                                use for the y-axis domain
 * @class
 * @constructor
 * @returns {sn.powerPerSourceStackedLayerGenerator}
 */
sn.powerPerSourceStackedLayerGenerator = function(keyValueSet, valueProperty) {
	var sources = keyValueSet;
	var excludeSources;
	var stack = d3.layout.stack();
	var dataArray;
	
	var stackedLayerData = function() {
		if ( dataArray === undefined ) {
			return;
		}
		var layers = stack(sources.map(function(source) {
				var array = dataArray.map(function(d) {
						return {
							x: d.date, 
							y: (excludeSources !== undefined && excludeSources.enabled(source) 
								? 0 : d[source] !== undefined ? +d[source][valueProperty] : null)
						};
					});
				array.source = source;
				return array;
			}));
		layers.domainX = [layers[0][0].x, layers[0][layers[0].length - 1].x];
		layers.maxY = d3.max(layers[layers.length - 1], function(d) { return d.y0 + d.y; });
		return layers;
	};
	
	/**
	 * Get or set the data associated with this generator.
	 * 
	 * @param {array} data the array of data
	 * @return when used as a getter, the data array, otherwise this object
	 *         to allow method chaining
	 * @memberOf sn.powerPerSourceStackedLayerGenerator
	 */
	stackedLayerData.data = function(data) {
		if ( !arguments.length ) { return dataArray; }
		dataArray = data;
		return stackedLayerData;
	};
	
	/**
	 * Get or set the d3 stack offset method.
	 * 
	 * @param {string|function} [value] the offset method, e.g. <code>wiggle</code>
	 * @return the offset value when called as a getter, or this object when called as a setter
	 * @memberOf sn.powerPerSourceStackedLayerGenerator
	 */
	stackedLayerData.offset = function(value) {
		if ( !arguments.length ) { return stack.offset(); }
		stack.offset(value);
		return stackedLayerData;
	};
	
	/**
	 * Get or set the d3 stack order method.
	 * 
	 * @param {string|function} [value] the order method, e.g. <code>inside-out</code>
	 * @return the order value when called as a getter, or this object when called as a setter
	 * @memberOf sn.powerPerSourceStackedLayerGenerator
	 */
	stackedLayerData.order = function(value) {
		if ( !arguments.length ) { return stack.order(); }
		stack.order(value);
		return stackedLayerData;
	};
	
	/**
	 * Get or set a layer visibility configuration object.
	 * 
	 * @param excludeConfiguration {sn.Configuration} a configuration object, where the enabled status
	 *                                                of key values cause that layer to generate with
	 *                                                <strong>y</strong> values all set to <strong>0</strong>
	 * @return when used as a getter, the current configuration object, otherwise this object
	 *         to allow method chaining
	 * @memberOf sn.powerPerSourceStackedLayerGenerator
	 */
	stackedLayerData.excludeSources = function(excludeConfiguration) {
		if ( !arguments.length ) { return excludeSources; }
		excludeSources = excludeConfiguration;
		return stackedLayerData;
	};
	
	return stackedLayerData;
};

/**
 * @typedef sn.sourceColorMapping
 * @type {object}
 * @property {number} [width=812] - desired width, in pixels, of the chart
 * @property {number} [height=300] - desired height, in pixels, of the chart
 * @property {number[]} [padding=[30, 0, 30, 30]] - padding to inset the chart by, in top, right, bottom, left order
 * @property {number} [transitionMs=600] - transition time
 * @property {sn.Configuration} excludeSources - the sources to exclude from the chart
 */

/**
 * @typedef sn.sourceColorMapParameters
 * @type {object}
 * @property {function} [displayDataType] a function that accepts a data type and returns the display
 *                                        version of that data type
 * @property {function} [displayColor] a function that accepts a data type and a Colorbrewer color group
 * @property {boolean} [reverseColors] the Colorbrewer colors are reversed, unless this is set to {@code false}
 */

/**
 * Create mapping of raw sources, grouped by data type, to potentially alternate names,
 * and assign Colorbrewer color values to each source.
 * 
 * The input {@code sourceMap} should contain a mapping of data types to associatd arrays
 * of sources. This is the format returned by {@link sn.availableDataRange}, on the 
 * {@code availableSourcesMap} property. For example:
 * 
 * <pre>
 * {
 *     'Consumption' : [ 'Main', 'Shed' ],
 *     'Power' : [ 'Main' ]
 * }
 * </pre>
 * 
 * The returned {@link sn.sourceColorMapping} object contains 
 * 
 * <pre>
 * {
 *     sourceList : [ 'Consumption / Main', 'Consumption / Shed', 'Power / Main' ]
 *     displaySourceMap : {
 *         Consumption : {
 *             Main : 'Consumption / Main',
 *             Shed : 'Consumption / Shed'
 *         },
 *         Power : {
 *             Main : 'Power / Main'
 *         }
 *     },
 *     reverseDisplaySourceMap : {
 *         Consumption : {
 *             'Consumption / Main' : 'Main',
 *             'Consumption / Shed' : 'Shed'
 *         },
 *         Power : {
 *             'Power / Main' : 'Main',
 *         }
 *     },
 *     colorList : [ 'red', 'light-red', 'green' ]
 *     colorMap : {
 *         'Consumption / Main' : 'red',
 *         'Consumption / Shed' : 'light-red',
 *         'Power / Main' : 'green'
 *     }
 * }
 * </pre>
 * 
 * @params {sn.sourceColorMapParameters} [params] the parameters
 * @returns {sn.sourceColorMapping}
 */
sn.sourceColorMapping = function(sourceMap, params) {
	var p = (params || {});
	var chartSourceMap = {};
	var dataType;
	var sourceList = [];
	var colorGroup;
	var sourceColors = [];
	var typeSourceList = [];
	var colorGroupIndex;
	var colorSlice;
	var result = {};
	var displayDataTypeFn;
	var displaySourceFn;
	var displayColorFn;
	if ( typeof p.displayDataType === 'function' ) {
		displayDataTypeFn = p.displayDataType;
	} else {
		displayDataTypeFn = function(dataType) {
			return (dataType === 'Power' ? 'Generation' : dataType);
		};
	}
	if ( typeof p.displaySource === 'function' ) {
		displaySourceFn = p.displaySource;
	} else {
		displaySourceFn = function(dataType, sourceId) {
			return sourceId;
		};
	}
	if ( typeof p.displayColor === 'function' ) {
		displayColorFn = p.displayColor;
	} else {
		displayColorFn = function(dataType) {
			return (dataType === 'Consumption' ? colorbrewer.Blues : colorbrewer.Greens);
		};
	}
	function mapSources(dtype) {
		sourceMap[dtype].forEach(function(el) {
			var mappedSource;
			if ( el === '' || el === 'Main' ) {
				mappedSource = displayDataTypeFn(dtype);
			} else {
				mappedSource = displayDataTypeFn(dtype) +' / ' +displaySourceFn(dtype, el);
			}
			chartSourceMap[dtype][el] = mappedSource;
			if ( el === 'Main' ) {
				// also add '' for compatibility
				chartSourceMap[dtype][''] = mappedSource;
			}
			typeSourceList.push(mappedSource);
			sourceList.push(mappedSource);
		});
	}
	for ( dataType in sourceMap ) {
		if ( sourceMap.hasOwnProperty(dataType) ) {
			chartSourceMap[dataType] = {};
			typeSourceList.length = 0;
			mapSources(dataType);
			colorGroup = displayColorFn(dataType);
			if ( colorGroup[typeSourceList.length] === undefined ) {
				colorGroupIndex = (function() {
					var i;
					for ( i = typeSourceList.length; i < 30; i += 1 ) {
						if ( colorGroup[i] !== undefined ) {
							return i;
						}
					}
					return 0;
				}());
			} else {
				colorGroupIndex = typeSourceList.length;
			}
			colorSlice = colorGroup[colorGroupIndex].slice(-typeSourceList.length);
			if ( p.reverseColors !== false ) {
				colorSlice.reverse();
			}
			sourceColors = sourceColors.concat(colorSlice);
		}
	}
	
	// create a reverse display mapping
	var reverseDisplaySourceMap = {};
	var sourceId, displayMap;
	for ( dataType in chartSourceMap ) {
		if ( chartSourceMap.hasOwnProperty(dataType) ) {
			reverseDisplaySourceMap[dataType] = {};
			displayMap = chartSourceMap[dataType];
			for ( sourceId in  displayMap ) {
				if ( displayMap.hasOwnProperty(sourceId) ) {
					reverseDisplaySourceMap[displayMap[sourceId]] = sourceId;
				}
			}
		}
	}
	
	result.sourceList = sourceList;
	result.displaySourceMap = chartSourceMap;
	result.reverseDisplaySourceMap = reverseDisplaySourceMap;
	result.colorMap = sn.colorMap(sourceColors, sourceList);
	return result;
};

/**
 * Convert degrees to radians.
 * 
 * @param {number} deg - the degrees value to convert to radians
 * @returns {number} the radians
 */
sn.deg2rad = function(deg) {
	return deg * Math.PI / 180;
};

/**
 * Convert radians to degrees.
 * 
 * @param {number} rad - the radians value to convert to degrees
 * @returns {number} the degrees
 */
sn.rad2deg = function(rad) {
	return rad * 180 / Math.PI;
};

/**
 * Get the width of an element based on a selector, in pixels.
 * 
 * @param {string} selector - a selector to an element to get the width of
 * @returns {number} the width, or {@code undefined} if {@code selector} is undefined, 
 *                   or {@code null} if the width cannot be computed in pixels
 */
sn.pixelWidth = function(selector) {
	if ( selector === undefined ) {
		return undefined;
	}
	var styleWidth = d3.select(selector).style('width');
	if ( !styleWidth ) {
		return null;
	}
	var pixels = styleWidth.match(/([0-9.]+)px/);
	if ( pixels === null ) {
		return null;
	}
	var result = Math.floor(pixels[1]);
	if ( isNaN(result) ) {
		return null;
	}
	return result;
};

/**
 * Get a UTC season constant for a date. Seasons are groups of 3 months, e.g. 
 * Spring, Summer, Autumn, Winter. The returned value will be a number between
 * 0 and 3, where (Dec, Jan, Feb) = 0, (Mar, Apr, May) = 1, (Jun, Jul, Aug) = 2,
 * and (Sep, Oct, Nov) = 3.
 * 
 * @param {Date} date The date to get the season for.
 * @returns a season constant number, from 0 - 3
 */
 sn.seasonForDate = function(date) {
	if ( date.getUTCMonth() < 2 || date.getUTCMonth() === 11 ) {
		return 3;
	} else if ( date.getUTCMonth() < 5 ) {
		return 0;
	} else if ( date.getUTCMonth() < 8 ) {
		return 1;
	} else {
		return 2;
	}
};

/**
 * Generate a seasonal data set, suitable for line and bar charts.
 * 
 * @param {Object[]} dataArray the raw input data
 * @param {Object} sourceIdDataTypeMap object with source ID property names with associated SolarNet DataType
 *                 values, to use to map each raw source ID into its associated data type
 * @param {sn.Configuration} [excludeSources] the source IDs to exclude
 * @param {String[]} [yAxisProperties] the raw input data properties to include in the output data objects,
 *                                     defaults to {@code ['wattHours']}
 */
sn.seasonConsumptionPowerMap = function (dataArray, sourceIdDataTypeMap, excludeSources, yAxisProperties) {
	var yProps = (Array.isArray(yAxisProperties) ? yAxisProperties : ['wattHours']);
	var sources = [];
	var sourceMap = {};
	var seasonMap = (function() {
		var result = {},
			i;
		for ( i = 0; i < 4; i += 1 ) {
			result[String(i)] = {'Consumption':[], 'Power':[]};
		}
		return result;
	}());
	var dataType;
	var dateKey;
	var dateMap = {Consumption:{}, Power:{}};
	var domainY = {};
	var i, iMax;
	var p, pMax = yProps.length;
	var el, obj;
	var date;
	var prop;
	var lines = [];
	for ( i = 0, iMax = dataArray.length; i < iMax; i += 1 ) {
		el = dataArray[i];
		if ( sourceMap[el.sourceId] === undefined ) {
			sources.push(el.sourceId);
			sourceMap[el.sourceId] = 1;
		}
		
		if ( excludeSources !== undefined && excludeSources.enabled(el.sourceId) ) {
			continue;
		}

		dateKey = el.localDate +' ' +el.localTime;
		date = sn.dateTimeFormat.parse(dateKey);
		dataType = sourceIdDataTypeMap[el.sourceId];
		if ( dateMap[dataType][dateKey] === undefined ) {
			obj = {
					date : date,
					hour : date.getUTCHours(),
					day : date.getUTCDay(),
					type : dataType
			};
			// seasons are (northern hemi) [Spring, Summer, Autumn, Winter]
			// set the "season" to an index, 0-3, where 0 -> Dec,Jan,Feb, 1-> Mar,Apr,May, etc
			if ( date.getUTCMonth() < 2 || date.getUTCMonth() === 11 ) {
				obj.season = 3;
			} else if ( date.getUTCMonth() < 5 ) {
				obj.season = 0;
			} else if ( date.getUTCMonth() < 8 ) {
				obj.season = 1;
			} else {
				obj.season = 2;
			}
			
			dateMap[dataType][dateKey] = obj;
			seasonMap[String(obj.season)][dataType].push(obj);
		} else {
			obj = dateMap[dataType][dateKey];
		}
		
		for ( p = 0; p < pMax; p += 1 ) {
			prop = yProps[p];
			if ( obj[prop] === undefined ) {
				obj[prop] = null;
			}
			if ( el[prop] >= 0 ) {
				// map Y value as negative if this source is a consumption source
				obj[prop] += (dataType === 'Consumption' ? -el[prop] : el[prop]);
			}
			
			// compute y extents while iterating through array
			if ( domainY[prop] === undefined ) {
				domainY[prop] = [obj[prop], obj[prop]];
			} else {
				if ( obj[prop] < domainY[prop][0] ) {
					domainY[prop][0] = obj[prop];
				}
				if ( obj[prop] > domainY[prop][1] ) {
					domainY[prop][1] = obj[prop];
				}
			}
		}
	}

	for ( prop in seasonMap ) {
		if ( seasonMap.hasOwnProperty(prop) ) {
			lines.push(seasonMap[prop].Consumption);
			lines.push(seasonMap[prop].Power);
		}
	}

	return {seasonMap: seasonMap, domainY:domainY, lineData:lines, sources:sources};
};

/**
 * Load data for a set of data types, date range, and aggregate level. This object is designed 
 * to be used once per query. After creating the object and configuring an asynchronous
 * callback function with {@link #callback(function)}, call call {@link #load()} to start
 * loading the data. The callback function will be called once all data has been loaded.
 * 
 * @class
 * @param {string[]} dataTypes - array of data types to load data for
 * @param {function} dataTypeUrlHelperProvider - function that returns a {@link sn.nodeUrlHelper} for a given data type
 * @param {date} start - the start date
 * @param {date} end - the end date
 * @param {string} aggregate - optional aggregate level
 * @param {number} precision - optional precision level (for Minute level aggregation only)
 * @returns {sn.datumLoader}
 */
sn.datumLoader = function(dataTypes, dataTypeUrlHelperProvider,  start, end, aggregate, precision) {
	
	var that = {
			version : '1.0.0'
	};

	//var dataTypeSourceMapper = undefined;
	var requestOptions;
	var finishedCallback;
	var holeRemoverCallback;

	var state = {}; // keys are data types, values are 1:loading, 2:done
	var results = {};
	
	function aggregateValue() {
		return (aggregate === undefined ? 'Hour'  : aggregate);
	}
	
	function precisionValue() {
		return (precision === undefined ? 10 : precision);
	}
	
	function requestCompletionHandler(dataType) {
		state[dataType] = 2; // done
		
		// check if we're all done loading, and if so call our callback function
		if ( dataTypes.every(function(e) { return state[e] === 2; }) && finishedCallback ) {
			finishedCallback.call(that, results);
		}
	}

	function loadForDataType(dataType, dataTypeIndex, offset) {
		var urlHelper = dataTypeUrlHelperProvider(dataType, dataTypeIndex);
		var opts = {};
		var key;
		if ( requestOptions ) {
			for ( key in requestOptions ) {
				if ( requestOptions.hasOwnProperty(key) ) {
					opts[key] = requestOptions[key];
				}
			}
		}
		if ( offset ) {
			opts.offset = offset;
		}
		var url;
		var dataExtractor;
		var offsetExtractor;
		if ( aggregateValue() === 'Minute' ) {
			// use /query to normalize minutes; end date is inclusive
			url = urlHelper.dateTimeQuery(dataType, start, end, precisionValue(), opts);
			dataExtractor = function(json) {
				if ( json.success !== true || Array.isArray(json.data) !== true ) {
					return undefined;
				}
				var result = json.data;
				if ( holeRemoverCallback ) {
					result = holeRemoverCallback.call(that, result);
				}
				return result;
			};
			offsetExtractor = function() { return 0; };
		} else {
			// use /list for faster access; end date is exclusive
			url = urlHelper.dateTimeList(dataType, start, end, aggregateValue(), opts);
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
		}
		d3.json(url, function(error, json) {
			var dataArray,
				nextOffset;
			if ( error ) {
				sn.log('Error requesting data for node {0} data type {1}: {2}', urlHelper.nodeId(), dataType, error);
				return;
			}
			dataArray = dataExtractor(json);
			if ( dataArray === undefined ) {
				sn.log('No data available for node {0} data type {1}', urlHelper.nodeId(), dataType);
				if ( requestCompletionHandler ) {
					requestCompletionHandler.call(that, dataType);
				}
				return;
			}
			if ( results[dataType] === undefined ) {
				results[dataType] = dataArray;
			} else {
				results[dataType] = results[dataType].concat(dataArray);
			}
			
			// see if we need to load more results
			nextOffset = offsetExtractor(json);
			if ( nextOffset > 0 ) {
				loadForDataType(dataType, dataTypeIndex, nextOffset);
			} else if ( requestCompletionHandler ) {
				requestCompletionHandler.call(that, dataType);
			}
		});
	}
	
	/**
	 * Get or set the request options object.
	 * 
	 * @param {object} [value] the options to use
	 * @return when used as a getter, the current request options, otherwise this object
	 * @memberOf sn.datumLoader
	 */
	that.requestOptions = function(value) {
		if ( !arguments.length ) { return requestOptions; }
		requestOptions = value;
		return that;
	};

	/**
	 * Get or set the "hole remover" callback function, invoked on data that has been loaded
	 * via the /query API, which "fills" in holes for us. For consistency with the /list API,
	 * we can choose to remove those filled in data points, which can often adversely affect
	 * our desired results.
	 *
	 * The function will be passed a raw array of datum objects as its only parameter. It should
	 * return a new array of datum objects (or an empty array).
	 * 
	 * @param {function} [value] the hole remover function to use
	 * @return when used as a getter, the current hole remover function, otherwise this object
	 * @memberOf sn.datumLoader
	 */
	that.holeRemoverCallback = function(value) {
		if ( !arguments.length ) { return holeRemoverCallback; }
		if ( typeof value === 'function' ) {
			holeRemoverCallback = value;
		}
		return that;
	};
	
	/**
	 * Get or set the callback function, invoked after all data has been loaded.
	 * 
	 * @param {function} [value] the callback function to use
	 * @return when used as a getter, the current callback function, otherwise this object
	 * @memberOf sn.datumLoader
	 */
	that.callback = function(value) {
		if ( !arguments.length ) { return finishedCallback; }
		if ( typeof value === 'function' ) {
			finishedCallback = value;
		}
		return that;
	};
	
	/**
	 * Initiate loading the data.
	 * 
	 * @memberOf sn.datumLoader
	 */
	that.load = function() {
		dataTypes.forEach(function(e) {
			state[e] = 1; // loading
		});
		dataTypes.forEach(function(e, i) {
			loadForDataType(e, i);
		});
		return that;
	};

	return that;
};

/**
 * Get a query range appropriate for using with {@link sn.datumLoader}. Returns an object
 * with <code>start</code> and <code>end</code> Date properties, using the given <code>endDate</code>
 * parameter as the basis for calculating the start as an offset, based on the given <code>aggregate</code>
 * level.
 * 
 * @param {string} aggregate - the aggregate level
 * @param {number} precision - the precision, for the <b>Minute</b> aggregate level
 * @param {object} aggregateTimeCount - either a Number or an Object with Number properties named 
 *                 <code>numXs</code> where <code>X</code> is the aggregate level, representing
 *                 the number of aggregate time units to include in the query
 * @param {Date} endDate - the end date
 * @returns {Object}
 * @since 0.0.4
 */
sn.datumLoaderQueryRange = function(aggregate, precision, aggregateTimeCount, endDate) {
	var end,
		start,
		timeUnit,
		timeCount; 
	
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
	
	if ( aggregate === 'Month' ) {
		timeCount = timeCountValue('numYears');
		timeUnit = 'year';
		end = exclusiveEndDate(d3.time.month, endDate);
		start = d3.time.year.utc.offset(d3.time.month.utc.floor(endDate), -timeCount);
	} else if ( aggregate === 'Day' ) {
		timeCount = timeCountValue('numMonths');
		timeUnit = 'month';
		end = exclusiveEndDate(d3.time.day, endDate);
		start = d3.time.month.utc.offset(d3.time.day.utc.floor(endDate), -timeCount);
	} else if ( aggregate === 'Hour' ) {
		timeCount = timeCountValue('numDays');
		timeUnit = 'day';
		end = exclusiveEndDate(d3.time.hour, endDate);
		start = d3.time.day.utc.offset(d3.time.hour.utc.floor(end), -timeCount);
	} else {
		// assume Minute
		timeCount = timeCountValue('numHours');
		timeUnit = 'hour';
		end = d3.time.minute.utc.ceil(endDate);
		end.setUTCMinutes((end.getUTCMinutes() + precision - (end.getUTCMinutes() % precision)), 0, 0);
		start = d3.time.hour.utc.offset(end, -timeCount);
	}
	return {
		start : start, 
		end : end, 
		timeUnit : timeUnit, 
		timeCount : timeCount
	};
};

/**
 * Normalize the data arrays resulting from a <code>d3.nest</code> operation so that all
 * group value arrays have the same number of elements, based on a Date property named 
 * <code>date</code>. The data values are assumed to be sorted by <code>date</code> already.
 * The value arrays are modified in-place. This makes the data suitable to passing to 
 * <code>d3.stack</code>, which expects all stack data arrays to have the same number of 
 * values, for the same keys.
 * 
 * The <code>layerData</code> parameter should look something like this:
 * 
 * <pre>[
 *   { key : 'A', values : [{date : Date(2011-12-02 12:00)}, {date : Date(2011-12-02 12:10)}] },
 *   { key : 'B', values : [{date : Date(2011-12-02 12:00)}] }
 * ]</pre>
 * 
 * After calling this method, <code>layerData</code> would look like this (notice the 
 * filled in secod data value in the <b>B</b> group):
 * 
 * <pre>[
 *   { key : 'A', values : [{date : Date(2011-12-02 12:00)}, {date : Date(2011-12-02 12:10)}] },
 *   { key : 'B', values : [{date : Date(2011-12-02 12:00)}, {date : Date(2011-12-02 12:10)}] }] }
 * ]</pre>
 * 
 * @param {array} layerData - An arry of objects, each object with a <code>key</code> group ID
 *                            and a <code>values</code> array of data objects.
 * @param {object} fillTemplate - An object to use as a template for any "filled in" data objects.
 *                                The <code>date</code> property will be populated automatically.
 *
 * @param {array} fillFn - An optional function to fill in objects with.
 * @since 0.0.4
 */
sn.nestedStackDataNormalizeByDate = function(layerData, fillTemplate, fillFn) {
	var i = 0,
		j,
		k,
		jMax = layerData.length - 1,
		dummy,
		prop,
		copyIndex;
	// fill in "holes" for each stack, if more than one stack. we assume data already sorted by date
	if ( jMax > 0 ) {
		while ( i < d3.max(layerData.map(function(e) { return e.values.length; })) ) {
			dummy = undefined;
			for ( j = 0; j <= jMax; j++ ) {
				if ( layerData[j].values.length <= i ) {
					continue;
				}
				if ( j < jMax ) {
					k = j + 1;
				} else {
					k = 0;
				}
				if ( layerData[k].values.length <= i || layerData[j].values[i].date.getTime() < layerData[k].values[i].date.getTime() ) {
					dummy = {date : layerData[j].values[i].date, sourceId : layerData[k].key};
					if ( fillTemplate ) {
						for ( prop in fillTemplate ) {
							if ( fillTemplate.hasOwnProperty(prop) ) {
								dummy[prop] = fillTemplate[prop];
							}
						}
					}
					if ( fillFn ) {
						copyIndex = (layerData[k].values.length > i ? i : i > 0 ? i - 1 : null);
						fillFn(dummy, layerData[k].key, (copyIndex !== null ? layerData[k].values[copyIndex] : undefined));
					}
					layerData[k].values.splice(i, 0, dummy);
				}
			}
			if ( dummy === undefined ) {
				i++;
			}
		}
	}
};

/**
 * Get an appropriate display scale for a given value. This will return values suitable
 * for passing to {@link sn.displayUnitsForScale}.
 * 
 * @param {Number} value - The value, for example the maximum value in a range of values, 
 *                         to get a display scale factor for.
 * @return {Number} A display scale factor.
 * @since 0.0.7
 */
sn.displayScaleForValue = function(value) {
	var result = 1, num = Number(value);
	if ( isNaN(num) === false ) {
		if ( value >= 1000000000 ) {
			result = 1000000000;
		} else if ( value >= 1000000 ) {
			result = 1000000;
		} else if ( value >= 1000 ) {
			result = 1000;
		}
	}
	return result;
};

/**
 * Get an appropriate display unit for a given base unit and scale factor.
 *
 * @param {String} baseUnit - The base unit, for example <b>W</b> or <b>Wh</b>.
 * @param {Number} scale - The unit scale, which must be a recognized SI scale, such 
 *                         as <b>1000</b> for <b>k</b>.
 * @return {String} A display unit value.
 * @since 0.0.7
 */
sn.displayUnitsForScale = function(baseUnit, scale) {
	return (scale === 1000000000 ? 'G' 
			: scale === 1000000 ? 'M' 
			: scale === 1000 ? 'k' 
			: '') + baseUnit;
};

/**
 * Set the display units within a d3 selection based on a scale. This method takes a 
 * base unit and adds an SI prefix based on the provided scale. It replaces the text
 * content of any DOM node with a <code>unit</code> class that is a child of the given
 * selection.
 * 
 * @param {object} selection - A d3 selection that serves as the root search context.
 * @param {string} baseUnit - The base unit, for example <b>W</b> or <b>Wh</b>.
 * @param {number} scale - The unit scale, which must be a recognized SI scale, such 
 *                         as <b>1000</b> for <b>k</b>.
 * @param {string} unitKind - Optional text to replace all occurrences of <code>.unit-kind</code>
 *                            elements with.
 * @since 0.0.4
 */
sn.adjustDisplayUnits = function(selection, baseUnit, scale, unitKind) {
	var unit = sn.displayUnitsForScale(baseUnit, scale);
	selection.selectAll('.unit').text(unit);
	if ( unitKind !== undefined ) {
		selection.selectAll('.unit-kind').text(unitKind);
	}
};

/**
 * Combine the layers resulting from a d3.nest() operation into a single, aggregated
 * layer. This can be used to combine all sources of a single data type, for example
 * to show all "power" sources as a single layer of chart data. The resulting object
 * has the same structure as the input <code>layerData</code> parameter, with just a
 * single layer of data.
 * 
 * @param {object} layerData - An object resulting from d3.nest().entries()
 * @param {string} resultKey - The <code>key</code> property to assign to the returned layer.
 * @param {array} copyProperties - An array of string property names to copy as-is from 
 *                                 the <b>first</b> layer's data values.
 * @param {array} sumProperties - An array of string property names to add together from
 *                                <b>all</b> layer data.
 * @param {object} staticProperties - Static properties to copy as-is to all output data.
 * @return {object} An object with the same structure as returned by d3.nest().entries()
 * @since 0.0.4
 */
sn.aggregateNestedDataLayers = function(layerData, resultKey, copyProperties, sumProperties, staticProperties) {
	// combine all layers into a single source
	var layerCount = layerData.length,
		dataLength,
		i,
		j,
		k,
		copyPropLength = copyProperties.length,
		sumPropLength = sumProperties.length,
		d,
		val,
		clone,
		array;

	dataLength = layerData[0].values.length;
	if ( dataLength > 0 ) {
		array = [];
		for ( i = 0; i < dataLength; i += 1 ) {
			d = layerData[0].values[i];
			clone = {};
			if ( staticProperties !== undefined ) {
				for ( val in staticProperties ) {
					if ( staticProperties.hasOwnProperty(val) ) {
						clone[val] = staticProperties[val];
					}
				}
			}
			for ( k = 0; k < copyPropLength; k += 1 ) {
				clone[copyProperties[k]] = d[copyProperties[k]];
			}
			for ( k = 0; k < sumPropLength; k += 1 ) {
				clone[sumProperties[k]] = 0;
			}
			for ( j = 0; j < layerCount; j += 1 ) {
				for ( k = 0; k < sumPropLength; k += 1 ) {
					val = layerData[j].values[i][sumProperties[k]];
					if ( val !== undefined ) {
						clone[sumProperties[k]] += val;
					}
				}
			}
			array.push(clone);
		}
		layerData = [{ key : resultKey, values : array }];
	}

	return layerData;
};

/**
 * Get a proxy method for a "super" class' method on the `this` objct.
 * 
 * @param {String} name - The name of the method to get a proxy for.
 * @returns {Function} A function that calls the `name` function of the `this` object.
 * @since 0.0.4
 */
sn.superMethod = function(name) {
	var that = this,
		method = that[name];
	return function() {
		return method.apply(that, arguments);
    };
};

/**
 * Utility functions.
 * @namespace
 */
sn.util = {};

/**
 * Copy the enumerable own properties of `obj1` onto `obj2` and return `obj2`.
 * 
 * @param {Object} obj1 - The object to copy enumerable properties from.
 * @param {Object} [obj2] - The optional object to copy the properties to. If not
 *                          provided a new object will be created.
 * @returns {Object} The object whose properties were copied to.
 * @since 0.0.5
 */
sn.util.copy = function(obj1, obj2) {
	var prop, desc;
	if ( obj2 === undefined ) {
		obj2 = {};
	}
	for ( prop in obj1 ) {
		if ( obj1.hasOwnProperty(prop) ) {
			desc = Object.getOwnPropertyDescriptor(obj1, prop);
			if ( desc ) {
				Object.defineProperty(obj2, prop, desc);
			} else {
				obj2[prop] = obj1[prop];
			}
		}
	}
	return obj2;
};

/**
 * Copy the enumerable and non-enumerable own properties of `obj` onto `obj2` and return `obj2`.
 * 
 * @param {Object} obj1 - The object to copy enumerable properties from.
 * @param {Object} [obj2] - The optional object to copy the properties to. If not
 *                          provided a new object will be created.
 * @returns {Object} The object whose properties were copied to.
 * @since 0.0.5
 */
sn.util.copyAll = function(obj1, obj2) {
	var prop,
		keys = Object.getOwnPropertyNames(obj1),
		i, len,
		key,
		desc;
	if ( obj2 === undefined ) {
		obj2 = {};
	}
	for ( i = 0, len = keys.length; i < len; i += 1 ) {
		key = keys[i];
		desc = Object.getOwnPropertyDescriptor(obj1, key);
		if ( desc ) {
			Object.defineProperty(obj2, key, desc);
		} else {
			obj2[key] = obj1[key];
		}
	}
	return obj2;
};

// parse URL parameters into sn.env, e.g. ?nodeId=11 puts sn.env.nodeId === '11'
if ( window !== undefined && window.location.search !== undefined ) {
	sn.env = sn.parseURLQueryTerms(window.location.search);
}
if (typeof define === "function" && define.amd) {
	define(sn);
} else if (typeof module === "object" && module.exports) {
	module.exports = sn;
} else {
	window.sn = sn;
}
}());
