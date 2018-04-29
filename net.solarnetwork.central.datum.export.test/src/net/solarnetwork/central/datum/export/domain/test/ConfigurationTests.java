/* ==================================================================
 * ConfigurationTests.java - 25/04/2018 7:03:57 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.datum.export.domain.test;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicOutputConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatService;

/**
 * Test cases for the {@link Configuration} interface.
 * 
 * @author matt
 * @version 1.0
 */
public class ConfigurationTests {

	private static final DateTime TEST_DATE = new DateTime(2018, 4, 23, 7, 5, 33, 123, DateTimeZone.UTC);

	private static final String TEST_TZ = "Pacific/Auckland";

	private BasicConfiguration createConfiguration(String tzId, ScheduleType schedule) {
		BasicConfiguration conf = new BasicConfiguration();
		conf.setTimeZoneId(tzId);
		conf.setSchedule(schedule);
		return conf;
	}

	@Test
	public void dateFormatterHourly() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.print(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018-04-23T19:05"));
	}

	@Test
	public void dateFormatterDaily() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Daily);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.print(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018-04-23"));
	}

	@Test
	public void dateFormatterWeekly() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Weekly);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.print(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018W171"));
	}

	@Test
	public void dateFormatterMonthly() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Monthly);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.print(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018-04"));
	}

	@Test
	public void runtimePropsHourlyNoOutputService() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE, null, null);
		assertThat("Props created", props, notNullValue());
		assertThat("Props size", props.keySet(), hasSize(2));
		assertThat("Timestamp", props, hasEntry("ts", TEST_DATE.withZone(DateTimeZone.forID(TEST_TZ))));
		assertThat("Date", props, hasEntry("date", "2018-04-23T19:05"));
	}

	private DatumExportOutputFormatService createOutputService(String ext) {
		return new BaseDatumExportOutputFormatService(UUID.randomUUID().toString()) {

			@Override
			public String getDisplayName() {
				return "test " + ext;
			}

			@Override
			public String getExportFilenameExtension() {
				return ext;
			}

			@Override
			public String getExportContentType() {
				return null;
			}

			@Override
			public ExportContext createExportContext(OutputConfiguration config) {
				return null;
			}
		};
	}

	@Test
	public void runtimePropsWithOutputService() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);
		DatumExportOutputFormatService outputService = createOutputService("json");
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE, null, outputService);
		assertThat("Props created", props, notNullValue());
		assertThat("Props size", props.keySet(), hasSize(3));
		assertThat("Timestamp", props, hasEntry("ts", TEST_DATE.withZone(DateTimeZone.forID(TEST_TZ))));
		assertThat("Date", props, hasEntry("date", "2018-04-23T19:05"));
		assertThat("Extension", props, hasEntry("ext", "json"));
	}

	@Test
	public void runtimePropsWithOutputServiceAndCompression() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);
		BasicOutputConfiguration outputConf = new BasicOutputConfiguration();
		outputConf.setCompressionType(OutputCompressionType.GZIP);
		conf.setOutputConfiguration(outputConf);
		DatumExportOutputFormatService outputService = createOutputService("json");
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE, null, outputService);
		assertThat("Props created", props, notNullValue());
		assertThat("Props size", props.keySet(), hasSize(3));
		assertThat("Timestamp", props, hasEntry("ts", TEST_DATE.withZone(DateTimeZone.forID(TEST_TZ))));
		assertThat("Date", props, hasEntry("date", "2018-04-23T19:05"));
		assertThat("Extension", props, hasEntry("ext", "json.gz"));
	}

	@Test
	public void runtimePropsDailyNoOutputService() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Daily);
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE, null, null);
		assertThat("Props created", props, notNullValue());
		assertThat("Props size", props.keySet(), hasSize(2));
		assertThat("Timestamp", props, hasEntry("ts", TEST_DATE.withZone(DateTimeZone.forID(TEST_TZ))));
		assertThat("Date", props, hasEntry("date", "2018-04-23"));
	}

	@Test
	public void runtimePropsWeeklyNoOutputService() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Weekly);
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE, null, null);
		assertThat("Props created", props, notNullValue());
		assertThat("Props size", props.keySet(), hasSize(2));
		assertThat("Timestamp", props, hasEntry("ts", TEST_DATE.withZone(DateTimeZone.forID(TEST_TZ))));
		assertThat("Date", props, hasEntry("date", "2018W171"));
	}

	@Test
	public void runtimePropsMonthlyNoOutputService() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Monthly);
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE, null, null);
		assertThat("Props created", props, notNullValue());
		assertThat("Props size", props.keySet(), hasSize(2));
		assertThat("Timestamp", props, hasEntry("ts", TEST_DATE.withZone(DateTimeZone.forID(TEST_TZ))));
		assertThat("Date", props, hasEntry("date", "2018-04"));
	}
}
