var SNDemo = {};

SNDemo.getCredentials = function() {
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

SNDemo.showResult = function(msg) {
	$('#result').val(msg);
};

SNDemo.generateAuthorizationHeaderValue = function(params) {
	var msg = 
		(params.method === undefined ? 'GET' : params.method) + '\n\n\n'
		+params.date +'\n'
		+params.path;
	var hash = CryptoJS.HmacSHA1(msg, params.secret);
	var authHeader = params.token +':' +CryptoJS.enc.Base64.stringify(hash);
	return authHeader;
};

SNDemo.parseURLQueryTerms = function(search) {
	var params = {};
	var pairs;
	var pair;
	var i, len;
	if ( search !== undefined && search.length > 1 ) { // > 1 because includes '?' character
		search = search.substring(1);
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

SNDemo.authURLPath = function(url) {
	var a = document.createElement('a');
	a.href = url;
	var path = a.pathname;
	
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

SNDemo.getJSON = function(url, callback) {
	$.ajax({
		type: 'GET',
		url: url,
		dataType: 'json',
		beforeSend: function(xhr) {
			var date = new Date().toUTCString();
			var path = SNDemo.authURLPath(url);
			
			xhr.setRequestHeader('X-SN-Date', date);
			var auth = SNDemo.generateAuthorizationHeaderValue({
				method: 'GET',
				date: date,
				path: path,
				token: SNDemo.ajaxCredentials.token,
				secret: SNDemo.ajaxCredentials.secret
			});
			xhr.setRequestHeader('Authorization', 'SolarNetworkWS ' +auth);
		}
	}).done(callback).fail(function(status) {
		alert('fail: ' +status);
	});
};

$(document).ready(function() {
	$('#generic-path').submit(function(event) {
		event.preventDefault();
		var form = this;
		var params = SNDemo.getCredentials();
		params.method = $(form).find('input[name=method]:checked').val();
		params.path = form.elements['path'].value;
		SNDemo.ajaxCredentials = params;
		var authHeader = SNDemo.generateAuthorizationHeaderValue(params);
 	   	SNDemo.showResult('Date: ' +params.date 
	   		+'\nAuthorization: ' +authHeader
	   		+'\nCurl: ' +'curl -H "X-SN-Date: '+params.date +'" -H "Authorization: SolarNetworkWS ' 
	   			+authHeader +'" http://localhost:8680' +params.path);
 	   	SNDemo.getJSON('http://localhost:8680'+params.path, function(data) {
 	   		SNDemo.showResult(JSON.stringify(data));
 	   	});
	});
});
