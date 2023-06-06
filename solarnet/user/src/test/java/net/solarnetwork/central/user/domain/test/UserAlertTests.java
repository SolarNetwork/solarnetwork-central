/* ==================================================================
 * UserAlertTests.java - 7/06/2023 9:36:57 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;

/**
 * Test cases for the {@link UserAlert} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserAlertTests {

	@Test
	public void sourceIdOption_noOptions() {
		// GIVEN
		UserAlert a = new UserAlert();

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("Null returned", result, is(nullValue()));
	}

	@Test
	public void sourceIdOption_null() {
		// GIVEN
		UserAlert a = new UserAlert();
		a.setOptions(Collections.emptyMap());

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("Null returned", result, is(nullValue()));
	}

	@Test
	public void sourceIdOption_string() {
		// GIVEN
		UserAlert a = new UserAlert();
		a.setOptions(Collections.singletonMap(UserAlertOptions.SOURCE_IDS, "a"));

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("String converted to list", result, contains("a"));
	}

	@Test
	public void sourceIdOption_obj() {
		// GIVEN
		UserAlert a = new UserAlert();
		a.setOptions(Collections.singletonMap(UserAlertOptions.SOURCE_IDS, 123));

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("Object converted to list", result, contains("123"));
	}

	@Test
	public void sourceIdOption_array() {
		// GIVEN
		UserAlert a = new UserAlert();
		a.setOptions(Collections.singletonMap(UserAlertOptions.SOURCE_IDS, new String[] { "a", "b" }));

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("Array converted to list", result, contains("a", "b"));
	}

	@Test
	public void sourceIdOption_list() {
		// GIVEN
		UserAlert a = new UserAlert();
		a.setOptions(Collections.singletonMap(UserAlertOptions.SOURCE_IDS, Arrays.asList("a", "b")));

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("Array converted to list", result, contains("a", "b"));
	}

	@Test
	public void sourceIdOption_listObj() {
		// GIVEN
		UserAlert a = new UserAlert();
		a.setOptions(Collections.singletonMap(UserAlertOptions.SOURCE_IDS, Arrays.asList(123, 234)));

		// WHEN
		List<String> result = a.optionSourceIds();

		// THEN
		assertThat("Array converted to list", result, contains("123", "234"));
	}

}
