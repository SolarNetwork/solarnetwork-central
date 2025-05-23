/* ==================================================================
 * SimpleCsvDatumImportInputFormatServiceTests.java - 8/11/2018 1:19:01 PM
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService.ImportContext;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.biz.DatumImportValidationException;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.standard.CsvDatumImportInputProperties;
import net.solarnetwork.central.datum.imp.standard.SimpleCsvDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link SimpleCsvDatumImportInputFormatService} class.
 *
 * @author matt
 * @version 2.1
 */
public class SimpleCsvDatumImportInputFormatServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Test
	public void settings() {
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		List<SettingSpecifier> settings = service.getSettingSpecifiers();
		assertThat("Settings", settings, hasSize(9));

		List<String> keys = settings.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toList());
		assertThat("Setting keys", keys,
				contains("headerRowCount", "dateFormat", "nodeIdColumn", "sourceIdColumn",
						"dateColumnsValue", "instantaneousDataColumns", "accumulatingDataColumns",
						"statusDataColumns", "tagDataColumns"));
	}

	@Test
	public void parse_basic() throws IOException {
		// GIVEN
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration(TEST_USER_ID);
		config.setTimeZoneId("UTC");
		Map<String, Object> serviceProps = new LinkedHashMap<>(4);
		serviceProps.put("instantaneousDataColumns", "D-E");
		serviceProps.put("accumulatingDataColumns", "F");
		config.setServiceProps(serviceProps);
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-simple-data-01.csv", getClass()), "text/csv;charset=utf-8");

		DateTimeFormatter dateFormat = DateTimeFormatter
				.ofPattern(CsvDatumImportInputProperties.DEFAULT_DATE_FORMAT).withZone(ZoneOffset.UTC);

		// WHEN
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
								equalTo(dateFormat.parse("2017-04-17 14:30:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 11899),
										hasEntry("irradiance", (Number) new BigDecimal("696.000"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78434365));
						break;

					case 2:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parse("2017-04-17 14:35:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 9843),
										hasEntry("irradiance", (Number) new BigDecimal("691.668"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78436074));
						break;

					case 3:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parse("2017-04-17 14:40:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 6934),
										hasEntry("irradiance", (Number) new BigDecimal("687.336"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78437105));
						break;

					case 4:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parse("2017-04-17 14:45:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 27314),
										hasEntry("irradiance", (Number) new BigDecimal("683.004"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78438990));
						break;

					case 5:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parse("2017-04-17 14:50:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 13630),
										hasEntry("irradiance", (Number) new BigDecimal("678.672"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78440411));
						break;

					case 6:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parse("2017-04-17 14:55:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 8740),
										hasEntry("irradiance", (Number) new BigDecimal("674.340"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78441320));
						break;

				}
			}
		}

		// THEN
		assertThat("Progress was made", progress, not(hasSize(0)));
		assertThat("Progress complete", progress.get(progress.size() - 1), equalTo((Double) 1.0));

		assertThat("Datum parsed", count, equalTo(6));
	}

	@Test
	public void parseCustomDateFormat() throws IOException {
		// GIVEN
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration(TEST_USER_ID);
		Map<String, Object> serviceProps = new LinkedHashMap<>(4);
		serviceProps.put("instantaneousDataColumns", "D-E");
		serviceProps.put("accumulatingDataColumns", "F");
		String datePattern = "MM/dd/yyyy HH:mm:ss";
		serviceProps.put("dateFormat", datePattern);
		config.setServiceProps(serviceProps);
		config.setTimeZoneId("UTC");
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-simple-data-02.csv", getClass()), "text/csv;charset=utf-8");

		DateTimeFormatter dateFormat = DateTimeFormatter
				.ofPattern(CsvDatumImportInputProperties.DEFAULT_DATE_FORMAT).withZone(ZoneOffset.UTC);

		// WHEN
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
								equalTo(dateFormat.parse("2017-04-17 14:30:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 11899),
										hasEntry("irradiance", (Number) new BigDecimal("696.000"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78434365));
						break;

					case 2:
						assertThat("date " + count, d.getCreated(),
								equalTo(dateFormat.parse("2017-04-17 14:35:00", Instant::from)));
						assertThat("i data " + count, d.getSamples().getInstantaneous(),
								allOf(hasEntry("watts", (Number) 9843),
										hasEntry("irradiance", (Number) new BigDecimal("691.668"))));
						assertThat("a data " + count, d.getSamples().getAccumulating(),
								hasEntry("wattHours", (Number) 78436074));
						break;

				}
			}
		}

		// THEN
		assertThat("Progress was made", progress, not(hasSize(0)));
		assertThat("Progress complete", progress.get(progress.size() - 1), equalTo((Double) 1.0));

		assertThat("Datum parsed", count, equalTo(2));
	}

	@Test
	public void badNodeId() throws IOException {
		// GIVEN
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration(TEST_USER_ID);
		Map<String, Object> serviceProps = new LinkedHashMap<>(4);
		serviceProps.put("instantaneousDataColumns", "D");
		config.setServiceProps(serviceProps);
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-simple-data-03.csv", getClass()), "text/csv;charset=utf-8");

		// WHEN
		thenExceptionOfType(DatumImportValidationException.class).isThrownBy(() -> {
			try (ImportContext ctx = service.createImportContext(config, resource, null)) {
				for ( @SuppressWarnings("unused")
				GeneralNodeDatum d : ctx ) {
					// nothing
				}
			}
		}).returns("Error parsing node ID from column 1.",
				from(DatumImportValidationException::getMessage))
				.returns(2L, from(DatumImportValidationException::getLineNumber))
				.returns("A,/DE/G1/B600/GEN/1,2017-04-17 14:30:00,11899",
						from(DatumImportValidationException::getLine));
	}

	@Test
	public void columZero() throws IOException {
		// GIVEN
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration(TEST_USER_ID);
		Map<String, Object> serviceProps = new LinkedHashMap<>(4);
		serviceProps.put("instantaneousDataColumns", "0");
		config.setServiceProps(serviceProps);
		config.setTimeZoneId("UTC");
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-simple-data-04.csv", getClass()), "text/csv;charset=utf-8");

		// WHEN
		thenIllegalArgumentException().isThrownBy(() -> {
			try (ImportContext ctx = service.createImportContext(config, resource, null)) {
				for ( @SuppressWarnings("unused")
				GeneralNodeDatum d : ctx ) {
					// nothing
				}
			}
		});
	}

	@Test
	public void badDstDate() throws IOException {
		// GIVEN
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration(TEST_USER_ID);
		Map<String, Object> serviceProps = new LinkedHashMap<>(4);
		serviceProps.put("instantaneousDataColumns", "D");
		String datePattern = "yyyy-MM-dd HH:mm:ss";
		serviceProps.put("dateFormat", datePattern);
		config.setServiceProps(serviceProps);
		config.setTimeZoneId("America/New_York");
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-simple-data-05.csv", getClass()), "text/csv;charset=utf-8");

		// WHEN
		List<Double> progress = new ArrayList<>(8);
		thenExceptionOfType(DatumImportValidationException.class).isThrownBy(() -> {
			try (ImportContext ctx = service.createImportContext(config, resource,
					new ProgressListener<DatumImportService>() {

						@Override
						public void progressChanged(DatumImportService context, double amountComplete) {
							assertThat("Context is service", context, sameInstance(service));
							progress.add(amountComplete);
						}
					})) {
				for ( @SuppressWarnings("unused")
				GeneralNodeDatum d : ctx ) {
					// nothing
				}
			}
		});
	}

	@Test
	public void badDate() throws IOException {
		// GIVEN
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();
		BasicInputConfiguration config = new BasicInputConfiguration(TEST_USER_ID);
		Map<String, Object> serviceProps = new LinkedHashMap<>(4);
		serviceProps.put("instantaneousDataColumns", "D");
		String datePattern = "yyyy-MM-dd HH:mm:ss";
		serviceProps.put("dateFormat", datePattern);
		config.setServiceProps(serviceProps);
		config.setTimeZoneId("America/New_York");
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-simple-data-06.csv", getClass()), "text/csv;charset=utf-8");

		// WHEN
		List<Double> progress = new ArrayList<>(8);
		thenExceptionOfType(DatumImportValidationException.class).isThrownBy(() -> {
			try (ImportContext ctx = service.createImportContext(config, resource,
					new ProgressListener<DatumImportService>() {

						@Override
						public void progressChanged(DatumImportService context, double amountComplete) {
							assertThat("Context is service", context, sameInstance(service));
							progress.add(amountComplete);
						}
					})) {
				for ( @SuppressWarnings("unused")
				GeneralNodeDatum d : ctx ) {
					// nothing
				}
			}
		});
	}

}
