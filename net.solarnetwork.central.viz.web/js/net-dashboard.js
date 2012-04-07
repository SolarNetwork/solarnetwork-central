var featureConsumption = false;
var mainChart;
var rangeChart;
var rollingDailyGenerationChart;
var rollingMonthlyGenerationChart;

var dateTimeDataFormat = '%Y-%m-%d %H:%M';
var dateDataFormat = '%Y-%m-%d';

/**
 * NetGenerationChart constructor.
 * 
 * The opts object supports the following:
 * 
 * - consumptionSourceId:	the source ID of the consumption node to monitor
 * - feature:				object with consumption and gridPrice boolean flags
 * 
 * @param divId the name of the element to hold the chart (string)
 * @param interval date range interval, with startDate, endDate, sDate, eDate properties
 * @param opts object with NetChart options (required)
 * @param chartOpts optional object with jqPlot options
 * @return NetGenerationChart object
 */
function NetGenerationChart(divId, interval, opts, chartOpts) {
	
	this.divId = divId;
	this.interval = interval,
	this.opts = opts || {};
	this.chartOpts = chartOpts || {};
	this.kwHourSeries = [];
	this.dateTicks = [];
	this.dateLabelFormat = null;
	this.dateTickInterval = null;
	this.timeReportingLevel = false;
	
	this.loadData = function() {
		this.kwHourSeries = [];
		this.dateTicks = [];
		this.dateTickInterval = null;
		
		var me = this;
		var queryParams = {
				startDate: this.interval.startDate,
				endDate: this.interval.endDate
			};
		var dt = this.interval.eDate.diff(this.interval.sDate, 'days', true);
		var endDateEOD = false;
		if ( dt <= 1 ) {
			queryParams.precision = 5;
			this.dateTickInterval = '4 hours';
		} else if ( dt <=  2 ) {
			queryParams.precision = 10;
			this.dateTickInterval = '6 hours';
		} else if ( dt <=  4 ) {
			queryParams.precision = 20;
			this.dateTickInterval = '12 hours';
		} else if ( dt <= 9 ) {
			if ( dt <= 6 ) {
				queryParams.precision = 30;
			}
			this.dateTickInterval = '1 day';
		} else if ( dt <= 21 ) {
			this.dateTickInterval = '2 days';
		} else if ( dt < 70 ) {
			this.dateTickInterval = '1 week';
		} else if ( dt < 145 ) {
			this.dateTickInterval = '2 weeks';
		} else if ( dt <= 310 ) {
			this.dateTickInterval = '1 month';
		} else if ( dt <= 730 ) {
			this.dateTickInterval = '2 months';
		} else if ( dt <= 1460 ) {
			this.dateTickInterval = '6 months';
		} else {
			this.dateTickInterval = '1 year';
		}
		
		if ( this.chartOpts.cursor.show && dt > 6 ) {
			// this is detailed chart, so specify hour-level up to 16 days, and 
			// day-level data up to 6 months range
			if ( dt < 16 ) {
				queryParams.aggregate = 'Hour';
				endDateEOD = true;
			} else if ( dt < 180 ) {
				queryParams.aggregate = 'Day';
			}
		}

		this.dateLabelFormat = '%#d %b ' +(this.dateTickInterval.search(/hour/i) != -1 ? '%y %H:%M' : '%Y');
		
		if ( queryParams.precision || endDateEOD ) {
			// make sure end date includes minutes
			queryParams.endDate = this.interval.eDate.strftime(dateTimeDataFormat);
		}
		this.timeReportingLevel = queryParams.precision != null 
			|| queryParams.aggregate == 'Minute' || queryParams.aggregate == 'Hour';

		// set up date ticks
		this.setupDateTicks();

		$.getJSON('generationData.json', queryParams,
				function(data) {
					$(data.data).each(function(i, obj) {
						var dateVal = obj.localDate;
						if ( me.timeReportingLevel ) {
							dateVal += ' ' + obj.localTime;
						}
						me.kwHourSeries.push([dateVal, obj.wattHours < 0 ? 0 : obj.wattHours / 1000]);
					});
					me.drawChart();
				});
		return this;
	};
	
	this.setupDateTicks = function() {
		// we assume here sDate always has time set to midnight
		var currDate = this.interval.sDate.clone();
		this.dateTicks.push([currDate.strftime(dateTimeDataFormat), ' ']);
		
		var intervalParts = this.dateTickInterval.split(' ');
        if ( intervalParts.length == 1 ) {
        	intervalParts = [1, intervalParts[0]];
        }
		
        if ( this.timeReportingLevel && intervalParts[1].search('day') != -1 ) {
        	// we have hourly data, but day interval, so make labels centered at noon, not midnight
        	currDate.add(12, 'hours');
        } else if ( intervalParts[1].search('week') != -1 ) {
        	// make week labels start on Monday
        	if ( currDate.getDay() < 1 ) {
        		currDate.add(1, 'day');
        	} else if ( currDate.getDay() > 1 ) {
        		currDate.add( 7 - currDate.getDay() + 1, 'day');
        	}
        } else if ( intervalParts[1].search('month') != -1 ) {
        	// make month labels start on 1st of month
        	if ( currDate.getDate() > 1 ) {
        		currDate.add(1, 'month');
        		currDate.setDate(1);
        	}
        } else {
        	// jump to next interval
        	currDate.add(intervalParts[0], intervalParts[1]);
        }
        
		while (  currDate.getTime() < this.interval.eDate.getTime() ) {
			this.dateTicks.push([currDate.strftime(dateTimeDataFormat), currDate.strftime(this.dateLabelFormat)]);
			currDate.add(intervalParts[0], intervalParts[1]);
		}
		
		this.dateTicks.push([this.interval.eDate.strftime(dateTimeDataFormat), ' ']);
	};
	
	this.changeDateRange = function(startDate, endDate) {
		this.interval.sDate = startDate;
		this.interval.eDate = endDate;
		this.interval.startDate = startDate.strftime(dateDataFormat);
		this.interval.endDate = endDate.strftime(dateDataFormat);
		this.loadData();
	};

	this.drawChart = function() {
		var opts = {};
		$.extend(true, opts, {
			axes: {
				xaxis:{ 
					renderer: $.jqplot.DateAxisRenderer, 
					pad: 0, 
					ticks: this.dateTicks,
					//tickInterval:this.dateTickInterval,
					tickOptions: {formatString:this.dateLabelFormat}
				},
				yaxis:{min:0,  pad:1}
			},
			axesDefaults:{useSeriesColor:true},
			seriesDefaults:{lineWidth:2, showMarker:false, pointLabels:{show:false}},
			series:[
			        {label:'kWh', yaxis:'yaxis'}
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		var seriesArray = [this.kwHourSeries];
		$.jqplot(this.divId, seriesArray,  opts);
		$(document).trigger("NetGenerationChart", [this]);
		return this;
	};
}

function NetGenerationBarChart(divId, interval, opts, chartOpts) {
	
	this.divId = divId;
	this.interval = interval,
	this.chartOpts = chartOpts || {};
	this.opts = opts || {};
	this.kwHourSeries = [];
	this.dateTicks = [];
	this.dateTicks2 = [];
	this.pointLabels = [];
	
	this._aggregate = function() {
		return this.opts.aggregate ? this.opts.aggregate : 'Month';
	},
	
	this._dateFormat = function() {
		var agg = this._aggregate();
		if ( agg == 'Day' ) {
			// week day abbr
			return '%a';
		}
		// default is month name abbr
		return '%b';
	},
	
	this.loadData = function() {
		this.kwHourSeries = [];
		this.dateTicks = [];
		this.dateTicks2 = [];
		this.pointLabels = [];
		var me = this;
		var queryParams = {
			startDate: this.interval.startDate,
			endDate: this.interval.endDate,
			aggregate: this._aggregate()
		};
		
		$.getJSON('generationData.json', queryParams,
				function(data) {
					var datePattern = me._dateFormat();
					var agg = me._aggregate() == 'Day' ? 0 : 1;
					$(data.data).each(function(i, obj) {
						// setup 1st level date ticks
						var d = Date.create(obj.localDate);
						me.dateTicks.push(d.strftime(datePattern));
						
						// setup 2nd level date ticks
						var dateTick2 = ' ';
						if ( agg == 0 && d.getDay() == 0 ) {
							// for days, add date on sunday
							dateTick2 = d.strftime('%#d %b');
						} else if ( agg == 1 && d.getMonth() == 0 ) {
							// for months, add date on january
							dateTick2 = d.strftime('%Y');
						}
						me.dateTicks2.push(dateTick2);
						
						me.kwHourSeries.push(obj.wattHours < 0 ? 0 : obj.wattHours / 1000);
						
						// set up point labels
						var pl = me.kwHourSeries[me.kwHourSeries.length-1];
						if ( pl == 0 ) {
							pl = null;
						} else {
							pl = pl.toFixed(2);
						}
						me.pointLabels.push(pl);
					});
					me.drawChart();
				});
		return this;
	};
	
	this.changeDateRange = function(startDate, endDate) {
		this.interval.sDate = startDate;
		this.interval.eDate = endDate;
		this.interval.startDate = startDate.strftime(dateDataFormat);
		this.interval.endDate = endDate.strftime(dateDataFormat);
		this.loadData();
	},

	this.drawChart = function() {
		var opts = {};
		$.extend(true, opts, {
			axes: {
				xaxis:{
					ticks:this.dateTicks,
					renderer:$.jqplot.CategoryAxisRenderer
					//pad:0
				},
				x2axis:{
					show:true,
					ticks:this.dateTicks2,
					renderer:$.jqplot.CategoryAxisRenderer
				},
				yaxis:{
					min: 0,
					label: 'kWh',
					labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
					labelOptions: {
						enableFontSupport: true,
						fontFamily: 'Helvetica',
						fontSize: '9pt',
						fontWeight: 'bold',
						angle: -90
					}
				}
			},
			axesDefaults: {useSeriesColor:false},
			legend: {show:false},
			cursor: {showTooltip:false, zoom:false},
			seriesDefaults: {showLine:true, showMarker:false, renderer:$.jqplot.BarRenderer},
			highlighter: {show:false},
			series: [
			        {label:'kWh', yaxis:'yaxis', pointLabels:{labels:this.pointLabels, escapeHTML:false, edgeTolerance:-20}}
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		$.jqplot(this.divId, [this.kwHourSeries], opts);
		$(document).trigger("NetGenerationBarChartReady", [this]);
		return this;
	};
}

function setupRangeChart(interval) {
	rangeChart = new NetGenerationChart('chart-overview-div', 
			interval, {
				feature: {consumption: featureConsumption}
			}, {
				legend:{show:false},
				cursor: {show:false},
				highlighter: {show:false},
				seriesDefaults: {lineWidth:1, shadow:false},
				axesDefaults: {useSeriesColor:false}
			});
	rangeChart.loadData();
}

function setupChart(interval) {
	mainChart = new NetGenerationChart('chart-div', 
			interval, {
				feature: {consumption: featureConsumption}
			}, {
				legend:{show:false},
				cursor: {show:true, tooltipLocation:'sw', zoom:true, clickReset:true},
				axes: {
					yaxis: {
						label:'kWh',
						labelRenderer:$.jqplot.CanvasAxisLabelRenderer,
						labelOptions:{
							enableFontSupport:true,
							angle:-90
						}
					}
				}
			});
	mainChart.loadData();
}

var SLIDER_PRECISION = 60000;

function updateDateRangeDisplay(ui) {
	$("#date-range-display").val(
			new Date(ui.values[0] * SLIDER_PRECISION).strftime(dateDataFormat)
			+ ' / ' + 
			new Date(ui.values[1] * SLIDER_PRECISION).strftime(dateDataFormat));
}

function setupRangeSlider() {
	// we make the slider use minutes
	var sMin = rangeChart.interval.sDate.getTime() / SLIDER_PRECISION;
	var sMax = rangeChart.interval.eDate.getTime() / SLIDER_PRECISION;
	var dMin = mainChart.interval.sDate.getTime() / SLIDER_PRECISION;
	var dMax = mainChart.interval.eDate.getTime() / SLIDER_PRECISION;
	
	//.jqplot-series-canvas = width of slider
	//.jqplot-yaxis = width of left y axis
	
	var l = $('#chart-overview-div .jqplot-yaxis').width();
	var w = $('#chart-overview-div .jqplot-series-canvas').width();
	$('#date-picker-div').css({ 'margin-left':l+'px', 'width':w+'px' });
	$('#date-slider').slider({
		range: true,
		min: sMin,
		max: sMax,
		values: [dMin, dMax],
		slide: function(event, ui) {
			updateDateRangeDisplay(ui);
		},
		change: function(event, ui) {
			// update chart date range
			var d1 = new Date(ui.values[0] * SLIDER_PRECISION);
			var d2 = new Date(ui.values[1] * SLIDER_PRECISION);
			
			// for now we're rounding to whole day values only!
			d1.setHours(0,0,0,0);
			d2.setHours(23,59,59,999);
			
			mainChart.changeDateRange(d1, d2);
		}
	});
	updateDateRangeDisplay({
		values:$('#date-slider').slider('values')
	});
}

function setupRollingMonthlyGenerationChart(interval) {
	if ( $("#monthly-generation-chart-div").size() < 1 ) {
		return;
	}
	rollingMonthlyGenerationChart = new NetGenerationBarChart('monthly-generation-chart-div',
			interval, {aggregate: 'Month'}, {legend:{location:'nw'}});
	$("#monthly-generation-div").data('chart', rollingMonthlyGenerationChart);
}

function setupRollingDailyGenerationChart(interval) {
	if ( $("#daily-generation-chart-div").size() < 1 ) {
		return;
	}
	rollingDailyGenerationChart = new NetGenerationBarChart('daily-generation-chart-div',
			interval, {aggregate: 'Day'}, {legend:{location:'nw'}});
	$("#daily-generation-div").data('chart', rollingDailyGenerationChart);
}

function chartSwitcher(div) {
	var currIdx = div.data('chartSwitcherSelectedIndex');
	if ( currIdx == undefined ) {
		currIdx = 0;
	}
	var switchables = $(div).children('.switchable');
	var nextIdx = currIdx + 1;
	if ( nextIdx >= switchables.size() ) {
		nextIdx = 0;
	}
	var curr = switchables.get(currIdx);
	var next = switchables.get(nextIdx);
	$(curr).slideToggle('fast');
	$(next).slideToggle('fast');
	div.data('chartSwitcherSelectedIndex', nextIdx);
	chartSwitcherLoadChartData(next);
}

function chartSwitcherLoadChartData(switchable) {
	var chart = $(switchable).data('chart');
	if ( chart ) {
		chart.loadData();
		$(switchable).removeData('chart');
	}
}

$(document).bind("NetGenerationChart", function(e, nodeChart) {
	if ( nodeChart.divId == 'chart-overview-div' ) {
		setupRangeSlider();
	}
	return false;
});

$(document).ready(function() {
	featureConsumption = $('#feature-consumption').val() == 'true' ? true : false;
	
	$.getJSON('reportableInterval.json', {
				types:['Power']
			},
			function(data) {
				var reportableInterval = {};
				reportableInterval.startDate = data.data.startDate;
				reportableInterval.sDate = Date.create(data.data.startDate);
				reportableInterval.endDate = data.data.endDate;
				reportableInterval.eDate = Date.create(reportableInterval.endDate);
				
				var chartInterval = {};
				chartInterval.eDate = Date.create(reportableInterval.endDate);
				chartInterval.eDate.setHours(23,59,59,999);
				chartInterval.endDate = chartInterval.eDate.strftime(dateDataFormat);
				chartInterval.sDate = chartInterval.eDate.clone().add(-1, 'week');
				chartInterval.sDate.setHours(0,0,0,0);
				chartInterval.startDate = chartInterval.sDate.strftime(dateDataFormat);
				
				setupChart(chartInterval);
				setupRangeChart(reportableInterval);
			});

	var rollingDayInterval = {eDate : Date.create(new Date().strftime(dateDataFormat))};
	rollingDayInterval.endDate = rollingDayInterval.eDate.strftime(dateDataFormat);
	rollingDayInterval.sDate = rollingDayInterval.eDate.clone().add(-3, 'weeks');
	rollingDayInterval.startDate = rollingDayInterval.sDate.strftime(dateDataFormat);
	setupRollingDailyGenerationChart(rollingDayInterval);
	
	var rollingMonthInterval = {eDate : Date.create(new Date().strftime('%Y-%m-01'))};
	rollingMonthInterval.endDate = rollingMonthInterval.eDate.strftime(dateDataFormat);
	rollingMonthInterval.sDate = rollingMonthInterval.eDate.clone().add(-24, 'months');
	rollingMonthInterval.startDate = rollingMonthInterval.sDate.strftime(dateDataFormat);
	setupRollingMonthlyGenerationChart(rollingMonthInterval);
	
	$('div.chart-switcher').each(function() {
		// enable the switchable onclick handler
		$(this).click(function() {
			chartSwitcher($(this).parent());
		});
		
		// now load the first switchable's chart data
		$(this).parent().children('.switchable:first').each(function() {
			chartSwitcherLoadChartData(this);	
		});
	});
	
});
