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

SNDemo.getJSON = function(url, callback) {
	$.ajax({
		type: 'GET',
		url: url,
		dataType: 'json',
		beforeSend: function(xhr) {
			var date = new Date().toUTCString();
			var a = document.createElement('a');
			a.href = url;
			var path = a.pathname;
			
			// TODO: we need to parse and sort query params here...
			var queryParamsIndex = a.href.indexOf('?');
			if ( queryParamsIndex >= 0 ) {
				path = path + a.href.substring(queryParamsIndex);
			}
			
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
