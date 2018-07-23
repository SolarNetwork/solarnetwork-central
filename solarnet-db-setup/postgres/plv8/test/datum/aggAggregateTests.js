import test from 'ava';
import moment from 'moment';

import parseDatumCSV from './_parseDatumCSV.js'
import aggAggregate from '../../src/datum/aggAggregate.js'

test('datum:aggAggregate:create', t => {
	const service = aggAggregate('Foo', 123);
	t.is(service.sourceId, 'Foo');
	t.is(service.ts, 123);
});

test('datum:aggAggregate:15mAggToHour', t => {
	const start = moment('2014-03-01 08:00:00+13');
	const sourceId = 'Foo';
	const service = aggAggregate(sourceId, start.valueOf());
	t.is(service.sourceId, sourceId);
	t.is(service.ts, start.valueOf());

	const data = parseDatumCSV('/agg-datum-m-01.csv');

	data.forEach(rec => {
		service.addDatumRecord(rec);
	});
	const aggResult = service.finish();

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), start.valueOf());

	t.deepEqual(aggResult.jdata.i, {watts:300.708, watts_min:225, watts_max:1014});
	t.deepEqual(aggResult.jmeta.i, {watts:{count:24, min:225, max:1014}});
	t.deepEqual(aggResult.jdata.a, {wattHours:325.408});
});

test('datum:aggAggregate:1hAggToDay', t => {
	const start = moment('2014-03-01 08:00:00+13');
	const sourceId = 'Foo';
	const service = aggAggregate(sourceId, start.valueOf());
	t.is(service.sourceId, sourceId);
	t.is(service.ts, start.valueOf());

	const data = parseDatumCSV('/agg-datum-h-01.csv');

	data.forEach(rec => {
		service.addDatumRecord(rec);
	});
	const aggResult = service.finish();

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), start.valueOf());

	t.deepEqual(aggResult.jdata.i, {watts:326.88, watts_min:225, watts_max:1014});
	t.deepEqual(aggResult.jmeta.i, {watts:{count:50, min:225, max:1014}});
	t.deepEqual(aggResult.jdata.a, {wattHours:1056.252});
});

test('datum:aggAggregate:1dAggToMonth', t => {
	const start = moment('2014-03-01 00:00:00+13');
	const sourceId = 'Foo';
	const service = aggAggregate(sourceId, start.valueOf());
	t.is(service.sourceId, sourceId);
	t.is(service.ts, start.valueOf());

	const data = parseDatumCSV('/agg-datum-d-01.csv');

	data.forEach(rec => {
		service.addDatumRecord(rec);
	});
	const aggResult = service.finish();

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), start.valueOf());

	t.deepEqual(aggResult.jdata.i, {watts:678.88, watts_min:144, watts_max:4599});
	t.deepEqual(aggResult.jmeta.i, {watts:{count:4753, min:144, max:4599}});
	t.deepEqual(aggResult.jdata.a, {wattHours:48292.963});
});
