/* ==================================================================
 * GeneralObjectDatum.java - 26/02/2024 10:35:10 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumSamplesContainer;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * API for a general object/source/timestamp primary key style entity.
 *
 * @author matt
 * @version 2.1
 */
public interface GeneralObjectDatum<K extends Comparable<K> & Serializable & GeneralObjectDatumKey>
		extends Entity<K>, DatumSamplesContainer, DatumIdentity {

	@JsonIgnore
	@Override
	default ObjectDatumKind getKind() {
		return id().getKind();
	}

	@JsonIgnore
	@Override
	default Long getObjectId() {
		return id().getObjectId();
	}

	@Override
	default String getSourceId() {
		return id().getSourceId();
	}

	@JsonIgnore
	@Override
	default Instant getTimestamp() {
		return id().getTimestamp();
	}

}
