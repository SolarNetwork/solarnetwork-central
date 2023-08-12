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
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.supercsv.prefs.CsvPreference.STANDARD_PREFERENCE;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationsInput;
import net.solarnetwork.central.user.dnp3.domain.ServerControlConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerMeasurementConfigurationInput;
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

	private CsvListReader csvResource(String resource) {
		return new CsvListReader(new InputStreamReader(getClass().getResourceAsStream(resource), UTF_8),
				STANDARD_PREFERENCE);
	}

	@Test
	public void importExample() throws IOException {
		// GIVEN

		// WHEN
		ServerConfigurationsInput result = null;
		try (ICsvListReader in = csvResource("server-confs-example-01.csv")) {
			result = new ServerConfigurationsCsvParser(messageSource, locale).parse(in);
		}

		// THEN
		then(result).as("Result provided").isNotNull();

		// @formatter:off
		then(result.getMeasurementConfigs()).map(ServerMeasurementConfigurationInput::isEnabled)
			.as("Measurement enabled set to TRUE")
			.allMatch(Boolean.TRUE::equals);

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getNodeId)
			.as("Parsed measurement node ID values")
			.containsExactly(123L, 123L);

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getSourceId)
			.as("Parsed measurement source ID values")
			.containsExactly("power/1", "power/2");

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getProperty)
			.as("Parsed measurement property values")
			.containsExactly("watts", "frequency");

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getType)
			.as("Parsed measurement type values")
			.containsExactly(MeasurementType.AnalogInput, MeasurementType.AnalogInput);

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getMultiplier)
			.as("Parsed measurement multiplier values")
			.containsExactly(new BigDecimal("0.001"), null);

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getOffset)
			.as("Parsed measurement offset values")
			.containsExactly(null, null);

		then(result.getMeasurementConfigs()).extracting(ServerMeasurementConfigurationInput::getScale)
			.as("Parsed measurement scale values")
			.containsExactly(3, 1);
	
		
		then(result.getControlConfigs()).map(ServerControlConfigurationInput::isEnabled)
			.as("Control enabled set to TRUE")
			.allMatch(Boolean.TRUE::equals);

		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getNodeId)
			.as("Parsed control node ID values")
			.containsExactly(234L, 345L);
	
		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getSourceId)
			.as("Parsed control control ID values")
			.containsExactly("switch/1", "setpoint/1");
	
		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getProperty)
			.as("Parsed control property values")
			.containsExactly(null, null);
	
		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getType)
			.as("Parsed control type values")
			.containsExactly(ControlType.Binary, ControlType.Analog);
	
		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getMultiplier)
			.as("Parsed control multiplier values")
			.containsExactly(null, new BigDecimal("0.1"));
	
		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getOffset)
			.as("Parsed control offset values")
			.containsExactly(null, new BigDecimal("100"));
	
		then(result.getControlConfigs()).extracting(ServerControlConfigurationInput::getScale)
			.as("Parsed control scale values")
			.containsExactly(null, 1);
	
		// @formatter:on
	}

	private static String csv(String body) {
		return """
				Node ID,Source ID,Property, DNP3 Type,Enabled,Multiplier,Offset,Decimal Scale
				""".concat(body);
	}

	private CsvListReader csvData(String data) {
		return new CsvListReader(new StringReader(csv(data)), STANDARD_PREFERENCE);
	}

	private final Pattern ROW_COL_REGEX = Pattern.compile("row (\\d+) column (\\d+)");

	private Predicate<Throwable> rowColumnMessage(int row, int col) {
		return (t) -> {
			if ( t == null ) {
				return false;
			}
			final String s = t.getMessage();
			if ( s == null ) {
				return false;
			}
			final Matcher m = ROW_COL_REGEX.matcher(s);
			if ( !m.find() ) {
				return false;
			}
			try {
				int r = Integer.parseInt(m.group(1));
				if ( r != row ) {
					return false;
				}
				int c = Integer.parseInt(m.group(2));
				return (c == col);
			} catch ( NumberFormatException e ) {
				// shouldn't be here
				return false;
			}
		};
	}

	@Test
	public void missingNodeId() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					,power/1,watts,AnalogInput,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from missing node ID")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 1));
		// @formatter:on
	}

	@Test
	public void invalidNodeId() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					blah,power/1,watts,AnalogInput,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from invalid node ID")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 1));
		// @formatter:on
	}

	@Test
	public void missingSourceId() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,,watts,AnalogInput,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from missing source ID")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 2));
		// @formatter:on
	}

	@Test
	public void whiespaceSourceId() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123, ,watts,AnalogInput,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from whitespace source ID")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 2));
		// @formatter:on
	}

	@Test
	public void missingProperty() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1,,AnalogInput,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from missing property")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 3));
		// @formatter:on
	}

	@Test
	public void whitespaceProperty() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1, ,AnalogInput,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from whitespace property")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 3));
		// @formatter:on
	}

	@Test
	public void missingProperty_allowedForControl() throws IOException {
		// GIVEN

		// WHEN
		ServerConfigurationsInput result = null;
		try (ICsvListReader in = csvData("""
				123,switch/1,,ControlBinary
				""")) {
			result = new ServerConfigurationsCsvParser(messageSource, locale).parse(in);
		}
		// @formatter:off
		then(result.getControlConfigs()).hasSize(1).element(0)
			.as("Node ID parsed")
			.returns(123L, ServerControlConfigurationInput::getNodeId)
			.as("Control ID parsed")
			.returns("switch/1", ServerControlConfigurationInput::getSourceId)
			.as("Property is null")
			.returns(null, ServerControlConfigurationInput::getProperty)
			.as("Type parsed")
			.returns(ControlType.Binary, ServerControlConfigurationInput::getType)
			.as("Enabled implied")
			.returns(false, ServerControlConfigurationInput::isEnabled)
			;
		// @formatter:on
	}

	@Test
	public void missingType() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1,watts,,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser( messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from missing type")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 4));
		// @formatter:on
	}

	@Test
	public void invalidType() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1,watts,FooBar,TRUE,0.1,100,1
					""")) {
				new ServerConfigurationsCsvParser( messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from invalid type")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 4));
		// @formatter:on
	}

	@Test
	public void wonkyEnabled() throws IOException {
		// GIVEN

		// WHEN
		ServerConfigurationsInput result = null;
		try (ICsvListReader in = csvData("""
				123,switch/1,,ControlBinary,NO WAY NO HOW
				""")) {
			result = new ServerConfigurationsCsvParser(messageSource, locale).parse(in);
		}
		// @formatter:off
		then(result.getControlConfigs()).hasSize(1).element(0)
			.as("Node ID parsed")
			.returns(123L, ServerControlConfigurationInput::getNodeId)
			.as("Control ID parsed")
			.returns("switch/1", ServerControlConfigurationInput::getSourceId)
			.as("Property is null")
			.returns(null, ServerControlConfigurationInput::getProperty)
			.as("Type parsed")
			.returns(ControlType.Binary, ServerControlConfigurationInput::getType)
			.as("Wonky enabled parsed to fales")
			.returns(false, ServerControlConfigurationInput::isEnabled)
			;
		// @formatter:on
	}

	@Test
	public void invalidMultiplier() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1,watts,AnalogInput,TRUE,a,100,1
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from invalid multiplier")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 6));
		// @formatter:on
	}

	@Test
	public void invalidOffset() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1,watts,AnalogInput,TRUE,1,a,1
					""")) {
				new ServerConfigurationsCsvParser( messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from invalid offset")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 7));
		// @formatter:on
	}

	@Test
	public void invalidScale() throws IOException {
		// GIVEN

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			try (ICsvListReader in = csvData("""
					123,power/1,watts,AnalogInput,TRUE,1,1,a
					""")) {
				new ServerConfigurationsCsvParser(messageSource, locale)
						.parse(in);
			}
		}).as("Validation exception thrown from invalid scale")
				.isInstanceOf(IllegalArgumentException.class)
				.as("Exception message referes to row/column")
				.matches(rowColumnMessage(2, 8));
		// @formatter:on
	}

}
