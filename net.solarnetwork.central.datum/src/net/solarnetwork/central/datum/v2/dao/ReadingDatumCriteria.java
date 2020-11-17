/* ==================================================================
 * ReadingDatumCriteria.java - 17/11/2020 7:43:19 am
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

import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.LocalDateRangeCriteria;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.SortCriteria;

/**
 * Search criteria for datum reading results.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ReadingDatumCriteria extends DateRangeCriteria, LocalDateRangeCriteria,
		NodeMetadataCriteria, LocationMetadataCriteria, UserCriteria, AggregationCriteria,
		OptimizedQueryCriteria, PaginationCriteria, SortCriteria {

	/**
	 * Get the datum reading type.
	 * 
	 * @return the type
	 */
	DatumReadingType getReadingType();

}
