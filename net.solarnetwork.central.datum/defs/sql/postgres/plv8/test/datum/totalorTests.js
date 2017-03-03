'use strict';

import test from 'ava';
import moment from 'moment';

import parseDatumCSV from './_parseDatumCSV.js'

import totalor from '../../src/datum/totalor'

test('datum:aggregator:createWithoutConfig', t => {
	const service = totalor();
	t.truthy(service);
});

test('datum:totalor:processRecords:1h', t => {
	const service = totalor();

	const data = parseDatumCSV('running-total-01.csv');

	data.forEach(service.addDatumRecord);

	var aggResults = service.finish();

	var expected = [
		{ i: {foo:12.16, foo_min:9, foo_max:19}, a: {bar:305}},
	];

	t.is(aggResults.length, expected.length, 'expected total result count');

	aggResults.forEach((aggResult, i) => {
		t.is(aggResult.source_id, 'Foo');
		t.deepEqual(aggResult.jdata.i, expected[i].i, 'instantaneous');
		t.deepEqual(aggResult.jdata.a, expected[i].a, 'accumulating');
	});
});
