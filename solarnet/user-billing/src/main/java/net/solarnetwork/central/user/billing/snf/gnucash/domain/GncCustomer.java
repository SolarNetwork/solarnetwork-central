/* ==================================================================
 * GncCustomer.java - 11/10/2026 2:44:00 pm
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
 * A GnuCash customer.
 *
 * @param guid
 *        the GUID
 * @param id
 *        the customer ID, such as {@code SN_123}
 * @param name
 *        the customer name
 * @param notes
 *        the notes
 * @param active
 *        {@code true} if the customer is active
 * @param currencyGuid
 *        the GUID of the customer's currency commodity
 * @param taxOverride
 *        {@code true} if {@code taxTableGuid} overrides the default tax table
 * @param taxIncluded
 *        the tax included setting, such as {@link #TAX_INCLUDED_USE_GLOBAL}
 * @param taxTableGuid
 *        the GUID of the customer's tax table
 * @param address
 *        the billing address
 * @param shipAddress
 *        the shipping address
 * @author matt
 * @version 1.0
 */
public record GncCustomer(String guid, String id, String name, String notes, boolean active,
		String currencyGuid, boolean taxOverride, int taxIncluded, @Nullable String taxTableGuid,
		GncAddress address, GncAddress shipAddress) {

	/** The tax included setting for prices that include tax. */
	public static final int TAX_INCLUDED_YES = 1;

	/** The tax included setting for prices that exclude tax. */
	public static final int TAX_INCLUDED_NO = 2;

	/** The tax included setting to use the global preference. */
	public static final int TAX_INCLUDED_USE_GLOBAL = 3;

}
