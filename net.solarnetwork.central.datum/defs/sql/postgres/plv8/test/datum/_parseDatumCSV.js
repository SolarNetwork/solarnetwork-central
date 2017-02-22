import csvParse from 'csv-parse/lib/sync';
import fs from 'fs';
import moment from 'moment';

export default function parseDatumCSV(filename) {
	var data = fs.readFileSync(__dirname+'/'+filename, { encoding : 'utf8' });
	var records = csvParse(data, {
			auto_parse : true,
			columns : true,
			comment : '#',
			skip_empty_lines : true,
		});
	var i, record;
	for ( i = 0; i < records.length; i+= 1 ) {
		record = records[i];
		// convert ts into actual Date object
		if ( record.ts ) {
			record.ts = moment(record.ts).toDate();
		}
		// convert ts_start into actual Date object
		if ( record.ts_start ) {
			record.ts_start = moment(record.ts_start).toDate();
		}
		// convert jdata into JSON object
		if ( record.jdata ) {
			record.jdata = JSON.parse(record.jdata);
		}
	}
	return records;
}
