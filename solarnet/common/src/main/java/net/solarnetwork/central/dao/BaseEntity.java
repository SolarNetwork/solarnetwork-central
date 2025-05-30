/* ==================================================================
 * BaseEntity.java - Feb 20, 2011 2:31:11 PM
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
 */

package net.solarnetwork.central.dao;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import net.solarnetwork.central.domain.BaseIdentity;
import net.solarnetwork.dao.Entity;

/**
 * Base class for SolarNetwork entities.
 *
 * @author matt
 * @version 2.0
 */
public abstract class BaseEntity<T extends BaseEntity<T>> extends BaseIdentity<T>
		implements Entity<T, Long>, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 6006487859490874703L;

	private Instant created = null;

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

}
