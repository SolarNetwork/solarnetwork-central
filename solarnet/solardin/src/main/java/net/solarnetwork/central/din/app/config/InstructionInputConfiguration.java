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
 * Marker interface for instruction input configuration.
 *
 * @author matt
 * @version 1.0
 */
public interface InstructionInputConfiguration extends SolarDinAppConfiguration {

	/** A qualifier for endpoint configuration. */
	String INSTR_ENDPOINT_CONF = "instr-endpoint-conf";

	/** A qualifier for request transform configuration. */
	String REQ_TRANSFORM_CONF = "req-transform-conf";

	/** A qualifier for response transform configuration. */
	String RES_TRANSFORM_CONF = "res-transform-conf";

	/** A qualifier for user metadata. */
	String USER_METADATA = "user-metadata";

	/** A qualifier for user metadata path. */
	String USER_METADATA_PATH = "user-metadata-path";

}
