'use strict';

import test from 'ava';

import searchFilter from '../../src/util/searchFilter'

test('util:searchFilter:createEmpty', t => {
	const service = searchFilter();
	t.falsy(service.rootNode);
});

test('util:searchFilter:parseSimple', t => {
	const service = searchFilter('(foo=bar)');
	const root = service.rootNode;
	t.deepEqual(root, {key:'foo', op:'=', val:'bar'});
});

test('util:searchFilter:parseMultiNoRootGroup', t => {
	const service = searchFilter('(foo=bar)(bim=bam)');
	const root = service.rootNode;
	t.deepEqual(root, {key:'foo', op:'=', val:'bar'});
});

test('util:searchFilter:parseSimpleNested', t => {
	const service = searchFilter('(&(foo=bar))');
	const root = service.rootNode;
	t.deepEqual(root, {op:'&',
		children: [{key:'foo', op:'=', val:'bar'}]
	});
});

test('util:searchFilter:parseNested', t => {
	const service = searchFilter('(& (/m/foo=bar) (| (/pm/bam/pop~=whiz) (/pm/boo/boo>0) (!(/pm/bam/ding<=9))))');
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

