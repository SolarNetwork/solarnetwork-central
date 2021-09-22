/* ==================================================================
 * BaseObjectEntity.java - 27/08/2017 2:47:45 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base implementation of {@link Entity} using a comparable, serializable
 * primary key.
 * 
 * @author matt
 * @version 2.0
 * @since 1.34
 */
public class BaseObjectEntity<PK extends Comparable<PK> & Serializable> extends BaseObjectIdentity<PK>
		implements Cloneable, Serializable, Entity<PK> {

	private static final long serialVersionUID = 6151623706137372281L;

	private Instant created = null;
	private Instant modified = null;

	/**
	 * Get the creation date.
	 * 
	 * @return the created
	 */
	@Override
	public Instant getCreated() {
		return created;
	}

	/**
	 * Set the creation date.
	 * 
	 * @param created
	 *        the created to set
	 */
	public void setCreated(Instant created) {
		this.created = created;
	}

	/**
	 * Get the modification date.
	 * 
	 * @return the modification date
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 * 
	 * @param modified
	 *        the modification date to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

}
