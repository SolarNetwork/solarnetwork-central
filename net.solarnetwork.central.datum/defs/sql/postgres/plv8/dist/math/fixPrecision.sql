DELETE FROM public.plv8_modules WHERE module = 'math/fixPrecision';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('math/fixPrecision', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = fixPrecision;
/**
 * Round a floating point number the precision of a floating point number. This method
 * works by multiplying, rounding, and then dividing by a provided amount. To save 3
 * decimal digits of precision, you'd pass <b>1000</b> for <code>amt</code>.
 *
 * @param {Number} val The value to fix. If not a Number it will be returned unchanged.
 * @param {Number} amt The amount of precision to keep. Defaults to <b>1000</b>. If <code>null</code>
 *                     then no rounding will be done and <code>val</code> will be returned directly.
 * @returns {Number} the fixed value, or <code>val</code> if <code>val</code> is not a Number.
 */
function fixPrecision(val, amt) {
  if (typeof val !== 'number' || amt === null) {
    return val;
  }
  if (amt === undefined) {
    amt = 1000;
  }
  return Math.round(val * amt) / amt;
}$FUNCTION$);