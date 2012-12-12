/* ==================================================================
 * DrasIbatisFilterableDaoSupport.java - Jun 8, 2011 6:44:15 PM
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

package net.solarnetwork.central.dao.ibatis;

import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterMatch;

/**
 * Abstract base iBATIS FilterableDao for the common case of Long primary keys.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class IbatisFilterableDaoSupport<T extends Entity<Long>, M extends FilterMatch<Long>, F extends Filter>
		extends IbatisBaseFilterableDaoSupport<T, M, F, Long> {

	/**
	 * Constructor.
	 * 
	 * @param domainClass
	 *        the domain class
	 * @param filterResultClass
	 *        the filter result class
	 */
	public IbatisFilterableDaoSupport(Class<? extends T> domainClass,
			Class<? extends M> filterResultClass) {
		super(domainClass, Long.class, filterResultClass);
	}

}
