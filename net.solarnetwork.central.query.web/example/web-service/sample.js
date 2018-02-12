'use strict';

var SNAPI = {};

SNAPI.encodeURIComponent = function(str) {
  return encodeURIComponent(str).replace(/[!'()*]/g, function(c) {
    return '%' + c.charCodeAt(0).toString(16).toUpperCase();
  });
};

SNAPI.shouldIncludeContentDigest = function(contentType) {
	// we don't send Content-MD5/Digest for form data, because server treats this as URL parameters
	return (contentType && contentType.indexOf('application/x-www-form-urlencoded') < 0);
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
 * @param {Date} params.date the HTTP request date
 * @param {String} params.path the SolarNetworkWS canonicalized path value
 * @return {String} the authorization message value
 */
SNAPI.generateAuthorizationMessage = function(params) {
	// first line always HTTP verb
	var msg = (params.method === undefined ? 'GET' : params.method.toUpperCase()) + '\n';
	if ( params.authType < 2 ) {
	 	msg +=
	 		(params.data !== undefined && SNAPI.shouldIncludeContentDigest(params.contentType)
					? CryptoJS.enc.Base64.stringify(CryptoJS.MD5(params.data)) : '') + '\n'
			+(params.contentType === undefined ? '' : params.contentType) + '\n'
			+params.date.toUTCString() +'\n'
			+params.path;
	} else {
		// 2: Canonical URI
		msg += params.path  + '\n';

		// 3: Canonical query string
		msg += params.query + '\n';

		// 4: Canonical headers
		params.signedHeaderNames.forEach(function(headerName) {
			msg += headerName + ':' + params.signedHeaders[headerName] +'\n';
		});

		// 5: Signed headers
		msg += params.signedHeaderNames.join(';') + '\n';

		// 6: Content SHA256 as Hex
		msg += CryptoJS.enc.Hex.stringify(CryptoJS.SHA256(
			params.data !== undefined && SNAPI.shouldIncludeContentDigest(params.contentType) ? params.data : ''));
	}
	return msg;
};

/**
 * Generate a semicolon-delimited string from the signed header names of a request.
 *
 * @param {Number} params.authType The authentication type, e.g. 1 for V1, 2 for V2.
 * @param {Date} params.date The request date.
 * @param {String} params.secret The token secret key.
 * @return {Object} The secret key to use for signing the request.
 */
SNAPI.signingSecretKey = function(params) {
	if ( params.authType > 1 ) {
		// for V2, the secret key is derived from the token secret like
		// HmacSHA256("snws2_request", HmacSHA256("20160301", "SNWS2"+tokenSecretKey))
		var dateString = SNAPI.iso8601Date(params.date);
		return CryptoJS.HmacSHA256('snws2_request', CryptoJS.HmacSHA256(dateString, 'SNWS2' + params.secret));
	} else {
		// for V1, the secret key is the token secret
		return params.secret;
	}
}

SNAPI.setupHeaderNamesToSign = function(params) {
	if ( params.authType < 2 ) {
		params.signedHeaderNames = [];
		params.signedHeaders = {};
		return;
	}
	params.signedHeaderNames = ['host', 'x-sn-date'];
	params.signedHeaders = { host : SNAPI.hostHeaderValue(params.host), 'x-sn-date' : params.date.toUTCString() };
	if ( params.contentType ) {
		params.signedHeaderNames.push('content-type');
		params.signedHeaders['content-type'] = params.contentType;
	}
	if ( SNAPI.shouldIncludeContentDigest(params.contentType) ) {
		params.signedHeaderNames.push('digest');
		params.signedHeaders['digest'] = 'sha-256='+CryptoJS.enc.Base64.stringify(CryptoJS.SHA256(params.data));
	}
	params.signedHeaderNames.sort();
};

SNAPI.iso8601Date = function(date, includeTime) {
	return ''+date.getUTCFullYear()
			+(date.getUTCMonth() < 9 ? '0' : '') +(date.getUTCMonth()+1)
			+(date.getUTCDate() < 10 ? '0' : '') + date.getUTCDate()
			+(includeTime ?
				'T'
				+(date.getUTCHours() < 10 ? '0' : '') + date.getUTCHours()
				+(date.getUTCMinutes() < 10 ? '0' : '') + date.getUTCMinutes()
				+(date.getUTCSeconds() < 10 ? '0' : '') +date.getUTCSeconds()
				+'Z'
				: '');
};

SNAPI.generateAuthorizationMessageV2 = function(params, canonicalRequestData) {
	return 'SNWS2-HMAC-SHA256\n' +SNAPI.iso8601Date(params.date, true) +'\n'
			+ CryptoJS.enc.Hex.stringify(CryptoJS.SHA256(canonicalRequestData));
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
	var authHeader,
		signature,
		secretKey = SNAPI.signingSecretKey(params);
	if ( params.authType > 1 ) {
		msg = SNAPI.generateAuthorizationMessageV2(params, msg);
		signature = CryptoJS.HmacSHA256(msg, secretKey);
		authHeader = 'SNWS2 Credential='+params.token
			+',SignedHeaders='+params.signedHeaderNames.join(';')
			+',Signature='+CryptoJS.enc.Hex.stringify(signature);
	} else {
		signature = CryptoJS.HmacSHA1(msg, secretKey);
		authHeader = 'SolarNetworkWS ' +params.token +':' +CryptoJS.enc.Base64.stringify(signature);
	}
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

SNAPI.normalizedQueryTermsString = function(authType, url, data) {
	// handle query params, which must be sorted
	var a = document.createElement('a');
	a.href = url;
	var result = '';

	var params = SNAPI.parseURLQueryTerms(data === undefined ? a.search : data);
	var sortedKeys = [], key = undefined;
	var i, len;
	var first = true;

	for ( key in params ) {
		sortedKeys.push(key);
	}
	sortedKeys.sort();
	if ( sortedKeys.length > 0 ) {
		for ( i = 0, len = sortedKeys.length; i < len; i++ ) {
			if ( first ) {
				first = false;
			} else {
				result += '&';
			}
			result +=  (authType < 2 ? sortedKeys[i] : SNAPI.encodeURIComponent(sortedKeys[i]));
			result += '=';
			result += (authType < 2 ? params[sortedKeys[i]] : SNAPI.encodeURIComponent(params[sortedKeys[i]]));
		}
	}
	return result;
};

/**
 * Generate the SolarNetworkWS path required by the authorization header value.
 *
 * <p>This method will parse the given URL and then apply the path canonicalization
 * rules defined by the SolarNetworkWS scheme.</p>
 *
 * @param {Number} authType The authorization version (1, 2, etc)
 * @param {String} url the request URL
 * @param {String} queryString the normalized query string (used in V1)
 * @return {String} path the canonicalized path value to use in the authorization message
 */
SNAPI.authURLPath = function(authType, url, queryString) {
	var a = document.createElement('a');
	a.href = url;
	var path = a.pathname,
		queryString;

	if ( authType < 2 && queryString !== undefined && queryString.length > 0) {
		path += '?' + queryString;
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
	var authType = +SNAPI.ajaxCredentials.authType;
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
			if ( authType > 0 ) {
				// get a date, which we must include as a header as well as include in the
				// generated authorization hash
				var date = new Date();
				var a = document.createElement('a');
				a.href = url;

				// construct our canonicalized path value from our URL
				var query = SNAPI.normalizedQueryTermsString(authType, url,
					(cType !== undefined && cType.indexOf('application/x-www-form-urlencoded') === 0 ? data : undefined));
				var path = SNAPI.authURLPath(authType, url, query);

				var params = {
					authType : authType,
					method: method,
					date: date,
					host: (a.protocol+'//' +a.host),
					path: path,
					query: query,
					token: SNAPI.ajaxCredentials.token,
					secret: SNAPI.ajaxCredentials.secret,
					data: data,
					contentType: cType
				};

				SNAPI.setupHeaderNamesToSign(params);

				// generate the authorization hash value now (cryptographically signing our request)
				var auth = SNAPI.generateAuthorizationHeaderValue(params);

				// set the headers on our request
				xhr.setRequestHeader('X-SN-Date', date.toUTCString());
				xhr.setRequestHeader('Authorization', auth);
			}
			if ( data !== undefined && SNAPI.shouldIncludeContentDigest(cType) ) {
				if ( authType > 1 ) {
					xhr.setRequestHeader('Digest', 'sha-256=' + CryptoJS.enc.Base64.stringify(CryptoJS.SHA256(data)))
				} else {
					xhr.setRequestHeader('Content-MD5', CryptoJS.enc.Base64.stringify(CryptoJS.MD5(data)));
				}
			}
		}
	});
	return ajax;
};

