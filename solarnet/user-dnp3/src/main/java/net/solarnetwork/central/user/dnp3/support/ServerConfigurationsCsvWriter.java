/* ==================================================================
 * ServerConfigurationsCsvWriter.java - 12/08/2023 6:09:58 pm
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

import static java.util.Arrays.fill;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.supercsv.io.ICsvListWriter;
import net.solarnetwork.central.dnp3.domain.BaseServerDatumStreamConfiguration;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;
import net.solarnetwork.domain.CodedValue;

/**
 * Generate a CSV resource from server measurement and control configurations.
 * 
 * <p>
 * The output structure of the CSV is:
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
public class ServerConfigurationsCsvWriter {

	private final ICsvListWriter writer;
	private final MessageSource messageSource;
	private final Locale locale;
	private final int rowLen;

	/**
	 * Constructor.
	 * 
	 * @param writer
	 *        the writer
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the locale for messages
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public ServerConfigurationsCsvWriter(ICsvListWriter writer, MessageSource messageSource,
			Locale locale) throws IOException {
		super();
		this.writer = requireNonNullArgument(writer, "writer");
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.locale = requireNonNullArgument(locale, "locale");
		rowLen = ServerConfigurationsCsvColumn.values().length;
	}

	/**
	 * Generate CSV.
	 * 
	 * @param configurations
	 *        the configurations to generate CSV for
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void generateCsv(ServerConfigurations configurations) throws IOException {
		String[] row = new String[rowLen];
		for ( ServerConfigurationsCsvColumn col : ServerConfigurationsCsvColumn.values() ) {
			row[col.getCode()] = messageSource.getMessage("dnp3.config.import.csv.col." + col.name(),
					null, col.getName(), locale);
		}
		writer.writeHeader(row);
		if ( configurations == null || configurations.isEmpty() ) {
			return;
		}
		if ( configurations.measurementConfigs() != null ) {
			for ( ServerMeasurementConfiguration config : configurations.measurementConfigs() ) {
				fill(row, null);
				populateRow(config, row);
				writer.write(row);
			}
		}
		if ( configurations.controlConfigs() != null ) {
			for ( ServerControlConfiguration config : configurations.controlConfigs() ) {
				fill(row, null);
				populateRow(config, row);
				writer.write(row);
			}
		}
	}

	private void populateRow(BaseServerDatumStreamConfiguration<?, ?> config, String[] row) {
		row[ServerConfigurationsCsvColumn.NODE_ID
				.getCode()] = (config.getNodeId() != null ? config.getNodeId().toString() : null);
		row[ServerConfigurationsCsvColumn.SOURCE_ID.getCode()] = config.getSourceId();
		row[ServerConfigurationsCsvColumn.PROPERTY.getCode()] = config.getProperty();
		row[ServerConfigurationsCsvColumn.TYPE.getCode()] = typeValue(config.getType());
		row[ServerConfigurationsCsvColumn.ENABLED.getCode()] = String.valueOf(config.isEnabled());
		row[ServerConfigurationsCsvColumn.MULTIPLIER.getCode()] = decimalValue(config.getMultiplier());
		row[ServerConfigurationsCsvColumn.OFFSET.getCode()] = decimalValue(config.getOffset());
		row[ServerConfigurationsCsvColumn.DECIMAL_SCALE.getCode()] = numberValue(config.getScale());
	}

	private String typeValue(Enum<? extends CodedValue> type) {
		if ( type == null ) {
			return null;
		}
		if ( type instanceof ControlType t ) {
			return "Control" + t.name();
		}
		return type.name();
	}

	private String decimalValue(BigDecimal n) {
		return (n != null ? n.toPlainString() : null);
	}

	private String numberValue(Number n) {
		return (n != null ? n.toString() : null);
	}

}
