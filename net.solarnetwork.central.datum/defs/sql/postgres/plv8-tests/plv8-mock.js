'use strict';
var plv8 = plv8js();
var NOTICE = 'NOTICE',
	DEBUG = 'DEBUG';
function plv8js() {
	var that = {};

	var nextData = [];
	var resultData = [];
	
	that.elog = function() {
		if ( !arguments.length ) {
			return;
		}
		var msg = Array.prototype.slice.call(arguments).join(' ');
		console.log(msg);
	};
	
	that.return_next = function(obj) {
		resultData.push(obj);
	};
	
	that.prepare = function(sql) {
		var stmt = {};
		var free = false;
		resultData.length = 0;
		
		that.elog(DEBUG, 'Preparing SQL:', sql);
	
		stmt.cursor = function(args) {
			var crsr = {},
				data = nextData.concat([]),
				pos = -1,
				placeholders = [],
				open = true;
			if ( Array.isArray(args) ) {
				placeholders = args;
				that.elog(DEBUG, 'Applying SQL parameters:', args);
			}
		
			crsr.fetch = function() {
				pos += 1;
				return (open === true && pos < data.length ? data[pos] : null);
			};
			
			crsr.close = function() {
				open = false;
			};
		
			return crsr;
		};
		
		stmt.free = function() {
			free = true;
		};
	
		return stmt;
	};
	
	Object.defineProperties(that, {
		data : { 
			enumerable : true, 
			get : function() { return nextData; },
			set : function(obj) { nextData = (Array.isArray(obj) ? obj : []); }
		},
		resultData : {
			enumerable : true,
			value : resultData
		}
	});
	
	return that;
}
