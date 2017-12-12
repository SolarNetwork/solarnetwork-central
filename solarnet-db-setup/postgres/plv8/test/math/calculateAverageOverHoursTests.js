import test from 'ava';
import calculateAverageOverHours from '../../src/math/calculateAverageOverHours'

test('math:calculateAverageOverHours:zeroMs', t => {
	t.is(calculateAverageOverHours(1, 2, 0), 0);
});

test('math:calculateAverageOverHours:undefinedMs', t => {
	t.is(calculateAverageOverHours(1, 2), 0);
});

test('math:calculateAverageOverHours:hour', t => {
	t.is(calculateAverageOverHours(1, 2, 3600000), 1.5);
});

test('math:calculateAverageOverHours:hourNoDiff', t => {
	t.is(calculateAverageOverHours(1, 1, 3600000), 1);
});

test('math:calculateAverageOverHours:hourZero', t => {
	t.is(calculateAverageOverHours(1, -1, 3600000), 0);
});

test('math:calculateAverageOverHours:30min', t => {
	t.is(calculateAverageOverHours(1, 2, 1800000), 0.75);
});

test('math:calculateAverageOverHours:39minZero', t => {
	t.is(calculateAverageOverHours(1, -1, 1800000), 0);
});

