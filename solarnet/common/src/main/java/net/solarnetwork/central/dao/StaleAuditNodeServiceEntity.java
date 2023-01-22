/* ==================================================================
 * StaleAuditNodeServiceEntity.java - 22/01/2023 3:11:26 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.time.Instant;
import net.solarnetwork.central.domain.AggregateDatumId;
import net.solarnetwork.central.domain.StaleAuditNodeServiceValue;
import net.solarnetwork.dao.BasicEntity;

/**
 * Stale audit node service entity.
 * 
 * @author matt
 * @version 1.0
 */
public class StaleAuditNodeServiceEntity extends BasicEntity<AggregateDatumId>
		implements StaleAuditNodeServiceValue, Cloneable, Serializable {

	private static final long serialVersionUID = -3150400939940353987L;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public StaleAuditNodeServiceEntity(AggregateDatumId id, Instant created) {
		super(id, created);
	}

	@Override
	public StaleAuditNodeServiceEntity clone() {
		return (StaleAuditNodeServiceEntity) super.clone();
	}

}
