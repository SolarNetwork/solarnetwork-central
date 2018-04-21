/* ==================================================================
 * DatumExportRequest.java - 21/04/2018 2:36:13 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.domain;

import java.util.UUID;
import org.joda.time.DateTime;
import net.solarnetwork.domain.Identity;

/**
 * API for a {@link Configuration} associated with an identity.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumExportRequest extends Identity<UUID> {

	/**
	 * Get the configuration associated with this entity.
	 * 
	 * @return the configuration
	 */
	Configuration getConfiguration();

	/**
	 * Get the data export starting date.
	 * 
	 * @return the export date
	 */
	DateTime getExportDate();

}
