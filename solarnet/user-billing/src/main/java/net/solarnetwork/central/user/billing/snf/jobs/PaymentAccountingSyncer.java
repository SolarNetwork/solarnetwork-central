/* ==================================================================
 * PaymentAccountingSyncer.java - 14/10/2026 10:48:12 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.PaymentDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.Payment;

/**
 * Record payments in the accounting system.
 *
 * <p>
 * The task data holds the payment ID in the {@link AccountTask#ID_PARAM}
 * parameter and the user ID in the {@link AccountTask#USER_ID_PARAM}
 * parameter. Exceptions thrown by the accounting system are not handled, so
 * the task is attempted again later.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class PaymentAccountingSyncer implements AccountTaskHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AccountDao accountDao;
	private final PaymentDao paymentDao;
	private final SnfAccountingSystem accountingSystem;

	/**
	 * Constructor.
	 *
	 * @param accountDao
	 *        the account DAO
	 * @param paymentDao
	 *        the payment DAO
	 * @param accountingSystem
	 *        the accounting system
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public PaymentAccountingSyncer(AccountDao accountDao, PaymentDao paymentDao,
			SnfAccountingSystem accountingSystem) {
		super();
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.paymentDao = requireNonNullArgument(paymentDao, "paymentDao");
		this.accountingSystem = requireNonNullArgument(accountingSystem, "accountingSystem");
	}

	@Override
	public boolean handleTask(AccountTask task) {
		assert task.getTaskType() == AccountTaskType.SyncPaymentToAccounting;
		final UUID paymentId = uuidParam(task, AccountTask.ID_PARAM);
		final Long userId = longParam(task, AccountTask.USER_ID_PARAM);
		if ( paymentId == null || userId == null ) {
			return true;
		}
		final Account account = accountDao.getForUser(userId);
		if ( account == null ) {
			log.error("Account task {} cannot be handled because user {} has no billing account.",
					task, userId);
			return true;
		}
		final UserUuidPK id = new UserUuidPK(userId, paymentId);
		final Payment payment = paymentDao.get(id);
		if ( payment == null ) {
			log.error("Account task {} cannot be handled because payment {} is not found.", task,
					paymentId);
			return true;
		}
		accountingSystem.syncPayment(payment, paymentDao.findInvoicePayments(id), account);
		return true;
	}

	private @Nullable Object param(AccountTask task, String key) {
		final Object val = (task.getTaskData() != null ? task.getTaskData().get(key) : null);
		if ( val == null ) {
			log.error("Account task {} cannot be handled because no '{}' parameter is available.",
					task, key);
		}
		return val;
	}

	private @Nullable UUID uuidParam(AccountTask task, String key) {
		final Object val = param(task, key);
		if ( val == null ) {
			return null;
		}
		try {
			return (val instanceof UUID u ? u : UUID.fromString(val.toString()));
		} catch ( IllegalArgumentException e ) {
			log.error("Account task {} cannot be handled because '{}' parameter is not a valid UUID: {}",
					task, key, val);
			return null;
		}
	}

	private @Nullable Long longParam(AccountTask task, String key) {
		final Object val = param(task, key);
		if ( val == null ) {
			return null;
		}
		try {
			return (val instanceof Number n ? (Long) n.longValue() : Long.valueOf(val.toString()));
		} catch ( IllegalArgumentException e ) {
			log.error(
					"Account task {} cannot be handled because '{}' parameter is not a valid number: {}",
					task, key, val);
			return null;
		}
	}

}
