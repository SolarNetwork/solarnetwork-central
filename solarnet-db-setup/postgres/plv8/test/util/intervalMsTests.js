import test from 'ava';
import sinon from 'sinon';

global.plv8 = {};

import intervalMs from '../../src/util/intervalMs';

test.afterEach.always(t => {
	var prop;
	for ( prop in global.plv8 ) {
		delete global.plv8;
	}
});

test('util:intervalMs:mockCall', t => {
	// stub our plv8 environment; we aren't actually testing against plv8 here
	const execute = sinon.stub();
	execute.withArgs(['01:00:00']).returns([{date_part: 3600}]);
	const prepare = sinon.stub();
	prepare.returns({
		execute : execute,
	});
	global.plv8.prepare = prepare;

	const result = intervalMs('01:00:00');

	t.true(prepare.calledOnce);
	t.is(result, 3600000);
});

