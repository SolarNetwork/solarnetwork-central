/* ==================================================================
 * DatumImportUserEvents.java - 19/04/2026 7:03:40 am
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

package net.solarnetwork.central.datum.imp.domain;

import java.util.List;
import net.solarnetwork.central.datum.domain.DatumUserEvents;

/**
 * Constants and helpers for datum import user event handling.
 *
 * @author matt
 * @version 1.0
 */
public interface DatumImportUserEvents extends DatumUserEvents {

	/** A user event tag for datum import handling. */
	String IMPORT_TAG = "import";

	/** User event data key for a transaction mode. */
	String TRANSACTION_MODE_DATA_KEY = "txMode";

	/** Tags for a datum import error event. */
	List<String> DATUM_IMPORT_ERROR_TAGS = List.of(DATUM_TAG, ERROR_TAG, IMPORT_TAG);

	/** Tags for a datum import. */
	List<String> DATUM_IMPORT_TAGS = DATUM_IMPORT_ERROR_TAGS.stream().filter(t -> !ERROR_TAG.equals(t))
			.toList();

	/** Tags for a datum import progress event. */
	List<String> DATUM_IMPORT_PROGRESS_TAGS = List.of(DATUM_TAG, IMPORT_TAG, PROGRESS_TAG);

}
