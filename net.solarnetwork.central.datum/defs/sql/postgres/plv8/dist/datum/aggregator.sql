DELETE FROM public.plv8_modules WHERE module = 'datum/aggregator';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('datum/aggregator', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
	value: true
});
exports.default = aggregator;

var _datumAggregate = require('datum/datumAggregate');

var _datumAggregate2 = _interopRequireDefault(_datumAggregate);

var _mergeObjects = require('../util/mergeObjects');

var _mergeObjects2 = _interopRequireDefault(_mergeObjects);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Aggregate rows of datum records for a single node ID and any number of source IDs
 * across a fixed time span into a single aggregate result per source ID.
 *
 * Once instantiated, call the <code>addDatumRecord()</code> method repeatedly
 * for each datum row. When all rows have been processed, call the
 * <code>finish()</code> method to complete the aggregate processing and return
 * all the aggregate result objects.
 *
 * @param {Object} configuration The set of configuration properties.
 * @param {Number} configuration.startTs     The timestamp associated with this
 *                                           aggregate result (e.g. time span).
 * @param {Number} configuration.endTs       The timestamp (exclusive) of the end of
 *                                           this aggregate result.
 */
function aggregator(configuration) {
	var self = {
		version: '1'
	};

	/** The overall starting timestamp. */
	var startTs = configuration && configuration.startTs > 0 ? configuration.startTs : new Date().getTime();
	var startDate = new Date(startTs);

	/** The overall ending timestamp. */
	var endTs = configuration && configuration.endTs > 0 ? configuration.endTs : new Date().getTime();

	/** A mapping of source ID -> array of objects. */
	var resultsBySource = {};

	var resultsByOrder = [];

	/**
  * Add another datum record.
  *
  * @param {Object} record            The record to add.
  * @param {Date}   record[ts]        The datum timestamp.
  * @param {String} record[source_id] The datum source ID.
  * @param {Object} record[jdata]     The datum JSON data object.
  */
	function addDatumRecord(record) {
		if (!(record || record.source_id || record.ts)) {
			return;
		}
		var sourceId = record.source_id;
		var recTs = record.ts.getTime();
		var currResult = resultsBySource[sourceId];
		var recToAdd = record;

		if (currResult === undefined) {
			currResult = (0, _datumAggregate2.default)(sourceId, startTs, endTs, configuration);

			// keep track of results by source ID for fast lookup
			resultsBySource[sourceId] = currResult;

			// also keep track of order we obtain sources, so results ordered in same way
			resultsByOrder.push(currResult);
		}

		// when adding records within the time span, force the time slot to our start date so they all aggregate into one;
		// otherwise set the time slot to the record date itself
		recToAdd = (0, _mergeObjects2.default)({
			ts_start: recTs > startTs && recTs < endTs ? startDate : record.ts }, record, undefined, true);

		currResult.addDatumRecord(recToAdd);
	}

	/**
  * Finish all aggregate processing and return an array of all aggregate records.
  *
  * @return {Array} An array of aggregate record objects for each source ID encountered by
  *                 all previous calls to <code>addDatumRecord()</code>, or an empty array
  *                 if there aren't any.
  */
	function finish() {
		var aggregateResults = [],
		    i,
		    aggResult;
		for (i = 0; i < resultsByOrder.length; i += 1) {
			aggResult = resultsByOrder[i].finish();
			if (aggResult) {
				aggregateResults.push(aggResult);
			}
		}
		return aggregateResults;
	}

	return Object.defineProperties(self, {
		startTs: { value: startTs },
		endTs: { value: endTs },
		configuration: { value: configuration },

		addDatumRecord: { value: addDatumRecord },
		finish: { value: finish }
	});
}$FUNCTION$);