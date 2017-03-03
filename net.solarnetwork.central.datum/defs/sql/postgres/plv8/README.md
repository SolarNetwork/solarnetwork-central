# SolarNet plv8 scripts

This is a [NPM][npm] project for developing, testing, and generating various JavaScript
functions for use in the SolarNet Postgres database via the [plv8][plv8] extension. At
the time of this writing SolarNet is using plv8 version *1.4.x* with [v8][v8] version
*3.18.x*.

# Module Support

This project is structured so that during development ES6 modules are written. These
modules are then transpiled to ES5 JavaScript modules using the CommonJS (Node) style.
The CommonJS modules are then loaded into a special <code>plv8_modules</code> table
so that imported modules can be loaded at runtime.

For example, here's a simple ES6 module, `myModule.js`:

```JavaScript
export default function myModule() {
	return 'Hello, world.';
}
```

Here's a Postgres statement that could then use that module:

```sql
=> DO $$ plv8.elog(NOTICE, require('myModule').default()); $$ LANGUAGE plv8;
NOTICE:  Hello, world.
```

# Load modules into database

To load all modules into the database (replacing duplicates that may exist), run the
[postgres-init-plv8-modules.sql](postgres-init-plv8-modules.sql) file in `psql`,
for example:

	psql -U user -d dbname -f postgres-init-plv8-modules.sql

# Build modules

All modules are located in the `src` directory, and the build process generates SQL
files in the `dist` directory. If you have not already done so once, you must initialize
the node environment with

	npm install

Then to build all modules from the `src` directory, run

	npm run build

You can run the build with verbose output to see what gets generated like

	npm run build -- --verbose

# Testing

Unit tests for the modules are located in the `test` directory. The project
is using [ava][ava] as the test framework, so all tests are written using ES6
modules. To run all tests you'd do this:

	npm run test

To see more verbose output (e.g. each test that ran) you can do this:

	npm run test -- --verbose

To debug a single test file (and you have Chrome installed) you can
use the [inspect][inspect-process] command. First install the required package:

	npm install --global inspect-process

Then, launch like this:

	inspect --debug-brk ./node_modules/ava/profile.js test/myModuleTests.js


 [ava]: https://github.com/avajs/ava
 [npm]: https://www.npmjs.com/
 [plv8]: https://github.com/plv8/plv8
 [v8]: https://en.wikipedia.org/wiki/V8_(JavaScript_engine)
 [inspect-process]: https://github.com/jaridmargolin/inspect-process

