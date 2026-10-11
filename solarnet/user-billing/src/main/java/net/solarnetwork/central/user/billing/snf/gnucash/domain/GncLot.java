/* ==================================================================
 * GncLot.java - 11/10/2026 2:44:32 pm
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
 * A GnuCash lot, which groups the splits of a business document with the
 * payments and credits applied to it.
 *
 * @param guid
 *        the GUID
 * @param accountGuid
 *        the GUID of the account
 * @param closed
 *        the closed state, such as {@link #CLOSED_UNKNOWN}
 * @author matt
 * @version 1.0
 */
public record GncLot(String guid, String accountGuid, int closed) {

	/** The closed state GnuCash uses until it has computed it. */
	public static final int CLOSED_UNKNOWN = -1;

}
