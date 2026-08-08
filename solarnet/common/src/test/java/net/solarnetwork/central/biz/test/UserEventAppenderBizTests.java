/* ==================================================================
 * UserEventAppenderBizTests.java - 8 Aug 2026 4:53:43 pm
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

package net.solarnetwork.central.biz.test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.test.CommonTestUtils;

/**
 * Test cases for the {@link UserEventAppenderBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserEventAppenderBizTests {

	@Test
	public void taggedTopic() {
		// GIVEN
		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a", "b", "c" }, "Test message.", "{}");

		// WHEN
		final String result = UserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN.apply(event);

		// THEN
		then(result).isEqualTo("user/%d/event/a/b/c".formatted(event.getUserId()));
	}

	@Test
	public void taggedTopic_oneTag() {
		// GIVEN
		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a" }, "Test message.", "{}");

		// WHEN
		final String result = UserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN.apply(event);

		// THEN
		then(result).isEqualTo("user/%d/event/a".formatted(event.getUserId()));
	}

	@Test
	public void taggedErrorTopic() {
		// GIVEN
		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a", "b", "c" }, "Test message.", "{}");

		// WHEN
		final String result = UserEventAppenderBiz.SOLARFLUX_TAGGED_ERROR_TOPIC_FN.apply(event, null);

		// THEN
		then(result).isEqualTo("user/%d/event/a/error/b/c".formatted(event.getUserId()));
	}

	@Test
	public void taggedErrorTopic_oneTag() {
		// GIVEN
		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a" }, "Test message.", "{}");

		// WHEN
		final String result = UserEventAppenderBiz.SOLARFLUX_TAGGED_ERROR_TOPIC_FN.apply(event, null);

		// THEN
		then(result).isEqualTo("user/%d/event/a/error".formatted(event.getUserId()));
	}

	@Test
	public void reducedSizeData() {
		// GIVEN
		final String largeContent = CommonTestUtils.utf8StringResource("large-event-data-01.json",
				getClass());

		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a", "b", "c" }, "Test message.", largeContent);

		// WHEN
		final String result = UserEventAppenderBiz.limitedSizeEventData(event, 8192);

		// THEN
		// @formatter:off
		then(result)
			.as("Data has been altered.")
			.isNotEqualTo(largeContent)
			.asInstanceOf(JSON)
			.isObject()
			.isEqualTo("""
					{
						  "cp":        "chgr02A"
						, "messageId": "9e850331-0000-0000-0000-b6ee304b2b29"
						, "action":    "StopTransaction"
						, "message": {
							  "transactionId": 621397
							, "timestamp":     "2026-08-06T21:43:12.000Z"
							, "meterStop":     83866817
							, "reason":        "EVDisconnected"
						}
					}
					""")
			;
		// @formatter:on
	}

	@Test
	public void reducedSizeData_nothingLeft() {
		// GIVEN
		final String largeContent = CommonTestUtils.utf8StringResource("large-event-data-02.json",
				getClass());

		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a", "b", "c" }, "Test message.", largeContent);

		// WHEN
		final String result = UserEventAppenderBiz.limitedSizeEventData(event, 8192);

		// THEN
		// @formatter:off
		then(result)
			.as("Data has been altered.")
			.isNotEqualTo(largeContent)
			.asInstanceOf(JSON)
			.isObject()
			.isEqualTo("""
					{"message":"Content too large to preserve."}
					""")
			;
		// @formatter:on
	}

}
