/* ==================================================================
 * GncInvoiceRef.java - 11/10/2026 2:44:32 pm
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
import org.jspecify.annotations.Nullable;

/**
 * A reference to an existing GnuCash business document.
 *
 * @param guid
 *        the GUID
 * @param id
 *        the document ID
 * @param ownerGuid
 *        the GUID of the document's owner
 * @param lotGuid
 *        the GUID of the lot the document was posted to, or {@code null} if
 *        not posted
 * @param accountGuid
 *        the GUID of the account the document was posted to, or {@code null}
 *        if not posted
 * @param balance
 *        the balance of the document's lot: the amount still due on an
 *        invoice, or the negative of the credit left on a credit note; or
 *        {@code null} if not posted
 * @author matt
 * @version 1.0
 */
public record GncInvoiceRef(String guid, String id, @Nullable String ownerGuid,
		@Nullable String lotGuid, @Nullable String accountGuid, @Nullable BigDecimal balance) {

}
