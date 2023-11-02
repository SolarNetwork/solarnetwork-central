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
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import net.solarnetwork.util.ClassUtils;

/**
 * Common test utilities.
 * 
 * @author matt
 * @version 1.1
 */
public final class CommonTestUtils {

	/** A random number generator. */
	public static final SecureRandom RNG = new SecureRandom();

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
}
