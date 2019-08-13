/* ==================================================================
 * BasicCsvDatumImportInputFormatServiceTests.java - 8/11/2018 1:19:01 PM
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

package net.solarnetwork.central.datum.imp.standard.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService.ImportContext;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.biz.DatumImportValidationException;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.standard.BasicCsvDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.standard.CsvDatumImportInputProperties;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ProgressListener;

/**
 * Test cases for the {@link BasicCsvDatumImportInputFormatService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCsvDatumImportInputFormatServiceTests {

	@Test
	public void settings() {
		BasicCsvDatumImportInputFormatService service = new BasicCsvDatumImportInputFormatService();
		List<SettingSpecifier> settings = service.getSettingSpecifiers();
		assertThat("Settings", settings, hasSize(9));

		List<String> keys = settings.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toList());
		assertThat("Setting keys", keys,
				contains("headerRowCount", "dateFormat", "nodeIdColumn", "sourceIdColumn",
						"dateColumnsValue", "instantaneousDataColumn", "accumulatingDataColumn",
						"statusDataColumn", "tagDataColumn"));
	}

	@Test
	public void parseDefaultConfig() throws IOException {
		// given
		BasicCsvDatumImportInputFormatService service = new BasicCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration();
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-01.csv", getClass()), "text/csv;charset=utf-8");

		DateTimeFormatter dateFormat = DateTimeFormat
				.forPattern(CsvDatumImportInputProperties.DEFAULT_DATE_FORMAT);

		// when
		List<Double> progress = new ArrayList<>(8);
		int count = 0;
		try (ImportContext ctx = service.createImportContext(config, resource,
				new ProgressListener<DatumImportService>() {

					@Override
					public void progressChanged(DatumImportService context, double amountComplete) {
						assertThat("Context is service", context, sameInstance(service));
						progress.add(amountComplete);
					}
				})) {
			for ( GeneralNodeDatum d : ctx ) {
				count++;
				assertThat("samples " + count, d.getSamples(), notNullValue());
				assertThat("node ID " + count, d.getNodeId(), equalTo(1L));
				assertThat("source ID " + count, d.getSourceId(), equalTo("/DE/G1/B600/GEN/1"));
				switch (count) {
					case 1:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:30:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 11899),
										hasEntry("irradiance", (Number) new BigDecimal("696.000"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78434365));
						break;

					case 2:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:35:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 9843),
										hasEntry("irradiance", (Number) new BigDecimal("691.668"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78436074));
						break;

					case 3:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:40:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 6934),
										hasEntry("irradiance", (Number) new BigDecimal("687.336"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78437105));
						break;

					case 4:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:45:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 27314),
										hasEntry("irradiance", (Number) new BigDecimal("683.004"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78438990));
						break;

					case 5:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:50:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 13630),
										hasEntry("irradiance", (Number) new BigDecimal("678.672"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78440411));
						break;

					case 6:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:55:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 8740),
										hasEntry("irradiance", (Number) new BigDecimal("674.340"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78441320));
						break;

				}
			}
		}

		// then
		assertThat("Progress was made", progress, not(hasSize(0)));
		assertThat("Progress complete", progress.get(progress.size() - 1), equalTo((Double) 1.0));

		assertThat("Datum parsed", count, equalTo(6));
	}

	@Test
	public void parseCustomDateFormat() throws IOException {
		// given
		BasicCsvDatumImportInputFormatService service = new BasicCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration();
		String datePattern = "MM/dd/yyyy HH:mm:ss";
		config.setServiceProps(Collections.singletonMap("dateFormat", datePattern));
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-02.csv", getClass()), "text/csv;charset=utf-8");

		DateTimeFormatter dateFormat = DateTimeFormat
				.forPattern(CsvDatumImportInputProperties.DEFAULT_DATE_FORMAT);

		// when
		List<Double> progress = new ArrayList<>(8);
		int count = 0;
		try (ImportContext ctx = service.createImportContext(config, resource,
				new ProgressListener<DatumImportService>() {

					@Override
					public void progressChanged(DatumImportService context, double amountComplete) {
						assertThat("Context is service", context, sameInstance(service));
						progress.add(amountComplete);
					}
				})) {
			for ( GeneralNodeDatum d : ctx ) {
				count++;
				assertThat("samples " + count, d.getSamples(), notNullValue());
				assertThat("node ID " + count, d.getNodeId(), equalTo(1L));
				assertThat("source ID " + count, d.getSourceId(), equalTo("/DE/G1/B600/GEN/1"));
				switch (count) {
					case 1:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:30:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 11899),
										hasEntry("irradiance", (Number) new BigDecimal("696.000"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78434365));
						break;

					case 2:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parseDateTime("2017-04-17 14:35:00")));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 9843),
										hasEntry("irradiance", (Number) new BigDecimal("691.668"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78436074));
						break;

				}
			}
		}

		// then
		assertThat("Progress was made", progress, not(hasSize(0)));
		assertThat("Progress complete", progress.get(progress.size() - 1), equalTo((Double) 1.0));

		assertThat("Datum parsed", count, equalTo(2));
	}

	@Test
	public void badNodeId() throws IOException {
		// given
		BasicCsvDatumImportInputFormatService service = new BasicCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration();
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-03.csv", getClass()), "text/csv;charset=utf-8");

		// when
		try (ImportContext ctx = service.createImportContext(config, resource, null)) {
			for ( @SuppressWarnings("unused")
			GeneralNodeDatum d : ctx ) {
				// nothing
			}
			fail("Should have thrown DatumImportValidationException");
		} catch ( DatumImportValidationException e ) {
			assertThat("Message", e.getMessage(), equalTo("Error parsing node ID from column 1."));
			assertThat("Line number", e.getLineNumber(), equalTo(2L));
			assertThat("Line", e.getLine(),
					equalTo("A,/DE/G1/B600/GEN/1,2017-04-17 14:30:00,\"{\"\"watts\"\":11899}\""));
		}
	}

	@Test
	public void columZeroIgnored() throws IOException {
		// given
		BasicCsvDatumImportInputFormatService service = new BasicCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration();
		config.setServiceProps(Collections.singletonMap("instantaneousDataColumn", "0"));
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-04.csv", getClass()), "text/csv;charset=utf-8");

		// when
		try (ImportContext ctx = service.createImportContext(config, resource, null)) {
			int count = 0;
			for ( GeneralNodeDatum d : ctx ) {
				assertThat("Node ID", d.getNodeId(), equalTo(123L));
				assertThat("Source ID not parsed from invalid column 0", d.getSourceId(),
						equalTo("/DE/G1/B600/GEN/100"));
				assertThat("Watts", d.getSamples(), nullValue());
				count++;
			}
			assertThat("Row count", count, equalTo(1));
		}
	}
}
