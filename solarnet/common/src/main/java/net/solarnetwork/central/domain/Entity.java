/* ==================================================================
 * Entity.java - Dec 11, 2009 8:50:35 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

import java.time.Instant;

/**
 * Base domain object API.
 * 
 * @author matt
 * @version 2.0
 */
public interface Entity<PK> extends net.solarnetwork.domain.Identity<PK> {

	/**
	 * Get the date this datum was created.
	 * 
	 * @return the created date
	 */
	public Instant getCreated();

}
