/* ==================================================================
 * BasicDestinationConfiguration.java - 21/03/2018 11:23:25 AM
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

package net.solarnetwork.central.datum.domain.export;

import java.io.Serializable;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;

/**
 * Basic implementation of {@link DestinationConfiguration}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDestinationConfiguration extends BasicIdentifiableConfiguration
		implements DestinationConfiguration, Serializable {

	private static final long serialVersionUID = 7134357946005308817L;

}