SNAPI.hostHeaderValue = function(url) {
	var a = document.createElement('a');
	a.href = url;
	return a.host;
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
			params.date = new Date(form.elements['date'].value);
		} else {
			params.date = new Date();
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
		var authType = +SNAPI.ajaxCredentials.authType;
		var cType = (params.data && params.contentType === undefined
				? 'application/x-www-form-urlencoded; charset=UTF-8' : params.contentType);
		var query = SNAPI.normalizedQueryTermsString(authType, url,
			(cType !== undefined && cType.indexOf('application/x-www-form-urlencoded') === 0 ? params.data : undefined));
		var path = SNAPI.authURLPath(authType, url, query);
		var submitParams = {
			authType: authType,
			method: params.method,
			date: params.date,
			host: params.host,
			path: path,
			query: query,
			token: params.token,
			secret: params.secret,
			data: params.data,
			contentType: cType
		};
		SNAPI.setupHeaderNamesToSign(submitParams);
		var authHeader = SNAPI.generateAuthorizationHeaderValue(submitParams);
		var msgData = SNAPI.generateAuthorizationMessage(submitParams);

		$('#auth-header').text('Authorization: ' +authHeader);
		$('#req-message').text(authType > 1 ? msgData : '');
		$('#auth-message').text(authType < 2 ? msgData : SNAPI.generateAuthorizationMessageV2(submitParams, msgData));
		$('#sign-key').text(authType < 2 ? params.secret : CryptoJS.enc.Hex.stringify(SNAPI.signingSecretKey(submitParams)));
		$('#curl-command').text('curl '
				+'-H \'Accept: ' +(params.output === 'xml' ? 'text/xml'
						: params.output === 'csv' ? 'text/csv' : 'application/json') +'\' '
				+'-H \'X-SN-Date: '+params.date.toUTCString() +'\' -H \'Authorization: '
	   			+authHeader +'\' \'' +params.host +params.path +'\''
	   			+(params.data !== undefined && params.method !== 'GET'
	   				? (authType < 2
	   					? ' -H \'Content-MD5: ' + CryptoJS.enc.Base64.stringify(CryptoJS.MD5(params.data)) +'\''
	   					: ' -H \'Digest: sha-256=' + CryptoJS.enc.Base64.stringify(CryptoJS.SHA256(params.data)) +'\'')
	   				: '')
	   			+(params.data !== undefined && params.method !== 'GET'
	   				? ' -H \'Content-Type: ' + cType +'\' -d \'' +params.data +'\''
	   				: ''));
	};

	$('#shortcuts').change(function(event) {
		event.preventDefault();
		var form = this.form,
			val = this.value,
			method;
		if ( $(form).find('input[name=useAuth]:checked').val() === '0' ) {
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

	function setupForUseAuth() {
		var form = this.form,
			val = form.elements['path'].value,
			credForm = $('#credentials')[0];
		if ( $(this).val() === '0' ) {
			val = val.replace(/\/sec\//, '/pub/');
			$(credForm.elements['token']).attr('disabled', 'disabled');
			$(credForm.elements['secret']).attr('disabled', 'disabled');
			$('#auth-result').hide();
		} else {
			val = val.replace(/\/pub\//, '/sec/');
			$(credForm.elements['token']).removeAttr('disabled');
			$(credForm.elements['secret']).removeAttr('disabled');
			$('#auth-result .V2').toggle($(this).val() > 1);
			$('#auth-result').show();
		}
		form.elements['path'].value = val;
	}

	$('input[name=useAuth]').change(function(event) {
		event.preventDefault();
		setupForUseAuth.call(this);
	}).filter(':checked').first().each(setupForUseAuth);

	function setupForMethod() {
		var val = $(this).val();
		if ( val === 'POST' || val === 'PUT' || val === 'PATCH' ) {
			$('#upload').show();
		} else {
			$('#upload').hide();
		}
	}

	$('input[name=method]').change(function(event) {
		event.preventDefault();
		setupForMethod.call(this);
	}).filter(':checked').first().each(setupForMethod);

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
		params.authType = $(form).find('input[name=useAuth]:checked').val();
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
