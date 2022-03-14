/* ==================================================================
 * MyBatisTaxCodeDao.java - 24/07/2020 6:38:41 AM
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis;

import java.util.List;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link TaxCodeDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisTaxCodeDao extends BaseMyBatisGenericDaoSupport<TaxCode, Long>
		implements TaxCodeDao {

	/** Query name enumeration. */
	public enum QueryName {

		FindFiltered("find-TaxCode-for-filter");

		private final String queryName;

		private QueryName(String queryName) {
			this.queryName = queryName;
		}

		/**
		 * Get the query name.
		 * 
		 * @return the query name
		 */
		public String getQueryName() {
			return queryName;
		}
	}

	/**
	 * Constructor.
	 */
	public MyBatisTaxCodeDao() {
		super(TaxCode.class, Long.class);
	}

	@Override
	public FilterResults<TaxCode, Long> findFiltered(TaxCodeFilter filter, List<SortDescriptor> sorts,
			Integer offset, Integer max) {
		List<TaxCode> results = selectList(QueryName.FindFiltered.getQueryName(), filter, offset, max);
		return new BasicFilterResults<>(results, null, offset != null ? offset.intValue() : 0,
				results.size());
	}

}
