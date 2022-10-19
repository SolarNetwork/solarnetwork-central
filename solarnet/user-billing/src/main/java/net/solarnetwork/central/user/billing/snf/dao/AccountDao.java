/* ==================================================================
 * AccountDao.java - 20/07/2020 4:17:18 PM
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

import java.math.BigDecimal;
import java.time.LocalDate;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link Account} entities.
 * 
 * @author matt
 * @version 1.1
 */
public interface AccountDao extends GenericDao<Account, UserLongPK> {

	/**
	 * Get an account for a given user ID.
	 * 
	 * @param userId
	 *        the ID of the user to get the account for
	 * @return the account, or {@literal null} if not available
	 */
	Account getForUser(Long userId);

	/**
	 * Get an account for a given user ID using details (such as address) from a
	 * specific date.
	 * 
	 * @param userId
	 *        the ID of the user to get the account for
	 * @return the account, or {@literal null} if not available
	 * @since 1.1
	 */
	Account getForUser(Long userId, LocalDate at);

	/**
	 * Get the overall account balance for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the account balance for
	 * @return the account balance, or {@literal null} if not available
	 */
	AccountBalance getBalanceForUser(Long userId);

	/**
	 * Lay claim to a portion of an account balance credit.
	 * 
	 * <p>
	 * This should only be invoked within a transaction, so other tasks cannot
	 * also claim the credit returned by this method.
	 * </p>
	 * 
	 * @param accountId
	 *        the account ID to claim credit from
	 * @param max
	 *        the maximum amount of credit to claim, or {@literal null} to claim
	 *        all available credit
	 * @return the claimed credit, never {@literal null} but possibly less than
	 *         the requested {@code max} if not enough credit is available
	 */
	BigDecimal claimAccountBalanceCredit(Long accountId, BigDecimal max);

}
