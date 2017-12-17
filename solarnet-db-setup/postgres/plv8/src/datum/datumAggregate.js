'use strict';

import calculateAverages from 'math/calculateAverages'
import calculateAverageOverHours from 'math/calculateAverageOverHours'
import fixPrecision from 'math/fixPrecision'
import addTo from 'util/addTo'
import mergeObjects from 'util/mergeObjects'

var kDefaultHourFill = {watts: 'wattHours'};

/**
 * An aggregate record object that helps keep track of the raw data needed to
 * calculate a single aggregate result from many input records.
 *
 * @param {String} sourceId    The source ID.
 * @param {Number} ts          The timestamp associated with this aggregate result (e.g. time slot).
 * @param {Number} endTs       The timestamp (exclusive) of the end of this aggregate result (e.g. next time slot).
 *
 * @param {Object} configuration An optional set of configuration properties.
 * @param {Number} configuration.toleranceMs The number of milliseconds tolerance before/after
 *                                           time slot to allow calculating accumulating values
 *                                           from. Defaults to 3600000.
 * @param {Object} configuration.hourFill    An object whose keys represent instantaneous datum
 *                                           properties that should used to derive accumulating
 *                                           values named for the associated property value.
 */
export default function datumAggregate(sourceId, ts, endTs, configuration) {
	var self = {
		version : '1'
	};

	/** The number of milliseconds tolerance before/after time span to allow finding prev/next records. */
	var toleranceMs = (configuration && configuration.toleranceMs !== undefined ? configuration.toleranceMs : 3600000);

	/** A mapping of instantaneous datum property keys to associated accumulating property keys that should be derived.  */
	var hourFill = 	(configuration && configuration.hourFill !== undefined ? configuration.hourFill : kDefaultHourFill);

	/** The number of milliseconds in this time slot. */
	var slotMs = endTs - ts;

	var aobj = {};
	var iobj = {};
	var iobjCounts = {};
	var iobjStats = {};
	var sobj = {};
	var tarr = [];
	var prevRecord;
	var finishRecord;

	function addInstantaneousValues(inst) {
		var prop;
		for ( prop in inst ) {
			addTo(prop, inst[prop], iobj, 1, iobjCounts, iobjStats);
		}
	}

	function addStaticValues(stat) {
		var prop;
		for ( prop in stat ) {
			sobj[prop] = stat[prop];
		}
	}

	function addTagValues(tags) {
		var i, t;
		for ( i = 0; i < tags.length; i += 1 ) {
			t = tags[i];
			if ( tarr.indexOf(t) === -1 ) {
				tarr.push(t);
			}
		}
	}

	function addAccumulatingValues(accu, inst, recTs, recDate) {
		var percent = 1,
			recTime = recDate.getTime(),
			recTimeDiff = 0,
			prevRecTime,
			prevAccu,
			prevInst,
			prop;

		if ( recTs < ts || !prevRecord ) {
			// this record is from before our time slot or no previous record yet; no accumulation yet
			return;
		}

		prevRecTime = prevRecord.ts.getTime();
		recTimeDiff = recTime - prevRecTime;

		if ( recTimeDiff > toleranceMs ) {
			// this record is too far away from previous record to treat as accumulating
			percent = 0;
		} else if ( recTime > endTs && (prevRecTime + slotMs > recTime)  ) {
			// this record is from after our time slot and this slot crosses its slot boundary; accumulate leading fractional values
			percent = ((endTs - prevRecTime) / recTimeDiff);
		} else if ( prevRecTime < ts ) {
			// this is the first record in our time slot, following another in the previous slot;
			// accumulate trailing fractional values
			percent = ((recTime - ts) / recTimeDiff);
		}

		if ( !(percent > 0) ) {
			return;
		}

		// calculate accumulating values
		prevAccu = prevRecord.jdata.a;
		if ( prevAccu ) {
			for ( prop in accu ) {
				addTo(prop, calculateAccumulatingValue(accu[prop], prevAccu[prop]), aobj, percent);
			}
		}

		// handle any accumulating values derived from instantaneous (via hourFill)
		prevInst = prevRecord.jdata.i;
		if ( inst && prevInst ) {
			for ( prop in hourFill ) {
				// only derive from instantaneous if property does not already exist as accumulating
				if ( (!accu || accu[hourFill[prop]] === undefined) && inst[prop] !== undefined && prevInst[prop] !== undefined ) {
					addTo(hourFill[prop], calculateAverageOverHours(inst[prop], prevInst[prop], recTimeDiff), aobj, percent);
				}
			}
		}
	}

	function calculateAccumulatingValue(val, prevVal) {
		// TODO: port extra logic for handling common data errors
		if ( prevVal === undefined || val === undefined ) {
			return 0;
		}
		var diff = (val - prevVal);
		return diff;
	}

	/**
	 * Add another datum record.
	 *
	 * @param {Object} record            The record to add.
	 * @param {Date}   record[ts]        The datum timestamp.
	 * @param {Date}   record[ts_start]  The datum time slot.
	 * @param {String} record[source_id] The datum source ID.
	 * @param {Object} record[jdata]     The datum JSON data object.
	 */
	function addDatumRecord(record) {
		if ( !(record && record.jdata && record.ts_start && record.ts) ) {
			return;
		}

		var recTs = record.ts_start.getTime(),
			accu = record.jdata.a,
			inst = record.jdata.i,
			stat = record.jdata.s,
			tags = record.jdata.t;

		// as long as this record's time slot falls within the configured time slot,
		// handle instantaneous, static, and tag values
		if ( recTs === ts ) {
			if ( inst ) {
				// add instantaneous values
				addInstantaneousValues(inst);
			}

			// merge in static values
			if ( stat ) {
				addStaticValues(stat);
			}

			// add tag values
			if ( Array.isArray(tags) ) {
				addTagValues(tags);
			}
		}

		addAccumulatingValues(accu, inst, recTs, record.ts);

		// save curr record as previous for future calculations
		prevRecord = record;
	}

	/**
	 * Finish the aggregate collection.
	 *
	 * @param {Object} nextRecord An optional "next" record that may contain partial
	 *                            data to associate with this aggregate result.
	 * @returns {Object} An aggregate datum record.
	 */
	function finish(nextRecord) {
		var aggRecord = {
				ts_start  : new Date(ts),
				source_id : sourceId,
				jdata     : {},
			},
			prop,
			aggInst,
			aggInstStat;

		// handle any fractional portion of the next record
		if ( nextRecord ) {
			// only add if the next record is not too far into the future from the last record
			if ( prevRecord && nextRecord.ts.getTime() < prevRecord.ts.getTime() + toleranceMs ) {
				// calling addDatumRecord will update prevRecord, so keep a reference to the current value
				finishRecord = prevRecord;
				addDatumRecord(nextRecord);
				prevRecord = finishRecord;
			} else {
				prevRecord = undefined;
			}
			finishRecord = nextRecord;
		}

		// calculate our instantaneous average values
		aggInst = calculateAverages(iobj, iobjCounts);

		// inject min/max statistic values for instantaneous average values
		for ( prop in aggInst ) {
			if ( aggRecord.jdata.i === undefined ) {
				aggRecord.jdata.i = aggInst;
			}
			if ( iobjStats[prop] !== undefined ) {
				aggInstStat = fixPrecision(iobjStats[prop].min);
				if ( aggInstStat !== undefined && aggInstStat !== aggInst[prop] ) {
					aggInst[prop+'_min'] = aggInstStat;
				}
				aggInstStat = fixPrecision(iobjStats[prop].max);
				if ( aggInstStat !== undefined && aggInstStat !== aggInst[prop] ) {
					aggInst[prop+'_max'] = aggInstStat;
				}
			}
		}

		// use for:in as easy test for an enumerable prop
		for ( prop in aobj ) {
			// add accumulating results via merge() to pick fixPrecision() values
			aggRecord.jdata.a = mergeObjects({}, aobj);
			break;
		}

		for ( prop in sobj ) {
			// add accumulating results via merge() to pick fixPrecision() values
			aggRecord.jdata.s = sobj;
			break;
		}

		if ( tarr.length > 0 ) {
			aggRecord.jdata.t = tarr;
		}

		if ( aggRecord.jdata.i === undefined && aggRecord.jdata.a === undefined
				&& aggRecord.jdata.s === undefined && aggRecord.jdata.t === undefined ) {
			// no data for this aggregate
			return undefined;
		}

		return aggRecord
	}

	/**
	 * Create a new aggregate based on the configured properties of this object
	 * and a new time slot. If a record was passed to <code>finishRecord()</code>
	 * then the record previous to that one, and that one, will be automatically
	 * added to the new aggregate.
	 *
	 * @param {Number} nextTs     The timestamp associated with the new aggregate result.
	 * @param {Number} nextEndTs  The timestamp (exclusive) of the end of the new aggregate result.
	 *
	 * @returns {Object} A new <code>datumAggregate</code> object.
	 */
	function startNext(nextTs, nextEndTs) {
		var result = datumAggregate(sourceId, nextTs, nextEndTs, configuration);
		if ( finishRecord ) {
			if ( prevRecord ) {
				result.addDatumRecord(prevRecord);
			}
			result.addDatumRecord(finishRecord);
		}
		return result;
	}

	return Object.defineProperties(self, {
		sourceId			: { value : sourceId },
		ts					: { value : ts },
		endTs				: { value : endTs },
		toleranceMs			: { value : toleranceMs },
		hourFill            : { value : hourFill },

		addDatumRecord		: { value : addDatumRecord },
		finish				: { value : finish },
		startNext			: { value : startNext },
	});
}
