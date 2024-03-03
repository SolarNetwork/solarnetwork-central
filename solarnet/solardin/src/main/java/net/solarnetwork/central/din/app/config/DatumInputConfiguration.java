/* ==================================================================
 * DatumInputConfiguration.java - 24/02/2024 4:11:38 pm
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

package net.solarnetwork.central.din.app.config;

/**
 * Marker interface for datum input configuration.
 *
 * @author matt
 * @version 1.0
 */
public interface DatumInputConfiguration {

	/** A qualifier for endpoint configuration. */
	String ENDPOINT_CONF = "endpoint-conf";

	/** A qualifier for transform configuration. */
	String TRANSFORM_CONF = "transform-conf";

	/** A qualifier for caching support. */
	String CACHING = "caching";

	/** A qualifier for datum support. */
	String DATUM = "datum";

	/** A qualifier to use for datum buffer objects. */
	String DATUM_BUFFER = DATUM + "-buffer";

}
