import fixPrecision from 'math/fixPrecision';

/**
 * Calculate average values of all properties of an object using "count" values from
 * another object with the same properties. The keys of <code>obj</code> are iterated over
 * and for any corresponding key in <code>counts</code> the value in <code>obj</code>
 * will be divided by the corresponding value in <code>counts</code>.
 *
 * @param {Object} obj       The object whose Number property values are dividends.
 * @param {Object} counts    The object whose Number property values are divisors.
 * @param {Number} precision An optional precision to pass to {@link fixPrecision}
 *                           to round the result to.
 * @returns {Object} A new object with the same properties defined as <code>obj</code> but
 *                   whose values are the computed division results.
 */
export default function calculateAverages(obj, counts, precision) {
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
				result[prop] = fixPrecision(obj[prop] / count, precision);
			}
		}
	}
	return result;
}
