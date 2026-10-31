/* ==================================================================
 * InvoiceConsolidationGroup.java - 15/10/2026 9:52:18 am
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

package net.solarnetwork.central.user.billing.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;

/**
 * A group of accounts invoiced together with a consolidated invoice.
 *
 * <p>
 * Each account in the group still gets its own invoice; the consolidated
 * invoice combines them, and is billed to the primary account.
 * </p>
 *
 * <p>
 * A group whose configuration is invalid has a {@code problem} that says why,
 * as a message to resolve in the locale of whoever reads it. The accounts of an
 * invalid group are <i>held</i>: they must not be invoiced until the
 * configuration is fixed.
 * </p>
 *
 * @param primaryUserId
 *        the user ID of the primary account, to which consolidated invoices are
 *        billed
 * @param userIds
 *        the user IDs of all accounts in the group, starting with the primary
 *        account, in the order the accounts are listed in a consolidated
 *        invoice
 * @param deliverMemberInvoices
 *        {@code true} to also deliver the invoice of each account other than
 *        the primary account to that account
 * @param problem
 *        why the group configuration is invalid, as a message to resolve, or
 *        {@code null} if it is valid
 * @author matt
 * @version 1.0
 */
public record InvoiceConsolidationGroup(Long primaryUserId, List<Long> userIds,
		boolean deliverMemberInvoices, @Nullable MessageSourceResolvable problem) {

	/**
	 * Constructor.
	 *
	 * @param primaryUserId
	 *        the user ID of the primary account, to which consolidated invoices
	 *        are billed
	 * @param userIds
	 *        the user IDs of all accounts in the group, starting with the
	 *        primary account, in the order the accounts are listed in a
	 *        consolidated invoice
	 * @param deliverMemberInvoices
	 *        {@code true} to also deliver the invoice of each account other
	 *        than the primary account to that account
	 * @param problem
	 *        why the group configuration is invalid, as a message to resolve,
	 *        or {@code null} if it is valid
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code problem} is {@code null}, or
	 *         {@code userIds} does not start with {@code primaryUserId}
	 */
	public InvoiceConsolidationGroup {
		requireNonNullArgument(primaryUserId, "primaryUserId");
		userIds = List.copyOf(requireNonNullArgument(userIds, "userIds"));
		if ( userIds.isEmpty() || !primaryUserId.equals(userIds.getFirst()) ) {
			throw new IllegalArgumentException(
					"The userIds argument must start with the primary user ID.");
		}
	}

	/**
	 * Constructor for a valid group.
	 *
	 * @param primaryUserId
	 *        the user ID of the primary account, to which consolidated invoices
	 *        are billed
	 * @param userIds
	 *        the user IDs of all accounts in the group, starting with the
	 *        primary account, in the order the accounts are listed in a
	 *        consolidated invoice
	 * @param deliverMemberInvoices
	 *        {@code true} to also deliver the invoice of each account other
	 *        than the primary account to that account
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}, or {@code userIds} does not
	 *         start with {@code primaryUserId}
	 */
	public InvoiceConsolidationGroup(Long primaryUserId, List<Long> userIds,
			boolean deliverMemberInvoices) {
		this(primaryUserId, userIds, deliverMemberInvoices, null);
	}

	/**
	 * Test if the group configuration is valid.
	 *
	 * @return {@code true} if the group has no problem, so its accounts can be
	 *         invoiced
	 */
	public boolean isValid() {
		return problem == null;
	}

	/**
	 * Test if a user is the primary account of this group.
	 *
	 * @param userId
	 *        the user ID to test
	 * @return {@code true} if {@code userId} is the primary account user ID
	 */
	public boolean isPrimary(Long userId) {
		return primaryUserId.equals(userId);
	}

	/**
	 * Get the user IDs of the accounts other than the primary account.
	 *
	 * @return the member account user IDs, never {@code null}
	 */
	public List<Long> memberUserIds() {
		return userIds.subList(1, userIds.size());
	}

}
