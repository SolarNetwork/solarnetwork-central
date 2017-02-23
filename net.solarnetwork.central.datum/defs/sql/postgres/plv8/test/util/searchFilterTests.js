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

test('util:searchFilter:parseJunk', t => {
	const service = searchFilter('Hey! This is junk.');
	const root = service.rootNode;
	t.falsy(root);
});

test('util:searchFilter:nefarious', t => {
	const service = searchFilter('Hey! This is junk. (Or is it?)');
	const root = service.rootNode;
	t.falsy(root);
});

test('util:searchFilter:simpleMissingEnd', t => {
	const service = searchFilter('(foo=bar');
	const root = service.rootNode;
	t.deepEqual(root, {key:'foo', op:'=', val:'bar'});
});

test('util:searchFilter:complexMissingEnd', t => {
	const service = searchFilter('(&(foo=bar)(bim=bam)');
	const root = service.rootNode;
	t.deepEqual(root, {op:'&', children:[
		{key:'foo', op:'=', val:'bar'},
		{key:'bim', op:'=', val:'bam'},
	]});
});

test('util:searchFilter:emptyComplex', t => {
	const service = searchFilter('(&)');
	const root = service.rootNode;
	t.deepEqual(root, {op:'&', children:[]});
});

test('util:searchFilter:simpleNestedMissingEnds', t => {
	const service = searchFilter('(&(foo=bar');
	const root = service.rootNode;
	t.deepEqual(root, {op:'&', children:[
		{key:'foo', op:'=', val:'bar'},
	]});
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

test('util:searchFilter:walkSimple', t => {
	const service = searchFilter('(foo=bar)');
	var nodes = [];
	service.walk(function(err, node) {
		if ( node ) {
			nodes.push(node);
		}
	});
	t.deepEqual(nodes, [{key:'foo', op:'=', val:'bar'}]);
});

test('util:searchFilter:walkComplex', t => {
	const service = searchFilter('(& (/m/foo=bar) (| (/pm/bam/pop~=whiz) (/pm/boo/boo>0) (!(/pm/bam/ding<=9))))');
	var nodes = [];
	service.walk(function(err, node) {
		if ( node ) {
			if ( node.children ) {
				nodes.push({op:node.op});
			} else {
				nodes.push(node);
			}
		}
	});
	t.deepEqual(nodes, [
		{op:'&'},
		{key:'/m/foo', op:'=', val:'bar'},
		{op:'|'},
		{key:'/pm/bam/pop', op:'~=', val:'whiz'},
		{key:'/pm/boo/boo', op:'>', val:'0'},
		{op:'!'},
		{key:'/pm/bam/ding', op:'<=', val:'9'},
	]);
});

test('util:searchFilter:walkAbortEarly', t => {
	const service = searchFilter('(&(foo=bar)(bim=bam)');
	var nodes = [];
	service.walk(function(err, node) {
		if ( node ) {
			if ( node.children ) {
				nodes.push({op:node.op});
			} else {
				nodes.push(node);
				return false
			}
		}
	});
	t.deepEqual(nodes, [
		{op:'&'},
		{key:'foo', op:'=', val:'bar'},
	]);
});

