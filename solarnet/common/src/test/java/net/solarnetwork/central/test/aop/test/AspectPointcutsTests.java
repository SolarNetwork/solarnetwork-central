/* ==================================================================
 * AspectPointcutsTests.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.aop.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.test.aop.AspectPointcuts;

/**
 * Test cases for the {@link AspectPointcuts} class.
 *
 * @author matt
 * @version 1.0
 */
public class AspectPointcutsTests {

	@Test
	public void allMatched() {
		// WHEN
		final List<String> result = AspectPointcuts.unmatchedPointcuts(ToySecurityAspect.class,
				DaoToyBiz.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Every pointcut matches some service method")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void allMatched_securable() {
		// WHEN
		final List<String> result = AspectPointcuts
				.unmatchedPointcuts(ToySecurableSecurityAspect.class, DaoToyBiz.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Pointcuts with runtime target conditions are assumed to match")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void unmatched() {
		// WHEN
		final List<String> result = AspectPointcuts
				.unmatchedPointcuts(ToyDeadPointcutSecurityAspect.class, DaoToyBiz.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Pointcuts for a missing method, and with wrong arguments, match nothing")
			.containsExactly("readMissing", "readWrongArgs")
			;
		// @formatter:on
	}

}
