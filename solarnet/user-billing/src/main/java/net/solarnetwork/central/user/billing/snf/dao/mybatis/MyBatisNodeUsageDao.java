/* ==================================================================
 * MyBatisNodeUsageDao.java - 22/07/2020 10:34:03 AM
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

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.UsageTier;
import net.solarnetwork.central.user.billing.snf.domain.UsageTiers;

/**
 * MyBatis implementation of {@link NodeUsageDao}.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisNodeUsageDao extends BaseMyBatisGenericDaoSupport<NodeUsage, Long>
		implements NodeUsageDao {

	/** Query name enumeration. */
	public enum QueryName {

		FindEffectiveUsageTierForDate("find-EffectiveUsageTier-for-date"),

		/** Find all available usage for a given user and date range. */
		FindMonthlyUsageForAccount("find-Usage-for-account"),

		/**
		 * Find all available usage for a given user and date range, by node.
		 */
		FindMonthlyNodeUsageForAccount("find-NodeUsage-for-account"),

		;

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
	public MyBatisNodeUsageDao() {
		super(NodeUsage.class, Long.class);
	}

	@Override
	public UsageTiers effectiveUsageTiers(LocalDate date) {
		if ( date == null ) {
			date = LocalDate.now();
		}
		List<UsageTier> results = selectList(QueryName.FindEffectiveUsageTierForDate.getQueryName(),
				date, null, null);
		if ( results == null ) {
			return null;
		}
		return new UsageTiers(results, date);
	}

	private List<NodeUsage> usageForUser(String queryName, Long userId, LocalDate startDate,
			LocalDate endDate) {
		if ( userId == null ) {
			throw new IllegalArgumentException("The userId argument must be provided.");
		}
		if ( startDate == null ) {
			throw new IllegalArgumentException("The month argument must be provided.");
		}
		Map<String, Object> params = new LinkedHashMap<>(2);
		params.put("userId", userId);
		params.put("startDate", startDate);
		params.put("endDate", startDate.plusMonths(1));
		return selectList(queryName, params, null, null);
	}

	@Override
	public List<NodeUsage> findUsageForAccount(Long userId, LocalDate startDate, LocalDate endDate) {
		return usageForUser(QueryName.FindMonthlyUsageForAccount.getQueryName(), userId, startDate,
				endDate);
	}

	@Override
	public List<NodeUsage> findNodeUsageForAccount(Long userId, LocalDate startDate, LocalDate endDate) {
		return usageForUser(QueryName.FindMonthlyNodeUsageForAccount.getQueryName(), userId, startDate,
				endDate);
	}

}
