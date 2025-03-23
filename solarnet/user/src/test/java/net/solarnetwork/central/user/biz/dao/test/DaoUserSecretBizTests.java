/* ==================================================================
 * DaoUserSecretBizTests.java - 22/03/2025 2:26:51 pm
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

package net.solarnetwork.central.user.biz.dao.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import javax.cache.Cache;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
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
import net.solarnetwork.central.user.biz.dao.DaoUserSecretBiz;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.UserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.UserKeyPairFilter;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.domain.UserKeyPair;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.central.user.domain.UserKeyPairInput;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.central.user.domain.UserSecretInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link DaoUserSecretBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserSecretBizTests {

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
	private String hmacKey;
	private String secretSalt;

	private DaoUserSecretBiz biz;

	@BeforeEach
	public void setup() {
		certificateService = new BCCertificateService();
		hmacKey = randomString();
		secretSalt = randomString().substring(0, 8);
		biz = new DaoUserSecretBiz(clock, secretsBiz, certificateService, keyPairDao, secretDao, hmacKey,
				secretSalt);
	}

	@Test
	public void saveUserKeyPair() {
		// GIVEN
		final Long userId = randomLong();
		final String key = randomString();
		final String pem = utf8StringResource("test-rsa-private-key-01.pem", getClass());

		final var keyPairPk = new UserStringCompositePK(userId, key);
		final var daoKeyPairEntity = new UserKeyPairEntity(keyPairPk, randomBytes());
		given(keyPairDao.create(eq(userId), any())).willReturn(keyPairPk);
		given(keyPairDao.get(keyPairPk)).willReturn(daoKeyPairEntity);

		// WHEN
		UserKeyPairInput input = new UserKeyPairInput();
		input.setKey(key);
		input.setPassword(randomString());
		input.setKeyPem(pem);
		UserKeyPair result = biz.saveUserKeyPair(userId, input);

		// THEN
		// @formatter:off
		final String passwordKey = daoKeyPairEntity.secretsBizKey();
		then(secretsBiz).should().putSecret(eq(passwordKey), passwordCaptor.capture());
		
		final String expectedPassword = Base64.getUrlEncoder().encodeToString(
				new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hmacKey).hmac(input.getPassword()));
		and.then(passwordCaptor.getValue())
			.as("Password saved to SecretsBiz is Base64 encoded HmacSha256 of given password + environment key")
			.isEqualTo(expectedPassword)
			;
		
		then(keyPairDao).should().create(eq(userId), keyPairEntityCaptor.capture());
		and.then(keyPairEntityCaptor.getValue())
			.as("Non-null UserKeyPairEntity persisted to DAO")
			.isNotNull()
			.as("UserKeyPairEntity persisted to DAO is for primary key based on given user ID and key")
			.returns(keyPairPk, from(UserKeyPairEntity::getId))
			.as("Creation date assigned from clock")
			.returns(clock.instant(), from(UserKeyPairEntity::getCreated))
			.satisfies(entity -> {
				and.then(entity.getModified())
					.as("Modification date is same instance as creation date")
					.isSameAs(entity.getCreated())
					;
				
				KeyPair expectedKp = RsaKeyHelper.parseKeyPair(pem);
				KeyPair kp = entity.keyPair(expectedPassword);
				and.then(kp)
					.as("KeyPair encrypted with expected password")
					.isNotNull()
					;
				and.then(kp.getPrivate())
					.as("Private key is RSA key from PEM")
					.isEqualTo(expectedKp.getPrivate())
					;
				and.then(kp.getPublic())
					.as("Public key is RSA key from PEM")
					.isEqualTo(expectedKp.getPublic())
					;
			})
			;
		
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(daoKeyPairEntity)
			;
		// @formatter:on
	}

	@Test
	public void deleteUserKeyPair() {
		// GIVEN
		final Long userId = randomLong();
		final String key = randomString();

		final var keyPairPk = new UserStringCompositePK(userId, key);
		final var daoKeyPairEntity = new UserKeyPairEntity(keyPairPk, randomBytes());

		given(keyPairDao.entityKey(keyPairPk)).willReturn(daoKeyPairEntity);

		// WHEN
		biz.deleteUserKeyPair(userId, key);

		// THEN
		// @formatter:off
		final String passwordKey = daoKeyPairEntity.secretsBizKey();
		then(secretsBiz).should().deleteSecret(eq(passwordKey));
		
		then(keyPairDao).should().delete(keyPairEntityCaptor.capture());
		and.then(keyPairEntityCaptor.getValue())
			.as("Non-null UserKeyPairEntity persisted to DAO")
			.isNotNull()
			.as("UserKeyPairEntity passed to DAO is for primary key based on given user ID and key")
			.returns(keyPairPk, from(UserKeyPairEntity::getId))
			;
		// @formatter:on
	}

	@Test
	public void listKeyPairsForUser() {
		// GIVEN
		final Long userId = randomLong();
		final String key = randomString();

		final var keyPairPk = new UserStringCompositePK(userId, key);
		final var daoKeyPairEntity = new UserKeyPairEntity(keyPairPk, randomBytes());

		final var daoFilterResults = new BasicFilterResults<>(List.of(daoKeyPairEntity));
		given(keyPairDao.findFiltered(any())).willReturn(daoFilterResults);

		// WHEN
		BasicUserSecretFilter filter = new BasicUserSecretFilter();
		FilterResults<? extends UserKeyPair, UserStringCompositePK> result = biz
				.listKeyPairsForUser(userId, filter);

		// THEN
		// @formatter:off
		then(keyPairDao).should().findFiltered(keyPairFilterCaptor.capture());
		and.then(keyPairFilterCaptor.getValue())
			.as("Non-null filter passed to DAO")
			.isNotNull()
			.as("Filter has user IDs forced to given user ID")
			.returns(new Long[] { userId }, from(UserKeyPairFilter::getUserIds))
			;
		
		and.then(result)
			.as("Rersult from DAO returned")
			.isSameAs(daoFilterResults)
			;
		// @formatter:on
	}

	@Test
	public void saveUserSecret() {
		// GIVEN
		final Long userId = randomLong();
		final String topic = "%s/%s".formatted(randomString(), randomString());
		final String key = randomString();

		// look up user key pair to encrypt secret with
		final var keyPairPk = new UserStringCompositePK(userId, topic);
		final var keyPair = RsaKeyHelper.generateKeyPair();
		final var keyPairPassword = randomString();
		final var daoKeyPairEntity = UserKeyPairEntity.withKeyPair(userId, topic, clock.instant(),
				clock.instant(), keyPair, keyPairPassword, certificateService);
		given(keyPairDao.get(keyPairPk)).willReturn(daoKeyPairEntity);

		// look up key pair password from secrets biz
		final var passwordKey = daoKeyPairEntity.secretsBizKey();
		given(secretsBiz.getSecret(passwordKey)).willReturn(keyPairPassword);

		final var secretPk = new UserStringStringCompositePK(userId, topic, key);
		final var daoSecretEntity = new UserSecretEntity(secretPk, randomBytes());
		given(secretDao.create(eq(userId), eq(topic), any())).willReturn(secretPk);
		given(secretDao.get(secretPk)).willReturn(daoSecretEntity);

		// WHEN
		final String secretValue = randomString();
		UserSecretInput input = new UserSecretInput();
		input.setTopic(topic);
		input.setKey(key);
		input.setSecretValue(secretValue);
		UserSecret result = biz.saveUserSecret(userId, input);

		// THEN
		// @formatter:off
		then(secretDao).should().create(eq(userId), eq(topic), secretEntityCaptor.capture());
		and.then(secretEntityCaptor.getValue())
			.as("Non-null UserSecretEntity persisted to DAO")
			.isNotNull()
			.as("UserSecretEntity persisted to DAO is for primary key based on given user ID and key")
			.returns(secretPk, from(UserSecretEntity::getId))
			.as("Creation date assigned from clock")
			.returns(clock.instant(), from(UserSecretEntity::getCreated))
			.satisfies(entity -> {
				and.then(entity.getModified())
					.as("Modification date is same instance as creation date")
					.isSameAs(entity.getCreated())
					;
				
				byte[] decryptedDaoSecret = new RsaSecretEncryptor(keyPair, RsaAlgorithm.DEFAULT, secretSalt, true)
						.decrypt(entity.secret());				
				and.then(decryptedDaoSecret)
					.as("Secret value persisted in encrypted form, using UserKeyPair associated with topic ID")
					.isEqualTo(secretValue.getBytes(UTF_8))
					;
			})
			;
		
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(daoSecretEntity)
			;
		// @formatter:on
	}

	@Test
	public void deleteUserSecret() {
		// GIVEN
		final Long userId = randomLong();
		final String topic = randomString();
		final String key = randomString();

		final var secretPk = new UserStringStringCompositePK(userId, topic, key);
		final var daoSecretEntity = new UserSecretEntity(secretPk, randomBytes());

		given(secretDao.entityKey(secretPk)).willReturn(daoSecretEntity);

		// WHEN
		biz.deleteUserSecret(userId, topic, key);

		// THEN
		// @formatter:off
		then(secretDao).should().delete(secretEntityCaptor.capture());
		and.then(secretEntityCaptor.getValue())
			.as("Non-null UserSecretEntity persisted to DAO")
			.isNotNull()
			.as("UserSecretEntity passed to DAO is for primary key based on given user ID, topic ID, and key")
			.returns(secretPk, from(UserSecretEntity::getId))
			;
		// @formatter:on
	}

	@Test
	public void listSecretsForUser() {
		// GIVEN
		final Long userId = randomLong();
		final String topic = randomString();
		final String key = randomString();

		final var secretPk = new UserStringStringCompositePK(userId, topic, key);
		final var daoSecretEntity = new UserSecretEntity(secretPk, randomBytes());

		final var daoFilterResults = new BasicFilterResults<>(List.of(daoSecretEntity));
		given(secretDao.findFiltered(any())).willReturn(daoFilterResults);

		// WHEN
		BasicUserSecretFilter filter = new BasicUserSecretFilter();
		FilterResults<? extends UserSecret, UserStringStringCompositePK> result = biz
				.listSecretsForUser(userId, filter);

		// THEN
		// @formatter:off
		then(secretDao).should().findFiltered(secretFilterCaptor.capture());
		and.then(secretFilterCaptor.getValue())
			.as("Non-null filter passed to DAO")
			.isNotNull()
			.as("Filter has user IDs forced to given user ID")
			.returns(new Long[] { userId }, from(UserSecretFilter::getUserIds))
			;
		
		and.then(result)
			.as("Rersult from DAO returned")
			.isSameAs(daoFilterResults)
			;
		// @formatter:on
	}

}
