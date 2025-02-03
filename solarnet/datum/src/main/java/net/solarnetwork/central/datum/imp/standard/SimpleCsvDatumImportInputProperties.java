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
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.IntRangeSet;

/**
 * Service properties for simple CSV based datum import.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleCsvDatumImportInputProperties extends CsvDatumImportInputProperties {

	private String instantaneousDataColumns;
	private String accumulatingDataColumns;
	private String statusDataColumns;
	private String tagDataColumns;

	/**
	 * Get settings for configuring an instance of this class.
	 *
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> getSimpleCsvSettingSpecifiers() {
		List<SettingSpecifier> result = CsvDatumImportInputProperties.getCsvSettingSpecifiers();

		result.add(new BasicTextFieldSettingSpecifier("instantaneousDataColumns", ""));
		result.add(new BasicTextFieldSettingSpecifier("accumulatingDataColumns", ""));
		result.add(new BasicTextFieldSettingSpecifier("statusDataColumns", ""));
		result.add(new BasicTextFieldSettingSpecifier("tagDataColumns", ""));

		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleCsvDatumImportInputProperties{");
		if ( getNodeIdColumn() != null ) {
			builder.append("node=");
			builder.append(getNodeIdColumn());
			builder.append(", ");
		}
		if ( getSourceIdColumn() != null ) {
			builder.append("source=");
			builder.append(getSourceIdColumn());
			builder.append(", ");
		}
		if ( getDateColumns() != null ) {
			builder.append("date=");
			builder.append(getDateColumns());
			builder.append(", ");
		}
		if ( instantaneousDataColumns != null ) {
			builder.append("i=");
			builder.append(instantaneousDataColumns);
			builder.append(", ");
		}
		if ( accumulatingDataColumns != null ) {
			builder.append("a=");
			builder.append(accumulatingDataColumns);
			builder.append(", ");
		}
		if ( statusDataColumns != null ) {
			builder.append("s=");
			builder.append(statusDataColumns);
			builder.append(", ");
		}
		if ( tagDataColumns != null ) {
			builder.append("t=");
			builder.append(tagDataColumns);
			builder.append(", ");
		}
		if ( getHeaderRowCount() != null ) {
			builder.append("headerRowCount=");
			builder.append(getHeaderRowCount());
			builder.append(", ");
		}
		if ( getDateFormat() != null ) {
			builder.append("dateFormat=");
			builder.append(getDateFormat());
			builder.append(", ");
		}
		if ( getTimeZoneId() != null ) {
			builder.append("timeZoneId=");
			builder.append(getTimeZoneId());
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public boolean isValid() {
		boolean valid = super.isValid();
		if ( valid ) {
			// at least 1 header row and at least one data column must be present
			valid = getHeaderRowCount() != null && getHeaderRowCount() > 0
					&& (isValidColumnsReference(instantaneousDataColumns)
							|| isValidColumnsReference(accumulatingDataColumns)
							|| isValidColumnsReference(statusDataColumns));
		}
		return valid;
	}

	@Override
	public Map<String, Object> toServiceProperties() {
		Map<String, Object> result = super.toServiceProperties();
		if ( isValidColumnsReference(instantaneousDataColumns) ) {
			result.put("instantaneousDataColumns", instantaneousDataColumns);
		}
		if ( isValidColumnsReference(accumulatingDataColumns) ) {
			result.put("accumulatingDataColumns", accumulatingDataColumns);
		}
		if ( isValidColumnsReference(statusDataColumns) ) {
			result.put("statusDataColumns", statusDataColumns);
		}
		if ( isValidColumnsReference(tagDataColumns) ) {
			result.put("tagDataColumns", tagDataColumns);
		}
		return result;
	}

	/**
	 * Get the set of columns configured for a given type.
	 *
	 * @param type
	 *        the type of column to get the set for
	 * @return the set, or {@literal null} if nothing configured for that type
	 */
	public IntRangeSet columnsForType(DatumSamplesType type) {
		String refs = switch (type) {
			case Instantaneous -> getInstantaneousDataColumns();
			case Accumulating -> getAccumulatingDataColumns();
			case Status -> getStatusDataColumns();
			case Tag -> getTagDataColumns();
		};
		return CsvUtils.parseColumnsReference(refs);
	}

	public String getInstantaneousDataColumns() {
		return instantaneousDataColumns;
	}

	public void setInstantaneousDataColumns(String instantaneousDataColumns) {
		this.instantaneousDataColumns = instantaneousDataColumns;
	}

	public String getAccumulatingDataColumns() {
		return accumulatingDataColumns;
	}

	public void setAccumulatingDataColumns(String accumulatingDataColumns) {
		this.accumulatingDataColumns = accumulatingDataColumns;
	}

	public String getStatusDataColumns() {
		return statusDataColumns;
	}

	public void setStatusDataColumns(String statusDataColumns) {
		this.statusDataColumns = statusDataColumns;
	}

	public String getTagDataColumns() {
		return tagDataColumns;
	}

	public void setTagDataColumns(String tagDataColumns) {
		this.tagDataColumns = tagDataColumns;
	}

}
