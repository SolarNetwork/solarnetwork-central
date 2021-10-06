/* ==================================================================
 * InvoiceMatch.java - 25/08/2017 2:34:43 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import java.math.BigDecimal;
import org.joda.time.DateTime;
import net.solarnetwork.central.domain.FilterMatch;

/**
 * Search resulut match for an Invoice.
 * 
 * @author matt
 * @version 1.1
 */
public interface InvoiceMatch extends FilterMatch<String> {

	/**
	 * Get the date this invoice was created.
	 * 
	 * @return the created date
	 */
	DateTime getCreated();

	/**
	 * Get the time zone this invoice was created in.
	 * 
	 * @return the time zone ID
	 */
	String getTimeZoneId();

	/**
	 * Get a reference invoice "number".
	 * 
	 * @return the invoice number
	 */
	String getInvoiceNumber();

	/**
	 * Get the amount charged on this invoice.
	 * 
	 * @return the amount
	 */
	BigDecimal getAmount();

	/**
	 * Get the current invoice balance (unpaid amount).
	 * 
	 * <p>
	 * If this is positive then the invoice has outstanding payment due.
	 * </p>
	 * 
	 * @return the invoice balance
	 */
	BigDecimal getBalance();

	/**
	 * Get the total amount of all tax invoice items.
	 * 
	 * @return the total tax amount
	 * @since 1.1
	 */
	BigDecimal getTaxAmount();

	/**
	 * Get the currency this invoice is in, as a string currency code like
	 * {@literal NZD} or {@literal USD}.
	 * 
	 * @return the currency code
	 */
	String getCurrencyCode();

}
