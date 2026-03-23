/* ==================================================================
 * BasicCsvDatumImportInputProperties.java - 7/11/2018 8:07:27 PM
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

import java.util.List;
import java.util.Map;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Service properties for basic CSV based datum import.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicCsvDatumImportInputProperties extends CsvDatumImportInputProperties {

	/** The default instantaneous JSON column: {@literal 4}. */
	public static final Integer DEFAULT_INSTANTANEOUS_DATA_COLUMN = 4;

	/** The default accumulating JSON column: {@literal 5}. */
	public static final Integer DEFAULT_ACCUMULATING_DATA_COLUMN = 5;

	/** The default status JSON column: {@literal 6}. */
	public static final Integer DEFAULT_STATUS_DATA_COLUMN = 6;

	/** The default tag JSON column: {@literal 7}. */
	public static final Integer DEFAULT_TAG_DATA_COLUMN = 7;

	private String instantaneousDataColumn = DEFAULT_INSTANTANEOUS_DATA_COLUMN.toString();
	private String accumulatingDataColumn = DEFAULT_ACCUMULATING_DATA_COLUMN.toString();
	private String statusDataColumn = DEFAULT_STATUS_DATA_COLUMN.toString();
	private String tagDataColumn = DEFAULT_TAG_DATA_COLUMN.toString();

	public static List<SettingSpecifier> getBasicCsvSettingSpecifiers() {
		List<SettingSpecifier> result = CsvDatumImportInputProperties.getCsvSettingSpecifiers();

		result.add(new BasicTextFieldSettingSpecifier("instantaneousDataColumn",
				DEFAULT_INSTANTANEOUS_DATA_COLUMN.toString()));
		result.add(new BasicTextFieldSettingSpecifier("accumulatingDataColumn",
				DEFAULT_ACCUMULATING_DATA_COLUMN.toString()));
		result.add(new BasicTextFieldSettingSpecifier("statusDataColumn",
				DEFAULT_STATUS_DATA_COLUMN.toString()));
		result.add(
				new BasicTextFieldSettingSpecifier("tagDataColumn", DEFAULT_TAG_DATA_COLUMN.toString()));

		return result;
	}

	@Override
	public boolean isValid() {
		boolean valid = super.isValid();
		if ( valid ) {
			// at least one data column must be present
			valid = (isValidColumnsReference(instantaneousDataColumn)
					|| isValidColumnsReference(accumulatingDataColumn)
					|| isValidColumnsReference(statusDataColumn));
		}
		return valid;
	}

	@Override
	public Map<String, Object> toServiceProperties() {
		Map<String, Object> result = super.toServiceProperties();
		if ( isValidColumnsReference(instantaneousDataColumn) ) {
			result.put("instantaneousDataColumn", instantaneousDataColumn);
		}
		if ( isValidColumnsReference(accumulatingDataColumn) ) {
			result.put("accumulatingDataColumn", accumulatingDataColumn);
		}
		if ( isValidColumnsReference(statusDataColumn) ) {
			result.put("statusDataColumn", statusDataColumn);
		}
		if ( isValidColumnsReference(tagDataColumn) ) {
			result.put("tagDataColumn", tagDataColumn);
		}
		return result;
	}

	public Integer instantaneousDataColumn() {
		try {
			return CsvUtils.parseColumnReference(instantaneousDataColumn);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			return null;
		}
	}

	public String getInstantaneousDataColumn() {
		return instantaneousDataColumn;
	}

	public void setInstantaneousDataColumn(String instantaneousDataColumn) {
		this.instantaneousDataColumn = instantaneousDataColumn;
	}

	public Integer accumulatingDataColumn() {
		try {
			return CsvUtils.parseColumnReference(accumulatingDataColumn);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			return null;
		}
	}

	public String getAccumulatingDataColumn() {
		return accumulatingDataColumn;
	}

	public void setAccumulatingDataColumn(String accumulatingDataColumn) {
		this.accumulatingDataColumn = accumulatingDataColumn;
	}

	public Integer statusDataColumn() {
		try {
			return CsvUtils.parseColumnReference(statusDataColumn);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			return null;
		}
	}

	public String getStatusDataColumn() {
		return statusDataColumn;
	}

	public void setStatusDataColumn(String statusDataColumn) {
		this.statusDataColumn = statusDataColumn;
	}

	public Integer tagDataColumn() {
		try {
			return CsvUtils.parseColumnReference(tagDataColumn);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			return null;
		}
	}

	public String getTagDataColumn() {
		return tagDataColumn;
	}

	public void setTagDataColumn(String tagDataColumn) {
		this.tagDataColumn = tagDataColumn;
	}

}
