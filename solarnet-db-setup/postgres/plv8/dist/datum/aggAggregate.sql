DELETE FROM public.plv8_modules WHERE module = 'datum/aggAggregate';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('datum/aggAggregate', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.deserialize = deserialize;
exports.default = aggAggregate;

var _fixPrecision = _interopRequireDefault(require("../math/fixPrecision"));

var _addTo = _interopRequireDefault(require("../util/addTo"));

var _mergeObjects = _interopRequireDefault(require("../util/mergeObjects"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Deserialize a serialized state object.
 * 
 * @param {Object} state a serialized state previously returned from a call to the `serialize()` function
 * @returns {Object} a new `aggAggregate` object
 */
function deserialize(state) {
  return aggAggregate(state.sourceId, state.ts, state);
}
/**
 * An aggregate record object that helps keep track of the raw data needed to
 * calculate a single aggregate result from many input aggregate records.
 * 
 * The input records must provide a `jmeta` object with the necessary aggregate
 * metadata needed to calculate the final aggregate value. Specifically, it must
 * provide:
 * 
 * i: object whose properties contain an object with `count`, `min`, and `max` values
 *
 * @param {String} sourceId    The source ID.
 * @param {Number} ts          The timestamp associated with this aggregate result (e.g. time slot).
 * @param {Object} [state]     Optional state, only for deserializing.
 */


function aggAggregate(sourceId, ts, state) {
  var self = {
    version: '1'
  };
  var aobj = state ? state.aobj : {}; // output accumulating

  var sobj = state ? state.sobj : {}; // output static

  var tarr = state ? state.tarr : []; // output tags

  var imeta = state ? state.imeta : {}; // ouutput instantaneous metadata, e.g. {temp:{count:84,min:4,max:22}}

  /** Object with count arrays per instantaneous property, e.g. {temp:[60, 24]}  */

  var iobjCounts = state ? state.iobjCounts : {};
  /** Object with value arrays per instantaneous property, e.g. {temp:[9, 12]} */

  var iobjValues = state ? state.iobjValues : {};
  /**
   * Serialize the state of this object.
   */

  function serialize() {
    return {
      sourceId: sourceId,
      ts: ts,
      aobj: aobj,
      sobj: sobj,
      tarr: tarr,
      imeta: imeta,
      iobjCounts: iobjCounts,
      iobjValues: iobjValues
    };
  }

  function addInstantaneousValues(inst, meta) {
    var prop, val, count, propMeta, min, max, propStats;

    for (prop in inst) {
      if (/_m(?:in|ax)$/.test(prop)) {
        continue;
      }

      val = inst[prop];

      if (val === undefined) {
        continue;
      }

      if (!Array.isArray(iobjValues[prop])) {
        iobjValues[prop] = [];
        iobjCounts[prop] = [];
      }

      propMeta = meta && meta[prop] ? meta[prop] : undefined;
      count = propMeta && propMeta.count ? propMeta.count : 1;
      min = propMeta && propMeta.min !== undefined ? propMeta.min : val;
      max = propMeta && propMeta.max !== undefined ? propMeta.max : val;
      propStats = imeta[prop];
      iobjValues[prop].push(val);
      iobjCounts[prop].push(count);

      if (!propStats) {
        imeta[prop] = {
          count: count,
          min: min,
          max: max
        };
      } else {
        propStats.count += count;

        if (min < propStats.min) {
          propStats.min = min;
        }

        if (max > propStats.max) {
          propStats.max = max;
        }
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
      (0, _addTo.default)(prop, accu[prop], aobj);
    }
  }
  /**
   * Add another datum record.
   *
   * @param {Object} record            The record to add.
   * @param {String} record[source_id] The datum source ID.
   * @param {Object} record[jdata]     The datum JSON data object.
   * @param {Object} record[jmeta]     The datum metadata JSON data object.
   */


  function addDatumRecord(record) {
    if (!(record && record.jdata)) {
      return;
    }

    var accu = record.jdata.a,
        inst = record.jdata.i,
        stat = record.jdata.s,
        tags = record.jdata.t,
        instMeta = record.jmeta ? record.jmeta.i : undefined;

    if (inst) {
      addInstantaneousValues(inst, instMeta);
    }

    if (accu) {
      addAccumulatingValues(accu);
    }

    if (stat) {
      addStaticValues(stat);
    }

    if (Array.isArray(tags)) {
      addTagValues(tags);
    }
  }

  function calculateAggAverages(values, counts, meta) {
    var result = {},
        prop,
        countSum,
        valArray,
        countArray,
        propMeta,
        i,
        len,
        avg;

    for (prop in values) {
      valArray = values[prop];
      countArray = counts[prop];
      propMeta = meta[prop];

      if (!(Array.isArray(valArray) && Array.isArray(countArray) && valArray.length === countArray.length)) {
        continue;
      }

      countSum = propMeta && propMeta.count !== undefined ? propMeta.count : 1;
      avg = 0;

      for (i = 0, len = valArray.length; i < len; i += 1) {
        avg += valArray[i] * countArray[i] / countSum;
      }

      avg = (0, _fixPrecision.default)(avg);
      result[prop] = avg;

      if (propMeta.min !== undefined && propMeta.min !== avg) {
        result[prop + '_min'] = propMeta.min;
      }

      if (propMeta.max !== undefined && propMeta.max !== avg) {
        result[prop + '_max'] = propMeta.max;
      }
    }

    return result;
  }
  /**
   * Finish the aggregate collection.
   *
   * @returns {Object} An aggregate datum record.
   */


  function finish() {
    var aggRecord = {
      ts_start: new Date(ts),
      source_id: sourceId,
      jdata: {},
      jmeta: null
    },
        prop,
        aggInst; // calculate our instantaneous average values

    aggInst = calculateAggAverages(iobjValues, iobjCounts, imeta); // add jmeta to result, removing any min/max values that are redundant

    for (prop in imeta) {
      if (aggRecord.jmeta === null) {
        aggRecord.jmeta = {
          i: imeta
        };
      }

      if (imeta[prop].min === imeta[prop].max) {
        delete imeta[prop].min;
        delete imeta[prop].max;
      }

      break;
    } // use for:in as easy test for an enumerable prop


    for (prop in aggInst) {
      aggRecord.jdata.i = aggInst;
      break;
    }

    for (prop in aobj) {
      aggRecord.jdata.a = (0, _mergeObjects.default)({}, aobj);
      break;
    }

    for (prop in sobj) {
      aggRecord.jdata.s = sobj;
      break;
    }

    if (tarr.length > 0) {
      aggRecord.jdata.t = tarr;
    }

    if (aggRecord.jdata.i === undefined && aggRecord.jdata.a === undefined && aggRecord.jdata.s === undefined && aggRecord.jdata.t === undefined) {
      // no data for this aggregate
      return undefined;
    }

    return aggRecord;
  }

  return Object.defineProperties(self, {
    sourceId: {
      value: sourceId
    },
    ts: {
      value: ts
    },
    addDatumRecord: {
      value: addDatumRecord
    },
    finish: {
      value: finish
    },
    serialize: {
      value: serialize
    }
  });
}$FUNCTION$);