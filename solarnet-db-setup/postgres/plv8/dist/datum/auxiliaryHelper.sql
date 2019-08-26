DELETE FROM public.plv8_modules WHERE module = 'datum/auxiliaryHelper';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('datum/auxiliaryHelper', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = auxiliaryHelper;
var kAuxResetTag = 'Reset';
var kAuxResetFinalTag = 'final';
var kAuxResetStartTag = 'start';
/**
 * A helper object for dealing with datum auxiliary records.
 */

function auxiliaryHelper() {
  var self = {
    version: '1'
  };
  /**
   * Find out the kind of reset record a datum represents, if any.
   * 
   * @param {object} datum the datum to inspect
   * @returns {string|null} the type of reset record, either `final`, `start`, or `null` if it isn't one
   */

  function datumResetKind(datum) {
    if (!(datum && Array.isArray(datum.t) && datum.t.length > 1 && datum.t.indexOf(kAuxResetTag) >= 0)) {
      return null;
    }

    return datum.t.indexOf(kAuxResetFinalTag) >= 0 ? kAuxResetFinalTag : datum.t.indexOf(kAuxResetStartTag) >= 0 ? kAuxResetStartTag : null;
  }

  return Object.defineProperties(self, {
    resetTag: {
      value: kAuxResetTag
    },
    resetFinalTag: {
      value: kAuxResetFinalTag
    },
    resetStartTag: {
      value: kAuxResetStartTag
    },
    datumResetKind: {
      value: datumResetKind
    }
  });
}$FUNCTION$);