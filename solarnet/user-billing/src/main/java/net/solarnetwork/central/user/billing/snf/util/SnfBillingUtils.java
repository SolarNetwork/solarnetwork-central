/* ==================================================================
 * SnfBillingUtils.java - 6/08/2020 8:56:24 AM
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

package net.solarnetwork.central.user.billing.snf.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for SNF Billing.
 * 
 * @author matt
 * @version 1.0
 */
public final class SnfBillingUtils {

	private SnfBillingUtils() {
		// don't construct
	}

	/** A prefix added to invoice number values. */
	public static final String INVOICE_NUM_PREFIX = "INV-";

	/**
	 * A regular expression that matches invoice number values.
	 * 
	 * @see #invoiceIdForNum(String)
	 */
	public static final Pattern INVOICE_NUM_PATTERN = Pattern.compile("INV-([0-9A-Za-z]+)");

	/**
	 * Parse an invoice number value into it's corresponding ID.
	 * 
	 * @param num
	 *        the value to parse
	 * @return the ID, or {@literal null} if {@code num} cannot be parsed as an
	 *         invoice number
	 * @see #invoiceNumForId(Long)
	 */
	public static Long invoiceIdForNum(String num) {
		if ( num == null ) {
			return null;
		}
		Matcher m = INVOICE_NUM_PATTERN.matcher(num);
		if ( !m.matches() ) {
			return null;
		}
		try {
			return Long.parseUnsignedLong(m.group(1), 36);
		} catch ( NumberFormatException e ) {
			return null;
		}
	}

	/**
	 * Format an invoice ID as an invoice number.
	 * 
	 * <p>
	 * Invoice numbers start with {@link #INVOICE_NUM_PREFIX} and are followed
	 * by an upper case base-36 encoded integer value, for example
	 * {@literal INV-S2E0}.
	 * </p>
	 * 
	 * @param id
	 *        the invoice ID to format
	 * @return the invoice number, or {@literal null} if {@code id} is
	 *         {@literal null}
	 */
	public static String invoiceNumForId(Long id) {
		if ( id == null ) {
			return null;
		}
		return INVOICE_NUM_PREFIX + Long.toUnsignedString(id, 36).toUpperCase();
	}

}
