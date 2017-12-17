DELETE FROM public.plv8_modules WHERE module = 'util/searchFilter';
INSERT INTO public.plv8_modules (module, autoload, source) VALUES ('util/searchFilter', FALSE,
$FUNCTION$'use strict';

Object.defineProperty(exports, "__esModule", {
	value: true
});
exports.default = searchFilter;
var kOpAnd = '&';
var kOpOr = '|';
var kOpNot = '!';

var kOpEqual = '=';
var kOpApprox = '~=';
var kOpLT = '<';
var kOpLTE = '<=';
var kOpGT = '>';
var kOpGTE = '>=';

/** RegExp to split a string into filter tokens. */
var kTokenRegExp = /\s*(\([&|!]|\(|\))\s*/g;

// RegExp to match key, op, val of a comparison token
var kCompRegExp = /(.+?)(=|~=|<=?|>=?)(.+)/;

function isLogicOp(text) {
	return text === kOpAnd || text === kOpOr || text === kOpNot;
}

/**
 * Create a new logic node out of a logic operator.
 *
 * @param {String} op The logic operator, e.g. <code>&</code>.
 *
 * @returns {Object} A logic node.
 * @constructor
 */
function logicNode(op) {
	var self = {};
	var children = [];

	/**
  * Add a child node. If this node does not allow children, the
  * <code>child</code> will not be added.
  *
  * @param {pathNode} child The node to add.
  *
  * @returns {pathNode} This object.
  */
	function addChild(child) {
		children.push(child);
		return self;
	}

	return Object.defineProperties(self, {
		op: { value: op, enumerable: true },
		children: { value: children, enumerable: true },

		addChild: { value: addChild }
	});
}

/**
 * Parse a simple search filter like <code>(foo=bar)</code> into a node object.
 *
 * @param {String} text The simple search filter text to parse.
 *
 * @returns {Object} The parsed node object, or <code>undefined</code> if not parsable.
 * @constructor
 */
function compNode(text) {
	var self = {};

	var key, op, val;

	(function () {
		var match = text ? text.match(kCompRegExp) : undefined;
		if (match) {
			key = match[1];
			op = match[2];
			val = match[3];
		}
	})();

	return key === undefined ? undefined : Object.defineProperties(self, {
		/** The property the search filter applies to. */
		key: { value: key, enumerable: true },

		/** The comparison operation. */
		op: { value: op, enumerable: true },

		/** The property value to compare with. */
		val: { value: val, enumerable: true }
	});
}

function walkNode(node, parent, callback) {
	var i, len;
	if (node === undefined) {
		return;
	}
	if (callback(null, node, parent) === false) {
		return false;
	}
	if (node.children !== undefined) {
		for (i = 0, len = node.children.length; i < len; i += 1) {
			if (walkNode(node.children[i], node, callback) === false) {
				return false;
			}
		}
	}
	return true;
}

/**
 * Create a new search filter.
 *
 * Search filters are expressed in LDAP search filter notation, for example
 * <code>(name=Bob)</code> is described as "find objects whose name is Bob".
 * Complex logic can be expressed using logical and, or, and not expressions.
 * For example <code>(&(name=Bob)(age>20))</code> is described as "find objects
 * whose name is Bob and age is greater than 20".
 *
 * @param {String} filterText The search filter to parse.
 * @constructor
 */
function searchFilter(filterText) {
	var self = {
		version: '1'
	};

	var rootNode;

	/**
  * Walk the node tree, invoking a callback function for each node.
  *
  * @param {Function} callback A callback function, which will be passed an error parameter,
  *                            the current node, and the current node's parent (or
  *                            <code>undefined</code> for the root node). If the callback
  *                            returns <code>false</code> the walking will stop.
  */
	function walk(callback) {
		walkNode(rootNode, undefined, callback);
	}

	/**
  * Parse an array of search filter tokens, as created via splitting a
  * string with the <code>kTokenRegExp</code> regular expression. For
  * example the simple filter <code>(foo=bar)</code> could be expressed
  * as the tokens <code>["(", "foo=bar", ")"]</code> while the complex
  * filter <code>(&(foo=bar)(bim>1))</code> could be expressed as the
  * tokens <code>["(&", "(", "foo=bar", ")", "(", "bim>1", ")", ")"]</code>.
  *
  * Note than empty string tokens are ignored.
  */
	function parseTokens(tokens, start, end) {
		var i,
		    c,
		    topNode,
		    node,
		    tok,
		    stack = [];
		for (i = start; i < end; i += 1) {
			tok = tokens[i];
			if (tok.length < 1) {
				continue;
			}
			c = tok.charAt(0);
			if (c === '(') {
				// starting new item
				if (tok.length > 1) {
					// starting new logical group
					c = tok.charAt(1);
					node = logicNode(c);
					if (topNode) {
						topNode.addChild(node);
					}
					stack.push(node);
					topNode = node;
				} else {
					// starting a key/value pair
					if (i + 1 < end) {
						node = compNode(tokens[i + 1]);
					}
					if (topNode) {
						topNode.addChild(node);
					} else {
						// our top node is not a group node, so only one node is possible and we can return now
						return node;
					}
					i += 2; // skip the comparison token + our assumed closing paren
				}
			} else if (c === ')') {
				if (stack.length > 1) {
					stack.length -= 1;
					topNode = stack[stack.length - 1];
				} else {
					return topNode;
				}
			}
		}

		// don't expect to get here, unless badly formed filter
		return stack.length > 0 ? stack[0] : topNode;
	}

	function parseFilterText(text) {
		var tokens = text ? text.split(kTokenRegExp) : undefined;
		if (!tokens) {
			return;
		}
		return parseTokens(tokens, 0, tokens.length);
	}

	rootNode = parseFilterText(filterText);

	return Object.defineProperties(self, {
		/** The root node, which could be either a comparison node or logic node. */
		rootNode: { value: rootNode },

		walk: { value: walk }
	});
}$FUNCTION$);