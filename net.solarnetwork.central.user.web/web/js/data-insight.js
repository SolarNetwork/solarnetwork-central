$(document).ready(function() {
	'use strict';
	
	function createLocalizedNumbers(item) {
		var prop;
		for ( prop in item ) {
			if ( !isNaN(Number(item[prop])) ) {
				item[prop+'Display'] = item[prop].toLocaleString();
			}
		}
	}
	
	function populateOverviewStats(container, model) {
		createLocalizedNumbers(model);
		SolarReg.Templates.replaceTemplateProperties(container, model);
	}
	
	function populateCounts(container, counts) {
		var i, len, item, prop;
		for ( i = 0, len = counts.length; i < len; i+=1 ) {
			item = counts[i];
			createLocalizedNumbers(item);
			item.dateDisplay = moment(item.ts).format('D MMM YYYY');
		}
		SolarReg.Templates.populateTemplateItems(container, counts);
	}

	$('#data-insight-overview').first().each(function() {
		var section = $(this);
		$.getJSON(SolarReg.solarUserURL('/sec/data-insight/overall'), function(json) {
			if ( json && json.success === true && json.data ) {
				populateOverviewStats(section, json.data);
				populateOverviewStats($('#data-insight-overview-datum'), json.data);
				populateCounts($('#data-insight-recent'), json.data.counts);
			}
		});
	});
	
});