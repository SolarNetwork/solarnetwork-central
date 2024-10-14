/* ==================================================================
 * BaseIdentifiableUserModifiableEntityTests.java - 8/10/2024 4:01:12 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.PrefixedTextEncryptor;

/**
 * Test cases for the {@link BaseIdentifiableUserModifiableEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BaseIdentifiableUserModifiableEntityTests {

	private static final class TestEntity
			extends BaseIdentifiableUserModifiableEntity<TestEntity, UserLongCompositePK> {

		private static final long serialVersionUID = 7716175118480893490L;

		private TestEntity(UserLongCompositePK id, Instant created) {
			super(id, created);
		}

		@Override
		public TestEntity copyWithId(UserLongCompositePK id) {
			TestEntity copy = new TestEntity(id, getCreated());
			copyTo(copy);
			return copy;
		}

	}

	private PrefixedTextEncryptor encryptor;

	@BeforeEach
	public void setup() {
		encryptor = PrefixedTextEncryptor.aesTextEncryptor(randomString(), "12345678");
	}

	@Test
	public void maskSensitive() {
		// GIVEN
		TestEntity entity = new TestEntity(new UserLongCompositePK(randomLong(), randomLong()),
				Instant.now());

		Map<String, Object> props = Map.of("foo", "bar", "bim", "bam");
		entity.setServiceProps(props);
		entity.setServiceIdentifier(randomString());

		// WHEN
		entity.maskSensitiveInformation((serviceIdentifier) -> {
			then(serviceIdentifier).as("Entity service identifier passed to function")
					.isSameAs(entity.getServiceIdentifier());
			return Set.of("foo");
		}, encryptor);

		// THEN
		// @formatter:off
		then(entity.getServiceProps())
			.as("Service properties map instance changed")
			.isNotSameAs(props)
			.as("Has same keys as input props")
			.containsOnlyKeys(props.keySet())
			.as("Non-secure key value unchanged")
			.containsEntry("bim", props.get("bim"))
			.as("Secure key value encrypted")
			.hasEntrySatisfying("foo", val -> {
				then(val)
					.asInstanceOf(InstanceOfAssertFactories.STRING)
					.as("Encrypted value has encryptor prefix")
					.startsWith(encryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						then(encryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo(props.get("foo"))
							;
					})
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void unmaskSensitive() {
		// GIVEN
		TestEntity entity = new TestEntity(new UserLongCompositePK(randomLong(), randomLong()),
				Instant.now());

		String plainText = randomString();
		Map<String, Object> props = Map.of("foo", encryptor.encrypt(plainText), "bim", "bam");
		entity.setServiceProps(props);
		entity.setServiceIdentifier(randomString());

		// WHEN
		entity.unmaskSensitiveInformation((serviceIdentifier) -> {
			then(serviceIdentifier).as("Entity service identifier passed to function")
					.isSameAs(entity.getServiceIdentifier());
			return Set.of("foo");
		}, encryptor);

		// THEN
		// @formatter:off
		then(entity.getServiceProps())
			.as("Service properties map instance changed")
			.isNotSameAs(props)
			.as("Has same keys as input props")
			.containsOnlyKeys(props.keySet())
			.as("Non-secure key value unchanged")
			.containsEntry("bim", props.get("bim"))
			.as("Secure key value decrypted")
			.hasEntrySatisfying("foo", val -> {
				then(val)
					.asInstanceOf(InstanceOfAssertFactories.STRING)
					.as("Decrypted value is plaintext value")
					.isEqualTo(plainText)
					;
			})
			;
		// @formatter:on
	}

}
