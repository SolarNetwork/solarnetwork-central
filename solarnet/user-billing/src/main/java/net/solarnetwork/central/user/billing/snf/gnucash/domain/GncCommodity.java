/* ==================================================================
 * GncCommodity.java - 11/10/2026 2:44:00 pm
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
 * A GnuCash commodity, such as a currency.
 *
 * @param guid
 *        the GUID
 * @param namespace
 *        the namespace, such as {@link #CURRENCY_NAMESPACE}
 * @param mnemonic
 *        the mnemonic, such as {@code NZD}
 * @param fraction
 *        the smallest fraction, such as {@literal 100}
 * @author matt
 * @version 1.0
 */
public record GncCommodity(String guid, String namespace, String mnemonic, int fraction) {

	/** The namespace used for currencies. */
	public static final String CURRENCY_NAMESPACE = "CURRENCY";

	/**
	 * Get the number of decimal places of the smallest fraction.
	 *
	 * @return the number of decimal places, such as {@literal 2} for a
	 *         fraction of {@literal 100}
	 * @throws IllegalStateException
	 *         if the fraction is not a power of ten
	 */
	public int scale() {
		int scale = 0;
		int f = fraction;
		while ( f > 1 && f % 10 == 0 ) {
			f /= 10;
			scale++;
		}
		if ( f != 1 ) {
			throw new IllegalStateException(
					"Commodity %s fraction %d is not a power of ten.".formatted(mnemonic, fraction));
		}
		return scale;
	}

}
