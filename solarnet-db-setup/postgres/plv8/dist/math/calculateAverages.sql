DELETE FROM public.plv8_modules WHERE module = 'math/calculateAverages';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('math/calculateAverages', FALSE,
$FUNCTION$"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = calculateAverages;

var _fixPrecision = _interopRequireDefault(require("math/fixPrecision"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

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
function calculateAverages(obj, counts, precision) {
  var prop,
      count,
      result = {};

  if (!obj) {
    return result;
  }

  if (!counts) {
    return obj;
  }

  for (prop in obj) {
    if (obj.hasOwnProperty(prop)) {
      count = counts[prop];

      if (count > 0) {
        result[prop] = (0, _fixPrecision.default)(obj[prop] / count, precision);
      }
    }
  }

  return result;
}$FUNCTION$);