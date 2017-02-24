'use strict';

import searchFilter from 'util/searchFilter';

function walkObjectPathValues(obj, pathTokens, callback) {
	var i,
		end,
		pathToken,
		currPath = [],
		prop,
		val,
		currPathIdx;

	function handleCallbackValue(value) {
		var j, len;
		if ( Array.isArray(value) ) {
			for ( j = 0, len = value.length; j < len; j += 1 ) {
				if ( callback(currPath, value[j]) === false ) {
					return false;
				}
			}
		} else if ( callback(currPath, value) === false ) {
			return false;
		}
		return true;
	}

	for ( i = 0, end = pathTokens.length - 1; i <= end; i += 1 ) {
		pathToken = pathTokens[i];
		if ( pathToken.length < 1 ) {
			continue;
		}
		currPath.push(pathToken);
		if ( pathToken === '*' && i === end ) {
			currPathIdx = currPath.length - 1;
			for ( prop in obj ) {
				currPath[currPathIdx] = prop;
				val = obj[prop];
				if ( handleCallbackValue(val) === false ) {
					return false;
				}
			}
		} else if ( pathToken === '**' && i < end ) {
			currPathIdx = currPath.length - 1;
			for ( prop in obj ) {
				currPath[currPathIdx] = prop;
				val = obj[prop];

				// check if prop after ** exists
				if ( prop === pathTokens[i+1] ) {
					if ( val !== undefined ) {
						if ( typeof val !== 'object' && i + 1 === end ) {
							// looking for **/X and found X
							if ( handleCallbackValue(val) === false ) {
								return false;
							}
						} else if ( typeof val === 'object' ) {
							// looking for **/X/Y and found X
							if ( walkObjectPathValues(val, (i === 0 ? pathTokens : pathTokens.slice(i)), function(nestedPath, nestedVal) {
									var nestedPath = currPath.concat(nestedPath);
									return callback(nestedPath, nestedVal);
										}) === false ) {
								return false;
							}
						}
					}
				} else if ( typeof val === 'object' ) {
					if ( walkObjectPathValues(val, (i === 0 ? pathTokens : pathTokens.slice(i)), function(nestedPath, nestedVal) {
								var nestedPath = currPath.concat(nestedPath);
								return callback(nestedPath, nestedVal);
							}) === false ) {
						return false;
					}
				} else if ( pathTokens[i+1] === '*' ) {
					if ( handleCallbackValue(val) === false ) {
						return false;
					}
				}
			}
			break;
		} else if ( i === end ) {
			if ( handleCallbackValue(obj[pathToken]) === false ) {
				return false;
			}
		} else if ( obj[pathToken] !== undefined ) {
			obj = obj[pathToken];
		} else {
			// prop pathToken not found on obj
			break;
		}
	}
	return true;
}

function evaluateFilter(obj, filter) {
	var logicStack = [],
		stackIdx = -1,
		logicStackSatisfiedIdx = -1, // index in logicStack of OR that has been satisfied
		currLogicOp = '&', // default is AND for simple filters
		foundMatch = false;

	function shortCircuitIfPossible(match) {
		var keepWalking = true;
		if ( currLogicOp === '&' ) {
			if ( match === false ) {
				if ( stackIdx >= 0 ) {
					logicStack[stackIdx].result = false;
				}
				if ( stackIdx < 1 ) {
					// top-level AND has failed; all done
					foundMatch = false;
					keepWalking = false;
				} else {
					logicStackSatisfiedIdx = stackIdx;
				}
			} else if ( stackIdx < 0 ) {
				foundMatch = true;
			} else {
				logicStack[stackIdx].result = true;
			}
		} else if ( currLogicOp === '|' ) {
			if ( match === true ) {
				if ( stackIdx >= 0 ) {
					logicStack[stackIdx].result = true;
				}
				if ( stackIdx < 1 ) {
					// top-level OR has failed; all done
					foundMatch = true;
					keepWalking = false;
				} else {
					logicStackSatisfiedIdx = stackIdx;
				}
			}
		}
		return !keepWalking;
	}

	filter.walk(function(err, node, parent) {
		var match = false;
		if ( parent !== undefined ) {
			if ( parent !== logicStack[stackIdx].node ) {
				while ( stackIdx >= 0 ) {
					if ( logicStack[stackIdx].node === parent ) {
						currLogicOp = logicStack[stackIdx].op;
						if ( shortCircuitIfPossible(logicStack[logicStack.length - 1].result) ) {
							return false;
						}
						break;
					}
					stackIdx -= 1;
				}

				// trim the stack to the current parent
				logicStack.length = stackIdx + 1;

				if ( stackIdx < 0 ) {
					return false;
				}

				// if we've popped back before a satisfied condition, reset that index
				if ( logicStackSatisfiedIdx > stackIdx ) {
					logicStackSatisfiedIdx = -1;
				}
			}
		}
		if ( node.children ) {
			// new logic grouping
			logicStack.push({op:node.op, result:false, node:node});
			currLogicOp = node.op;
			stackIdx += 1;
		} else if ( logicStackSatisfiedIdx === -1 ) {
			walkObjectPathValues(obj, node.key.split('/'), function(path, value) {
				if ( node.op === '=' ) {
					// note using lax equality here for automatic coersion
					if ( value == node.val ) {
						match = true;
					}
				} else if ( node.op === '~=' ) {
					if ( value !== undefined && value.search(new RegExp(node.val)) !== -1 ) {
						match = true;
					}
				} else if ( node.op === '<' ) {
					if ( value < node.val ) {
						match = true;
					}
				} else if ( node.op === '<=' ) {
					if ( value <= node.val ) {
						match = true;
					}
				} else if ( node.op === '>' ) {
					if ( value > node.val ) {
						match = true;
					}
				} else if ( node.op === '>=' ) {
					if ( value >= node.val ) {
						match = true;
					}
				}
				// in case of wildcard path, only keep looking for more path matches if we haven't found a value match
				return !match;
			});

			if ( shortCircuitIfPossible(match) ) {
				return false;
			}
		}
		return true;
	});

	if ( logicStack.length > 0 ) {
		foundMatch = logicStack[stackIdx].result;
	}

	return foundMatch;
}

export default function objectPathMatcher(obj) {
	var self = {
		version : '1'
	};

	/**
	 * Test if a filter matches the configured object.
	 *
	 * @param {String} filterText The filter to parse and then test for matches.
	 *
	 * @returns {Boolean} <code>true</code> if the filter matches
	 */
	function matches(filterText) {
		return matchesFilter(searchFilter(filterText));
	}

	/**
	 * Test if a filter matches the configured object.
	 *
	 * @param {Object} filter The <code>searchFilter</code> to test for matches.
	 *
	 * @returns {Boolean} <code>true</code> if the filter matches
	 */
	function matchesFilter(filter) {
		return evaluateFilter(obj, filter);
	}

	return Object.defineProperties(self, {
		/** The object being compared to. */
		obj			: { value : obj },

		matches		: { value : matches },
	});
}
