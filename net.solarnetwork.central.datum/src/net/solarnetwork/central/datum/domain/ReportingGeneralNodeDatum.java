/* ==================================================================
 * ReportingGeneralNodeDatum.java - Aug 22, 2014 7:07:49 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import java.util.Map;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.SerializeIgnore;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

/**
 * Extension of {@link GeneralNodeDatum} with some additional properties geared
 * towards reporting.
 * 
 * @author matt
 * @version 1.0
 */
public class ReportingGeneralNodeDatum extends GeneralNodeDatum implements
		ReportingGeneralNodeDatumMatch {

	private static final long serialVersionUID = 7232170887492262841L;

	private LocalDateTime localDateTime;
	private Map<String, Object> sampleData;

	@Override
	public LocalDate getLocalDate() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalDate();
	}

	@Override
	public LocalTime getLocalTime() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalTime();
	}

	@JsonIgnore
	@SerializeIgnore
	public LocalDateTime getLocalDateTime() {
		return localDateTime;
	}

	public void setLocalDateTime(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}

	/**
	 * Returns the value of {link {@link #getSampleJson()} directly as a Map.
	 * For reporting data, the JSON is flattened so we don't have an actual
	 * {@link GeneralNodeDatumSamples} object to work with.
	 * 
	 * @return the sample data, or <em>null</em> if none available
	 */
	@Override
	@SuppressWarnings("unchecked")
	@JsonUnwrapped
	@JsonAnyGetter
	public Map<String, ?> getSampleData() {
		if ( sampleData == null && getSampleJson() != null ) {
			try {
				sampleData = OBJECT_MAPPER.readValue(getSampleJson(), Map.class);
			} catch ( Exception e ) {
				LOG.error("Exception unmarshalling sampleJson {}", getSampleJson(), e);
			}
		}
		return sampleData;
	}

}
