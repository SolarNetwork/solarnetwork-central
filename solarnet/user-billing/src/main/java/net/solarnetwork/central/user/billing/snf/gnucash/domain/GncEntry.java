/* ==================================================================
 * GncEntry.java - 11/10/2026 2:44:32 pm
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
import org.jspecify.annotations.Nullable;

/**
 * A GnuCash invoice line.
 *
 * <p>
 * Only the invoice side of an entry is modelled; the bill fields are written
 * with the values GnuCash uses for an invoice line.
 * </p>
 *
 * @param guid
 *        the GUID
 * @param date
 *        the line date
 * @param dateEntered
 *        the date the line was entered
 * @param description
 *        the description
 * @param action
 *        the action
 * @param notes
 *        the notes
 * @param quantity
 *        the quantity, negated for a credit note
 * @param accountGuid
 *        the GUID of the account the line is posted to
 * @param price
 *        the unit price
 * @param taxable
 *        {@code true} if the line is taxable
 * @param taxIncluded
 *        {@code true} if the price includes tax
 * @param taxTableGuid
 *        the GUID of the line's tax table
 * @param invoiceGuid
 *        the GUID of the invoice
 * @author matt
 * @version 1.0
 */
public record GncEntry(String guid, Instant date, Instant dateEntered, String description,
		String action, String notes, GncNumeric quantity, String accountGuid, GncNumeric price,
		boolean taxable, boolean taxIncluded, @Nullable String taxTableGuid, String invoiceGuid) {

}
