/* ==================================================================
 * UserSecretEntityTests.java - 23/03/2025 1:15:18 pm
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

package net.solarnetwork.central.user.domain.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * Test cases for the {@link UserSecretEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserSecretEntityTests {

	@Test
	public void stringValue() {
		// GIVEN
		String secret = randomString();

		var entity = new UserSecretEntity(randomLong(), randomString(), randomString(), now(), now(),
				secret);

		// THEN
		// @formatter:off
		then(entity.secretValue())
			.as("Secret string value is preserved")
			.isEqualTo(secret)
			;
		
		then(entity.secret())
			.as("Secret raw value is UTF-8 bytes")
			.isEqualTo(secret.getBytes(UTF_8))
			;
		// @formatter:on
	}

	@Test
	public void asJson() {
		// GIVEN
		Long userId = randomLong();
		String topicId = randomString();
		String key = randomString();
		String secret = randomString();

		var ts = now().truncatedTo(MILLIS);
		var mod = ts.plusSeconds(1);
		var entity = new UserSecretEntity(userId, topicId, key, ts, mod, secret);

		// WHEN
		String result = getJSONString(entity);

		then(result).as("JSON encoding").isEqualTo("""
				{"userId":%d,"topic":%s,"key":%s,"created":%s,"modified":%s}""".formatted(userId,
				getJSONString(topicId), getJSONString(key), getJSONString(ts), getJSONString(mod)));
	}

}
