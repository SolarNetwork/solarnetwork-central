var SNAPI = {};

SNAPI.shouldIncludeContentMD5 = function(contentType) {
	// we don't send Content-MD5 for form data, because server treats this as URL parameters
	return (contentType !== null && contentType.indexOf('application/x-www-form-urlencoded') < 0);
};

/**
 * Generate the raw authorization message value.
 *
 * This value produces the data that must be hashed to form the HTTP
 * authorization data value.
 *
 * @param {Object} params the request parameters
 * @param {String} params.method the HTTP request method, or 'GET' if not defined
 * @param {String} params.contentType the HTTP content type, for HTTP POST requests
 * @param {String} params.data the HTTP request body data
 * @param {String} params.date the formatted HTTP request date
 * @param {String} params.path the SolarNetworkWS canonicalized path value
 * @return {String} the authorization message value
 */
SNAPI.generateAuthorizationMessage = function(params) {
	var msg =
		(params.method === undefined ? 'GET' : params.method.toUpperCase()) + '\n'
		+(params.data !== undefined && SNAPI.shouldIncludeContentMD5(params.contentType) ? CryptoJS.MD5(params.data) : '') + '\n'
		+(params.contentType === undefined ? '' : params.contentType) + '\n'
		+params.date +'\n'
		+params.path;
	return msg;
};

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
 * @param {String} params.token the authentication token
 * @param {String} params.secret the authentication token secret
 * @return {String} the authorization header value
 * @see SNAPI.generateAuthorizationMessage()
 */
