'use strict';

import runningTotal from 'datum/runningTotal';

/**
 * Aggregate rows of datum records for a single node ID and any number of source IDs
 * into a single running total result per source ID.
 *
 * Once instantiated, call the <code>addDatumRecord()</code> method repeatedly
 * for each datum row. When all rows have been processed, call the
 * <code>finish()</code> method to complete the aggregate processing and return
 * all the aggregate result objects.
 */
export default function totalor() {
	var self = {
		version : '1'
	};

	/** A mapping of source ID -> array of objects. */
	var resultsBySource = {};

	var resultsByOrder = [];

	/**
	 * Add another datum record.
	 *
	 * @param {Object} record            The record to add.
	 * @param {String} record.source_id  The datum source ID.
	 * @param {Number} record.weight     The weight of the record.
	 * @param {Object} record.jdata      The datum JSON data object.
	 */
	function addDatumRecord(record) {
		if ( !(record || record.source_id) ) {
			return;
		}
		var sourceId = record.source_id;
		var currResult = resultsBySource[sourceId];

		if ( currResult === undefined ) {
			currResult = runningTotal(sourceId);

			// keep track of results by source ID for fast lookup
			resultsBySource[sourceId] = currResult;

			// also keep track of order we obtain sources, so results ordered in same way
			resultsByOrder.push(currResult);
		}

		currResult.addDatumRecord(record);
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
			i, aggResult;
		for ( i = 0; i < resultsByOrder.length; i += 1 ) {
			aggResult = resultsByOrder[i].finish();
			if ( aggResult ) {
				aggregateResults.push(aggResult);
			}
		}
		return aggregateResults;
	}

	return Object.defineProperties(self, {
		addDatumRecord		: { value : addDatumRecord },
		finish				: { value : finish },
	});
}
