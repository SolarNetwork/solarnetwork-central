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

import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hamcrest.Matchers;
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
 * @version 2.0
 */
public class ConfigurationTests {

	private static final ZonedDateTime TEST_DATE = ZonedDateTime.of(2018, 4, 23, 7, 5, 33, 123,
			ZoneOffset.UTC);

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
		String result = fmt.format(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018-04-23T19:05"));
	}

	@Test
	public void dateFormatterDaily() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Daily);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.format(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018-04-23"));
	}

	@Test
	public void dateFormatterWeekly() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Weekly);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.format(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018W171"));
	}

	@Test
	public void dateFormatterMonthly() {
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Monthly);
		DateTimeFormatter fmt = conf.createDateTimeFormatterForSchedule();
		String result = fmt.format(TEST_DATE);
		assertThat(result, Matchers.equalTo("2018-04"));
	}

	@Test
	public void runtimePropsHourlyNoOutputService() {
		// GIVEN
		final long now = System.currentTimeMillis();
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null, null);

		// THEN
		// @formatter:off
		then(props)
			.as("Expected props created")
			.hasSize(3)
			.as("Date time provided")
			.containsEntry(Configuration.PROP_DATE_TIME, TEST_DATE.withZoneSameInstant(ZoneId.of(TEST_TZ)))
			.as("Formatted date")
			.containsEntry(Configuration.PROP_DATE, "2018-04-23T19:05")
			.as("Current time epoch")
			.extractingByKey(Configuration.PROP_CURRENT_TIME, InstanceOfAssertFactories.LONG)
			.isGreaterThanOrEqualTo(now);
			;
		// @formatter:on
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
		// GIVEN
		final long now = System.currentTimeMillis();
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);
		DatumExportOutputFormatService outputService = createOutputService("json");

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null,
				outputService);

		// THEN
		// @formatter:off
		then(props)
			.as("Expected props created")
			.hasSize(4)
			.as("File extension provided")
			.containsEntry(Configuration.PROP_FILENAME_EXTENSION, "json")
			.as("Date time provided")
			.containsEntry(Configuration.PROP_DATE_TIME, TEST_DATE.withZoneSameInstant(ZoneId.of(TEST_TZ)))
			.as("Formatted date")
			.containsEntry(Configuration.PROP_DATE, "2018-04-23T19:05")
			.as("Current time epoch")
			.extractingByKey(Configuration.PROP_CURRENT_TIME, InstanceOfAssertFactories.LONG)
			.isGreaterThanOrEqualTo(now);
			;
		// @formatter:on
	}

	@Test
	public void runtimePropsWithOutputServiceAndCompression() {
		// GIVEN
		final long now = System.currentTimeMillis();
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Hourly);
		BasicOutputConfiguration outputConf = new BasicOutputConfiguration();
		outputConf.setCompressionType(OutputCompressionType.GZIP);
		conf.setOutputConfiguration(outputConf);
		DatumExportOutputFormatService outputService = createOutputService("json");

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null,
				outputService);

		// THEN
		// @formatter:off
		then(props)
			.as("Expected props created")
			.hasSize(4)
			.as("File extension provided")
			.containsEntry(Configuration.PROP_FILENAME_EXTENSION, "json.gz")
			.as("Date time provided")
			.containsEntry(Configuration.PROP_DATE_TIME, TEST_DATE.withZoneSameInstant(ZoneId.of(TEST_TZ)))
			.as("Formatted date")
			.containsEntry(Configuration.PROP_DATE, "2018-04-23T19:05")
			.as("Current time epoch")
			.extractingByKey(Configuration.PROP_CURRENT_TIME, InstanceOfAssertFactories.LONG)
			.isGreaterThanOrEqualTo(now);
			;
		// @formatter:on
	}

	@Test
	public void runtimePropsDailyNoOutputService() {
		// GIVEN
		final long now = System.currentTimeMillis();
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Daily);

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null, null);

		// THEN
		// @formatter:off
		then(props)
			.as("Expected props created")
			.hasSize(3)
			.as("Date time provided")
			.containsEntry(Configuration.PROP_DATE_TIME, TEST_DATE.withZoneSameInstant(ZoneId.of(TEST_TZ)))
			.as("Formatted date")
			.containsEntry(Configuration.PROP_DATE, "2018-04-23")
			.as("Current time epoch")
			.extractingByKey(Configuration.PROP_CURRENT_TIME, InstanceOfAssertFactories.LONG)
			.isGreaterThanOrEqualTo(now);
			;
		// @formatter:on
	}

	@Test
	public void runtimePropsWeeklyNoOutputService() {
		// GIVEN
		final long now = System.currentTimeMillis();
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Weekly);

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null, null);

		// THEN
		// @formatter:off
		then(props)
			.as("Expected props created")
			.hasSize(3)
			.as("Date time provided")
			.containsEntry(Configuration.PROP_DATE_TIME, TEST_DATE.withZoneSameInstant(ZoneId.of(TEST_TZ)))
			.as("Formatted date")
			.containsEntry(Configuration.PROP_DATE, "2018W171")
			.as("Current time epoch")
			.extractingByKey(Configuration.PROP_CURRENT_TIME, InstanceOfAssertFactories.LONG)
			.isGreaterThanOrEqualTo(now);
			;
		// @formatter:on
	}

	@Test
	public void runtimePropsMonthlyNoOutputService() {
		// GIVEN
		final long now = System.currentTimeMillis();
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Monthly);

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null, null);

		// THEN
		// @formatter:off
		then(props)
			.as("Expected props created")
			.hasSize(3)
			.as("Date time provided")
			.containsEntry(Configuration.PROP_DATE_TIME, TEST_DATE.withZoneSameInstant(ZoneId.of(TEST_TZ)))
			.as("Formatted date")
			.containsEntry(Configuration.PROP_DATE, "2018-04")
			.as("Current time epoch")
			.extractingByKey(Configuration.PROP_CURRENT_TIME, InstanceOfAssertFactories.LONG)
			.isGreaterThanOrEqualTo(now);
			;
		// @formatter:on
	}

	@Test
	public void runtimePropsJobName() {
		// GIVEN
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Monthly);
		conf.setName(UUID.randomUUID().toString());

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null, null);

		// THEN
		// @formatter:off
		then(props)
			.as("Props created with name")
			.containsEntry(Configuration.PROP_JOB_NAME, conf.getName())
			;
		// @formatter:on

	}

	@Test
	public void runtimePropsJobName_sanitize() {
		// GIVEN
		BasicConfiguration conf = createConfiguration(TEST_TZ, ScheduleType.Monthly);
		conf.setName("All the fun characters:/ ⲁあアピⰄ⠷☃😀 / oh yeah!");

		// WHEN
		Map<String, Object> props = conf.createRuntimeProperties(TEST_DATE.toInstant(), null, null);

		// THEN
		// @formatter:off
		then(props)
			.as("Props created with sanitized name, preserving as many language characters as possible")
			.containsEntry(Configuration.PROP_JOB_NAME, "All_the_fun_characters_ⲁあアピⰄ_oh_yeah_")
			;
		// @formatter:on

	}
}
