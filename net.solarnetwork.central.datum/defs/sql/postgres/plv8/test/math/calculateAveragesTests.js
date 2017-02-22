import test from 'ava';
import calculateAverages from '../../src/math/calculateAverages'

test('math:calculateAverages:typical', t => {
	const result = calculateAverages({
		foo : 55,
		bar : 12,
	}, {
		foo : 4,
		bar : 4,
	});
	t.deepEqual(result, { foo : 13.75, bar : 3 });
});

test('math:calculateAverages:noCounts', t => {
	const values = { foo : 55, bar : 12 };
	const result = calculateAverages(values);
	t.is(result, values, 'Without counts, the input object is returned.');
});

test('math:calculateAverages:missingCount', t => {
	const values = { foo : 55, bar : 12 };
	const result = calculateAverages(values, { foo : 4 });
	t.deepEqual(result, { foo : 13.75 }, 'With missing count, the input value is omitted.');
});

test('math:calculateAverages:zeroCount', t => {
	const values = { foo : 55 };
	const result = calculateAverages(values, { foo : 0 });
	t.deepEqual(result, {}, 'With zero count, the input value is omitted.');
});

test('math:calculateAverages:negativeCount', t => {
	const values = { foo : 55 };
	const result = calculateAverages(values, { foo : -1 });
	t.deepEqual(result, {}, 'With negative count, the input value is omitted.');
});

test('math:calculateAverages:nanCount', t => {
	const values = { foo : 55 };
	const result = calculateAverages(values, { foo : 'one' });
	t.deepEqual(result, {}, 'With NaN count, the input value is omitted.');
});
