DELETE FROM public.plv8_modules WHERE module = 'util/intervalMs';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('util/intervalMs', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
	value: true
});
exports.default = intervalMs;
var intervalMsStmt;

/**
 * Convert an interval string into milliseconds.
 *
 * @param {String} intervalValue The interval string, for example <code>1 day</code> or <code>01:00:00</code>.
 * @returns {Number} The millisecond value, or <em>null</em> if could not be converted.
 */
function intervalMs(intervalValue) {
	if (intervalMsStmt === undefined) {
		intervalMsStmt = plv8.prepare('SELECT EXTRACT(EPOCH FROM $1::interval)', ['text']);
	}
	var secs = intervalMsStmt.execute([intervalValue]);
	if (secs.length > 0 && secs[0].date_part !== undefined) {
		return secs[0].date_part * 1000;
	}
	return null;
}$FUNCTION$);