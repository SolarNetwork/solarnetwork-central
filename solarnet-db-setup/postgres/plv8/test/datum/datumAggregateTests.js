import test from 'ava';
import moment from 'moment';

import parseDatumCSV from './_parseDatumCSV.js'
import datumAggregate from '../../src/datum/datumAggregate.js'

test('datum:datumAggregate:create', t => {
	const service = datumAggregate('Foo', 123, 234);
	t.is(service.sourceId, 'Foo');
	t.is(service.ts, 123);
	t.is(service.endTs, 234);
	t.is(service.toleranceMs, 3600000);
	t.deepEqual(service.hourFill, {watts: 'wattHours'});
});

test('datum:datumAggregate:createWithTolerance', t => {
	const service = datumAggregate('Foo', 123, 234, {toleranceMs:345});
	t.is(service.sourceId, 'Foo');
	t.is(service.ts, 123);
	t.is(service.endTs, 234);
	t.is(service.toleranceMs, 345);
});

test('datum:datumAggregate:createWithHourFill', t => {
	const hf = {foo: 'bar'};
	const service = datumAggregate('Foo', 123, 234, {hourFill:hf});
	t.is(service.sourceId, 'Foo');
	t.is(service.ts, 123);
	t.is(service.endTs, 234);
	t.is(service.toleranceMs, 3600000);
	t.deepEqual(service.hourFill, hf);
});

test('datum:datumAggregate:processRecords:15m:1', t => {
	const slotTs = 1476046800000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs, {hourFill:{foo:'fooHours'}});
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-01.csv').slice(0, 5);

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < endTs ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:2, foo_min:1, foo_max:3});
	t.deepEqual(aggResult.jmeta.i, {foo:{count:3, min:1, max:3}});
	t.deepEqual(aggResult.jdata.a, {bar:25, fooHours:0.625});
});

test('datum:datumAggregate:processRecords:15m:trailingFraction', t => {
	const slotTs = 1476050400000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs);
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-02.csv');

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < endTs ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:15, foo_min:13, foo_max:17});
	t.deepEqual(aggResult.jmeta.i, {foo:{count:3, min:13, max:17}});
	t.deepEqual(aggResult.jdata.a, {bar:16.667}, '2/3 of last record\'s accumulation counts towards this result');

	// verify call to startNext()
	var next = service.startNext(endTs, endTs + (15 * 60 * 1000));
	t.is(next.sourceId, sourceId);
	aggResult = next.finish();
	t.deepEqual(aggResult.jdata.i, {foo:19});
	t.deepEqual(aggResult.jdata.a, {bar:3.333}, '1/3 of previous record\'s accumulation counts towards next result');
});

test('datum:datumAggregate:processRecords:15m:trailingPastTolerance', t => {
	const slotTs = 1476050400000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs);
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-02a.csv');

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < endTs ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:15, foo_min:13, foo_max:17});
	t.deepEqual(aggResult.jmeta.i, {foo:{count:3, min:13, max:17}});
	t.deepEqual(aggResult.jdata.a, {bar:10}, 'last record past tolerance so does not contribute');

	// verify call to startNext()
	var nextTs = moment('2016-10-10 13:15:00+13').toDate().getTime();
	var next = service.startNext(nextTs, nextTs + (15 * 60 * 1000));
	t.is(next.sourceId, sourceId);
	aggResult = next.finish();
	t.deepEqual(aggResult.jdata.i, {foo:19});
	t.truthy(aggResult.jdata.a === undefined, 'no previous record to accumulate from');
});

test('datum:datumAggregate:processRecords:15m:leadingFraction', t => {
	const slotTs = 1476050400000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs);
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-03.csv');

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < endTs ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:15, foo_min:13, foo_max:17});
	t.deepEqual(aggResult.jdata.a, {bar:21.667}, '1/6 of first record\'s accumulation counts towards this result');
});

test('datum:datumAggregate:processRecords:15m:leadingAndTrailingFractions', t => {
	const slotTs = 1476050400000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs);
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-04.csv');

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < endTs ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:15, foo_min:13, foo_max:17});
	t.deepEqual(aggResult.jmeta.i, {foo:{count:3, min:13, max:17}});
	t.deepEqual(aggResult.jdata.a, {bar:18.333}, '1/6 of first; 2/3 of last record\'s accumulation counts towards this result');

	// verify call to startNext()
	var next = service.startNext(endTs, endTs + (15 * 60 * 1000));
	t.is(next.sourceId, sourceId);
	aggResult = next.finish();
	t.deepEqual(aggResult.jdata.i, {foo:19});
	t.deepEqual(aggResult.jdata.a, {bar:3.333}, '1/3 of previous record\'s accumulation counts towards next result');
});

test('datum:datumAggregate:processRecords:15m:endWithinSlot', t => {
	const slotTs = 1476050400000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs);
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-05.csv');

	var aggResult;
	data.forEach(rec => {
		service.addDatumRecord(rec);
	});

	// note call to finish() here, as data ended in middle of slot
	aggResult = service.finish();


	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:15, foo_min:13, foo_max:17});
	t.deepEqual(aggResult.jmeta.i, {foo:{count:3, min:13, max:17}});
	t.deepEqual(aggResult.jdata.a, {bar:10});
});

test('datum:datumAggregate:processRecords:15m:noPreviousSlot', t => {
	const slotTs = 1476050400000;
	const endTs = slotTs + (15 * 60 * 1000);
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, slotTs, endTs);
	t.is(service.sourceId, sourceId);
	t.is(service.ts, slotTs);
	t.is(service.endTs, endTs);

	const data = parseDatumCSV('/find-datum-for-minute-time-slots-06.csv');

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < endTs ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	t.is(aggResult.source_id, sourceId);
	t.is(aggResult.ts_start.getTime(), slotTs);

	t.deepEqual(aggResult.jdata.i, {foo:15, foo_min:13, foo_max:17});
	t.deepEqual(aggResult.jmeta.i, {foo:{count:3, min:13, max:17}});
	t.deepEqual(aggResult.jdata.a, {bar:20});
});

test('datum:datumAggregate:processRecords:15m:roundedStats', t => {
	const start = moment('2016-10-10 11:00:00+13');
	const end = start.clone().add(15, 'minutes');
	const sourceId = 'Foo';
	const service = datumAggregate(sourceId, start.valueOf(), end.valueOf());
	t.is(service.sourceId, sourceId);
	t.is(service.ts, start.valueOf());
	t.is(service.endTs, end.valueOf());

	const data = parseDatumCSV('find-datum-for-minute-time-slots-14.csv');

	var aggResult;
	data.forEach(rec => {
		if ( rec.ts_start.getTime() < end.valueOf() ) {
			service.addDatumRecord(rec);
		} else {
			aggResult = service.finish(rec);
		}
	});

	var expected =
		{ ts_start: start.toDate(), source_id: sourceId,
			jdata: {i: {foo:15.515, foo_min:13.123, foo_max:17.432}},
			jmeta: {i: {foo:{count:3, min:13.123, max:17.432}}}
		};

	t.deepEqual(aggResult, expected);
});
