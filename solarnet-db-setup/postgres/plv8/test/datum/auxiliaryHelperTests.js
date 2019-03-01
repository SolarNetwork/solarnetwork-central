import test from 'ava';

import auxiliaryHelper from '../../src/datum/auxiliaryHelper.js'

test('datum:auxiliaryHelper:create', t => {
	const helper = auxiliaryHelper();
	t.is(helper.resetTag, 'Reset');
	t.is(helper.resetFinalTag, 'final');
	t.is(helper.resetStartTag, 'start');
});

test('datum:auxiliaryHelper:datumResetKind:undefined', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind(), null);
});

test('datum:auxiliaryHelper:datumResetKind:empty', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({}), null);
});

test('datum:auxiliaryHelper:datumResetKind:tagsNotArray', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:'foo'}), null);
});

test('datum:auxiliaryHelper:datumResetKind:tagsEmpty', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:[]}), null);
});

test('datum:auxiliaryHelper:datumResetKind:tagsOnlyReset', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:['Reset']}), null);
});

test('datum:auxiliaryHelper:datumResetKind:tagsResetButNoStartOrFinal', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:['Reset','foo','bar']}), null);
});

test('datum:auxiliaryHelper:datumResetKind:tagsResetAndFinal', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:['Reset','final']}), 'final');
});

test('datum:auxiliaryHelper:datumResetKind:tagsResetAndStart', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:['Reset','start']}), 'start');
});

test('datum:auxiliaryHelper:datumResetKind:tagsResetAndStartAndFinal', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:['Reset','start','final']}), 'final', 'final has precidence over start');
});

test('datum:auxiliaryHelper:datumResetKind:tagsResetAndStartAndOthers', t => {
	const helper = auxiliaryHelper();
	t.is(helper.datumResetKind({t:['foo','bar','Reset','start']}), 'start');
});
