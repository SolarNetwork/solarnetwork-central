/* ==================================================================
 * UserTests.java - 21/08/2017 6:57:03 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain.test;

import java.util.TimeZone;
import org.junit.Assert;
import org.junit.Test;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.user.domain.User;

/**
 * Test cases for the {@link User} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserTests {

	private static final Long TEST_LOCATION_ID = -1L;

	private SolarLocation testLocation() {
		SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOCATION_ID);
		loc.setCountry("NZ");
		loc.setTimeZoneId("Pacific/Auckland");
		return loc;
	}

	@Test
	public void locationIdResetLocationDifferentId() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocation(loc);
		Assert.assertSame(loc, user.getLocation());
		user.setLocationId(-2L);
		Assert.assertNull("Location reset", user.getLocation());
	}

	@Test
	public void locationIdPreserveLocationSameId() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocation(loc);
		user.setLocationId(TEST_LOCATION_ID);
		Assert.assertSame("Location preserved", loc, user.getLocation());
	}

	@Test
	public void locationIdSetFromLocation() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocationId(-2L);
		user.setLocation(loc);
		Assert.assertEquals("Location ID set", TEST_LOCATION_ID, user.getLocationId());
	}

	@Test
	public void timeZoneFromLocation() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocation(loc);
		Assert.assertEquals("TimeZone extracted", TimeZone.getTimeZone(loc.getTimeZoneId()),
				user.getTimeZone());
	}

	@Test
	public void timeZoneFromNullLocation() {
		User user = new User();
		Assert.assertNull("No location", user.getTimeZone());
	}

	@Test
	public void timeZoneFromLocationNullTimeZoneId() {
		SolarLocation loc = testLocation();
		loc.setTimeZoneId(null);
		User user = new User();
		user.setLocation(loc);
		Assert.assertNull("No time zone ID", user.getTimeZone());
	}

}
