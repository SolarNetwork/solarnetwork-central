'use strict';

import test from 'ava';
import moment from 'moment';

import parseDatumCSV from './_parseDatumCSV.js'

import aggregator from '../../src/datum/aggregator'

test('datum:aggregator:createWithoutConfig', t => {
	const now = new Date().getTime();
	const service = aggregator();
	t.true(service.startTs >= now);
	t.true(service.endTs >= now);
});

test('datum:aggregator:createWithEmptyConfig', t => {
	const now = new Date().getTime();
	const service = aggregator({});
	t.true(service.startTs >= now);
	t.true(service.endTs >= now);
});

test('datum:aggregator:createWithStartTs', t => {
	const service = aggregator({ startTs : 456 });
	t.is(service.startTs, 456);
});

test('datum:aggregator:createWithEndTs', t => {
	const service = aggregator({ endTs : 321 });
	t.is(service.endTs, 321);
});

test('datum:aggregator:processRecords:1h', t => {
	const start = moment('2016-10-10 10:00:00+13');
	const end = start.clone().add(1, 'hour');
	const service = aggregator({
		startTs : start.valueOf(),
		endTs : end.valueOf(),
	});

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-01.csv');

	data.forEach(rec => {
		delete rec.ts_start; // not provided for single aggregate span
		service.addDatumRecord(rec);
	});

	var aggResults = service.finish();

	var expected = [
		{ i: {foo:5.333, foo_min:1, foo_max:11}, a: {bar:105}},
	];

	t.is(aggResults.length, expected.length, 'there is one aggregate result');

	aggResults.forEach((aggResult, i) => {
		t.is(aggResult.source_id, 'Foo');
		t.is(aggResult.ts_start.getTime(), start.valueOf(), 'start time');
		t.deepEqual(aggResult.jdata.i, expected[i].i, 'instantaneous');
		t.deepEqual(aggResult.jdata.a, expected[i].a, 'accumulating');
	});

});

test('datum:aggregator:processRecords:onlyAdjacentRows', t => {
	const start = moment('2016-10-10 12:00:00+13');
	const end = start.clone().add(1, 'hour');
	const service = aggregator({
		startTs : start.valueOf(),
		endTs : end.valueOf(),
	});

	const data = parseDatumCSV('find-datum-for-minute-time-slots-07.csv');

	var aggResults = [];
	data.forEach(rec => {
		delete rec.ts_start; // not provided for single aggregate span
		var aggResult = service.addDatumRecord(rec);
		if ( aggResult ) {
			aggResults.push(aggResult);
		}
	});
	aggResults = aggResults.concat(service.finish());

	var expected = [
	];

	t.deepEqual(aggResults, expected);
});

test('datum:aggregator:processRecords:noTrailing', t => {
	const start = moment('2016-10-10 11:00:00+13');
	const end = start.clone().add(1, 'hour');
	const service = aggregator({
		startTs : start.valueOf(),
		endTs : end.valueOf(),
	});

	const data = parseDatumCSV('find-datum-for-minute-time-slots-08.csv');

	var aggResults = [];
	data.forEach(rec => {
		delete rec.ts_start; // not provided for single aggregate span
		var aggResult = service.addDatumRecord(rec);
		if ( aggResult ) {
			aggResults.push(aggResult);
		}
	});
	aggResults = aggResults.concat(service.finish());

	var expected = [
		{ i: {foo:17, foo_min:13, foo_max:21}, a: {bar:25}},
	];

	aggResults.forEach((aggResult, i) => {
		t.is(aggResult.source_id, 'Foo');
		t.is(aggResult.ts_start.getTime(), start.valueOf(), 'start time');
		t.deepEqual(aggResult.jdata.i, expected[i].i, 'instantaneous');
		t.deepEqual(aggResult.jdata.a, expected[i].a, 'accumulating');
	});
});


test('datum:aggregator:processRecords:sOnly', t => {
	const start = moment('2017-01-01 10:00:00+13');
	const end = start.clone().add(1, 'hour');
	const service = aggregator({
		startTs : start.valueOf(),
		endTs : end.valueOf(),
	});

	const data = parseDatumCSV('find-datum-for-minute-time-slots-13.csv');

	var aggResults = [];
	data.forEach(rec => {
		delete rec.ts_start; // not provided for single aggregate span
		var aggResult = service.addDatumRecord(rec);
		if ( aggResult ) {
			aggResults.push(aggResult);
		}
	});
	aggResults = aggResults.concat(service.finish());

	var expected = [
		{ ts_start: moment('2017-01-01 10:00:00+13').toDate(), source_id: 'level',
			jdata: {s: {val:555}}},
		{ ts_start: moment('2017-01-01 10:00:00+13').toDate(), source_id: 'percent',
			jdata: {s: {val:3330}}},
	];

	t.deepEqual(aggResults, expected);
});

test('datum:aggregator:processRecords:accumulatingPreferredOverHourFill', t => {
	const start = moment('2017-01-01 10:00:00+13');
	const end = start.clone().add(1, 'hour');
	const service = aggregator({
		startTs : start.valueOf(),
		endTs : end.valueOf(),
	});

	const data = parseDatumCSV('find-datum-for-minute-time-slots-15.csv');

	var aggResults = [];
	data.forEach(rec => {
		delete rec.ts_start; // not provided for single aggregate span
		var aggResult = service.addDatumRecord(rec);
		if ( aggResult ) {
			aggResults.push(aggResult);
		}
	});
	aggResults = aggResults.concat(service.finish());

	var expected = [
		{ ts_start: start.toDate(), source_id: 'Main',
			jdata: {i:{watts:12}, a: {wattHours:10}}},
	];

	t.deepEqual(aggResults, expected);
});
