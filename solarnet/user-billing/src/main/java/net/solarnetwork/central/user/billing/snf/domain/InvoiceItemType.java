/* ==================================================================
 * InvoiceItemType.java - 20/07/2020 11:54:54 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import net.solarnetwork.domain.CodedValue;

/**
 * Type of invoice item.
 * 
 * <p>
 * The ordering of the enumeration is used as a sort order for invoice items.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum InvoiceItemType implements CodedValue {

	Unknown(0),

	/** A recurring charge. */
	Recurring(1),

	/** A one-off fixed charge. */
	Fixed(2),

	/** A change based on usage. */
	Usage(3),

	/**
	 * A credit adjustment.
	 */
	Credit(4),

	/**
	 * A tax item.
	 */
	Tax(5);

	private final byte code;

	private InvoiceItemType(int code) {
		this.code = (byte) code;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code value
	 */
	@Override
	public int getCode() {
		return code & 0xFF;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the status, never {@literal null} and set to {@link #Unknown} if
	 *         not any other valid code
	 */
	public static InvoiceItemType forCode(int code) {
		final byte c = (byte) code;
		for ( InvoiceItemType v : values() ) {
			if ( v.code == c ) {
				return v;
			}
		}
		return InvoiceItemType.Unknown;
	}

}
