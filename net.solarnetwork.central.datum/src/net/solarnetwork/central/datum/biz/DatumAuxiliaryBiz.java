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
import net.solarnetwork.central.domain.SortDescriptor;

/**
 * API for manipulating datum auxiliary data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
public interface DatumAuxiliaryBiz {

	/**
	 * Store datum auxiliary data, replacing any existing auxiliary data with
	 * the provided auxiliary data.
	 * 
	 * @param datum
	 *        the auxiliary data to store
	 */
	void storeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliary datum);

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
	FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> findGeneralNodeDatumMetadata(
			GeneralNodeDatumAuxiliaryFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

}
