/* ==================================================================
 * GncAddress.java - 11/10/2026 2:44:00 pm
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
 * A GnuCash business address.
 *
 * <p>
 * GnuCash stores unused address fields as empty strings.
 * </p>
 *
 * @param name
 *        the name
 * @param addr1
 *        the first address line
 * @param addr2
 *        the second address line
 * @param addr3
 *        the third address line
 * @param addr4
 *        the fourth address line
 * @param phone
 *        the phone number
 * @param fax
 *        the fax number
 * @param email
 *        the email address
 * @author matt
 * @version 1.0
 */
public record GncAddress(String name, String addr1, String addr2, String addr3, String addr4,
		String phone, String fax, String email) {

	/** An address with no information. */
	public static final GncAddress EMPTY = new GncAddress("", "", "", "", "", "", "", "");

}
