/* ==================================================================
 * GncTransaction.java - 11/10/2026 2:44:32 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.domain;

import java.time.Instant;

/**
 * A GnuCash transaction.
 *
 * @param guid
 *        the GUID
 * @param currencyGuid
 *        the GUID of the transaction's currency commodity
 * @param num
 *        the number
 * @param postDate
 *        the date posted
 * @param enterDate
 *        the date entered
 * @param description
 *        the description
 * @author matt
 * @version 1.0
 */
public record GncTransaction(String guid, String currencyGuid, String num, Instant postDate,
		Instant enterDate, String description) {

	/** The {@code trans-txn-type} slot value for an invoice transaction. */
	public static final String TXN_TYPE_INVOICE = "I";

	/** The {@code trans-txn-type} slot value for a payment transaction. */
	public static final String TXN_TYPE_PAYMENT = "P";

	/** The {@code trans-txn-type} slot value for a lot link transaction. */
	public static final String TXN_TYPE_LINK = "L";

}
