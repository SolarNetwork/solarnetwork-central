/* ==================================================================
 * DatumAuxiliaryEntityDao.java - 28/11/2020 8:40:43 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link DatumAuxiliaryEntity} objects.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumAuxiliaryEntityDao extends GenericDao<DatumAuxiliaryEntity, DatumAuxiliaryPK>,
		FilterableDao<DatumAuxiliary, DatumAuxiliaryPK, DatumAuxiliaryCriteria> {

	/**
	 * Move auxiliary data from one primary key to another.
	 * 
	 * <p>
	 * This essentially performs an update of an existing record, so no changes
	 * will be made if a record with a primary key of {@code from} does not
	 * exist.
	 * </p>
	 * 
	 * @param from
	 *        the primary key to move the data from
	 * @param to
	 *        the data to store, including the primary key to store it at
	 * @return {@literal true} if the record existed and was moved
	 */
	boolean move(DatumAuxiliaryPK from, DatumAuxiliaryEntity to);

}
