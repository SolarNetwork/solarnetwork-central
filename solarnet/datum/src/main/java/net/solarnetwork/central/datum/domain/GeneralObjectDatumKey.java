/* ==================================================================
 * GeneralObjectDatumKey.java - 26/02/2024 10:38:05 am
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
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A general datum key, suitable for node and location datum.
 *
 * @author matt
 * @version 1.0
 */
public interface GeneralObjectDatumKey extends Cloneable, Serializable {

	/**
	 * Get the object kind.
	 *
	 * @return the object kind
	 */
	ObjectDatumKind getKind();

	/**
	 * Get a domain-specific ID related to the object kind.
	 *
	 * @return the object ID, or {@literal null}
	 */
	Long getObjectId();

	/**
	 * Get the date this datum is associated with, which is often equal to
	 * either the date it was persisted or the date the associated data in this
	 * object was captured.
	 *
	 * @return the timestamp
	 */
	Instant getTimestamp();

	/**
	 * Get a unique source ID for this datum.
	 *
	 * <p>
	 * A single datum type may collect data from many different sources.
	 * </p>
	 *
	 * @return the source ID
	 */
	String getSourceId();

}
