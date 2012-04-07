<%@ page contentType="application/xhtml+xml; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://packtag.sf.net" prefix="pack" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<head>
	<title><fmt:message key="app.displayName"/> ${nodeId} <fmt:message key="dashboard.generation.displayName"/></title>
	<meta name="viewport" content="width=1120" />
	<pack:style>
		<src>/css/global.css</src>
		<src>/css/net-dashboard.css</src>
		<src>/css/smoothness/jquery-ui-1.7.2.custom.css</src>
		<src>/css/ui.daterangepicker.css</src>
		<src>/css/jquery.jqplot.css</src>
	</pack:style>
	<pack:script> 
		<src>/js/jquery-1.3.2.js</src> 
		<src>/js/jquery-ui-1.7.2.custom.min.js</src>
		<src>/js/jquery.jqplot.js</src>
		<src>/js/jqplot-plugins/jqplot.canvasTextRenderer.js</src>
		<src>/js/jqplot-plugins/jqplot.canvasAxisLabelRenderer.js</src>
		<src>/js/jqplot-plugins/jqplot.categoryAxisRenderer.js</src>
		<src>/js/jqplot-plugins/jqplot.dateAxisRenderer.js</src>
		<src>/js/jqplot-plugins/jqplot.barRenderer.js</src>
		<src>/js/jqplot-plugins/jqplot.ohlcRenderer.js</src>
		<%--src>/js/jqplot-plugins/jqplot.trendline.js</src--%>
		<src>/js/jqplot-plugins/jqplot.cursor.js</src>
		<src>/js/jqplot-plugins/jqplot.highlighter.js</src>
		<src>/js/jqplot-plugins/jqplot.pointLabels.js</src>
		<src>/js/net-dashboard.js</src>
	</pack:script>
</head>
<body>
<div id="chart-main" class="chart-box">
	<div id="chart-div" class="chart-container" style="width:840px;height:280px;"></div>
	<div id="chart-overview-div" class="chart-container" style="width:840px;height:80px;"></div>
	<div id="date-picker-div">
		<div id="date-slider"/>
		<input type="text" id="date-range-display" />
	</div>
</div>
<div id="chart-box2" class="chart-box">
	<div class="chart-switcher">~</div>
	<div id="daily-generation-div" class="switchable">
		<h2><fmt:message key="dashboard.dailyGeneration.title"/></h2>
		<div id="daily-generation-chart-div" class="chart-container" style="width:840px;height=250px"></div>
	</div>
	<div id="monthly-generation-div" class="switchable" style="display: none;">
		<h2><fmt:message key="dashboard.monthlyGeneration.title"/></h2>
		<div id="monthly-generation-chart-div" class="chart-container" style="width:840px;height=250px"></div>
	</div>
</div>
<div style="display: none">
	<input type="hidden" id="feature-consumption" name="feature.consumption" value="false" />
</div>
</body>
