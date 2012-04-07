/* ==================================================================
 * GenericDao.java - Dec 11, 2009 8:40:05 PM
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.util.List;

import net.solarnetwork.central.domain.Entity;

/**
 * Generic DAO API.
 * 
 * <p>Based in part on 
 * http://www-128.ibm.com/developerworks/java/library/j-genericdao.html.</p>
 * 
 * @param <T> the domain object type
 * @param <PK> the primary key type
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public interface GenericDao<T extends Entity<PK>, PK extends Serializable> {

	/**
	 * Get the class supported by this Dao.
	 * 
	 * @return class
	 */
	Class<? extends T> getObjectType();

   /**
     * Persist the domainObject object into database, 
     * creating or updating as appropriate.
     * 
     * @param domainObject the domain object so store
     * @return the primary key of the stored object
     */
    PK store(T domainObject);

    /** 
     * Get a persisted domain object by its primary key.
     * @param id the primary key to retrieve
     * @return the domain object
     */
    T get(PK id);

    /**
     * Get a list of persisted domain objects, optionally sorted in some way.
     * 
     * <p>The <code>sortDescriptors</code> parameter can be <em>null</em>, in
     * which case the sort order is not defined and implementation specific.</p>
     * 
     * @param sortDescriptors list of sort descriptors to sort the results by
     * @return list of all persisted domain objects, or empty list if none available
     * @since 1.2
     */
    List<T> getAll(List<SortDescriptor> sortDescriptors);
    
    /** 
     * Remove an object from persistent storage in the database.
     * @param domainObject the domain object to delete
     */
    void delete(T domainObject);
    
}
