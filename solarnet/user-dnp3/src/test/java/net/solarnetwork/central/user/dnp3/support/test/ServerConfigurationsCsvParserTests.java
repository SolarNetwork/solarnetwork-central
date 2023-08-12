/* ==================================================================
 * ServerConfigurationsCsvParserTests.java - 12/08/2023 9:53:51 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dnp3.support.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.BDDAssertions.then;
import static org.supercsv.prefs.CsvPreference.STANDARD_PREFERENCE;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;
import net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvParser;

/**
 * Test cases for the {@link ServerConfigurationsCsvParser} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerConfigurationsCsvParserTests {

	private MessageSource messageSource;
	private Locale locale;

	@BeforeEach
	public void setup() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename(ServerConfigurationsCsvParser.class.getName());
		messageSource = ms;
		locale = Locale.getDefault();
	}

	private CsvListReader csvReader(String resouce) {
		return new CsvListReader(
				new InputStreamReader(getClass().getResourceAsStream("server-confs-example-01.csv"),
						UTF_8),
				STANDARD_PREFERENCE);
	}

	@Test
	public void importExample() throws IOException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final Long serverId = UUID.randomUUID().getMostSignificantBits();

		// WHEN
		final Instant now = Instant.now();
		ServerConfigurations result = null;
		try (ICsvListReader in = csvReader("server-confs-example-01.csv")) {
			result = new ServerConfigurationsCsvParser(userId, serverId, now, messageSource, locale)
					.parse(in);
		}

		// THEN
		then(result).as("Result provided").isNotNull();

		// @formatter:off
		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getNodeId)
			.as("Parsed measurement node ID values")
			.containsExactly(123L, 123L);

		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getSourceId)
			.as("Parsed measurement source ID values")
			.containsExactly("power/1", "power/2");

		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getProperty)
			.as("Parsed measurement property values")
			.containsExactly("watts", "frequency");

		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getType)
			.as("Parsed measurement type values")
			.containsExactly(MeasurementType.AnalogInput, MeasurementType.AnalogInput);

		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getMultiplier)
			.as("Parsed measurement multiplier values")
			.containsExactly(new BigDecimal("0.001"), null);

		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getOffset)
			.as("Parsed measurement offset values")
			.containsExactly(null, null);

		then(result.measurementConfigs()).extracting(ServerMeasurementConfiguration::getScale)
			.as("Parsed measurement scale values")
			.containsExactly(3, 1);
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getNodeId)
			.as("Parsed control node ID values")
			.containsExactly(234L, 345L);
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getControlId)
			.as("Parsed control control ID values")
			.containsExactly("switch/1", "setpoint/1");
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getProperty)
			.as("Parsed control property values")
			.containsExactly(null, null);
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getType)
			.as("Parsed control type values")
			.containsExactly(ControlType.Binary, ControlType.Analog);
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getMultiplier)
			.as("Parsed control multiplier values")
			.containsExactly(null, new BigDecimal("0.1"));
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getOffset)
			.as("Parsed control offset values")
			.containsExactly(null, new BigDecimal("100"));
	
		then(result.controlConfigs()).extracting(ServerControlConfiguration::getScale)
			.as("Parsed control scale values")
			.containsExactly(null, 1);
	
		// @formatter:on
	}

}
