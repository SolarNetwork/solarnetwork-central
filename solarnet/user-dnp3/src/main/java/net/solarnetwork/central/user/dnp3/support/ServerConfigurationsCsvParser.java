/* ==================================================================
 * ServerConfigurationsCsvParser.java - 12/08/2023 8:54:26 am
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

package net.solarnetwork.central.user.dnp3.support;

import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.ENABLED;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.MULTIPLIER;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.NODE_ID;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.OFFSET;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.PROPERTY;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.SOURCE_ID;
import static net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvColumn.TYPE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.parseBoolean;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.supercsv.io.ICsvListReader;
import net.solarnetwork.central.dnp3.domain.BaseServerDatumStreamConfiguration;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;
import net.solarnetwork.domain.CodedValue;

/**
 * Parse a CSV resource of server measurement and control configurations.
 * 
 * <p>
 * The expected structure of the CSV is:
 * </p>
 * 
 * <ol>
 * <li><b>Node ID</b> - a datum stream node ID</li>
 * <li><b>Source ID</b> - a datum stream source ID, or control ID for control
 * types</li>
 * <li><b>Property</b> - the datum stream property name; optional for control
 * types</li>
 * <li><b>Type</b> - a {@link MeasurementType} or {@link ControlType}</li>
 * <li><b>Enabled</b> - a boolean on/off state</li>
 * <li><b>Multiplier</b> - an optional number to multiple property values
 * by</li>
 * <li><b>Offset</b> - an optional number to add to property values</li>
 * <li><b>Decimal Scale</b> - an optional integer decimal scale to round
 * decimals to; empty or -1 for no rounding</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class ServerConfigurationsCsvParser {

	private final Long userId;
	private final Long serverId;
	private final Instant date;
	private final MessageSource messageSource;
	private final Locale locale;

	private final List<ServerMeasurementConfiguration> measurements = new ArrayList<>(16);
	private final List<ServerControlConfiguration> controls = new ArrayList<>(16);

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID
	 * @param date
	 *        the date to assign
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the locale for messages
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerConfigurationsCsvParser(Long userId, Long serverId, Instant date,
			MessageSource messageSource, Locale locale) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.serverId = requireNonNullArgument(serverId, "serverId");
		this.date = requireNonNullArgument(date, "date");
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.locale = requireNonNullArgument(locale, "locale");
	}

	/**
	 * Parse CSV.
	 * 
	 * @param csv
	 *        the CSV to parse
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws IllegalArgumentException
	 *         if invalid data is parsed
	 */
	public ServerConfigurations parse(ICsvListReader csv) throws IOException {
		if ( csv == null ) {
			return null;
		}
		csv.getHeader(true); // skip header
		List<String> row = null;
		while ( (row = csv.read()) != null ) {
			if ( row.isEmpty() || row.size() < 4
					|| (row.get(0) != null && row.get(0).startsWith("#")) ) {
				continue;
			}
			final int rowLen = row.size();
			final int rowNum = csv.getRowNumber();

			final Long nodeId = parseLongValue(row, rowLen, rowNum, NODE_ID, true);
			final String sourceId = parseStringValue(row, rowLen, rowNum, SOURCE_ID, true);
			final CodedValue type = parseTypeValue(row, rowLen, rowNum);
			final String property = parseStringValue(row, rowLen, rowNum, PROPERTY,
					type instanceof MeasurementType);
			final boolean enabled = parseBoolean(parseStringValue(row, rowLen, rowNum, ENABLED, false));
			final BigDecimal mult = parseBigDecimalValue(row, rowLen, rowNum, MULTIPLIER, false);
			final BigDecimal offset = parseBigDecimalValue(row, rowLen, rowNum, OFFSET, false);
			final Integer scale = parseIntegerValue(row, rowLen, rowNum, DECIMAL_SCALE, false);

			BaseServerDatumStreamConfiguration<?, ?> config = null;
			if ( type instanceof MeasurementType t ) {
				ServerMeasurementConfiguration c = new ServerMeasurementConfiguration(userId, serverId,
						measurements.size(), date);
				c.setType(t);
				config = c;
				measurements.add(c);
			} else if ( type instanceof ControlType t ) {
				ServerControlConfiguration c = new ServerControlConfiguration(userId, serverId,
						controls.size(), date);
				c.setType(t);
				config = c;
				controls.add(c);
			} else {
				// shouldn't be here
				throw new IllegalArgumentException("Unsupported type [" + type + "]");
			}
			config.setEnabled(enabled);
			config.setNodeId(nodeId);
			config.setSourceId(sourceId);
			config.setProperty(property);
			config.setMultiplier(mult);
			config.setOffset(offset);
			config.setScale(scale);
			if ( !config.isValid() ) {
				throw new IllegalArgumentException(
						messageSource.getMessage("dnp3.config.import.csv.error.invalidConfiguration",
								new Object[] { rowNum }, "Invalid configuration on row {0}.", locale));
			}
		}
		return new ServerConfigurations(measurements, controls);
	}

	private String colName(ServerConfigurationsCsvColumn col) {
		return messageSource.getMessage("dnp3.config.import.csv.col.%s".formatted(col.name()), null,
				col.getName(), locale);
	}

	private String parseStringValue(List<String> row, int rowLen, int rowNum,
			ServerConfigurationsCsvColumn col, boolean required) {
		final int colNum = col.getCode();
		String s = null;
		if ( colNum < rowLen ) {
			s = row.get(colNum);
			if ( s != null ) {
				s = s.trim();
			}
			if ( s != null && s.isEmpty() ) {
				s = null;
			}
		}
		if ( s == null && required ) {
			throw new IllegalArgumentException(
					messageSource.getMessage("dnp3.config.import.csv.error.missingValue",
							new Object[] { colName(col), rowNum, colNum + 1 },
							"Missing {0} value on row {1} column {2}.", locale));
		}
		return s;
	}

	private Integer parseIntegerValue(List<String> row, int rowLen, int rowNum,
			ServerConfigurationsCsvColumn col, boolean required) {
		final String s = parseStringValue(row, rowLen, rowNum, col, required);
		Integer result = null;
		if ( s != null ) {
			try {
				result = Integer.valueOf(s);
			} catch ( NumberFormatException e ) {
				throw new IllegalArgumentException(messageSource.getMessage(
						"dnp3.config.import.csv.error.invalidFormat",
						new Object[] { colName(col), s, rowNum, col.getCode() + 1, e.getMessage() },
						"Invalid {0} value [{1}] on row {2} column {3}: {4}", locale));
			}
		}
		return result;
	}

	private Long parseLongValue(List<String> row, int rowLen, int rowNum,
			ServerConfigurationsCsvColumn col, boolean required) {
		final String s = parseStringValue(row, rowLen, rowNum, col, required);
		Long result = null;
		if ( s != null ) {
			try {
				result = Long.valueOf(s);
			} catch ( NumberFormatException e ) {
				throw new IllegalArgumentException(messageSource.getMessage(
						"dnp3.config.import.csv.error.invalidFormat",
						new Object[] { colName(col), s, rowNum, col.getCode() + 1, e.getMessage() },
						"Invalid {0} value [{1}] on row {2} column {3}: {4}", locale));
			}
		}
		return result;
	}

	private BigDecimal parseBigDecimalValue(List<String> row, int rowLen, int rowNum,
			ServerConfigurationsCsvColumn col, boolean required) {
		String s = parseStringValue(row, rowLen, rowNum, col, required);
		if ( s != null ) {
			try {
				return new BigDecimal(s);
			} catch ( NumberFormatException e ) {
				throw new IllegalArgumentException(messageSource.getMessage(
						"dnp3.config.import.csv.error.invalidFormat",
						new Object[] { colName(col), s, rowNum, col.getCode() + 1, e.getMessage() },
						"Invalid {0} value [{1}] on row {2} column {3}: {4}", locale));
			}
		}
		return null;
	}

	private static final String CONTROL_PREFIX = "control";

	private CodedValue parseTypeValue(List<String> row, int rowLen, int rowNum) {
		String s = parseStringValue(row, rowLen, rowNum, TYPE, true);
		try {
			return MeasurementType.valueOf(s);
		} catch ( IllegalArgumentException e ) {
			// keep trying
		}
		try {
			return ControlType.valueOf(s);
		} catch ( IllegalArgumentException e ) {
			// keep trying
		}
		// try without "Control" prefix
		if ( s.toLowerCase().startsWith(CONTROL_PREFIX) ) {
			try {
				return ControlType.valueOf(s.substring(CONTROL_PREFIX.length()));
			} catch ( IllegalArgumentException e ) {
				// keep trying
			}
		}
		CodedValue result = null;
		if ( s.length() < 2 ) {
			result = CodedValue.forCodeValue(s.charAt(0), MeasurementType.class, null);
		} else {
			result = CodedValue.forCodeValue(s.charAt(1), ControlType.class, null);
		}
		if ( result == null ) {
			throw new IllegalArgumentException(
					messageSource.getMessage("dnp3.config.import.csv.error.invalidType",
							new Object[] { colName(TYPE), s, rowNum, TYPE.getCode() + 1 },
							"Invalid {0} value [{1}] on row {2} column {3}.", locale));
		}
		return result;
	}

}
