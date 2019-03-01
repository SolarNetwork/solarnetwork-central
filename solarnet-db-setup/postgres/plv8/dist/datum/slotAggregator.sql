DELETE FROM public.plv8_modules WHERE module = 'datum/slotAggregator';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('datum/slotAggregator', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
	value: true
});
exports.default = slotAggregator;

var _datumAggregate = require('datum/datumAggregate');

var _datumAggregate2 = _interopRequireDefault(_datumAggregate);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Aggregate rows of datum records for a single node ID and any number of source IDs
 * across a fixed time span into smaller time "slots". For example a time span might
 * be 24 hours while the time slots are 15minutes.
 *
 * Once instantiated, call the <code>addDatumRecord()</code> method repeatedly
 * for each datum row. That method may return an aggregate result object. When
 * all rows have been processed, call the <code>finish()</code> method to complete
 * the aggregate processing and return any outstanding aggregate result objects.
 *
 * @param {Object} configuration The set of configuration properties.
 * @param {Number} configuration.startTs     The timestamp associated with this
 *                                           aggregate result (e.g. time slot).
 * @param {Number} configuration.endTs       The timestamp (exclusive) of the end of
 *                                           this aggregate result (e.g. next time slot).
 * @param {Number} configuration.slotSecs    The number of seconds per slot to allocate.
 *                                           Defaults to 600.
 * @param {Number} configuration.toleranceMs The number of milliseconds tolerance before/after
 *                                           time slot to allow calculating accumulating values
 *                                           from. Defaults to 3600000.
 * @param {Object} configuration.hourFill    An object whose keys represent instantaneous datum
 *                                           properties that should used to derive accumulating
 *                                           values named for the associated property value.
 */
function slotAggregator(configuration) {
	var self = {
		version: '1'
	};

	/** The overall starting timestamp. */
	var startTs = configuration && configuration.startTs > 0 ? configuration.startTs : new Date().getTime();

	/** The overall ending timestamp. */
	var endTs = configuration && configuration.endTs > 0 ? configuration.endTs : new Date().getTime();

	/** The number of seconds per time slot. */
	var slotSecs = configuration && configuration.slotSecs > 0 ? configuration.slotSecs : 600;

	/** A mapping of source ID -> array of objects. */
	var resultsBySource = {};

	/**
  * Add another datum record.
  *
  * @param {Object} record            The record to add.
  * @param {Date}   record[ts]        The datum timestamp.
  * @param {Date}   record[ts_start]  The datum time slot.
  * @param {String} record[source_id] The datum source ID.
  * @param {Object} record[jdata]     The datum JSON data object.
  *
  * @returns {Object} If <code>record</code> is associated with a new time slot, then
  *                   an aggregate object will be returned for the data accumulated in
  *                   the previous slot.
  */
	function addDatumRecord(record) {
		if (!(record || record.source_id || record.ts_start)) {
			return;
		}
		var sourceId = record.source_id;
		var ts = record.ts_start.getTime();
		var currResult = resultsBySource[sourceId];
		var aggResult;

		if (currResult === undefined) {
			if (ts < startTs) {
				// allow leading data outside of overall time span (for accumulating calculations)
				ts = startTs;
			}
			currResult = (0, _datumAggregate2.default)(sourceId, ts, ts + slotSecs * 1000, configuration);
			currResult.addDatumRecord(record);
			resultsBySource[sourceId] = currResult;
		} else if (ts !== currResult.ts) {
			// record is in a new slot; finish up the current slot
			aggResult = currResult.finish(record);
			if (ts < endTs) {
				currResult = currResult.startNext(ts, ts + slotSecs * 1000);
				resultsBySource[sourceId] = currResult;
			} else {
				delete resultsBySource[sourceId];
			}
		} else {
			currResult.addDatumRecord(record);
		}
		return aggResult;
	}

	/**
  * Finish all aggregate processing and return an array of any remaining aggregate records.
  *
  * @return {Array} An array of aggregate record objects, or an empty array if there aren't any.
  */
	function finish() {
		var remainingAggregateResults = [];
		var result, prop, aggResult;
		for (prop in resultsBySource) {
			result = resultsBySource[prop];
			aggResult = result.finish();
			if (aggResult) {
				remainingAggregateResults.push(aggResult);
			}
			delete resultsBySource[prop];
		}
		return remainingAggregateResults;
	}

	return Object.defineProperties(self, {
		startTs: { value: startTs },
		endTs: { value: endTs },
		slotSecs: { value: slotSecs },
		configuration: { value: configuration },

		addDatumRecord: { value: addDatumRecord },
		finish: { value: finish }
	});
}$FUNCTION$);