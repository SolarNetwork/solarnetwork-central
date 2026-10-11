/* ==================================================================
 * GncTaxTableEntry.java - 11/10/2026 2:44:32 pm
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

/**
 * A GnuCash tax table entry: one tax, charged to one account.
 *
 * @param accountGuid
 *        the GUID of the account the tax is charged to
 * @param amount
 *        the tax amount, either a percentage or a fixed value depending on
 *        {@code type}
 * @param type
 *        the amount type, such as {@link #TYPE_PERCENT}
 * @author matt
 * @version 1.0
 */
public record GncTaxTableEntry(String accountGuid, GncNumeric amount, int type) {

	/** The amount type for a fixed value. */
	public static final int TYPE_VALUE = 1;

	/** The amount type for a percentage. */
	public static final int TYPE_PERCENT = 2;

	/**
	 * Test if the amount is a percentage.
	 *
	 * @return {@code true} if the amount is a percentage
	 */
	public boolean isPercent() {
		return type == TYPE_PERCENT;
	}

}
