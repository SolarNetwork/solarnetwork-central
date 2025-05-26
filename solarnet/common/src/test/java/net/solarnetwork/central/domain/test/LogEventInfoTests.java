/* ==================================================================
 * LogEventInfoTests.java - 27/05/2025 7:36:13 am
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

package net.solarnetwork.central.domain.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.LogEventInfo;

/**
 * Test cases for the {@link LogEventInfo} class.
 * 
 * @author matt
 * @version 1.0
 */
public class LogEventInfoTests {

	@Test
	public void create_listTags_noExtra() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data);

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list")
			.containsExactly("a", "b", "c")
			;
		// @formatter:on
	}

	@Test
	public void create_listTags_withExtra() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data, "d", "e", "f");

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list with extras")
			.containsExactly("a", "b", "c", "d", "e", "f")
			;
		// @formatter:on
	}

	@Test
	public void create_listTags_nullExtra() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data, (String[]) null);

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list with extras")
			.containsExactly("a", "b", "c")
			;
		// @formatter:on
	}

	@Test
	public void create_listTags_emptyExtra() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data, new String[0]);

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list with extras")
			.containsExactly("a", "b", "c")
			;
		// @formatter:on
	}

	@Test
	public void create_listTags_withExtraList() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data, List.of("d", "e", "f"));

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list with extras")
			.containsExactly("a", "b", "c", "d", "e", "f")
			;
		// @formatter:on
	}

	@Test
	public void create_listTags_nullExtraList() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data, (List<String>) null);

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list with extras")
			.containsExactly("a", "b", "c")
			;
		// @formatter:on
	}

	@Test
	public void create_listTags_emptyExtraList() {
		// GIVEN
		final List<String> baseTags = List.of("a", "b", "c");
		final String message = randomString();
		final String data = randomString();

		// WHEN
		LogEventInfo result = LogEventInfo.event(baseTags, message, data, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Event created")
			.isNotNull()
			.as("Message from arg")
			.returns(message, from(LogEventInfo::getMessage))
			.as("Data from arg")
			.returns(data, from(LogEventInfo::getData))
			.extracting(LogEventInfo::getTags)
			.asInstanceOf(array(String[].class))
			.as("Tags taken from base list with extras")
			.containsExactly("a", "b", "c")
			;
		// @formatter:on
	}

}
