/* ==================================================================
 * TaxCodeDao.java - 24/07/2020 6:27:38 AM
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

package net.solarnetwork.central.user.billing.snf.dao;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import java.util.List;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link TaxCode} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface TaxCodeDao extends FilterableDao<TaxCode, Long, TaxCodeFilter> {

	/**
	 * Sort descriptors to sort by tax code in ascending order, followed by ID
	 * in ascending order.
	 */
	// @formatter:off
	List<SortDescriptor> SORT_BY_ITEM_KEY = unmodifiableList(asList(
					new SimpleSortDescriptor(TaxCodeSortKey.CODE.toString()),
					new SimpleSortDescriptor(GenericDao.StandardSortKey.ID.toString())));
	// @formatter:on

	/**
	 * A sort key enumeration for {@link TaxCode} queries.
	 */
	enum TaxCodeSortKey {

		CODE,

	}

}
