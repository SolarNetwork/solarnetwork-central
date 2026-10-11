/* ==================================================================
 * GncAccount.java - 11/10/2026 2:44:00 pm
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

import org.jspecify.annotations.Nullable;

/**
 * A GnuCash account.
 *
 * @param guid
 *        the GUID
 * @param path
 *        the full account name, with the names of its ancestors separated by
 *        colons and not including the root account, such as
 *        {@code Assets:Accounts Receivable}
 * @param type
 *        the account type, such as {@link #TYPE_RECEIVABLE}
 * @param commodityGuid
 *        the GUID of the account's commodity
 * @param commodityScu
 *        the account's smallest commodity unit
 * @author matt
 * @version 1.0
 */
public record GncAccount(String guid, String path, String type, @Nullable String commodityGuid,
		int commodityScu) {

	/** The receivable account type. */
	public static final String TYPE_RECEIVABLE = "RECEIVABLE";

}
