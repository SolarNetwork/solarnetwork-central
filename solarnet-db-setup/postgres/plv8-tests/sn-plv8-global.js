'use strict';
/**
 * @namespace
 */
this.sn = {
	math : {
		util : {}
	},
	util : {}
};

/**
 * Add a number value to an object at a specific key, limited to a percentage value.
 * Optionally increment a number "counter" value for the same key, if a counter
 * object is provided. This is designed to support aggregation of values, both simple
 * sum aggregates and average aggregates where the count will be later used to calculate
 * an effective average of the computed sum.
 *
 * @param {String} k The object key.
 * @param {Number} v The value to add.
 * @param {Object} o The object to manipulate.
 * @param {Number} p A percentage (0-1) to apply to the value before adding. If not defined
 *                   then 1 is assumed.
 * @param {Object} c An optional "counter" object, whose <code>k</code> property will be incremented
 *                   by 1 if defined, or set to 1 if not.
 */
this.sn.math.util.addto = function(k, v, o, p, c) {
	if ( p === undefined ) {
		p = 1;
	}
	if ( o[k] === undefined ) {
		o[k] = (v * p);
	} else {
		o[k] += (v * p);
	}
	if ( c ) {
		if ( c[k] === undefined ) {
			c[k] = 1;
		} else {
			c[k] += 1;
		}
	}
};

/**
 * Calculate an average of two values projected over 1 hour.
 * 
 * @param {Number} w     The first value.
 * @param {Number} prevW The previous value, which will be subtracted from <code>w</code>.
 * @param {Number} milli The difference in time between the two values, in milliseconds.
 * @returns {Number} the average of the values, projected over an hour
 */
this.sn.math.util.calculateAverageOverHours = function(w, prevW, milli) {
	return (Math.abs((w + prevW) / 2) * (milli / 3600000));
};

/**
 * Round a floating point number the precision of a floating point number. This method
 * works by multiplying, rounding, and then dividing by a provided amount. To save 3
 * decimal digits of precision, you'd pass <b>1000</b> for <code>amt</code>.
 *
 * @param {Number} val The value to fix. If not a Number it will be returned unchanged.
 * @param {Number} amt The amount of precision to keep. Defaults to <b>1000</b>.
 * @returns {Number} the fixed value, or <code>val</code> if <code>val</code> is not a Number.
 */
this.sn.math.util.fixPrecision = function(val, amt) {
	if ( typeof val !== 'number' ) {
		return val;
	}
	if ( amt === undefined ) {
		amt = 1000;
	}
	return (Math.round(val * amt) / amt);
};

/**
 * Calculate average values of all properties of an object using "count" values
 * from another object with the same properties. This is designed to work with the
 * {@link #sn.math.util.addto} function. The keys of <code>obj</code> are iterated over
 * and for any corresponding key in <code>counts</code> the value in <code>obj</code>
 * will be divided by the value in <code>counts</code>.
 * 
 * @param {Object} obj       The object whose Number property values are dividends.
 * @param {Object} counts    The object whose Number property values are divisors.
 * @param {Number} precision An optional precision to pass to {@link sn.math.util.fixPrecision}
 *                           to round the result to.
 * @returns {Object} A new object with the same properties defined as <code>obj</code> but
 *                   whose values are the computed division results.
 */
this.sn.math.util.calculateAverages = function(obj, counts, precision) {
	var prop, count, result = {};
	if ( !obj ) {
		return result;
	}
	if ( !counts ) {
		return obj;
	}
	for ( prop in obj ) {
		if ( obj.hasOwnProperty(prop) ) {
			count = counts[prop];
			if ( count > 0 ) {
				result[prop] = sn.math.util.fixPrecision(obj[prop] / count, precision);
			}
		}
	}
	return result;
};

/**
 * Merge one object's properties onto another object. Note that although this method returns
 * an object, it is the same object passed as <code>result</code> and that object is modified
 * directly.
 *
 * @param {Object} result The object to merge properties onto.
 * @param {Object} obj    The object whose properties should be copied onto <code>result</code>.
 * @returns {Object} The <code>result</code> object.
 */
this.sn.util.merge = function(result, obj) {
	var prop;
	if ( obj ) {	
		for ( prop in obj ) {
			if ( obj.hasOwnProperty(prop) ) {
				result[prop] = sn.math.util.fixPrecision(obj[prop]);
			}
		}
	}

	return result;
};
