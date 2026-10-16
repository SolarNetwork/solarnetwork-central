/* ==================================================================
 * InvoiceConsolidationResolver.java - 15/10/2026 10:04:27 am
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

package net.solarnetwork.central.user.billing.biz;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;

/**
 * API for resolving the accounts invoiced together with consolidated invoices.
 *
 * @author matt
 * @version 1.0
 */
public interface InvoiceConsolidationResolver {

	/**
	 * Get the invoice consolidation group a user belongs to.
	 *
	 * <p>
	 * The user can be the primary account of the group, or one of its other
	 * accounts. A group is valid only if all of its accounts can be invoiced
	 * together, for example in the same currency, and none of them is in
	 * another group. An invalid group is still returned, with a
	 * {@link InvoiceConsolidationGroup#problem()} that says why it is invalid,
	 * as a message to resolve in the locale of whoever reads it. None of its
	 * accounts are to be invoiced until the configuration is fixed.
	 * </p>
	 *
	 * @param userId
	 *        the ID of the user to get the group for
	 * @return the group, or {@code null} if the user is not in a group
	 * @throws IllegalArgumentException
	 *         if {@code userId} is {@code null}
	 */
	@Nullable
	InvoiceConsolidationGroup groupForUser(Long userId);

}
