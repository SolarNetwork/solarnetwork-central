/* ==================================================================
 * OwnedGeneralNodeDatum.java - 11/10/2022 7:55:41 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Extension of {@link GeneralNodeDatum} with account ownership information.
 *
 * @author matt
 * @version 1.0
 */
public class OwnedGeneralNodeDatum extends GeneralNodeDatum {

	@Serial
	private static final long serialVersionUID = 8431178252197830478L;

	private final Long userId;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param userId
	 *        the user (owner) ID
	 */
	public OwnedGeneralNodeDatum(GeneralNodeDatumPK id, Long userId) {
		super(id);
		this.userId = requireNonNullArgument(userId, "userId");
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
	 * @param userId
	 *        the user (owner) ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public OwnedGeneralNodeDatum(@JsonProperty("nodeId") Long nodeId,
			@JsonProperty("created") Instant created, @JsonProperty("sourceId") String sourceId,
			@JsonProperty("userId") Long userId) {
		this(new GeneralNodeDatumPK(nodeId, created, sourceId), userId);
	}

	/**
	 * Get the user ID.
	 *
	 * @return the userId
	 */
	@JsonIgnore
	public Long getUserId() {
		return userId;
	}

	@Override
	public OwnedGeneralNodeDatum copyWithId(GeneralNodeDatumPK id) {
		OwnedGeneralNodeDatum copy = new OwnedGeneralNodeDatum(id, userId);
		copyTo(copy);
		return copy;
	}

}
