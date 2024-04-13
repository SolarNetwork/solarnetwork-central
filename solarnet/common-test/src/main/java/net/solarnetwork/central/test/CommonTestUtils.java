/* ==================================================================
 * CommonTestUtils.java - 6/10/2021 10:14:04 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import net.solarnetwork.util.ClassUtils;

/**
 * Common test utilities.
 *
 * @author matt
 * @version 1.2
 */
public final class CommonTestUtils {

	/** A random number generator. */
	public static final SecureRandom RNG = new SecureRandom();

	/** The default maximum scale. */
	public static final int DEFAULT_MAX_SCALE = 9;

	/**
	 * Create a {@link Matcher} for a string that compares to the contents of a
	 * text resource.
	 *
	 * @param resource
	 *        the name of the resource
	 * @param clazz
	 *        the class to load the resource from
	 * @return the matcher
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 */
	public static Matcher<String> equalToTextResource(String resource, Class<?> clazz) {
		return equalToTextResource(resource, clazz, null);
	}

	/**
	 * Create a {@link Matcher} for a string that compares to the contents of a
	 * text resource.
	 *
	 * @param resource
	 *        the name of the resource
	 * @param clazz
	 *        the class to load the resource from
	 * @param skip
	 *        an optional pattern that will be used to match against lines;
	 *        matches will be left out of the string used to match
	 * @return the matcher
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 */
	public static Matcher<String> equalToTextResource(String resource, Class<?> clazz, Pattern skip) {
		String txt = ClassUtils.getResourceAsString(resource, clazz, skip);
		return Matchers.equalToCompressingWhiteSpace(txt);
	}

	/**
	 * Get a random decimal number.
	 *
	 * @return the random decimal number
	 */
	public static BigDecimal randomDecimal() {
		return new BigDecimal(RNG.nextDouble(-1000.0, 1000.0)).setScale(4, RoundingMode.HALF_UP);
	}

	/**
	 * Get a random string value.
	 *
	 * @return the string
	 */
	public static String randomString() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Get a random long value.
	 *
	 * @return the long
	 */
	public static Long randomLong() {
		return RNG.nextLong();
	}

	/**
	 * Compare a decimal array using a maximum scale.
	 *
	 * <p>
	 * The {@link #DEFAULT_MAX_SCALE} maximum scale and
	 * {@link RoundingMode#DONW} rounding mode will be used.
	 * </p>
	 *
	 * @param expected
	 *        the expected decimals
	 * @return the matcher
	 * @since 1.2
	 */
	public static Matcher<BigDecimal[]> decimalArrayMatching(BigDecimal[] expected) {
		return decimalArrayMatching(expected, DEFAULT_MAX_SCALE, RoundingMode.DOWN);
	}

	/**
	 * Compare a decimal array using a maximum scale.
	 *
	 * @param expected
	 *        the expected decimals
	 * @param maxScale
	 *        the maximum scale to compare at
	 * @param mode
	 *        the rounding mode to use
	 * @return the matcher
	 * @since 1.2
	 */
	public static Matcher<BigDecimal[]> decimalArrayMatching(BigDecimal[] expected, int maxScale,
			RoundingMode mode) {
		return new org.hamcrest.CustomMatcher<BigDecimal[]>("decimal array") {

			@Override
			public boolean matches(Object actual) {
				if ( actual == null && expected != null ) {
					return false;
				} else if ( actual instanceof BigDecimal[] array ) {
					if ( array.length != expected.length ) {
						return false;
					}
					for ( int i = 0; i < expected.length; i++ ) {
						BigDecimal a = array[i];
						BigDecimal e = expected[i];
						if ( a == null ) {
							return false;
						}
						if ( e.compareTo(a) == 0 ) {
							return true;
						}
						if ( a.scale() != e.scale() ) {
							BigDecimal a1 = a.setScale(Math.min(maxScale, e.scale()), mode);
							BigDecimal e1 = e.setScale(Math.min(maxScale, e.scale()), mode);
							if ( e1.compareTo(a1) == 0 ) {
								return true;
							}
						}
					}
				}
				return false;
			}

			@Override
			public void describeMismatch(Object actual, Description description) {
				if ( actual == null && expected != null ) {
					description.appendText("was null");
				} else if ( actual instanceof BigDecimal[] array ) {
					if ( array.length != expected.length ) {
						description.appendText("length was %d ".formatted(expected.length))
								.appendValue(actual);
						return;
					}
					for ( int i = 0; i < expected.length; i++ ) {
						BigDecimal a = array[i];
						BigDecimal e = expected[i];
						if ( a == null ) {
							description.appendText("item %d not null".formatted(i));
							return;
						}
						if ( e.compareTo(a) == 0 ) {
							continue;
						}
						BigDecimal a1 = a.setScale(Math.min(maxScale, e.scale()), mode);
						BigDecimal e1 = e.setScale(Math.min(maxScale, e.scale()), mode);
						if ( e1.compareTo(a1) == 0 ) {
							continue;
						}
						description.appendText("item %d was ".formatted(i)).appendValue(a1);
						return;
					}
				} else {
					super.describeMismatch(actual, description);
				}
			}

		};
	}

}
