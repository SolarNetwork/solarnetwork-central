/* ==================================================================
 * DatumAuxiliaryBiz.java - 4/02/2019 8:34:35 am
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

package net.solarnetwork.central.datum.biz;

import java.util.List;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * API for manipulating datum auxiliary data.
 * 
 * @author matt
 * @version 1.1
 * @since 1.35
 */
public interface DatumAuxiliaryBiz {

	/**
	 * Get a specific datum auxiliary data.
	 * 
	 * @param id
	 *        the primary key of the data to get
	 * @return the datum, or {@literal null} if not found
	 */
	GeneralNodeDatumAuxiliary getGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id);

	/**
	 * Store datum auxiliary data, replacing any existing auxiliary data with
	 * the provided auxiliary data.
	 * 
	 * @param datum
	 *        the auxiliary data to store
	 */
	void storeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliary datum);

	/**
	 * Move datum auxiliary data, moving any existing auxiliary data to the
	 * provided auxiliary data.
	 * 
	 * <p>
	 * This provides a way to update the primary key of an existing datum and
	 * update its properties in one step. If no datum exists with the primary
	 * key {@code from} then no changes will be performed. Note that {@code to}
	 * must specify the complete set of properties desired in the new datum.
	 * </p>
	 * 
	 * @param from
	 *        the auxiliary primary key to move the data from
	 * @param to
	 *        the auxiliary data to store, including the primary key to store
	 *        the data at
	 * @return {@literal true} if a datum was moved
	 * @since 1.1
	 */
	boolean moveGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK from,
			GeneralNodeDatumAuxiliary to);

	/**
	 * Remove a specific datum auxiliary data.
	 * 
	 * @param id
	 *        the primary key of the data to remove
	 */
	void removeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id);

	/**
	 * Search for datum auxiliary data.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 */
	FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> findGeneralNodeDatumAuxiliary(
			GeneralNodeDatumAuxiliaryFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

}
