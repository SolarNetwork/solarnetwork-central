/* ==================================================================
 * DatumExpireUserEvents.java - 19/04/2026 7:03:40 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.expire.domain;

import java.util.List;
import net.solarnetwork.central.datum.domain.DatumUserEvents;

/**
 * Constants and helpers for datum export and delete user event handling.
 *
 * @author matt
 * @version 1.0
 */
public interface DatumExpireUserEvents extends DatumUserEvents {

	/** A user event tag for datum expire handling. */
	String EXPIRE_TAG = "expire";

	/** A user event tag for datum delete handling. */
	String DELETE_TAG = "delete";

	/** Tags for a datum expire error event. */
	List<String> DATUM_EXPIRE_ERROR_TAGS = List.of(DATUM_TAG, ERROR_TAG, EXPIRE_TAG);

	/** Tags for a datum expire. */
	List<String> DATUM_EXPIRE_TAGS = DATUM_EXPIRE_ERROR_TAGS.stream().filter(t -> !ERROR_TAG.equals(t))
			.toList();

	/** Tags for a datum delete error event. */
	List<String> DATUM_DELETE_ERROR_TAGS = List.of(DATUM_TAG, ERROR_TAG, DELETE_TAG);

	/** Tags for a datum delete. */
	List<String> DATUM_DELETE_TAGS = DATUM_DELETE_ERROR_TAGS.stream().filter(t -> !ERROR_TAG.equals(t))
			.toList();

	/** Tags for a datum delete progress. */
	List<String> DATUM_DELETE_PROGRESS_TAGS = List.of(DATUM_TAG, DELETE_TAG, PROGRESS_TAG);

}
