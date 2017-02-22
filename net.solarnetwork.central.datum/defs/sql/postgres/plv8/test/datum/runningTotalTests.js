import test from 'ava';
import moment from 'moment';

import parseDatumCSV from './_parseDatumCSV.js'
import runningTotal from '../../src/datum/runningTotal.js'

test('datum:runningTotal:create', t => {
	const service = runningTotal('Foo');
	t.is(service.sourceId, 'Foo');
});

test('datum:runningTotal:processRecords:1', t => {
	const sourceId = 'Foo';
	const service = runningTotal(sourceId);
	t.is(service.sourceId, sourceId);

	const data = parseDatumCSV('running-total-01.csv');

	data.forEach(service.addDatumRecord);

	var aggResult = service.finish();

	t.is(aggResult.source_id, sourceId);

	t.deepEqual(aggResult.jdata.i, {foo:12.16, foo_min:9, foo_max:19});
	t.deepEqual(aggResult.jdata.a, {bar:305});
});
