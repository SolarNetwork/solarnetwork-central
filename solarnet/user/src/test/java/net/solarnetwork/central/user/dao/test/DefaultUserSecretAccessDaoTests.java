/* ==================================================================
 * DefaultUserSecretAccessDaoTests.java - 23/03/2025 4:18:46 pm
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

package net.solarnetwork.central.user.dao.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.RsaAlgorithm;
import org.springframework.security.crypto.encrypt.RsaSecretEncryptor;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.RsaKeyHelper;
import net.solarnetwork.central.user.dao.DefaultUserSecretAccessDao;
import net.solarnetwork.central.user.dao.UserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.UserKeyPairFilter;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link DefaultUserSecretAccessDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DefaultUserSecretAccessDaoTests {

	@Mock
	private UserKeyPairEntityDao keyPairDao;

	@Mock
	private UserSecretEntityDao secretDao;

	@Mock
	private SecretsBiz secretsBiz;

	@Mock
	private Cache<UserStringStringCompositePK, UserSecretEntity> secretCache;

	@Mock
	private Cache<UserStringCompositePK, KeyPair> keyPairCache;

	@Captor
	private ArgumentCaptor<String> passwordCaptor;

	@Captor
	private ArgumentCaptor<UserKeyPairEntity> keyPairEntityCaptor;

	@Captor
	private ArgumentCaptor<UserKeyPairFilter> keyPairFilterCaptor;

	@Captor
	private ArgumentCaptor<UserSecretEntity> secretEntityCaptor;

	@Captor
	private ArgumentCaptor<UserSecretFilter> secretFilterCaptor;

	@Captor
	private ArgumentCaptor<KeyPair> keyPairCaptor;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);
	private BCCertificateService certificateService;
	private String secretSalt;

	private DefaultUserSecretAccessDao dao;

	@BeforeEach
	public void setup() {
		certificateService = new BCCertificateService();
		secretSalt = randomString().substring(0, 8);
		dao = new DefaultUserSecretAccessDao(secretsBiz, keyPairDao, secretDao, secretSalt);
	}

	@Test
	public void getUserSecret() {
		// GIVEN
		final Long userId = randomLong();
		final String topicId = randomString();
		final String key = randomString();

		final var secretPk = new UserStringStringCompositePK(userId, topicId, key);
		final var daoSecretEntity = new UserSecretEntity(secretPk, randomBytes());

		given(secretDao.get(secretPk)).willReturn(daoSecretEntity);

		// WHEN
		UserSecret result = dao.getUserSecret(userId, topicId, key);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(daoSecretEntity)
			;
		// @formatter:on
	}

	@Test
	public void getUserSecret_withCache_miss() {
		// GIVEN
		dao.setSecretCache(secretCache);

		final Long userId = randomLong();
		final String topicId = randomString();
		final String key = randomString();

		final var secretPk = new UserStringStringCompositePK(userId, topicId, key);
		final var daoSecretEntity = new UserSecretEntity(secretPk, randomBytes());

		given(secretDao.get(secretPk)).willReturn(daoSecretEntity);

		// WHEN
		UserSecret result = dao.getUserSecret(userId, topicId, key);

		// THEN
		then(secretCache).should().get(secretPk);

		then(secretCache).should().put(eq(secretPk), same(daoSecretEntity));

		// @formatter:off
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(daoSecretEntity)
			;
		// @formatter:on
	}

	@Test
	public void getUserSecret_withCache_hit() {
		// GIVEN
		dao.setSecretCache(secretCache);

		final Long userId = randomLong();
		final String topicId = randomString();
		final String key = randomString();

		final var secretPk = new UserStringStringCompositePK(userId, topicId, key);
		final var daoSecretEntity = new UserSecretEntity(secretPk, randomBytes());

		given(secretCache.get(secretPk)).willReturn(daoSecretEntity);

		// WHEN
		UserSecret result = dao.getUserSecret(userId, topicId, key);

		// THEN
		then(secretCache).shouldHaveNoMoreInteractions();

		then(secretDao).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Result from cache returned")
			.isSameAs(daoSecretEntity)
			;
		// @formatter:on
	}

	@Test
	public void decryptUserSecret() {
		// GIVEN
		final Long userId = randomLong();
		final String topicId = "%s/%s".formatted(randomString(), randomString());
		final String key = randomString();

		// look up user key pair to encrypt secret with
		final var keyPairPk = new UserStringCompositePK(userId, topicId);
		final var keyPair = RsaKeyHelper.generateKeyPair();
		final var keyPairPassword = randomString();
		final var daoKeyPairEntity = UserKeyPairEntity.withKeyPair(userId, topicId, clock.instant(),
				clock.instant(), keyPair, keyPairPassword, certificateService);
		given(keyPairDao.get(keyPairPk)).willReturn(daoKeyPairEntity);

		// look up key pair password from secrets dao
		final var passwordKey = daoKeyPairEntity.secretsBizKey();
		given(secretsBiz.getSecret(passwordKey)).willReturn(keyPairPassword);

		var encryptor = new RsaSecretEncryptor(keyPair, RsaAlgorithm.DEFAULT, secretSalt, true);

		final String secretValue = randomString();
		final var secret = new UserSecretEntity(userId, topicId, key, clock.instant(), clock.instant(),
				encryptor.encrypt(secretValue.getBytes(UTF_8)));

		// WHEN
		byte[] result = dao.decryptSecretValue(secret);

		// THEN
		// @formatter:off
		and.then(new String(result, UTF_8))
			.as("Decrypted secret value returned, using UserKeyPair to decrypt")
			.isEqualTo(secretValue)
			;
		// @formatter:on
	}

	@Test
	public void decryptUserSecret_withCache_miss() {
		// GIVEN
		dao.setKeyPairCache(keyPairCache);

		final Long userId = randomLong();
		final String topicId = "%s/%s".formatted(randomString(), randomString());
		final String key = randomString();

		// look up user key pair to encrypt secret with
		final var keyPairPk = new UserStringCompositePK(userId, topicId);
		final var keyPair = RsaKeyHelper.generateKeyPair();
		final var keyPairPassword = randomString();
		final var daoKeyPairEntity = UserKeyPairEntity.withKeyPair(userId, topicId, clock.instant(),
				clock.instant(), keyPair, keyPairPassword, certificateService);
		given(keyPairDao.get(keyPairPk)).willReturn(daoKeyPairEntity);

		// look up key pair password from secrets dao
		final var passwordKey = daoKeyPairEntity.secretsBizKey();
		given(secretsBiz.getSecret(passwordKey)).willReturn(keyPairPassword);

		var encryptor = new RsaSecretEncryptor(keyPair, RsaAlgorithm.DEFAULT, secretSalt, true);

		final String secretValue = randomString();
		final var secret = new UserSecretEntity(userId, topicId, key, clock.instant(), clock.instant(),
				encryptor.encrypt(secretValue.getBytes(UTF_8)));

		// WHEN
		byte[] result = dao.decryptSecretValue(secret);

		// THEN
		then(keyPairCache).should().get(keyPairPk);

		then(keyPairCache).should().put(eq(keyPairPk), keyPairCaptor.capture());

		// @formatter:off
		and.then(keyPairCaptor.getValue())
			.as("Non-null KeyPair persisted to cache")
			.isNotNull()
			.satisfies(kp -> {
				and.then(kp.getPrivate())
					.as("Private key is from UserKeyPairEntity")
					.isEqualTo(keyPair.getPrivate())
					;
				and.then(kp.getPublic())
					.as("Public key is from UserKeyPairEntity")
					.isEqualTo(keyPair.getPublic())
					;
			})
			;
	

		and.then(new String(result, UTF_8))
			.as("Decrypted secret value returned, using UserKeyPair to decrypt")
			.isEqualTo(secretValue)
			;
		// @formatter:on
	}

	@Test
	public void decryptUserSecret_withCache_hit() {
		// GIVEN
		dao.setKeyPairCache(keyPairCache);

		final Long userId = randomLong();
		final String topicId = "%s/%s".formatted(randomString(), randomString());
		final String key = randomString();

		// look up user key pair to encrypt secret with
		final var keyPairPk = new UserStringCompositePK(userId, topicId);
		final var keyPair = RsaKeyHelper.generateKeyPair();

		given(keyPairCache.get(keyPairPk)).willReturn(keyPair);

		var encryptor = new RsaSecretEncryptor(keyPair, RsaAlgorithm.DEFAULT, secretSalt, true);

		final String secretValue = randomString();
		final var secret = new UserSecretEntity(userId, topicId, key, clock.instant(), clock.instant(),
				encryptor.encrypt(secretValue.getBytes(UTF_8)));

		// WHEN
		byte[] result = dao.decryptSecretValue(secret);

		// THEN
		then(keyPairCache).shouldHaveNoMoreInteractions();

		then(keyPairDao).shouldHaveNoInteractions();

		then(secretsBiz).shouldHaveNoInteractions();

		// @formatter:off
		and.then(new String(result, UTF_8))
			.as("Decrypted secret value returned, using UserKeyPair to decrypt")
			.isEqualTo(secretValue)
			;
		// @formatter:on
	}

}
