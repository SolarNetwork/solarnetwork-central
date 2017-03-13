CREATE TABLE IF NOT EXISTS public.plv8_modules (
    module character varying(256) NOT NULL ,
    autoload bool NOT NULL DEFAULT FALSE,
    source text NOT NULL,
    CONSTRAINT plv8_modules_pkey PRIMARY KEY(module)
);
GRANT SELECT ON TABLE public.plv8_modules TO public;

CREATE EXTENSION IF NOT EXISTS plv8 with SCHEMA pg_catalog;

CREATE OR REPLACE FUNCTION public.plv8_startup()
  RETURNS void LANGUAGE plv8 AS
$BODY$
'use strict';

var moduleCache = {};

function load(key, source) {
	var module = {exports: {}};
	eval("(function(module, exports) {" + source + "; })")(module, module.exports);

	// store in cache
	moduleCache[key] = module.exports;
	return module.exports;
}

/**
 * Load a module.
 *
 * Inspired by https://rymc.io/2016/03/22/a-deep-dive-into-plv8/.
 *
 * @param {String} modulePath The path of the module to load. Relative path portions will be stripped
 *                            to form the final module name used to lookup a matching row in the
 *                            <code>plv8_modules</code> table.
 *
 * @returns {Object} The loaded module, or <code>null</code> if not found.
 */
this.require = function(modulePath) {
	var module = modulePath.replace(/\.{1,2}\//g, '');
	var code = moduleCache[module];
	if ( code ) {
		return code;
	}

	plv8.elog(NOTICE, 'Loading plv8 module: ' + module);
	var rows = plv8.execute("SELECT source FROM public.plv8_modules WHERE module = $1 LIMIT 1", [module]);

	if ( rows.length < 1 ) {
		plv8.elog(WARNING, 'Could not load module: ' + module);
		return null;
	}

	return load(module, rows[0].source);
};

/**
 * Release all cached modules.
 */
this.resetRequireCache = function() {
	var prop;
	for ( prop in moduleCache ) {
		delete moduleCache[prop];
	}
};

(function() {
	// Grab modules worth auto-loading at context start and let them cache
    var query = 'SELECT module, source from public.plv8_modules WHERE autoload = true';
    plv8.execute(query).forEach(function(row) {
		plv8.elog(NOTICE, 'Autoloading plv8 module: ' + row.module);
        load(row.module, row.source);
	});
}());
$BODY$;


\cd plv8
\i postgres-init-plv8-modules.sql
