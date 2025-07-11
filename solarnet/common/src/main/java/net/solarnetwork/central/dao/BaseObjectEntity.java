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

package net.solarnetwork.central.dao;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import net.solarnetwork.central.domain.BaseObjectIdentity;
import net.solarnetwork.dao.Entity;

/**
 * Base implementation of {@link Entity} using a comparable, serializable
 * primary key.
 *
 * @author matt
 * @version 3.0
 * @since 1.34
 */
public class BaseObjectEntity<K extends Comparable<K> & Serializable> extends BaseObjectIdentity<K>
		implements Cloneable, Serializable, Entity<K> {

	@Serial
	private static final long serialVersionUID = 3752078598919814010L;

	private Instant created = null;
	private Instant modified = null;

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
