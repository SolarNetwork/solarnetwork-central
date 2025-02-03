/* ==================================================================
 * CsvDatumImportInputProperties.java - 7/11/2018 1:43:29 PM
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

package net.solarnetwork.central.datum.imp.standard;

import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.datum.imp.standard.CsvUtils.parseColumnsReference;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Service properties for CSV based datum import.
 *
 * @author matt
 * @version 1.1
 */
public class CsvDatumImportInputProperties {

	/** The default header row count: {@literal 1}. */
	public static final Integer DEFAULT_HEADER_ROW_COUNT = 1;

	/** The default date format. */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/** The default node ID column: {@literal 1}. */
	public static final Integer DEFAULT_NODE_ID_COLUMN = 1;

	/** The default source ID column: {@literal 2}. */
	public static final Integer DEFAULT_SOURCE_ID_COLUMN = 2;

	/** The default date column number. */
	public static final Integer DEFAULT_DATE_COLUMN = 3;

	/** The default date column; contains just {@literal 3}. */
	public static final List<Integer> DEFAULT_DATE_COLUMNS = Collections
			.singletonList(DEFAULT_DATE_COLUMN);

	private Integer headerRowCount = DEFAULT_HEADER_ROW_COUNT;
	private String dateColumns = DEFAULT_DATE_COLUMN.toString();
	private String dateFormat = DEFAULT_DATE_FORMAT;
	private String nodeIdColumn = DEFAULT_NODE_ID_COLUMN.toString();
	private String sourceIdColumn = DEFAULT_SOURCE_ID_COLUMN.toString();
	private String timeZoneId;

	/**
	 * Get a set of {@link SettingSpecifier} instances suitable for configuring
	 * an instance of this class.
	 *
	 * @return the setting specifiers
	 */
	public static List<SettingSpecifier> getCsvSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		result.add(new BasicTextFieldSettingSpecifier("headerRowCount",
				DEFAULT_HEADER_ROW_COUNT.toString()));
		result.add(new BasicTextFieldSettingSpecifier("dateFormat", DEFAULT_DATE_FORMAT));
		result.add(
				new BasicTextFieldSettingSpecifier("nodeIdColumn", DEFAULT_NODE_ID_COLUMN.toString()));
		result.add(new BasicTextFieldSettingSpecifier("sourceIdColumn",
				DEFAULT_SOURCE_ID_COLUMN.toString()));
		result.add(new BasicTextFieldSettingSpecifier("dateColumnsValue",
				commaDelimitedStringFromCollection(DEFAULT_DATE_COLUMNS)));
		return result;
	}

	/**
	 * Test if the configuration appears valid.
	 *
	 * <p>
	 * This simply tests for non-null property values.
	 * </p>
	 *
	 * @return {@literal true} if the configuration appears valid
	 */
	public boolean isValid() {
		return (isValidColumnsReference(dateColumns) && dateFormat != null
				&& !dateFormat.trim().isEmpty() && isValidColumnsReference(nodeIdColumn)
				&& isValidColumnsReference(sourceIdColumn));
	}

	/**
	 * Test if a value is a valid columns reference.
	 *
	 * @param value
	 *        the value to test
	 * @return {@literal true} if {@code value} is a valid columns reference
	 */
	protected static boolean isValidColumnsReference(String value) {
		return CsvUtils.parseColumnsReference(value) != null;
	}

	/**
	 * Create a map of service properties from this instance.
	 *
	 * @return a map of service properties, never {@literal null}
	 */
	public Map<String, Object> toServiceProperties() {
		Map<String, Object> result = new LinkedHashMap<>(8);
		if ( headerRowCount != null ) {
			result.put("headerRowCount", headerRowCount);
		}
		if ( dateColumns != null && !dateColumns.isEmpty() ) {
			result.put("dateColumnsValue", getDateColumnsValue());
		}
		if ( dateFormat != null ) {
			result.put("dateFormat", dateFormat);
		}
		if ( nodeIdColumn != null ) {
			result.put("nodeIdColumn", nodeIdColumn);
		}
		if ( sourceIdColumn != null ) {
			result.put("sourceIdColumn", sourceIdColumn);
		}
		return result;
	}

	public Integer getHeaderRowCount() {
		return headerRowCount;
	}

	public void setHeaderRowCount(Integer headerRowCount) {
		this.headerRowCount = headerRowCount;
	}

	public List<Integer> getDateColumns() {
		Set<String> set = StringUtils.commaDelimitedStringToSet(dateColumns);
		List<Integer> cols;
		try {
			cols = set.stream().flatMap(s -> parseColumnsReference(s).stream()).collect(toList());
		} catch ( NumberFormatException e ) {
			// ignore
			cols = DEFAULT_DATE_COLUMNS;
		}
		return cols;
	}

	public void setDateColumns(List<Integer> dateColumnPositions) {
		setDateColumnsValue(StringUtils.commaDelimitedStringFromCollection(dateColumnPositions));
	}

	public String getDateColumnsValue() {
		return dateColumns;
	}

	public void setDateColumnsValue(String dateColumns) {
		this.dateColumns = dateColumns;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public Integer nodeIdColumn() {
		try {
			return CsvUtils.parseColumnReference(nodeIdColumn);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			return null;
		}
	}

	public String getNodeIdColumn() {
		return nodeIdColumn;
	}

	public void setNodeIdColumn(String nodeIdColumn) {
		this.nodeIdColumn = nodeIdColumn;
	}

	public Integer sourceIdColumn() {
		try {
			return CsvUtils.parseColumnReference(sourceIdColumn);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			return null;
		}
	}

	public String getSourceIdColumn() {
		return sourceIdColumn;
	}

	public void setSourceIdColumn(String sourceIdColumn) {
		this.sourceIdColumn = sourceIdColumn;
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
