'use strict';

var argv = require('yargs')
	.usage('Usage: $0 [glob]...')
	.example('$0 src/**/*.js', 'process all js files in a dir named src')
	.default('srcRoot', './src')
	.default('outRoot', './dist')
	.default('table', 'public.plv8_modules')
	.alias('s', 'srcRoot')
	.alias('o', 'outRoot')
	.alias('t', 'table')
	.count('verbose')
    .alias('v', 'verbose')
	.demandCommand(1)
	.argv;

var glob = require('glob');
var fs = require('fs');
var babel = require('babel-core');

function WARN()  { argv.verbose >= 0 && console.log.apply(console, arguments); }
function INFO()  { argv.verbose >= 1 && console.log.apply(console, arguments); }
function DEBUG() { argv.verbose >= 2 && console.log.apply(console, arguments); }

var sourceRoot = argv.srcRoot;
var outRoot = argv.outRoot;
var moduleTableName = argv.table;

var outputPathRegex = new RegExp('^'+sourceRoot.replace(/\.{1,2}\//g, '') +'/');
var localRequireRegex = /require\(\'\.\//g;

function outputPath(path) {
	var outName = path.replace(outputPathRegex, '');
	outName = outName.substring(0, outName.lastIndexOf('.'));

	return {
		path: outRoot +'/' +outName +'.sql',
		dir: outRoot +'/' +outName.substring(0, outName.lastIndexOf('/')),
		module : outName,
		pkg : (outName.lastIndexOf('/') > 0 ? outName.substring(0, outName.lastIndexOf('/') + 1) : '')
	};
}

function processFile(path) {
	DEBUG('Processing file: ' +path);
	var code = babel.transformFileSync(path, {
		presets: [ ['es2015', { 'modules': 'commonjs' }] ],
		plugins: [
			['module-resolver', { root: [ sourceRoot ] } ]
		]
	}).code;
	var out = outputPath(path);
	if ( !fs.existsSync(out.dir) ) {
		fs.mkdirSync(out.dir);
	}
	INFO(path, '=>', out.path);

	// translate any require('./foo') into full package e.g. require('util/foo')
	code = code.replace(localRequireRegex, 'require(\'' +out.pkg);

	DEBUG(code);

	code = 'DELETE FROM ' +moduleTableName +' WHERE module = \'' +out.module +'\';\n'
		+'INSERT INTO ' +moduleTableName +' (module, autoload, source) VALUES (\'' +out.module +'\', FALSE,\n$FUNCTION$'
		+code +'$FUNCTION$);';

	fs.writeFileSync(out.path, code, { encoding: 'utf8' });
	return out;
}

function generateLoadScript(generated) {
	var outPath = 'postgres-init-plv8-modules.sql';
	var sql = '';
	if ( generated ) {
		generated.forEach(function(gen) {
			sql += '\\i ' +outRoot +'/' +gen.module +'.sql\n'
		});
		fs.writeFileSync(outPath, sql, { encoding: 'utf8' });
		INFO('Generated SQL load script ', outPath);
	} else {
		fs.unlinkSync(outPath);
	}
}

var generated = [];
argv._.forEach(function(arg) {
	var matches = glob.sync(arg);
	matches.forEach(function(path) {
		var out = processFile(path);
		if ( out ) {
			generated.push(out);
		}
	});
});
generateLoadScript(generated);
