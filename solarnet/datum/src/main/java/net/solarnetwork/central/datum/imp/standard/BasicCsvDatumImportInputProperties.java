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
 * @version 1.0
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

	private Integer instantaneousDataColumn = DEFAULT_INSTANTANEOUS_DATA_COLUMN;
	private Integer accumulatingDataColumn = DEFAULT_ACCUMULATING_DATA_COLUMN;
	private Integer statusDataColumn = DEFAULT_STATUS_DATA_COLUMN;
	private Integer tagDataColumn = DEFAULT_TAG_DATA_COLUMN;

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
			valid = (instantaneousDataColumn != null || accumulatingDataColumn != null
					|| statusDataColumn != null);
		}
		return valid;
	}

	@Override
	public Map<String, Object> toServiceProperties() {
		Map<String, Object> result = super.toServiceProperties();
		if ( instantaneousDataColumn != null ) {
			result.put("instantaneousDataColumn", instantaneousDataColumn);
		}
		if ( accumulatingDataColumn != null ) {
			result.put("accumulatingDataColumn", accumulatingDataColumn);
		}
		if ( statusDataColumn != null ) {
			result.put("statusDataColumn", statusDataColumn);
		}
		if ( tagDataColumn != null ) {
			result.put("tagDataColumn", tagDataColumn);
		}
		return result;
	}

	public Integer getInstantaneousDataColumn() {
		return instantaneousDataColumn;
	}

	public void setInstantaneousDataColumn(Integer instantaneousDataColumn) {
		this.instantaneousDataColumn = instantaneousDataColumn;
	}

	public Integer getAccumulatingDataColumn() {
		return accumulatingDataColumn;
	}

	public void setAccumulatingDataColumn(Integer accumulatingDataColumn) {
		this.accumulatingDataColumn = accumulatingDataColumn;
	}

	public Integer getStatusDataColumn() {
		return statusDataColumn;
	}

	public void setStatusDataColumn(Integer statusDataColumn) {
		this.statusDataColumn = statusDataColumn;
	}

	public Integer getTagDataColumn() {
		return tagDataColumn;
	}

	public void setTagDataColumn(Integer tagDataColumn) {
		this.tagDataColumn = tagDataColumn;
	}

}
