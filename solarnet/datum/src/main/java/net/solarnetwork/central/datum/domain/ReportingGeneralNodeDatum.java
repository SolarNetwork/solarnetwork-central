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

import java.io.Serial;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Extension of {@link GeneralNodeDatum} with some additional properties geared
 * towards reporting.
 *
 * @author matt
 * @version 2.0
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "localDate", "localTime" })
public class ReportingGeneralNodeDatum extends GeneralNodeDatum
		implements ReportingGeneralNodeDatumMatch {

	@Serial
	private static final long serialVersionUID = -8529409354188959691L;

	private @Nullable LocalDateTime localDateTime;

	/**
	 * Constructor.
	 *
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ReportingGeneralNodeDatum(GeneralNodeDatumPK id) {
		super(id);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public ReportingGeneralNodeDatum(@JsonProperty("nodeId") Long nodeId,
			@JsonProperty("created") Instant created, @JsonProperty("sourceId") String sourceId) {
		super(nodeId, created, sourceId);
	}

	@Override
	public final @Nullable LocalDate getLocalDate() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalDate();
	}

	@JsonFormat(pattern = "HH:mm")
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
