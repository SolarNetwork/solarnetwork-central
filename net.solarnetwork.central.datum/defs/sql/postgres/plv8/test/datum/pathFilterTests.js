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
	t.deepEqual(root, {key:'foo', op:'=', val:'bar'});
});

test('datum:pathFilter:parseMultiNoRootGroup', t => {
	const service = pathFilter('(foo=bar)(bim=bam)');
	const root = service.rootNode;
	t.deepEqual(root, {key:'foo', op:'=', val:'bar'});
});

test('datum:pathFilter:parseSimpleNested', t => {
	const service = pathFilter('(&(foo=bar))');
	const root = service.rootNode;
	t.deepEqual(root, {op:'&',
		children: [{key:'foo', op:'=', val:'bar'}]
	});
});

test('datum:pathFilter:parseNested', t => {
	const service = pathFilter('(& (/m/foo=bar) (| (/pm/bam/pop~=whiz) (/pm/boo/boo>0) (!(/pm/bam/ding<=9))))');
	const root = service.rootNode;
	t.deepEqual(root, {op:'&', children:[
		{key:'/m/foo', op:'=', val:'bar'},
		{op:'|', children:[
			{key:'/pm/bam/pop', op:'~=', val:'whiz'},
			{key:'/pm/boo/boo', op:'>', val:'0'},
			{op:'!', children:[
				{key:'/pm/bam/ding', op:'<=', val:'9'},
			]}
		]}
	]});
});

