'use strict';

const kOpAnd = '&';
const kOpOr = '|';
const kOpNot = '!';

const kOpEqual = '=';
const kOpApprox = '~=';
const kOpLT = '<';
const kOpLTE = '<=';
const kOpGT = '>';
const kOpGTE = '>=';

// RegExp to split a string into filter tokens
const kTokenRegExp = /\s*(\([&|!]|\(|\))\s*/g;

// RegExp to match key, op, val of a comparison token
const kCompRegExp = /(.+?)(=|~=|<=?|>=?)(.+)/;

function isLogicOp(text) {
	return (text === kOpAnd || text === kOpOr || text === kOpNot);
}

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
		op		: { value : op },
	});
}

function compNode(text) {
	var self = {};

	var key,
		op,
		val;

	(function() {
		var match = (text ? text.match(kCompRegExp) : undefined);
		if ( match ) {
			key = match[1];
			op = match[2];
			val = match[3];
		}
	}());

	return Object.defineProperties(self, {
		key		: { value : key },
		op		: { value : op },
		val		: { value : val },
	});
}

export default function pathFilter(filterText) {
	var self = {
		version : '1'
	};

	var rootNode;

	function parseTokens(tokens, start, end) {
		var i,
			c,
			topNode,
			node,
			tok,
			stack = [];
		for ( i = start; i < end; i += 1 ) {
			tok = tokens[i];
			if ( tok.length < 1 ) {
				continue;
			}
			c = tok.charAt(0);
			if ( c === '(' ) {
				// starting new item
				if ( tok.length > 1 ) {
					// starting new logical group
					c = tok.charAt(1);
					node = logicNode(c);
					if ( topNode ) {
						topNode.addChild(node);
					}
					stack.push(node);
					topNode = node;
				} else {
					// starting a key/value pair
					if ( i + 1 < end ) {
						node = compNode(tokens[i+1]);
					}
					if ( topNode ) {
						topNode.addChild(node);
					} else {
						// our top node is not a group node, so only one node is possible and we can return now
						return node;
					}
					i += 2; // skip the comparison token + our assumed closing paren
				}
			} else if ( c === ')' ) {
				if ( stack.length > 1 ) {
					stack.length -= 1;
					topNode = stack[stack.length - 1];
				} else {
					return topNode;
				}
			}
		}

		// don't expect to get here, unless badly formed filter
		return (stack.length > 0 ? stack[0] : topNode);
	}

	function parseFilterText(text) {
		var tokens = (text ? text.split(kTokenRegExp) : undefined);

		if ( !tokens ) {
			return;
		}

		return parseTokens(tokens, 0, tokens.length);
	}

	rootNode = parseFilterText(filterText);

	return Object.defineProperties(self, {
		rootNode	: { value : rootNode },
	});
}
