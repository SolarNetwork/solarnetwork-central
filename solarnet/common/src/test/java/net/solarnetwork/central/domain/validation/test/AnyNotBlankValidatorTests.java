/* ==================================================================
 * AnyNotBlankValidatorTests.java - 28 Sept 2026 3:22:46 pm
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

package net.solarnetwork.central.domain.validation.test;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.validation.AnyNotBlankValidator;

/**
 * Test cases for the {@link AnyNotBlankValidator} class.
 *
 * @author matt
 * @version 1.0
 */
public class AnyNotBlankValidatorTests {

	private AnyNotBlankValidator validator;

	@BeforeEach
	public void setup() {
		validator = new AnyNotBlankValidator();
	}

	private boolean isValid(String... value) {
		return validator.isValid(value, null);
	}

	@Test
	public void nullArray() {
		// @formatter:off
		then(validator.isValid(null, null))
			.as("A null array has no non-blank element, so is not valid")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void emptyArray() {
		// @formatter:off
		then(isValid())
			.as("An empty array has no non-blank element, so is not valid")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void allBlank() {
		// @formatter:off
		then(isValid("", " ", "\t"))
			.as("An array of only blank elements is not valid")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void allNull() {
		// @formatter:off
		then(isValid(null, null))
			.as("An array of only null elements is not valid")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void oneNonBlank() {
		// @formatter:off
		then(isValid("123 Main Street"))
			.as("An array with a non-blank element is valid")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void nonBlankWithBlanks() {
		// @formatter:off
		then(isValid("123 Main Street", ""))
			.as("Blank elements are allowed alongside a non-blank element")
			.isTrue()
			;
		then(isValid(null, "Apt 3B"))
			.as("A non-blank element is enough wherever it appears")
			.isTrue()
			;
		// @formatter:on
	}

}
