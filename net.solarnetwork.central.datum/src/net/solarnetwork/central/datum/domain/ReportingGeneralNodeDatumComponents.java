/* ==================================================================
 * ReportingGeneralNodeDatumComponents.java - 14/11/2018 9:44:34 AM
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

package net.solarnetwork.central.datum.domain;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Extension of {@link GeneralNodeDatumComponents} with some additional
 * properties geared towards reporting.
 * 
 * @author matt
 * @version 1.0
 * @since 1.30
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "localDate", "localTime" })
public class ReportingGeneralNodeDatumComponents extends GeneralNodeDatumComponents
		implements ReportingGeneralNodeDatumMatch {

	private static final long serialVersionUID = -7585151200343112934L;

	private LocalDateTime localDateTime;

	/**
	 * Default constructor.
	 */
	public ReportingGeneralNodeDatumComponents() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the datum to copy
	 */
	public ReportingGeneralNodeDatumComponents(GeneralNodeDatum other) {
		super(other);
	}

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
}
