/* ==================================================================
 * SnfInvoiceDeliverer.java - 25/07/2020 3:18:14 PM
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

package net.solarnetwork.central.user.billing.snf;

import java.util.concurrent.CompletableFuture;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.IdentifiableConfiguration;

/**
 * API for a service that can deliver an invoice to an account holder.
 * 
 * @author matt
 * @version 2.0
 */
public interface SnfInvoiceDeliverer extends Identity<String> {

	/**
	 * Deliver an invoice.
	 * 
	 * @param invoice
	 *        the invoice to deliver
	 * @param account
	 *        the account that owns the invoice
	 * @param configuration
	 *        any account-specific configuration to use
	 * @return a future for the results
	 */
	CompletableFuture<Result<Object>> deliverInvoice(SnfInvoice invoice, Account account,
			IdentifiableConfiguration configuration);

}
