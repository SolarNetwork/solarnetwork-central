/* ==================================================================
 * ConsolidatedInvoiceDeliverer.java - 15/10/2026 3:02:16 pm
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
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;

/**
 * Deliver consolidated invoices to the primary account holder.
 *
 * <p>
 * Handles {@link AccountTaskType#DeliverConsolidatedInvoice} tasks, whose
 * {@link AccountTask#USER_ID_PARAM} and {@link AccountTask#ID_PARAM} task data
 * identify the consolidated invoice.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class ConsolidatedInvoiceDeliverer implements AccountTaskHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final SnfInvoicingSystem invoicingSystem;

	/**
	 * Constructor.
	 *
	 * @param invoicingSystem
	 *        the invoicing system
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ConsolidatedInvoiceDeliverer(SnfInvoicingSystem invoicingSystem) {
		super();
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
	}

	@Override
	public boolean handleTask(AccountTask task) {
		assert task.getTaskType() == AccountTaskType.DeliverConsolidatedInvoice;
		final Long invoiceId = longParameter(task, AccountTask.ID_PARAM);
		final Long userId = longParameter(task, AccountTask.USER_ID_PARAM);
		if ( invoiceId == null || userId == null ) {
			return true;
		}
		return invoicingSystem.deliverConsolidatedInvoice(new UserLongCompositePK(userId, invoiceId));
	}

	private @Nullable Long longParameter(AccountTask task, String name) {
		final Map<String, Object> taskData = task.getTaskData();
		final Object val = (taskData != null ? taskData.get(name) : null);
		if ( val == null ) {
			log.error("Account task {} cannot be handled because no '{}' parameter is available.",
					task, name);
			return null;
		}
		try {
			return (val instanceof Number n ? (Long) n.longValue() : Long.valueOf(val.toString()));
		} catch ( IllegalArgumentException e ) {
			log.error(
					"Account task {} cannot be handled because '{}' parameter is not a valid number: {}",
					task, name, val);
			return null;
		}
	}

}
