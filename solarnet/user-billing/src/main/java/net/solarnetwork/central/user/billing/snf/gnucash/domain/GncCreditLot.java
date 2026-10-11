/* ==================================================================
 * GncCreditLot.java - 11/10/2026 2:44:53 pm
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An open lot that holds credit for a customer: an unapplied payment, or a
 * credit note.
 *
 * @param guid
 *        the GUID
 * @param balance
 *        the balance, in the account commodity; customer credit in a
 *        receivable account is negative
 * @param firstPostDate
 *        the post date of the lot's earliest transaction
 * @param lastPostDate
 *        the post date of the lot's latest transaction
 * @param documentGuid
 *        the GUID of the lot's business document, or {@code null} for a
 *        payment
 * @param documentTitle
 *        the title of the lot's business document, such as
 *        {@code Credit Note 123}
 * @param splits
 *        the lot's splits, which are only loaded for a payment
 * @author matt
 * @version 1.0
 */
public record GncCreditLot(String guid, BigDecimal balance, Instant firstPostDate,
		Instant lastPostDate, @Nullable String documentGuid, @Nullable String documentTitle,
		List<GncLotSplit> splits) {

	/**
	 * Test if the lot belongs to a business document, such as a credit note.
	 *
	 * @return {@code true} if the lot belongs to a business document,
	 *         {@code false} if it holds a payment
	 */
	public boolean isDocument() {
		return documentGuid != null;
	}

	/**
	 * Get the amount of credit available.
	 *
	 * @return the credit, as a positive amount
	 */
	public BigDecimal credit() {
		return balance.negate();
	}

}
