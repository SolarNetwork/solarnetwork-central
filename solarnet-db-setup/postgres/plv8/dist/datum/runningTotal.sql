DELETE FROM public.plv8_modules WHERE module = 'datum/runningTotal';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('datum/runningTotal', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
	value: true
});
exports.default = runningTotal;

var _addTo = require('../util/addTo');

var _addTo2 = _interopRequireDefault(_addTo);

var _mergeObjects = require('../util/mergeObjects');

var _mergeObjects2 = _interopRequireDefault(_mergeObjects);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var kDefaultHourFill = { watts: 'wattHours' };
var kStatPropName = /^(.+)_(min|max)$/;

/**
 * A running total record object that helps keep track of the raw data needed to
 * calculate a single running total result from many aggregated input records.
 *
 * @param {String} sourceId    The source ID.
 */
function runningTotal(sourceId) {
	var self = {
		version: '1'
	};

	var aobj = {};
	var iobj = {};
	var iobjStats = {};
	var sobj = {};
	var tarr = [];
	var totalWeight = 0;

	function addInstantaneousValues(inst, weight) {
		var prop, statMatch;
		for (prop in inst) {
			// skip properties that appear to be statistic values, e.g. foo_min, foo_max
			statMatch = prop.match(kStatPropName);
			if (statMatch && inst[statMatch[1]] !== undefined) {
				if (iobjStats[statMatch[1]] === undefined) {
					iobjStats[statMatch[1]] = { min: inst[prop], max: inst[prop] };
				} else if (statMatch[2] === 'min' && inst[prop] < iobjStats[statMatch[1]].min) {
					iobjStats[statMatch[1]].min = inst[prop];
				} else if (inst[prop] > iobjStats[statMatch[1]].max) {
					// assume === 'max' here
					iobjStats[statMatch[1]].max = inst[prop];
				}
			} else {
				(0, _addTo2.default)(prop, inst[prop], iobj, weight);
			}
		}
	}

	function addStaticValues(stat) {
		var prop;
		for (prop in stat) {
			sobj[prop] = stat[prop];
		}
	}

	function addTagValues(tags) {
		var i, t;
		for (i = 0; i < tags.length; i += 1) {
			t = tags[i];
			if (tarr.indexOf(t) === -1) {
				tarr.push(t);
			}
		}
	}

	function addAccumulatingValues(accu) {
		var prop;
		for (prop in accu) {
			(0, _addTo2.default)(prop, accu[prop], aobj);
		}
	}

	/**
  * Add another datum record.
  *
  * @param {Object} record            The record to add.
  * @param {Number} record.weight     The weight of the record.
  * @param {Object} record.jdata      The datum JSON data object.
  */
	function addDatumRecord(record) {
		if (!(record && record.jdata && record.weight)) {
			return;
		}

		var accu = record.jdata.a,
		    inst = record.jdata.i,
		    stat = record.jdata.s,
		    tags = record.jdata.t;

		// add instantaneous values
		if (inst) {
			addInstantaneousValues(inst, record.weight);
		}

		// merge in static values
		if (stat) {
			addStaticValues(stat);
		}

		// add tag values
		if (Array.isArray(tags)) {
			addTagValues(tags);
		}

		// add accumulating values
		if (accu) {
			addAccumulatingValues(accu);
		}

		totalWeight += record.weight;
	}

	/**
  * Finish the aggregate collection.
  *
  * @returns {Object} An aggregate datum record.
  */
	function finish() {
		var aggRecord = {
			source_id: sourceId,
			jdata: {}
		},
		    prop,
		    aggInst = {};

		for (prop in iobj) {
			// apply weighted average to results
			aggInst[prop] = iobj[prop] / totalWeight;

			// inject min/max statistic values for instantaneous average values
			if (iobjStats[prop] !== undefined) {
				if (iobjStats[prop].min !== undefined && iobjStats[prop].min !== aggInst[prop]) {
					aggInst[prop + '_min'] = iobjStats[prop].min;
				}
				if (iobjStats[prop].max !== undefined && iobjStats[prop].max !== aggInst[prop]) {
					aggInst[prop + '_max'] = iobjStats[prop].max;
				}
			}
		}

		// use for:in as easy test for an enumerable prop
		for (prop in aggInst) {
			// add instantaneous results via merge() to pick fixPrecision() values
			aggRecord.jdata.i = (0, _mergeObjects2.default)({}, aggInst);
			break;
		}

		// use for:in as easy test for an enumerable prop
		for (prop in aobj) {
			// add accumulating results via merge() to pick fixPrecision() values
			aggRecord.jdata.a = (0, _mergeObjects2.default)({}, aobj);
			break;
		}

		if (aggRecord.jdata.i === undefined && aggRecord.jdata.a === undefined && aggRecord.jdata.s === undefined && aggRecord.jdata.t === undefined) {
			// no data for this aggregate
			return undefined;
		}

		return aggRecord;
	}

	return Object.defineProperties(self, {
		sourceId: { value: sourceId },

		addDatumRecord: { value: addDatumRecord },
		finish: { value: finish }
	});
}$FUNCTION$);