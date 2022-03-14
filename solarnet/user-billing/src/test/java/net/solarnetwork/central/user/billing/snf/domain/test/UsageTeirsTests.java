/* ==================================================================
 * UsageTeirsTests.java - 27/05/2021 8:02:28 PM
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

package net.solarnetwork.central.user.billing.snf.domain.test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.UsageTier;
import net.solarnetwork.central.user.billing.snf.domain.UsageTiers;

/**
 * Test cases for the {@link UsageTiers} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UsageTeirsTests {

	@Test
	public void asString() {
		// GIVEN
		// @formatter:off
		UsageTiers tiers = new UsageTiers(asList(
				UsageTier.tier(NodeUsage.DATUM_OUT_KEY, 1,  "0.0000004"),
				UsageTier.tier(NodeUsage.DATUM_OUT_KEY, 10,  "0.0000003"),
				UsageTier.tier(NodeUsage.DATUM_OUT_KEY, 100,  "0.000002"),
				UsageTier.tier(NodeUsage.DATUM_PROPS_IN_KEY, 2,  "0.000004"),
				UsageTier.tier(NodeUsage.DATUM_PROPS_IN_KEY, 20,  "0.000003"),
				UsageTier.tier(NodeUsage.DATUM_PROPS_IN_KEY, 200,  "0.00002"),
				UsageTier.tier(NodeUsage.DATUM_DAYS_STORED_KEY, 3,  "0.00004"),
				UsageTier.tier(NodeUsage.DATUM_DAYS_STORED_KEY, 30,  "0.00003"),
				UsageTier.tier(NodeUsage.DATUM_DAYS_STORED_KEY, 300,  "0.0002")
				));

		// WHEN
		String s = tiers.toString();

		// THEN
		assertThat("String output", s,
				  equalTo("| Key                  |  Quantity | Cost         |\n"
				  		+ "|----------------------|-----------|--------------|\n"
				  		+ "| datum-days-stored    |         3 | 0.0000400000 |\n"
				  		+ "| datum-days-stored    |        30 | 0.0000300000 |\n"
				  		+ "| datum-days-stored    |       300 | 0.0002000000 |\n"
				  		+ "| datum-out            |         1 | 0.0000004000 |\n"
				  		+ "| datum-out            |        10 | 0.0000003000 |\n"
				  		+ "| datum-out            |       100 | 0.0000020000 |\n"
				  		+ "| datum-props-in       |         2 | 0.0000040000 |\n"
				  		+ "| datum-props-in       |        20 | 0.0000030000 |\n"
				  		+ "| datum-props-in       |       200 | 0.0000200000 |"));
		// @formatter:on
	}

}
