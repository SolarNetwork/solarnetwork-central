/* ==================================================================
 * SolarNetCloudIntegrationsConfiguration.java - 30/09/2024 11:48:02 am
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

package net.solarnetwork.central.c2c.config;

/**
 * Marker interface for the cloud integrations configuration package.
 *
 * @author matt
 * @version 1.0
 */
public interface SolarNetCloudIntegrationsConfiguration {

	/** A qualifier for cloud integrations support. */
	String CLOUD_INTEGRATIONS = "c2c";

	/** A qualifier for cloud integrations expression support. */
	String CLOUD_INTEGRATIONS_EXPRESSIONS = "c2c-expr";

	/** A qualifier for cloud integrations polling support. */
	String CLOUD_INTEGRATIONS_POLL = "c2c-poll";

}