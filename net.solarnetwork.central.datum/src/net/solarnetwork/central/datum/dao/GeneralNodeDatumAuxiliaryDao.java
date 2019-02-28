/* ==================================================================
 * GeneralNodeDatumAuxiliaryDao.java - 4/02/2019 7:25:28 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;

/**
 * DAO API for {@link GeneralNodeDatumAuxiliary} entities.
 * 
 * @author matt
 * @version 1.1
 */
public interface GeneralNodeDatumAuxiliaryDao
		extends GenericDao<GeneralNodeDatumAuxiliary, GeneralNodeDatumAuxiliaryPK>,
		FilterableDao<GeneralNodeDatumAuxiliaryFilterMatch, GeneralNodeDatumAuxiliaryPK, GeneralNodeDatumAuxiliaryFilter> {

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
	 * @since 1.1
	 */
	boolean move(GeneralNodeDatumAuxiliaryPK from, GeneralNodeDatumAuxiliary to);

}
