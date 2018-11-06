/* ==================================================================
 * InputConfiguration.java - 6/11/2018 4:39:57 PM
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

package net.solarnetwork.central.datum.imp.domain;

import net.solarnetwork.domain.IdentifiableConfiguration;

/**
 * An input format configuration object for a datum import task.
 * 
 * <p>
 * This API defines the format that the data to import is in. The associated
 * {@link IdentifiableConfiguration#getServiceIdentifier()} associates the
 * configuration with a service that understands how to
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface InputConfiguration extends IdentifiableConfiguration {

	/**
	 * A time zone to apply to imported data.
	 * 
	 * <p>
	 * The data to import might have dates encoded without any time zone
	 * information. This property can be used to apply a single time zone to all
	 * data imported.
	 * </p>
	 * 
	 * @return the time zone to apply to all imported data, or {@literal null}
	 *         if the data already has time zone information
	 */
	String getTimeZoneId();

}
