/* ==================================================================
 * InvoiceDeliverer.java - 21/07/2020 12:27:20 PM
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

package net.solarnetwork.central.user.billing.snf.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Deliver invoices to the account holder.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceDeliverer implements AccountTaskHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final SnfInvoicingSystem invoicingSystem;

	/**
	 * Constructor.
	 * 
	 * @param invoicingSystem
	 *        the invoicing system
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public InvoiceDeliverer(SnfInvoicingSystem invoicingSystem) {
		if ( invoicingSystem == null ) {
			throw new IllegalArgumentException("The invoicingSystem argument must not be null.");
		}
		this.invoicingSystem = invoicingSystem;
	}

	@Override
	public boolean handleTask(AccountTask task) {
		assert task.getTaskType() == AccountTaskType.DeliverInvoice;
		final Object invoiceIdVal = task.getTaskData() != null
				? task.getTaskData().get(AccountTask.ID_PARAM)
				: null;
		final Object userIdVal = task.getTaskData() != null
				? task.getTaskData().get(AccountTask.USER_ID_PARAM)
				: null;
		if ( invoiceIdVal == null ) {
			log.error("Account task {} cannot be handled because no '{}' parameter is available.", task,
					AccountTask.ID_PARAM);
			return true;
		}
		if ( userIdVal == null ) {
			log.error("Account task {} cannot be handled because no '{}' parameter is available.", task,
					AccountTask.USER_ID_PARAM);
			return true;
		}
		final Long invoiceId;
		try {
			invoiceId = (invoiceIdVal instanceof Number ? ((Number) invoiceIdVal).longValue()
					: Long.valueOf(invoiceIdVal.toString()));
		} catch ( IllegalArgumentException e ) {
			log.error(
					"Account task {} cannot be handled because '{}' parameter is not a valid number: {}",
					task, AccountTask.ID_PARAM, invoiceIdVal);
			return true;
		}
		final Long userId;
		try {
			userId = (userIdVal instanceof Number ? ((Number) userIdVal).longValue()
					: Long.valueOf(userIdVal.toString()));
		} catch ( IllegalArgumentException e ) {
			log.error(
					"Account task {} cannot be handled because '{}' parameter is not a valid number: {}",
					task, AccountTask.USER_ID_PARAM, userIdVal);
			return true;
		}
		return invoicingSystem.deliverInvoice(new UserLongPK(userId, invoiceId));
	}

}
