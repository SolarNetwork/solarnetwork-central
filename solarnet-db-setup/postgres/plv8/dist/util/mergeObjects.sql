DELETE FROM public.plv8_modules WHERE module = 'util/mergeObjects';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('util/mergeObjects', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = mergeObjects;

var _fixPrecision = _interopRequireDefault(require("../math/fixPrecision"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Merge one object's properties onto another object. Note that although this method returns
 * an object, it is the same object passed as <code>result</code> and that object is modified
 * directly. All number values are passed to <code>fixPrecision()</code> for rounding.
 *
 * For properties that exist on both objects, values on <code>result</code> will be replaced
 * by the corresponding values from <code>obj</code>.
 *
 * @param {Object}  result       The object to merge properties onto.
 * @param {Object}  obj          The object whose properties should be copied onto <code>result</code>.
 * @param {Number}  precision    An optional precision to pass to <code>fixPrecision()</code>.
 * @param {Boolean} keepExisting If <code>true</code> then do not overwrite existing properties.
 *
 * @returns {Object} The <code>result</code> object.
 */
function mergeObjects(result, obj, precision, keepExisting) {
  var prop;

  if (obj) {
    for (prop in obj) {
      if (obj.hasOwnProperty(prop) && (!keepExisting || !result.hasOwnProperty(prop))) {
        result[prop] = (0, _fixPrecision.default)(obj[prop], precision);
      }
    }
  }

  return result;
}$FUNCTION$);