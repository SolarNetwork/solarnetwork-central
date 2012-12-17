var SNDemo = {};

/**
 * Generate the authorization header value for a set of request parameters.
 * 
 * <p>This returns just the authorization header value, without the scheme. For 
 * example this might return a value like 
 * <code>a09sjds09wu9wjsd9uya:6U2NcYHz8jaYhPd5Xr07KmfZbnw=</code>. To use
 * as a valid <code>Authorization</code> header, you must still prefix the
 * returned value with <code>SolarNetworkWS</code> (with a space between
 * that prefix and the associated value).</p>
 * 
 * <p>Note that the <b>Content-MD5</b> and <b>Content-Type</b> headers are <b>not</b>
 * supported.</p>
 * 
 * @param {Object} params the request parameters
 * @param {String} params.method the HTTP request method
 * @param {String} params.date the formatted HTTP request date
 * @param {String} params.path the SolarNetworkWS canonicalized path value
 * @param {String} params.token the authentication token
 * @param {String} params.secret the authentication token secret
 * @return {String} the authorization header value
 */
SNDemo.generateAuthorizationHeaderValue = function(params) {
	var msg = 
		(params.method === undefined ? 'GET' : params.method.toUpperCase()) + '\n\n\n'
		+params.date +'\n'
		+params.path;
	var hash = CryptoJS.HmacSHA1(msg, params.secret);
	var authHeader = params.token +':' +CryptoJS.enc.Base64.stringify(hash);
	return authHeader;
};

/**
 * Parse the query portion of a URL string, and return a parameter object for the
 * parsed key/value pairs.
 * 
 * <p>Multiple parameters of the same name are <b>not</b> supported.</p>
 * 
 * @param {String} search the query portion of the URL, which may optionally include 
 *                        the leading '?' character
 * @return {Object} the parsed query parameters, as a parameter object
 */
SNDemo.parseURLQueryTerms = function(search) {
	var params = {};
	var pairs;
	var pair;
	var i, len;
	if ( search !== undefined && search.length > 0 ) {
		// remove any leading ? character
		if ( search.match(/^\?/) ) {
			search = search.substring(1);
		}
		pairs = search.split('&');
		for ( i = 0, len = pairs.length; i < len; i++ ) {
			pair = pairs[i].split('=', 2);
			if ( pair.length === 2 ) {
				params[pair[0]] = pair[1];
			}
		}
	}
	return params;
};

/**
 * Generate the SolarNetworkWS path required by the authorization header value.
 * 
 * <p>This method will parse the given URL and then apply the path canonicalization
 * rules defined by the SolarNetworkWS scheme.</p>
 * 
 * @param {String} url the request URL
 * @return {String} path the canonicalized path value to use in the SolarNetworkWS 
 *                       authorization header value
 */
SNDemo.authURLPath = function(url) {
	var a = document.createElement('a');
	a.href = url;
	var path = a.pathname;
	
	// handle query params, which must be sorted
	var params = SNDemo.parseURLQueryTerms(a.search);
	var sortedKeys = [], key = undefined;
	var i, len;
	var first = true;

	for ( key in params ) {
		sortedKeys.push(key);
	}
	sortedKeys.sort();
	if ( sortedKeys.length > 0 ) {
		path += '?';
		for ( i = 0, len = sortedKeys.length; i < len; i++ ) {
			if ( first ) {
				first = false;
			} else {
				path += '&';
			}
			path +=  sortedKeys[i];
			path += '=';
			path += params[sortedKeys[i]];
		}
	}
	return path;
};

/**
 * Invoke the web service URL, adding the required SolarNetworkWS authorization
 * headers to the request.
 * 
 * <p>This method will construct the <code>X-SN-Date</code> and <code>Authorization</code>
 * header values needed to invoke the web service, the invoke it and call the provided
 * callback function on success. The callback function is passed to the jQuery 
 * <code>done()</code> function, so accepts the same parameters as that function.</p> 
 * 
 * @param {String} url the web service URL to invoke
 * @param {Function} callback the function to call on success
 */
SNDemo.requestJSON = function(url, callback, method) {
	method = (method === undefined ? 'GET' : method);
	$.ajax({
		type: method,
		url: url,
		dataType: 'json',
		beforeSend: function(xhr) {
			// get a date, which we must include as a header as well as include in the 
			// generated authorization hash
			var date = new Date().toUTCString();
			
			// construct our canonicalized path value from our URL
			var path = SNDemo.authURLPath(url);
			
			// generate the authorization hash value now (cryptographically signing our request)
			var auth = SNDemo.generateAuthorizationHeaderValue({
				method: method,
				date: date,
				path: path,
				token: SNDemo.ajaxCredentials.token,
				secret: SNDemo.ajaxCredentials.secret
			});
			
			// set the headers on our request
			xhr.setRequestHeader('X-SN-Date', date);
			xhr.setRequestHeader('Authorization', 'SolarNetworkWS ' +auth);
		}
	}).done(callback).fail(function(xhr, status, reason) {
		alert(reason + ': ' +status +' (' +xhr.status +')');
	});
};

$(document).ready(function() {
	var getCredentials = function() {
		var form = $('#credentials')[0];
		var params = {
				token: form.elements['token'].value,
				secret: form.elements['secret'].value
			};
		if ( form.elements['date'].value.length > 0 ) {
			params.date = form.elements['date'].value;
		} else {
			params.date = new Date().toUTCString();
		}
		return params;
	};

	var showResult = function(msg) {
		$('#result').val(msg);
	};

	$('#generic-path').submit(function(event) {
		event.preventDefault();
		var form = this;
		var params = getCredentials();
		params.method = $(form).find('input[name=method]:checked').val();
		params.path = form.elements['path'].value;
		SNDemo.ajaxCredentials = params;
		var authHeader = SNDemo.generateAuthorizationHeaderValue(params);
 	   	showResult('Date: ' +params.date 
	   		+'\nAuthorization: ' +authHeader
	   		+'\nCurl: ' +'curl -H "X-SN-Date: '+params.date +'" -H "Authorization: SolarNetworkWS ' 
	   			+authHeader +'" http://localhost:8680' +params.path);
 	   	SNDemo.requestJSON('http://localhost:8680'+params.path, function(data) {
 	   		showResult(JSON.stringify(data));
 	   	}, params.method);
	});
});
