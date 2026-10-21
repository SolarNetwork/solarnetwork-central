/* ==================================================================
 * SnfInvoicePreviewGroup.java - 22/10/2026 4:05:31 pm
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

package net.solarnetwork.central.user.billing.snf.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Map;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;

/**
 * An invoice consolidation group resolved for previewing its consolidated
 * invoice.
 *
 * <p>
 * Resolving a group and its account names takes several queries for each
 * account of the group, so this is cached while a preview is polled.
 * </p>
 *
 * @param group
 *        the valid group, of which {@code account} is the primary account
 * @param account
 *        the primary account
 * @param accountNames
 *        the name of each account of the group, by user ID
 * @author matt
 * @version 1.0
 */
public record SnfInvoicePreviewGroup(InvoiceConsolidationGroup group, Account account,
		Map<Long, String> accountNames) {

	/**
	 * Constructor.
	 *
	 * @param group
	 *        the valid group, of which {@code account} is the primary account
	 * @param account
	 *        the primary account
	 * @param accountNames
	 *        the name of each account of the group, by user ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SnfInvoicePreviewGroup {
		requireNonNullArgument(group, "group");
		requireNonNullArgument(account, "account");
		accountNames = Map.copyOf(requireNonNullArgument(accountNames, "accountNames"));
	}

	/**
	 * Get the name of an account of the group.
	 *
	 * @param userId
	 *        the user ID of the account
	 * @return the name, or an empty string if not known
	 */
	public String accountName(Long userId) {
		final String name = accountNames.get(userId);
		return (name != null ? name : "");
	}

}
