/* ==================================================================
 * MyBatisAccountDao.java - 20/07/2020 4:20:29 PM
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

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * MyBatis implementation of {@link AccountDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisAccountDao extends BaseMyBatisGenericDaoSupport<Account, UserLongPK>
		implements AccountDao {

	/** Query name enumeration. */
	public enum QueryName {

		/** Claim credit from account balance. */
		ClaimCreditFromAccountBalance("claim-AccountBalance-credit"),

		/** Get account balance for a user. */
		GetAccountBalanceForUser("get-AccountBalance-for-user"),

		/** Get an account for a user. */
		GetForUser("get-Account-for-user"),

		/**
		 * Get an account for a user at a specific date.
		 * 
		 * @since 1.1
		 */
		GetForUserAtDate("get-Account-for-user-at-date"),

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
	public MyBatisAccountDao() {
		super(Account.class, UserLongPK.class);
	}

	@Override
	public Account getForUser(Long userId) {
		return selectFirst(QueryName.GetForUser.getQueryName(), userId);
	}

	@Override
	public Account getForUser(Long userId, LocalDate at) {
		Map<String, Object> params = Map.of("userId", userId, "date", Date.valueOf(at));
		return selectFirst(QueryName.GetForUserAtDate.getQueryName(), params);
	}

	@Override
	public AccountBalance getBalanceForUser(Long userId) {
		return selectFirst(QueryName.GetAccountBalanceForUser.getQueryName(), userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public BigDecimal claimAccountBalanceCredit(Long accountId, BigDecimal max) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("accountId", accountId);
		if ( max != null ) {
			params.put("max", max);
		}
		BigDecimal claimed = selectFirst(QueryName.ClaimCreditFromAccountBalance.getQueryName(), params);
		return claimed != null ? claimed : BigDecimal.ZERO;
	}

}
