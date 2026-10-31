/* ==================================================================
 * InvoiceAccountingSyncer.java - 11/10/2026 2:56:38 pm
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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;

/**
 * Record invoices in the accounting system.
 *
 * <p>
 * Exceptions thrown by the accounting system are not handled, so the task is
 * attempted again later.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class InvoiceAccountingSyncer implements AccountTaskHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AccountDao accountDao;
	private final SnfInvoiceDao invoiceDao;
	private final SnfAccountingSystem accountingSystem;

	/**
	 * Constructor.
	 *
	 * @param accountDao
	 *        the account DAO
	 * @param invoiceDao
	 *        the invoice DAO
	 * @param accountingSystem
	 *        the accounting system
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public InvoiceAccountingSyncer(AccountDao accountDao, SnfInvoiceDao invoiceDao,
			SnfAccountingSystem accountingSystem) {
		super();
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.invoiceDao = requireNonNullArgument(invoiceDao, "invoiceDao");
		this.accountingSystem = requireNonNullArgument(accountingSystem, "accountingSystem");
	}

	@Override
	public boolean handleTask(AccountTask task) {
		assert task.getTaskType() == AccountTaskType.SyncInvoiceToAccounting;
		final Long invoiceId = longParam(task, AccountTask.ID_PARAM);
		final Long userId = longParam(task, AccountTask.USER_ID_PARAM);
		if ( invoiceId == null || userId == null ) {
			return true;
		}
		final Account account = accountDao.getForUser(userId);
		if ( account == null ) {
			log.error("Account task {} cannot be handled because user {} has no billing account.", task,
					userId);
			return true;
		}
		final SnfInvoice invoice = invoiceDao.get(new UserLongCompositePK(userId, invoiceId));
		if ( invoice == null ) {
			log.error("Account task {} cannot be handled because invoice {} is not found.", task,
					invoiceId);
			return true;
		}
		accountingSystem.syncInvoice(invoice, account);
		return true;
	}

	private @Nullable Long longParam(AccountTask task, String key) {
		final Object val = (task.getTaskData() != null ? task.getTaskData().get(key) : null);
		if ( val == null ) {
			log.error("Account task {} cannot be handled because no '{}' parameter is available.", task,
					key);
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
