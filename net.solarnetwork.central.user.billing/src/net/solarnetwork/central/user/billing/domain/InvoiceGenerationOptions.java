/* ==================================================================
 * InvoiceGenerationOptions.java - 31/05/2021 10:51:20 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.domain;

import java.time.YearMonth;

/**
 * Options to use when generating an invoice.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface InvoiceGenerationOptions {

	/**
	 * Get the year-month to generate the invoice for, or {@literal null} for
	 * the "current" month.
	 * 
	 * @return the year month
	 */
	YearMonth getMonth();

	/**
	 * Get the "use account credit" flag.
	 * 
	 * @return {@literal true} to use available account credit by adding a
	 *         credit item to the generated invoice
	 */
	boolean isUseAccountCredit();

}
