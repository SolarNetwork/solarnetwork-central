var nodeId = -1;
var featureConsumption = false;
var featureGridPrice = false;
var consumptionSourceId = '';
var mainChart;
var rangeChart;
var rollingWeekHourlyConsumptionChart;
var rollingMonthlyConsumptionChart;
var rollingDayConsumptionChart;
var rollingMonthlyGenerationChart;
var rollingDayGenerationChart;
var nodeWeather;
var rollingMonthlyWeatherChart;
var rollingDayWeatherChart;

var dateTimeDataFormat = '%Y-%m-%d %H:%M';
var dateDataFormat = '%Y-%m-%d';

/**
 * NodeChart constructor.
 * 
 * The opts object supports the following:
 * 
 * - consumptionSourceId:	the source ID of the consumption node to monitor
 * - feature:				object with consumption and gridPrice boolean flags
 * 
 * @param divId the name of the element to hold the chart (string)
 * @param nodeId the ID of the node (number)
 * @param interval date range interval, with startDate, endDate, sDate, eDate properties
 * @param opts object with NodeChart options (required)
 * @param chartOpts optional object with jqPlot options
 * @return NodeChart object
 */
function NodeChart(divId, nodeId, interval, opts, chartOpts) {
	
	this.divId = divId;
	this.nodeId = nodeId;
	this.interval = interval,
	this.opts = opts || {};
	this.chartOpts = chartOpts || {};
	this.powerWattSeries = [];
	this.consumptionWattSeries = [];
	this.gridPriceSeries = [];
	this.dateTicks = [];
	this.dateLabelFormat = null;
	this.dateTickInterval = null;
	this.numSeriesLoaded = 0;
	this.numSeries = 0;
	this.timeReportingLevel = false;
	this.showDaylight = false;
	this.consumptionSourceId = this.opts.consumptionSourceId;
	this.featureConsumption = this.opts.feature && this.opts.feature.consumption ? true : false;
	this.featureGridPrice = this.opts.feature && this.opts.feature.gridPrice ? true : false;
	
	this.loadData = function() {
		this.powerWattSeries = [];
		this.consumptionWattSeries = [];
		this.gridPriceSeries = [];
		this.dateTicks = [];
		this.dateTickInterval = null;
		this.numSeriesLoaded = 0;
		this.showDaylight = false;
		
		this.numSeries = 1;
		if ( this.featureConsumption ) {
			this.numSeries++;
		}
		if ( this.featureGridPrice ) {
			this.numSeries++;
		}
		
		var me = this;
		var queryParams = {
				nodeId: this.nodeId,
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
		
		if ( this.chartOpts.cursor.show && dt <= 10 ) {
			this.showDaylight = true;
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
						me.powerWattSeries.push([dateVal, obj.watts < 0 ? 0 : obj.watts]);
					});
					me.numSeriesLoaded++;
					me.drawChart();
				});
		if ( this.featureConsumption ) {
			var consumParams = queryParams;
			if ( this.consumptionSourceId ) {
				consumParams = {};
				$.extend(true, consumParams, queryParams);
				consumParams["properties['sourceId']"] = this.consumptionSourceId;
			}
			$.getJSON('consumptionData.json', consumParams,
					function(data) {
						$(data.data).each(function(i, obj) {
							var dateVal = obj.localDate;
							if ( me.timeReportingLevel ) {
								dateVal += ' ' + obj.localTime;
							}
							me.consumptionWattSeries.push([dateVal, obj.watts < 0 ? 0 : obj.watts]);
						});
						me.numSeriesLoaded++;
						me.drawChart();
					});
		}
		if ( this.featureGridPrice ) {
			$.getJSON('priceData.json', queryParams,
					function(data) {
						$(data.data).each(function(i, obj) {
							var dateVal = obj.localDate;
							if ( me.timeReportingLevel ) {
								dateVal += ' ' + obj.localTime;
							}
							me.gridPriceSeries.push([dateVal, obj.price < 0 ? 0 : obj.price]);
						});
						me.numSeriesLoaded++;
						me.drawChart();
					});
		}
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
		if ( this.numSeriesLoaded < this.numSeries ) {
			// don't draw chart until all series loaded
			return this;
		}
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
				yaxis:{min:0,  pad:1},
				y2axis:{min:0,  pad:1},
				y3axis:{min:0, pad:1, tickOptions:{formatString:'$%.2f'}}
			},
			axesDefaults:{useSeriesColor:true},
			seriesDefaults:{lineWidth:2, showMarker:false, pointLabels:{show:false}},
			series:[
			        {label:'PV Watts', yaxis:'yaxis'},
			        {label:'Consumption Watts', yaxis:'yaxis'},
			        {label:'Grid Price', yaxis:'y2axis', lineWidth:1}
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		var seriesArray = [this.powerWattSeries];
		if ( this.featureConsumption ) {
			seriesArray.push(this.consumptionWattSeries);
		}
		if ( this.featureGridPrice ) {
			seriesArray.push(this.gridPriceSeries);
		}
		$.jqplot(this.divId, seriesArray,  opts);
		if ( this.showDaylight ) {
			new DaylightCanvas(this.nodeId, $('#'+this.divId +' canvas.jqplot-series-canvas'), 
					this.interval.sDate.clone(), this.interval.eDate.clone()).showDaylight();
		}
		$(document).trigger("NodeChartReady", [this]);
		return this;
	};
}

