/* ==================================================================
 * AccountTests.java - 20/07/2020 3:57:29 PM
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

package net.solarnetwork.central.user.billing.snf.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.ZoneId;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * Test cases for the {@link Account} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AccountTests {

	@Test
	public void zone_notPresent() {
		// GIVEN
		Account account = new Account();

		// WHEN
		ZoneId zone = account.getTimeZone();

		// THEN
		assertThat("Zone is null", zone, nullValue());
	}

	@Test
	public void zone_invalidValue() {
		// GIVEN
		Account account = new Account();
		Address addr = new Address();
		addr.setTimeZoneId("foo/bar");
		account.setAddress(addr);

		// WHEN
		ZoneId zone = account.getTimeZone();

		// THEN
		assertThat("Zone is null", zone, nullValue());
	}

	@Test
	public void zone_valid() {
		// GIVEN
		Account account = new Account();
		Address addr = new Address();
		addr.setTimeZoneId("Pacific/Auckland");
		account.setAddress(addr);

		// WHEN
		ZoneId zone = account.getTimeZone();

		// THEN
		assertThat("Zone", zone, equalTo(ZoneId.of(addr.getTimeZoneId())));
	}
}
