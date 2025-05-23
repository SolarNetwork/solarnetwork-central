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

import static org.assertj.core.api.BDDAssertions.then;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
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
		then(loc).isSameAs(user.getLocation());
		user.setLocationId(-2L);
		then(user.getLocation()).as("Location reset").isNull();
	}

	@Test
	public void locationIdPreserveLocationSameId() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocation(loc);
		user.setLocationId(TEST_LOCATION_ID);
		then(loc).as("Location preserved").isSameAs(user.getLocation());
	}

	@Test
	public void locationIdSetFromLocation() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocationId(-2L);
		user.setLocation(loc);
		then(user.getLocationId()).as("Location ID set").isEqualTo(TEST_LOCATION_ID);
	}

	@Test
	public void timeZoneFromLocation() {
		SolarLocation loc = testLocation();
		User user = new User();
		user.setLocation(loc);
		then(user.getTimeZone()).as("TimeZone extracted")
				.isEqualTo(TimeZone.getTimeZone(loc.getTimeZoneId()));
	}

	@Test
	public void timeZoneFromNullLocation() {
		User user = new User();
		then(user.getTimeZone()).as("No location").isNull();
	}

	@Test
	public void timeZoneFromLocationNullTimeZoneId() {
		SolarLocation loc = testLocation();
		loc.setTimeZoneId(null);
		User user = new User();
		user.setLocation(loc);
		then(user.getTimeZone()).as("No time zone ID").isNull();
	}

	@Test
	public void putInternalDataInitial() {
		User u = new User();
		Object prev = u.putInternalDataValue("foo", "bar");
		then(u.getInternalData()).as("Internal data").isEqualTo(Map.of("foo", "bar"));
		then(prev).as("Previous value").isNull();
		then(u.getInternalDataJson()).as("Internal data JSON").isEqualTo("{\"foo\":\"bar\"}");
	}

	@Test
	public void putInternalDataReplace() {
		User u = new User();
		u.setInternalDataJson("{\"foo\":\"bim\"}");
		Object prev = u.putInternalDataValue("foo", "bar");
		then(u.getInternalData()).as("Internal data").isEqualTo(Map.of("foo", "bar"));
		then(prev).as("Previous value").isEqualTo("bim");
		then(u.getInternalDataJson()).as("Internal data JSON").isEqualTo("{\"foo\":\"bar\"}");

	}

	@Test
	public void putInternalDataRemove() {
		User u = new User();
		u.setInternalDataJson("{\"foo\":\"bim\"}");
		Object prev = u.putInternalDataValue("foo", null);
		then(u.getInternalData()).as("Internal data").isEqualTo(Map.of());
		then(prev).as("Previous value").isEqualTo("bim");
		then(u.getInternalDataJson()).as("Internal data JSON").isEqualTo("{}");
	}

}
