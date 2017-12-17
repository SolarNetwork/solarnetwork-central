/**
 * Calculate an average of two values projected over 1 hour.
 *
 * @param {Number} value     The first value.
 * @param {Number} prevValue The previous value, which will be subtracted from <code>w</code>.
 * @param {Number} ms        The difference in time between the two values, in milliseconds.
 *
 * @returns {Number} The average of the values, projected over an hour.
 */
export default function calculateAverageOverHours(value, prevValue, ms) {
	if ( !Number.isFinite(ms) ) {
		return 0;
	}
	return (((value + prevValue) / 2) * (ms / 3600000));
}