SNAPI.generateAuthorizationHeaderValue = function(params) {
	var msg = SNAPI.generateAuthorizationMessage(params);
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
SNAPI.parseURLQueryTerms = function(search) {
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
				params[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1]);
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
SNAPI.authURLPath = function(url, data) {
	var a = document.createElement('a');
	a.href = url;
	var path = a.pathname;

	// handle query params, which must be sorted
	var params = SNAPI.parseURLQueryTerms(data === undefined ? a.search : data);
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
 * header values needed to invoke the web service. It returns a jQuery AJAX object,
 * so you can call <code>.done()</code> or <code>.fail()</code> on that to handle
 * the response.</p>
 *
 * @param {String} url the web service URL to invoke
 * @param {String} method the HTTP method to use; e.g. GET or POST
 * @param {String} data the HTTP data to send, e.g. for POST
 * @param {String} contentType the HTTP content type; defaults to
 *                             <code>application/x-www-form-urlencoded; charset=UTF-8</code>
 * @return {Object} jQuery AJAX object
 */
SNAPI.requestJSON = function(url, method, data, contentType) {
	return SNAPI.request(url, 'json', method, data, contentType);
};

/**
 * Invoke the web service URL, adding the required SolarNetworkWS authorization
 * headers to the request.
 *
 * <p>This method will construct the <code>X-SN-Date</code> and <code>Authorization</code>
 * header values needed to invoke the web service. It returns a jQuery AJAX object,
 * so you can call <code>.done()</code> or <code>.fail()</code> on that to handle
 * the response.</p>
 *
 * @param {String} url the web service URL to invoke
 * @param {String} dataType one of 'json', 'xml', or 'csv'
 * @param {String} method the HTTP method to use; e.g. GET or POST
 * @param {String} data the HTTP data to send, e.g. for POST
 * @param {String} contentType the HTTP content type; defaults to
 *                             <code>application/x-www-form-urlencoded; charset=UTF-8</code>
 * @return {Object} jQuery AJAX object
 */
SNAPI.request  = function(url, dataType, method, data, contentType) {
	method = (method === undefined ? 'GET' : method.toUpperCase());
	var cType = (data && contentType === undefined
			? 'application/x-www-form-urlencoded; charset=UTF-8' : contentType);
	var accepts;
	var dType;
	if ( dataType === 'csv' ) {
		accepts = {text: 'text/csv'};
		dType = 'text';
	} else if ( dataType === 'xml' ) {
		accepts = {text: 'text/xml'};
		dType = 'text';
	} else {
		accepts = {json: 'application/json'};
		dType = 'json';
	}
	var ajax = $.ajax({
		type: method,
		url: url,
		accepts: accepts,
		dataType: dType,
		data: data,
		contentType: cType,
		beforeSend: function(xhr) {
			// get a date, which we must include as a header as well as include in the
			// generated authorization hash
			var date = new Date().toUTCString();

			// construct our canonicalized path value from our URL
			var path = SNAPI.authURLPath(url, (cType !== undefined && cType.indexOf('application/x-www-form-urlencoded') === 0 ? data : undefined));

			// generate the authorization hash value now (cryptographically signing our request)
			var auth = SNAPI.generateAuthorizationHeaderValue({
				method: method,
				date: date,
				path: path,
				token: SNAPI.ajaxCredentials.token,
				secret: SNAPI.ajaxCredentials.secret,
				data: data,
				contentType: cType
			});

			// set the headers on our request
			xhr.setRequestHeader('X-SN-Date', date);
			xhr.setRequestHeader('Authorization', 'SolarNetworkWS ' +auth);
			if ( data !== undefined && SNAPI.shouldIncludeContentMD5(cType) ) {
				xhr.setRequestHeader('Content-MD5', CryptoJS.MD5(data));
			}
		}
	});
	return ajax;
};

$(document).ready(function() {
	if ( window !== undefined && window.location.protocol !== undefined && window.location.protocol.toLowerCase().indexOf('http') === 0 ) {
		$('#credentials input[name=host]').val(window.location.protocol +'//'
			+window.location.host +(window.location.port && (Number(window.location.port) !== 80 || Number(window.location.port) !== 443) ? ':' +window.location.port : ''));
	}

	var getCredentials = function() {
		var form = $('#credentials')[0];
		var params = {
				token: form.elements['token'].value,
				secret: form.elements['secret'].value,
				host: form.elements['host'].value
			};
		if ( form.elements['date'].value.length > 0 ) {
			params.date = form.elements['date'].value;
		} else {
			params.date = new Date().toUTCString();
		}
		return params;
	};

	var showResult = function(msg) {
		var resultEl = $('#result');
		resultEl.text(msg).removeClass('prettyprinted');
		window.prettyPrint(resultEl.get(0));
	};

	var showAuthSupport = function(params) {
		var url = params.host +params.path;
		var cType = (params.data && params.contentType === undefined
				? 'application/x-www-form-urlencoded; charset=UTF-8' : params.contentType);
		var path = SNAPI.authURLPath(url, (cType !== undefined && cType.indexOf('application/x-www-form-urlencoded') === 0 ? params.data : undefined));
		var submitParams = {
			method: params.method,
			date: params.date,
			path: path,
			token: params.token,
			secret: params.secret,
			data: params.data,
			contentType: cType
		};
		var authHeader = SNAPI.generateAuthorizationHeaderValue(submitParams);

		$('#auth-header').text('Authorization: SolarNetworkWS ' +authHeader);
		$('#auth-message').text(SNAPI.generateAuthorizationMessage(submitParams));
		$('#curl-command').text('curl '
				+'-H \'Accept: ' +(params.output === 'xml' ? 'text/xml'
						: params.output === 'csv' ? 'text/csv' : 'application/json') +'\' '
				+'-H \'X-SN-Date: '+params.date +'\' -H \'Authorization: SolarNetworkWS '
	   			+authHeader +'\' \'' +params.host +params.path +'\''
	   			+(params.data !== undefined && params.method === 'POST'
	   				? ' -H \'Content-Type: ' + cType +'\' -d \'' +params.data +'\''
	   				: ''));
	};

	$('#shortcuts').change(function(event) {
		event.preventDefault();
		var form = this.form,
			val = this.value,
			method;
		if ( $(form).find('input[name=useAuth]:checked').val() === 'false' ) {
			val = val.replace(/\/sec\//, '/pub/');
		}
		form.elements['path'].value = val;
		method = $(this.options[this.selectedIndex]).data('method');
		method = (method || 'GET');
		$(form).find('input[name=method]').removeAttr('checked');
		$(form).find('input[name=method][value='+method+']').trigger('click');
	});

	$('#auth-result-toggle').click(function(event) {
		var btn = $(this);
		if ( btn.hasClass('fa-caret-down') ) {
			btn.addClass('fa-caret-right');
			btn.removeClass('fa-caret-down');
			$('#auth-result-container').hide();
		} else {
			btn.addClass('fa-caret-down');
			btn.removeClass('fa-caret-right');
			$('#auth-result-container').show();
		}
	});

	$('input[name=useAuth]').change(function(event) {
		event.preventDefault();
		var form = this.form,
			val = form.elements['path'].value,
			credForm = $('#credentials')[0];
		if ( $(this).val() === 'true' ) {
			val = val.replace(/\/pub\//, '/sec/');
			$(credForm.elements['token']).removeAttr('disabled');
			$(credForm.elements['secret']).removeAttr('disabled');
			$('#auth-result').show();
		} else {
			val = val.replace(/\/sec\//, '/pub/');
			$(credForm.elements['token']).attr('disabled', 'disabled');
			$(credForm.elements['secret']).attr('disabled', 'disabled');
			$('#auth-result').hide();
		}
		form.elements['path'].value = val;
	});

	$('input[name=method]').change(function(event) {
		event.preventDefault();
		var val = $(this).val();
		if ( val === 'POST' || val === 'PUT' || val === 'PATCH' ) {
			$('#upload').show();
		} else {
			$('#upload').hide();
		}
	});

	var formatXml = function(xml) {
		var formatted = '';
		var reg = /(>)(<)(\/*)/g;
		xml = xml.replace(reg, '$1\r\n$2$3');
		var pad = 0;
		jQuery.each(xml.split('\r\n'), function(index, node) {
			var indent = 0;
			if (node.match( /.+<\/\w[^>]*>$/ )) {
				indent = 0;
			} else if (node.match( /^<\/\w/ )) {
				if (pad != 0) {
					pad -= 1;
				}
			} else if (node.match( /^<\w[^>]*[^\/]>.*$/ )) {
				indent = 1;
			} else {
				indent = 0;
			}

			var padding = '';
			for (var i = 0; i < pad; i++) {
				padding += '  ';
			}

			formatted += padding + node + '\r\n';
			pad += indent;
		});

		return formatted;
	};

	var textForDisplay = function(xhr, output) {
		var result = '';
		if ( xhr.status >= 400 && xhr.status < 500 ) {
			result = 'Unauthorized.';
		} else if ( xhr.responseXML !== undefined ) {
			result = $('<div/>').text(formatXml(xhr.responseText)).html();
		} else if ( xhr.responseText ) {
			if ( output === 'json' ) {
				result = JSON.stringify(JSON.parse(xhr.responseText), null, 2);
			} else {
				result = xhr.responseText;
			}
		}
		return result;
	};

	$('#generic-path').submit(function(event) {
		event.preventDefault();
		var form = this;
		var params = getCredentials();
		params.method = $(form).find('input[name=method]:checked').val();
		params.output = $(form).find('input[name=output]:checked').val();
		params.path = form.elements['path'].value;
		if ( params.method === 'POST' || params.method === 'PUT' || params.method === 'PATCH' ) {
			params.data = $(form).find('textarea[name=upload]').val();
			if ( params.data.length < 1 ) {
				// move any parameters into post body
				var a = document.createElement('a');
				a.href = params.path;
				params.path = a.pathname;
				params.data = a.search;
				if ( params.data.indexOf('?') === 0 ) {
					params.data = params.data.substring(1);
				}
				params.contentType = 'application/x-www-form-urlencoded; charset=UTF-8';
			} else {
				// assume content type is json if post body provided
				params.contentType = 'application/json; charset=UTF-8';
			}
			if ( params.data !== undefined && params.data.length < 1 ) {
				delete params.data;
				delete params.contentType;
			}
		}
		SNAPI.ajaxCredentials = params;

		// show some developer info in the auth-message area
		showAuthSupport(params);

		$('#result').empty();

		// make HTTP request and show the results
		SNAPI.request(params.host +params.path, params.output, params.method, params.data, params.contentType).done(function (data, status, xhr) {
			showResult(textForDisplay(xhr, params.output));
		}).fail(function(xhr, status, reason) {
			showResult(textForDisplay(xhr, params.output));
			alert(reason + ': ' +status +' (' +xhr.status +')');
		});
	});
});
