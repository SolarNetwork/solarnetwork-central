/* ==================================================================
 * NamedCostTests.java - 27/05/2025 10:24:11 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomDecimal;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.snf.domain.NamedCost;

/**
 * Test cases for the {@link NamedCost} class.
 *
 * @author matt
 * @version 1.0
 */
public class NamedCostTests {

	@Test
	public void construct() {
		// GIVEN
		String name = randomString();
		BigInteger quantity = BigInteger.valueOf(randomLong());
		BigDecimal cost = randomDecimal();

		// WHEN
		NamedCost c = new NamedCost(name, quantity, cost);

		// @formatter:off
		then(c)
			.as("Name from constructor arg")
			.returns(name, from(NamedCost::getName))
			.as("Quantity from constructor arg")
			.returns(quantity, from(NamedCost::getQuantity))
			.as("Cost from constructor arg")
			.returns(cost, from(NamedCost::getCost))
			;
		// @formatter:on
	}

	@Test
	public void construct_withNullName() {
		// GIVEN
		BigInteger quantity = BigInteger.valueOf(randomLong());
		BigDecimal cost = randomDecimal();

		// THEN
		thenIllegalArgumentException().isThrownBy(() -> new NamedCost(null, quantity, cost));
	}

	@Test
	public void construct_withNulls() {
		// GIVEN
		String name = randomString();

		// WHEN
		NamedCost c = new NamedCost(name, null, null);

		// @formatter:off
		then(c)
			.as("Name from constructor arg")
			.returns(name, from(NamedCost::getName))
			.as("Quantity defaults to 0")
			.returns(BigInteger.ZERO, from(NamedCost::getQuantity))
			.as("Cost defaults to 0")
			.returns(BigDecimal.ZERO, from(NamedCost::getCost))
			;
		// @formatter:on
	}

	@Test
	public void equality() {
		// GIVEN
		NamedCost c = new NamedCost("a", BigInteger.valueOf(1), new BigDecimal("1.23"));

		// WHEN
		NamedCost other = new NamedCost("a", BigInteger.valueOf(1), new BigDecimal("1.23"));

		// THEN
		then(other).isEqualTo(c);
		then(other.isSameAs(c)).isTrue();
		then(other.differsFrom(c)).isFalse();
	}

	@Test
	public void equality_withoutQuantity() {
		// GIVEN
		NamedCost c = new NamedCost("a", null, new BigDecimal("1.23"));

		// WHEN
		NamedCost other = new NamedCost("a", null, new BigDecimal("1.23"));

		// THEN
		then(other).isEqualTo(c);
		then(other.isSameAs(c)).isTrue();
		then(other.differsFrom(c)).isFalse();
	}

	@Test
	public void equality_withoutCost() {
		// GIVEN
		NamedCost c = new NamedCost("a", BigInteger.valueOf(1), null);

		// WHEN
		NamedCost other = new NamedCost("a", BigInteger.valueOf(1), null);

		// THEN
		then(other).isEqualTo(c);
		then(other.isSameAs(c)).isTrue();
		then(other.differsFrom(c)).isFalse();
	}

	@Test
	public void equality_notByName() {
		// GIVEN
		NamedCost c = new NamedCost("a", BigInteger.valueOf(1), new BigDecimal("1.23"));

		// WHEN
		NamedCost other = new NamedCost("b", BigInteger.valueOf(1), new BigDecimal("1.23"));

		// THEN
		then(other).isNotEqualTo(c);
		then(other.isSameAs(c)).isFalse();
		then(other.differsFrom(c)).isTrue();
	}

	@Test
	public void equality_notByQuantity() {
		// GIVEN
		NamedCost c = new NamedCost("a", BigInteger.valueOf(1), new BigDecimal("1.23"));

		// WHEN
		NamedCost other = new NamedCost("a", BigInteger.valueOf(2), new BigDecimal("1.23"));

		// THEN
		then(other).isNotEqualTo(c);
		then(other.isSameAs(c)).isFalse();
		then(other.differsFrom(c)).isTrue();
	}

	@Test
	public void equality_notByCost() {
		// GIVEN
		NamedCost c = new NamedCost("a", BigInteger.valueOf(1), new BigDecimal("1.23"));

		// WHEN
		NamedCost other = new NamedCost("a", BigInteger.valueOf(1), new BigDecimal("2.34"));

		// THEN
		then(other).isNotEqualTo(c);
		then(other.isSameAs(c)).isFalse();
		then(other.differsFrom(c)).isTrue();
	}

}
