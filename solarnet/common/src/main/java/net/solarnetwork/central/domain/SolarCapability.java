/* ==================================================================
 * SolarCapability.java - Jun 6, 2011 1:34:53 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.domain;

import java.io.Serializable;
import net.solarnetwork.central.dao.BaseEntity;

/**
 * A set of capabilities.
 * 
 * @author matt
 * @version $Revision$
 */
public class SolarCapability extends BaseEntity implements Cloneable, Serializable {

	private static final long serialVersionUID = 1449569875028595935L;

	private Long generationCapacityWatts;
	private Long storageCapacityWattHours;

	public Long getGenerationCapacityWatts() {
		return generationCapacityWatts;
	}
	public void setGenerationCapacityWatts(Long generationCapacityWatts) {
		this.generationCapacityWatts = generationCapacityWatts;
	}
	public Long getStorageCapacityWattHours() {
		return storageCapacityWattHours;
	}
	public void setStorageCapacityWattHours(Long storageCapacityWattHours) {
		this.storageCapacityWattHours = storageCapacityWattHours;
	}

}