function ConsumptionHourlyCostChart(divId, nodeId, consumptionSourceId, interval, chartOpts) {
	
	this.divId = divId;
	this.nodeId = nodeId;
	this.consumptionSourceId = consumptionSourceId;
	this.interval = interval,
	this.chartOpts = chartOpts || {};
	this.kwHourSeries = [];
	this.costSeries = [];
	//this.dayData = [];
	this.currency = '';
	//this.conditionCache = {};
	
	this.loadData = function() {
		this.kwHourSeries = [];
		this.costSeries = [];
		var me = this;
		var queryParams = {
			nodeId: this.nodeId,
			startDate: this.interval.startDate,
			endDate: this.interval.endDate,
			//precision: 60,
			aggregate: 'Hour'
		};
		queryParams["properties['sourceId']"] = this.consumptionSourceId;
		
		$.getJSON('consumptionData.json', queryParams,
				function(data) {
					$(data.data).each(function(i, obj) {
						me.kwHourSeries.push([obj.localDate +' ' +obj.localTime, obj.wattHours < 0 ? 0 : obj.wattHours / 1000]);
						me.costSeries.push([obj.localDate +' ' +obj.localTime, obj.cost < 0 ? 0 : obj.cost]);
						if ( me.currency == '' && obj.currency != '' ) {
							me.currency = obj.currency;
						}
					});
					me.drawChart();
				});
		return this;
	};
	
	this.changeDateRange = function(startDate, endDate) {
		this.interval.sDate = startDate;
		this.interval.eDate = endDate;
		this.interval.startDate = startDate.strftime(dateTimeDataFormat);
		this.interval.endDate = endDate.strftime(dateTimeDataFormat);
		this.loadData();
	};

	this.drawChart = function() {
		var opts = {};
		$.extend(true, opts, {
			axes: {
				xaxis:{/*min:me.startDate, max:me.endDate,*/ 
					renderer:$.jqplot.DateAxisRenderer, 
					pad:0, 
					tickOptions:{formatString:'%#d %b %y %H:%M'}
				},
				yaxis:{min:0},
				y2axis:{min:0, tickOptions:{formatString:'$%.2f'}}
			},
			axesDefaults:{useSeriesColor:false},
			seriesDefaults:{lineWidth:2, showMarker:false, pointLabels:{show:false}},
			series:[
			        {label:'kWh', yaxis:'yaxis'/*, renderer:(
			        		this.queryOpts.aggregate && this.queryOpts.aggregate == 'Month' 
			        			? $.jqplot.BarRenderer : $.jqplot.LineRenderer),
			        			rendererOptions:{barPadding:8, barMargin:8}*/},
			        {label:this.currency, yaxis:'y2axis', lineWidth:1}
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		$.jqplot(
				this.divId,  
				[this.kwHourSeries, this.costSeries], 
				opts);
		new DaylightCanvas(this.nodeId, $('#'+this.divId +' canvas.jqplot-series-canvas'), 
				Date.create(this.kwHourSeries[0][0]), 
				Date.create(this.kwHourSeries[this.kwHourSeries.length-1][0])).showDaylight();
		$(document).trigger("ConsumptionHourlyCostChartReady", [this]);
		return this;
	};
}

var daylightIconCache = {};

function DaylightCanvas(nodeId, canvasDiv, startDate, endDate) {
	
	this.nodeId = nodeId;
	this.canvas = $(canvasDiv);
	this.startDate = startDate;
	this.endDate = endDate;
	this.dayData = [];
	this.conditionCache = daylightIconCache;
	
	this.showDaylight = function() {
		this.dayData = [];
		var me = this;
		var queryParams = {
			nodeId: this.nodeId,
			startDate: this.startDate.strftime(dateDataFormat),
			endDate: this.endDate.strftime(dateDataFormat),
			aggregate: 'Day'
		};
		$.getJSON('dayData.json', queryParams,
				function(data) {
					me.dayData = data.data;
					me.drawDaylight();
				});
	};
	
	this.drawDaylight = function() {
		var ctx = this.canvas.get(0).getContext("2d");
		var cWidth = this.canvas.get(0).width;
		var cHeight = this.canvas.get(0).height;
		var dateRange = this.endDate.getTime() - this.startDate.getTime();
		var mspx = cWidth / dateRange; // milliseconds per pixel
		var dayIdx;
		var sunset, sunrise, noon;
		var day, tom;
		var chartPos = this.canvas.position();
		
		// iterate over all day data, drawing a dark, vertical band represeting
		// night time, using the sunset/sunrise values in the day data
		for ( dayIdx = 0; dayIdx < this.dayData.length; dayIdx++ ) {
			day = this.dayData[dayIdx];
			tom = dayIdx == (this.dayData.length-1) 
				? this.dayData[dayIdx] : this.dayData[dayIdx+1];
			sunset = Date.create(day.day +' ' +day.sunset);
			sunrise = Date.create(tom.day +' ' +tom.sunrise);
			noon = Date.create(tom.day +' 12:00');
			if ( day === tom ) {
				// we don't have sunrise time for "tomorrow", so just use today's value
				// most likely this is will be drawn outside the bounds of the chart anyway
				sunrise.add(1, 'days');
			}
				
			var x = (sunset.getTime() - this.startDate.getTime()) * mspx;
			var w = (sunrise.getTime() - sunset.getTime()) * mspx;
			var gradient = ctx.createLinearGradient(x,0,x+w,0);
			gradient.addColorStop(0, 'rgba(0,0,0,0)');
			gradient.addColorStop(.2, 'rgba(0,0,0,0.2)');
			gradient.addColorStop(.8, 'rgba(0,0,0,0.2)');
			gradient.addColorStop(1, 'rgba(0,0,0,0)');
			ctx.fillStyle = gradient;
			ctx.fillRect(x,0,w,cHeight);
			
			// try to load weather icon for tomorrow day time
			if ( tom.condition && !(day === tom) ) {
				// also add day condition icon
				x = (noon.getTime() - this.startDate.getTime()) * mspx;
				var iconDiv = $('<div class="weather-icon ' +noon.strftime(dateDataFormat) +'"/>').css({
					display: 'none',
					position: 'absolute',
					top: chartPos.top +'px',
					left: (chartPos.left+x-15)+'px',
					width: '30px',
					height: '30px'
				});
				this.canvas.after(iconDiv);
				this.loadWeatherIcon(tom.condition, iconDiv);
			}
		}
	};
	
	this.loadWeatherIcon = function(condition, iconDiv) {
		var iconName = getWeatherIconNameFromCondition(condition);
		if ( this.conditionCache[iconName] ) {
			iconDiv.append(this.conditionCache[iconName].cloneNode(true)).show();
			return;
		}
		var me = this;
		$.ajax({
			type: 'GET',
			url: 'img/weather-' +iconName +'.svg',
			dataType: 'xml',
			success: function(data, textStatus) {
				var svg = document.importNode(data.documentElement, true);
				me.conditionCache[iconName] = svg;
				iconDiv.append(svg).show();
			}
		});
	};
}

function ConsumptionBarChart(divId, nodeId, consumptionSourceId, interval, queryOpts, chartOpts) {
	
	this.divId = divId;
	this.nodeId = nodeId;
	this.consumptionSourceId = consumptionSourceId;
	this.interval = interval,
	this.chartOpts = chartOpts || {};
	this.queryOpts = queryOpts || {};
	this.kwHourSeries = [];
	this.costSeries = [];
	this.dateTicks = [];
	this.pointLabels = [];
	this.currency = '';
	
	this._aggregate = function() {
		return this.queryOpts.aggregate ? this.queryOpts.aggregate : 'Month';
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
		this.costSeries = [];
		this.dateTicks = [];
		this.pointLabels = [];
		var me = this;
		var queryParams = {
			nodeId: this.nodeId,
			startDate: this.interval.startDate,
			endDate: this.interval.endDate,
			aggregate: this._aggregate()
		};
		queryParams["properties['sourceId']"] = this.consumptionSourceId;
		
		$.getJSON('consumptionData.json', queryParams,
				function(data) {
					var datePattern = me._dateFormat();
					$(data.data).each(function(i, obj) {
						me.dateTicks.push(Date.create(obj.localDate).strftime(datePattern));
						me.kwHourSeries.push(obj.wattHours < 0 ? 0 : obj.wattHours / 1000);
						var pl = me.kwHourSeries[me.kwHourSeries.length-1];
						if ( pl == 0 ) {
							pl = null;
						} else {
							pl = pl.toFixed(2);
							if ( obj.cost > 0 ) {
								pl += '<br /><span class="cost">$' +obj.cost.toFixed(2) +'</span>';
							}
						}
						me.pointLabels.push(pl);
						if ( me.currency == '' && obj.currency != '' ) {
							me.currency = obj.currency;
						}
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
				yaxis:{min:0}
			},
			axesDefaults: {useSeriesColor:false},
			legend: {show:true, location:'nw'},
			cursor: {showTooltip:false, zoom:false},
			seriesDefaults: {showLine:true, showMarker:false, renderer:$.jqplot.BarRenderer},
			highlighter: {show:false},
			series: [
			        {label:'kWh', yaxis:'yaxis', pointLabels:{labels:this.pointLabels, escapeHTML:false, edgeTolerance:-20}}
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		$.jqplot(this.divId, [this.kwHourSeries], opts);
		$(document).trigger("ConsumptionBarChartReady", [this]);
		return this;
	};
}

function WeatherChart(divId, nodeId, interval, queryOpts, chartOpts) {
	
	this.divId = divId;
	this.nodeId = nodeId;
	this.interval = interval,
	this.queryOpts = queryOpts || {};
	this.chartOpts = chartOpts || {};
	this.temperatureSeries = [];
	this.dateTicks = [];
	
	this._aggregate = function() {
		return this.queryOpts.aggregate ? this.queryOpts.aggregate : 'Day';
	},
	
	this._dateFormat = function() {
		var agg = this._aggregate();
		if ( agg == 'Month' ) {
			// week day abbr
			return '%b';
		}
		// default is month name abbr
		return '%a';
	},
	
	this.loadData = function() {
		this.temperatureSeries = [];
		this.dateTicks = [];
		var me = this;
		var queryParams = {
			nodeId: this.nodeId,
			startDate: this.interval.startDate,
			endDate: this.interval.endDate,
			aggregate: this._aggregate()
		};
		
		$.getJSON('dayData.json', queryParams,
				function(data) {
					var datePattern = me._dateFormat();
					$(data.data).each(function(i, obj) {
						me.dateTicks.push(Date.create(obj.day).strftime(datePattern));
						me.temperatureSeries.push(
								[(i+1), 
								 obj.temperatureStartCelcius ? obj.temperatureStartCelcius : obj.temperatureHighCelcius 
										 ? obj.temperatureHighCelcius : 0,
								 obj.temperatureHighCelcius ? obj.temperatureHighCelcius : 0,
								 obj.temperatureLowCelcius ? obj.temperatureLowCelcius : 0,
								 obj.temperatureEndCelcius ? obj.temperatureEndCelcius : obj.temperatureLowCelcius 
										 ? obj.temperatureLowCelcius : 0]);
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
					//renderer:$.jqplot.DateAxisRenderer,
					//tickOptions:{formatString:this._dateFormat()}
				},
				yaxis:{
					tickOptions:{formatString:'%d&#xb0;'}
				}
			},
			axesDefaults: {},
			legend: {show:false},
			cursor: {show:false, showTooltip:false, zoom:false},
			highlighter:{showMarker:false, tooltipAxes:'y', yvalues:4, formatString:'<table class="jqplot-highlighter">\
				  <tr><td>Start:</td><td>%s</td></tr>\
				  <tr><td>High:</td><td>%s</td></tr>\
				  <tr><td>Low:</td><td>%s</td></tr>\
				  <tr><td>End:</td><td>%s</td></tr>\
				</table>'},
			series: [
			        {renderer:$.jqplot.OHLCRenderer, yaxis:'yaxis', rendererOptions:{candleStick:true}, pointLabels:{show:false} }
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		$.jqplot(this.divId, [this.temperatureSeries], opts);
		$(document).trigger("WeatherChart", [this]);
		return this;
	};
	
}

function GenerationBarChart(divId, nodeId, interval, opts, chartOpts) {
	
	this.divId = divId;
	this.nodeId = nodeId;
	this.interval = interval,
	this.chartOpts = chartOpts || {};
	this.opts = opts || {};
	this.kwHourSeries = [];
	this.costSeries = [];
	this.dateTicks = [];
	this.pointLabels = [];
	this.currency = '';
	
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
		this.costSeries = [];
		this.dateTicks = [];
		this.pointLabels = [];
		var me = this;
		var queryParams = {
			nodeId: this.nodeId,
			startDate: this.interval.startDate,
			endDate: this.interval.endDate,
			aggregate: this._aggregate()
		};
		
		$.getJSON('generationData.json', queryParams,
				function(data) {
					var datePattern = me._dateFormat();
					$(data.data).each(function(i, obj) {
						me.dateTicks.push(Date.create(obj.localDate).strftime(datePattern));
						me.kwHourSeries.push(obj.wattHours < 0 ? 0 : obj.wattHours / 1000);
						var pl = me.kwHourSeries[me.kwHourSeries.length-1];
						if ( pl == 0 ) {
							pl = null;
						} else {
							pl = pl.toFixed(2);
							if ( obj.cost > 0 ) {
								pl += '<br /><span class="cost">$' +obj.cost.toFixed(2) +'</span>';
							}
						}
						me.pointLabels.push(pl);
						if ( me.currency == '' && obj.currency != '' ) {
							me.currency = obj.currency;
						}
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
			legend: {show:false},// location:'nw'},
			cursor: {showTooltip:false, zoom:false},
			seriesDefaults: {showLine:true, showMarker:false, renderer:$.jqplot.BarRenderer},
			highlighter: {show:false},
			series: [
			        {label:'kWh', yaxis:'yaxis', pointLabels:{labels:this.pointLabels, escapeHTML:false, edgeTolerance:-20}}
			        ]
		}, this.chartOpts);
		$('#'+this.divId).empty();
		$.jqplot(this.divId, [this.kwHourSeries], opts);
		$(document).trigger("GenerationBarChartReady", [this]);
		return this;
	};
}

function NodeWeather(divId, nodeId) {
	
	this.divId = divId;
	this.div = $('#' +divId);
	this.nodeId = nodeId;
	this.updateFrequency = 10; // minutes
	
	this.weather = {};
	this.day = {};
	this.tz = {};
	
	this.refresh = function() {
		var queryParams = {nodeId:this.nodeId};
		var me = this;
		$.getJSON('currentWeather.json', queryParams,
				function(data) {
					me.weather = data.weather;
					me.day = data.day;
					me.tz = data.tz;
					me.display();
				});
	};
	
	this.display = function() {
		this.loadIcon();
		if ( this.weather.temperatureCelcius ) {
			this.div.children('.current-temp').html(this.weather.temperatureCelcius +'&#xb0;');
		} else {
			this.div.children('.current-temp').hide();
		}
		if ( this.day.temperatureHighCelcius ) {
			this.div.find('.high .value').html(this.day.temperatureHighCelcius +'&#xb0;');
		} else {
			this.div.children('.high').hide();
		}
		if ( this.day.temperatureLowCelcius ) {
			this.div.find('.low .value').html(this.day.temperatureLowCelcius +'&#xb0;');
		} else {
			this.div.children('.low').hide();
		}
		if ( this.weather.humidity ) {
			this.div.find('.humidity .value').html(this.weather.humidity +'%');
		} else {
			this.div.children('.humidity').hide();
		}
		if ( this.day.sunrise ) {
			this.div.find('.sunrise .value').html(this.day.sunrise);
		} else {
			this.div.children('.sunrise').hide();
		}
		if ( this.day.sunset ) {
			this.div.find('.sunset .value').html(this.day.sunset);
		} else {
			this.div.children('.sunset').hide();
		}
		if ( this.weather.localDate || this.weather.localTime ) {
			var dateStr = (this.weather.localDate ? this.weather.localDate : '')
				+ (this.weather.localDate && this.weather.localTime ? ' ' : '')
				+ (this.weather.localTime ? this.weather.localTime : '');
			if ( this.tz && this.tz.ID ) {
				dateStr += '<br />' + this.tz.ID;
			}
			this.div.find('.updated .value').html(dateStr);
		} else {
			this.div.children('.updated').hide();
		}
		this.div.show();
	};
	
	this.loadIcon = function() {
		this.div.children('.weather-icon').hide();
		if ( !(this.weather && this.weather.condition) ) {
			return;
		}
		
		// convert camel-caps into dash delimited icon name
		var iconName = getWeatherIconNameFromCondition(this.weather.condition);
		var me = this;
		$.ajax({
			type: 'GET',
			url: 'img/weather-' +iconName +'.svg',
			dataType: 'xml',
			success: function(data, textStatus) {
				var svg = document.importNode(data.documentElement, true);
				//$(svg).css({width: '128px', height: '128px'});
				me.div.children('.weather-icon').append(svg).show();
			}
		});
		
	};

}

function getWeatherIconNameFromCondition(condition) {
	// convert camel-caps into dash delimited icon name
	var iconName = condition.charAt(0).toLowerCase() 
		+ condition.substring(1);
	var idx = -1;
	while ( (idx = iconName.search(/[A-Z]/)) != -1 ) {
		iconName = iconName.substring(0, idx)
			+ '-' + iconName.charAt(idx).toLowerCase()
			+ iconName.substring(idx+1);
	}
	return iconName;
}

function setupRangeChart(interval) {
	rangeChart = new NodeChart('chart-overview-div', 
			nodeId, 
			interval, {
				consumptionSourceId: consumptionSourceId,
				feature: {consumption: featureConsumption, gridPrice: featureGridPrice}
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
	mainChart = new NodeChart('chart-div', 
			nodeId, 
			interval, {
				consumptionSourceId: consumptionSourceId,
				feature: {consumption: featureConsumption, gridPrice: featureGridPrice}
			}, {
				legend:{show:true, location:'nw'},
				cursor: {show:true, tooltipLocation:'sw', zoom:true, clickReset:true},
				axes: {
					yaxis: {
						label:'Watts',
						labelRenderer:$.jqplot.CanvasAxisLabelRenderer,
						labelOptions:{
							enableFontSupport:true,
							angle:-90
						}
					},
					y2axis: {
						label:'NZD / MWh', // TODO use currency and unit from data
						labelRenderer:$.jqplot.CanvasAxisLabelRenderer,
						labelOptions:{
							enableFontSupport:true,
							angle:90
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

function setupRollingWeekHourlyConsumptionChart(interval) {
	if ( $("#hourly-cost-chart-div").size() < 1 ) {
		return;
	}
	rollingWeekHourlyConsumptionChart = new ConsumptionHourlyCostChart('hourly-cost-chart-div', 
			nodeId, 
			consumptionSourceId,
			interval, {
				legend:{show:true, location:'nw'},
				cursor: {showTooltip:false, zoom:true, clickReset:true}
			});
	
	rollingWeekHourlyConsumptionChart.loadData();
}

function setupRollingMonthConsumptionChart(interval) {
	if ( $("#monthly-cost-chart-div").size() < 1 ) {
		return;
	}
	rollingMonthlyConsumptionChart = new ConsumptionBarChart('monthly-cost-chart-div', 
			nodeId, 
			consumptionSourceId,
			interval);
	$("#monthly-cost-div").data('chart', rollingMonthlyConsumptionChart);
}

function setupRollingDayConsumptionChart(interval) {
	if ( $("#week-cost-chart-div").size() < 1 ) {
		return;
	}
	rollingDayConsumptionChart = new ConsumptionBarChart('week-cost-chart-div', 
			nodeId, 
			consumptionSourceId,
			interval, 
			{aggregate: 'Day'}, 
			{legend:{location:'ne'}});
	$("#week-cost-div").data('chart', rollingDayConsumptionChart);
}

function setupRollingDayGenerationChart(interval) {
	if ( $("#week-generation-chart-div").size() < 1 ) {
		return;
	}
	rollingDayGenerationChart = new GenerationBarChart('week-generation-chart-div',
			nodeId, interval, {aggregate: 'Day'}, {legend:{location:'ne'}});
	$("#week-generation-div").data('chart', rollingDayGenerationChart);
}

function setupRollingMonthGenerationChart(interval) {
	if ( $("#monthly-generation-chart-div").size() < 1 ) {
		return;
	}
	rollingMonthlyGenerationChart = new GenerationBarChart('monthly-generation-chart-div',
			nodeId, interval, {aggregate: 'Month'}, {legend:{location:'nw'}});
	$("#monthly-generation-div").data('chart', rollingMonthlyGenerationChart);
}

function setupRollingMonthWeatherChart(interval) {
	if ( $("#monthly-weather-chart-div").size() < 1 ) {
		return;
	}
	rollingMonthlyWeatherChart = new WeatherChart('monthly-weather-chart-div', 
			nodeId, 
			interval,
			{aggregate: 'Month'});
	$("#monthly-weather-div").data('chart', rollingMonthlyWeatherChart);
}

function setupRollingDayWeatherChart(interval) {
	if ( $("#week-weather-chart-div").size() < 1) {
		return;
	}
	rollingDayWeatherChart = new WeatherChart('week-weather-chart-div', 
			nodeId, 
			interval, 
			{aggregate: 'Day'});
	$("#week-weather-div").data('chart', rollingDayWeatherChart);
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
	//$(curr).fadeOut('fast');
	//$(next).fadeIn('fast');
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

$(document).bind("NodeChartReady", function(e, nodeChart) {
	if ( nodeChart.divId == 'chart-overview-div' ) {
		setupRangeSlider();
	}
	return false;
});

$(document).ready(function() {
	nodeId = $('#nodeId').val();
	consumptionSourceId =  $('#consumptionSourceId').val();
	featureConsumption = $('#feature-consumption').val() == 'true' ? true : false;
	featureGridPrice = $('#feature-gridPrice').val() == 'true' ? true : false;
	
	$.getJSON('reportableInterval.json', {
				nodeId:$('#nodeId').val(),
				types:['Consumption','Power']
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
	
	var rollingWeekInterval = {eDate : Date.create(new Date().strftime('%Y-%m-%d %H:00:00')).add(1, 'hours')};
	rollingWeekInterval.endDate = rollingWeekInterval.eDate.strftime(dateTimeDataFormat);
	rollingWeekInterval.sDate = rollingWeekInterval.eDate.clone().add(-6, 'days');
	rollingWeekInterval.startDate = rollingWeekInterval.sDate.strftime(dateTimeDataFormat);
	setupRollingWeekHourlyConsumptionChart(rollingWeekInterval);
	
	var rollingMonthInterval = {eDate : Date.create(new Date().strftime('%Y-%m-01'))};
	rollingMonthInterval.endDate = rollingMonthInterval.eDate.strftime(dateDataFormat);
	rollingMonthInterval.sDate = rollingMonthInterval.eDate.clone().add(-12, 'months');
	rollingMonthInterval.startDate = rollingMonthInterval.sDate.strftime(dateDataFormat);
	setupRollingMonthConsumptionChart(rollingMonthInterval);
	setupRollingMonthWeatherChart(rollingMonthInterval);
	setupRollingMonthGenerationChart(rollingMonthInterval);
	
	var rollingDayInterval = {eDate : Date.create(new Date().strftime(dateDataFormat))};
	rollingDayInterval.endDate = rollingDayInterval.eDate.strftime(dateDataFormat);
	rollingDayInterval.sDate = rollingDayInterval.eDate.clone().add(-7, 'days');
	rollingDayInterval.startDate = rollingDayInterval.sDate.strftime(dateDataFormat);
	setupRollingDayConsumptionChart(rollingDayInterval);
	setupRollingDayWeatherChart(rollingDayInterval);
	setupRollingDayGenerationChart(rollingDayInterval);
	
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
	
	nodeWeather = new NodeWeather('current-weather-div', nodeId);
	nodeWeather.refresh();
});
