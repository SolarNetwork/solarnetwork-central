import test from 'ava';
import fixPrecision from '../../src/math/fixPrecision'

test('math:fixPrecision:string', t => {
	t.is(fixPrecision('foo'), 'foo');
});

test('math:fixPrecision:1', t => {
	const v = fixPrecision(1.123456789, 1);
	t.is(v, 1);
	t.is(v.toString(), '1');
});

test('math:fixPrecision:10', t => {
	const v = fixPrecision(1.123456789, 10);
	t.is(v, 1.1);
	t.is(v.toString(), '1.1');
});

test('math:fixPrecision:10rounded', t => {
	const v = fixPrecision(1.19, 10);
	t.is(v, 1.2);
	t.is(v.toString(), '1.2');
});

test('math:fixPrecision:default', t => {
	const v = fixPrecision(1.123456789);
	t.is(v, 1.123);
	t.is(v.toString(), '1.123');
});

test('math:fixPrecision:nullAmount', t => {
	const v = fixPrecision(1.123456789, null);
	t.is(v, 1.123456789);
	t.is(v.toString(), '1.123456789');
});

test('math:fixPrecision:negative', t => {
	const v = fixPrecision(-1.123456789);
	t.is(v, -1.123);
	t.is(v.toString(), '-1.123');
});

test('math:fixPrecision:negativeRounded', t => {
	const v = fixPrecision(-1.19, 10);
	t.is(v, -1.2);
	t.is(v.toString(), '-1.2');
});

test('math:fixPrecision:half', t => {
	const v = fixPrecision(0.5, 1);
	t.is(v, 1);
	t.is(v.toString(), '1');
});

test('math:fixPrecision:negativeHalf', t => {
	const v = fixPrecision(-0.5, 1);
	t.is(v, -0, 'Math.round() rounds negative halves towards +infinity!');
	t.is(v.toString(), '0');
});

