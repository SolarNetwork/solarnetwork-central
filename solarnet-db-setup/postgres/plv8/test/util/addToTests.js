import test from 'ava';
import addTo from '../../src/util/addTo'

test('util:addTo:simple', t => {
	const d = {foo:1};
	addTo('foo', 123 ,d);
	t.is(d.foo, 124);
});

test('util:addTo:subtract', t => {
	const d = {foo:1};
	addTo('foo', -123 ,d);
	t.is(d.foo, -122);
});

test('util:addTo:missingProp', t => {
	const d = {};
	addTo('foo', 123 ,d);
	t.is(d.foo, 123, 'missing prop created');
});

test('util:addTo:percent', t => {
	const d = {foo:0};
	addTo('foo', 100 ,d, 0.2);
	t.is(d.foo, 20);
});

test('util:addTo:zeroPercent', t => {
	const d = {foo:10};
	addTo('foo', 50 ,d, 0);
	t.is(d.foo, 10);
});

test('util:addTo:count', t => {
	const d = {foo:10};
	const c = {foo:2};
	addTo('foo', 50 ,d, 1, c);
	t.is(d.foo, 60);
	t.is(c.foo, 3, 'count incremented by 1');
});

test('util:addTo:countDefined', t => {
	const d = {foo:-5};
	const c = {};
	addTo('foo', 8 ,d, 1, c);
	t.is(d.foo, 3);
	t.is(c.foo, 1, 'count starts at 1');
});

test('util:addTo:stats', t => {
	const d = {};
	const c = {};
	const s = {};
	addTo('foo', 7 ,d, 1, c, s);
	t.is(d.foo, 7);
	t.is(c.foo, 1);
	t.deepEqual(s, {foo: {min:7, max:7}});
});

test('util:addTo:statsNewMax', t => {
	const d = {foo:7};
	const c = {foo:1};
	const s = {foo: {min:7, max:7}};
	addTo('foo', 10 ,d, 1, c, s);
	t.is(d.foo, 17);
	t.is(c.foo, 2);
	t.deepEqual(s, {foo: {min:7, max:10}});
});

test('util:addTo:statsNewMin', t => {
	const d = {foo:7};
	const c = {foo:1};
	const s = {foo: {min:7, max:7}};
	addTo('foo', 2 ,d, 1, c, s);
	t.is(d.foo, 9);
	t.is(c.foo, 2);
	t.deepEqual(s, {foo: {min:2, max:7}});
});

test('util:addTo:statsUnchanged', t => {
	const d = {foo:7};
	const c = {foo:1};
	const s = {foo: {min:7, max:7}};
	addTo('foo', 7 ,d, 1, c, s);
	t.is(d.foo, 14);
	t.is(c.foo, 2);
	t.deepEqual(s, {foo: {min:7, max:7}});
});

