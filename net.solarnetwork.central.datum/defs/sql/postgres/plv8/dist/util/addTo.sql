DELETE FROM public.plv8_modules WHERE module = 'util/addTo';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('util/addTo', FALSE,
$FUNCTION$'use strict';

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
 * @param {Object} r An optional "stats" object, whose <code>k</code> property will be maintained
 *                   as a nested object with <code>min</code> and <code>max</code> properties.
 */

Object.defineProperty(exports, "__esModule", {
	value: true
});
exports.default = addTo;
function addTo(k, v, o, p, c, r) {
	var newVal;
	if (p === undefined) {
		p = 1;
	}
	newVal = v * p;
	if (o[k] === undefined) {
		o[k] = newVal;
	} else {
		o[k] += newVal;
	}
	if (c) {
		if (c[k] === undefined) {
			c[k] = 1;
		} else {
			c[k] += 1;
		}
	}
	if (r) {
		if (r[k] === undefined) {
			r[k] = { min: newVal, max: newVal };
		} else {
			if (r[k].min === undefined) {
				r[k].min = newVal;
			} else if (newVal < r[k].min) {
				r[k].min = newVal;
			}
			if (r[k].max === undefined) {
				r[k].max = newVal;
			} else if (newVal > r[k].max) {
				r[k].max = newVal;
			}
		}
	}
}$FUNCTION$);