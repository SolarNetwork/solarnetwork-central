/* ==================================================================
 * UserKeyPairEntityTests.java - 22/03/2025 11:46:41 am
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

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.domain.UserKeyPairEntity.withKeyPair;
import static net.solarnetwork.codec.jackson.JsonUtils.getJSONString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.BYTE_ARRAY;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.security.RsaKeyHelper;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link UserKeyPairEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserKeyPairEntityTests {

	@Test
	public void createFromKeyPair() {
		// GIVEN
		Long userId = randomLong();
		String key = randomString();
		String password = randomString();

		var service = new BCCertificateService();

		KeyPair kp = RsaKeyHelper.generateKeyPair();

		// WHEN
		var ts = now().truncatedTo(MILLIS);
		var result = withKeyPair(userId, key, ts, ts.plusSeconds(1), kp, password, service);

		// THEN
		// @formatter:off
		then(result)
			.as("Entity created")
			.isNotNull()
			.as("Given user ID used")
			.returns(userId, from(UserKeyPairEntity::getUserId))
			.as("Given key used")
			.returns(key, from(UserKeyPairEntity::getKey))
			.as("Given creation date used")
			.returns(ts, from(UserKeyPairEntity::getCreated))
			.as("Given modification date used")
			.returns(ts.plusSeconds(1), from(UserKeyPairEntity::getModified))
			.extracting(UserKeyPairEntity::keyStoreData, BYTE_ARRAY)
			.as("Key store data encoded")
			.isNotEmpty()
			;
		// @formatter:on
	}

	@Test
	public void asJson() {
		// GIVEN
		Long userId = randomLong();
		String key = randomString();
		String password = randomString();

		var service = new BCCertificateService();

		KeyPair kp = RsaKeyHelper.generateKeyPair();

		var ts = now().truncatedTo(MILLIS);
		var mod = ts.plusSeconds(1);
		var entity = withKeyPair(userId, key, ts, mod, kp, password, service);

		// WHEN
		String result = getJSONString(entity);

		then(result).as("JSON encoding").isEqualTo("""
				{"userId":%d,"key":%s,"created":%s,"modified":%s}""".formatted(userId,
				getJSONString(key), getJSONString(ts), getJSONString(mod)));
	}

}
