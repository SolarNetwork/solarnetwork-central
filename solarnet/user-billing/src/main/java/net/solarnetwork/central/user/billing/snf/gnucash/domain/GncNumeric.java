/* ==================================================================
 * GncNumeric.java - 11/10/2026 2:44:00 pm
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * A GnuCash rational number, stored as a numerator and denominator.
 *
 * @param num
 *        the numerator
 * @param denom
 *        the denominator, which must be positive
 * @author matt
 * @version 1.0
 */
public record GncNumeric(long num, long denom) {

	/** Zero, as {@literal 0/1}. */
	public static final GncNumeric ZERO = new GncNumeric(0, 1);

	/** One, as {@literal 1/1}. */
	public static final GncNumeric ONE = new GncNumeric(1, 1);

	/**
	 * Constructor.
	 *
	 * @param num
	 *        the numerator
	 * @param denom
	 *        the denominator
	 * @throws IllegalArgumentException
	 *         if {@code denom} is not positive
	 */
	public GncNumeric {
		if ( denom < 1 ) {
			throw new IllegalArgumentException("The denom argument must be positive.");
		}
	}

	/**
	 * Create a numeric from a decimal, using the smallest power of ten
	 * denominator that represents the decimal exactly.
	 *
	 * @param value
	 *        the value
	 * @return the numeric
	 * @throws ArithmeticException
	 *         if the value does not fit in a numerator and denominator
	 */
	public static GncNumeric of(BigDecimal value) {
		final BigDecimal v = value.stripTrailingZeros();
		final int scale = Math.max(v.scale(), 0);
		return new GncNumeric(v.movePointRight(scale).longValueExact(),
				BigInteger.TEN.pow(scale).longValueExact());
	}

	/**
	 * Create a numeric from a decimal, using a given denominator, such as a
	 * commodity's smallest fraction.
	 *
	 * @param value
	 *        the value
	 * @param fraction
	 *        the denominator to use
	 * @return the numeric
	 * @throws ArithmeticException
	 *         if the value cannot be represented exactly with the given
	 *         denominator
	 */
	public static GncNumeric of(BigDecimal value, long fraction) {
		return new GncNumeric(value.multiply(BigDecimal.valueOf(fraction)).longValueExact(),
				fraction);
	}

	/**
	 * Get the value as a decimal.
	 *
	 * @return the value
	 */
	public BigDecimal toBigDecimal() {
		return new BigDecimal(num).divide(new BigDecimal(denom), MathContext.DECIMAL128);
	}

	/**
	 * Get the negated value.
	 *
	 * @return the negated value
	 */
	public GncNumeric negate() {
		return new GncNumeric(-num, denom);
	}

	/**
	 * Test if the value is zero.
	 *
	 * @return {@code true} if the value is zero
	 */
	public boolean isZero() {
		return num == 0;
	}

}
