<title><fmt:message key="app.name"/></title>
<link rel="shortcut icon" href="<c:url value='/images/favicon.ico'/>" />
<%--
Pacj isn't taking the image root into account
<pack:style>
	<src>/css/global.css</src>
</pack:style>
<pack:style>
	<src>/css/cupertino/jquery-ui-1.8.13.custom.css</src>
</pack:style>
--%>
<link rel="stylesheet" type="text/css" href="<c:url value='/css/smoothness/jquery-ui-1.8.13.custom.css'/>" />
<link rel="stylesheet" type="text/css" href="<c:url value='/css/data-tables.css'/>" />
<link rel="stylesheet" type="text/css" href="<c:url value='/css/global.css'/>" />

<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=false&amp;key=${env['google.maps.key']}" type="text/javascript"></script>

<pack:script>
	<src>/js-lib/jquery-1.6.1.js</src>
	<src>/js-lib/jquery-ui-1.8.13.custom.min.js</src>
	<src>/js-lib/jquery.dataTables.min.js</src>
	<src>/js-lib/jquery-ui-timepicker-addon.js</src>
	<src>/js-lib/jquery.validate.min.js</src>
	<src>/js-lib/jquery.form.js</src>
	<src>/js/solarnetwork-base.js</src>
	<src>/js/dras.js</src>
	<src>/js/config.js</src>
	<src>/js/messages.js</src>
	<src>/js/about.js</src>
	<src>/js/map.js</src>
	<src>/js/observer.js</src>
	<src>/js/operator.js</src>
	<src>/js/user.js</src>
	<src>/js/program.js</src>
</pack:script>
