/* ==================================================================
 * BaseStringEntity.java - Dec 12, 2012 1:34:58 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.domain.BaseStringIdentity;
import net.solarnetwork.dao.Entity;

/**
 * Base class for SolarNetwork entities using string primary keys.
 * 
 * @author matt
 * @version 2.0
 */
public class BaseStringEntity extends BaseStringIdentity
		implements Entity<String>, Cloneable, Serializable {

	private static final long serialVersionUID = 5907827905456392556L;

	private Instant created = null;

	@Override
	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

}
