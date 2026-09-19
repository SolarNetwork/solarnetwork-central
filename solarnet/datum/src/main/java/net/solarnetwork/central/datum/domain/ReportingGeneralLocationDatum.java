/* ==================================================================
 * ReportingGeneralLocationDatum.java - Oct 17, 2014 2:27:14 PM
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

import java.io.Serial;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Extension of {@link GeneralLocationDatum} with some additional properties
 * geared towards reporting.
 *
 * @author matt
 * @version 2.0
 */
@JsonPropertyOrder({ "created", "locationId", "sourceId", "localDate", "localTime" })
public class ReportingGeneralLocationDatum extends GeneralLocationDatum
		implements ReportingGeneralLocationDatumMatch {

	@Serial
	private static final long serialVersionUID = 2701352841771399263L;

	private @Nullable LocalDateTime localDateTime;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ReportingGeneralLocationDatum(GeneralLocationDatumPK id) {
		super(id);
	}

	/**
	 * Constructor.
	 *
	 * @param locationId
	 *        the location ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public ReportingGeneralLocationDatum(@JsonProperty("locationId") Long locationId,
			@JsonProperty("created") Instant created, @JsonProperty("sourceId") String sourceId) {
		super(locationId, created, sourceId);
	}

	@Override
	public final @Nullable LocalDate getLocalDate() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalDate();
	}

	@Override
	public final @Nullable LocalTime getLocalTime() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalTime();
	}

	@JsonIgnore
	@SerializeIgnore
	public final @Nullable LocalDateTime getLocalDateTime() {
		return localDateTime;
	}

	public final void setLocalDateTime(@Nullable LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}

}
