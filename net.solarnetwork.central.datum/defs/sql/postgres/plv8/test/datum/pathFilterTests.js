'use strict';

import test from 'ava';

import pathFilter from '../../src/datum/pathFilter'

test('datum:pathFilter:createEmpty', t => {
	const service = pathFilter();
	t.falsy(service.rootNode);
});

test('datum:pathFilter:parseSimple', t => {
	const service = pathFilter('(foo=bar)');
	const root = service.rootNode;
	t.truthy(root);
	t.is(root.key, 'foo');
	t.is(root.op, '=');
	t.is(root.val, 'bar');
});

//'(& (/m/foo=bar) (| (/pm/bam/pop=whiz) (/pm/boo/boo=cry) (!(/pm/bam/ding=dong))))'
