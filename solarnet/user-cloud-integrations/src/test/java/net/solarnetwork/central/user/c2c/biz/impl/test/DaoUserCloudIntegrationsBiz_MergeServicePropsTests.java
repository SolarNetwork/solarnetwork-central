/* ==================================================================
 * DaoUserCloudIntegrationsBiz_MergeServicePropsTests.java - 21/07/2026 11:14:52 am
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

package net.solarnetwork.central.user.c2c.biz.impl.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.MutableClock;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.ModifiableServicePropertiesDao.MergeMode;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.user.c2c.biz.impl.DaoUserCloudIntegrationsBiz;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Test cases for the {@link DaoUserCloudIntegrationsBiz} class handling of
 * service property merging.
 *
 * @author matt
 * @version 1.7
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserCloudIntegrationsBiz_MergeServicePropsTests {

	private static final String TEST_SECURE_SETTING = "watchout";

	private static final String TEST_SERVICE_ID = randomString();

	private static final String TEST_ACCESS_TOKEN = """
			eyJhbGciOiJSUzI1NiJ9.eyJhcHBfdHlwZSI6InN5c3RlbSIsInVzZXJfbmFtZSI6Im1p\
			Y2FoLmJyaWxsQHNreXZpZXd2ZW50dXJlcy5jb20iLCJlbmxfY2lkIjoiNTk4MDUxIiwiZ\
			W5sX3Bhc3N3b3JkX2xhc3RfY2hhbmdlZCI6IjE1OTE2Njg4NjYiLCJhdXRob3JpdGllcy\
			I6WyJST0xFX1VTRVIiXSwiY2xpZW50X2lkIjoiYjdjYmU0YjcwYWRjNWRhOTExZWY1MWN\
			hZWFkZWRmMjQiLCJhdWQiOlsib2F1dGgyLXJlc291cmNlIl0sImlzX2ludGVybmFsX2Fw\
			cCI6ZmFsc2UsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE3NDEwMjc2NzYsI\
			mVubF91aWQiOiIxOTk1MjcyIiwiYXBwX0lkIjoiMTQwOTYyNDQ0Njk0NCIsImp0aSI6Im\
			QyZmFhM2IxLTA1NGEtNDljZC1iMDI0LTk5YjVmMDQ2MDM5MCJ9.IlUBREujj0BdZcHsdr\
			LH8XFutmJOvFjJ0O8zyWDz-UVKLMxGUAbAKKgLeyTGP3ym2Wz5_3WlQ3lTcXzogZSh0Q8\
			tjY34qBCA6tR4dSnt8Lw0sRxxL6n3ZQ_pwfGRVw5e5S1UTGRuRIuIIsVVlej4Bg3MuluE\
			Cd1E1AaHLIXe9co""";
	private static final String TEST_REFRESH_TOKEN = """
			eyJhbGciOiJSUzI1NiJ9.eyJhcHBfdHlwZSI6InN5c3RlbSIsInVzZXJfbmFtZSI6Im1p\
			Y2FoLmJyaWxsQHNreXZpZXd2ZW50dXJlcy5jb20iLCJlbmxfY2lkIjoiNTk4MDUxIiwiZ\
			W5sX3Bhc3N3b3JkX2xhc3RfY2hhbmdlZCI6IjE1OTE2Njg4NjYiLCJhdXRob3JpdGllcy\
			I6WyJST0xFX1VTRVIiXSwiY2xpZW50X2lkIjoiYjdjYmU0YjcwYWRjNWRhOTExZWY1MWN\
			hZWFkZWRmMjQiLCJhdWQiOlsib2F1dGgyLXJlc291cmNlIl0sImlzX2ludGVybmFsX2Fw\
			cCI6ZmFsc2UsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJhdGkiOiJkMmZhYTNiMS0wN\
			TRhLTQ5Y2QtYjAyNC05OWI1ZjA0NjAzOTAiLCJleHAiOjE3NDM1NzEwMjIsImVubF91aW\
			QiOiIxOTk1MjcyIiwiYXBwX0lkIjoiMTQwOTYyNDQ0Njk0NCIsImp0aSI6ImYyYmM3NGQ\
			0LTMxZTAtNDYzYi1iMjZkLWMxZmI3NjM1NTQyZCJ9.LkbUq7mm6wfW8k9zj_wPZ5IRw8g\
			Uywrn4E1LXMNtBzPxgHRnFKUKwR9-8SMjCod1u5xTY_2KjXg0AbnYAs5dmYUDARVfl2sY\
			HFlck7-sbua9nnpcEya3Py0w6ORadX6cyzpg0HRxEXQo3D0GFLCpc4uxbaQZWtLCGx5Ga\
			HxcYmI""";

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Mock
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	@Mock
	private CloudControlConfigurationDao controlDao;

	@Mock
	private CloudDatumStreamPollTaskDao datumStreamPollTaskDao;

	@Mock
	private CloudDatumStreamRakeTaskDao datumStreamRakeTaskDao;

	@Mock
	private CloudIntegrationService integrationService;

	@Mock
	private UserSettingsEntityDao userSettingsDao;

	@Mock
	private CloudDatumStreamSettingsEntityDao datumStreamSettingsDao;

	@Mock
	private ClientAccessTokenDao clientAccessTokenDao;

	@Captor
	private ArgumentCaptor<Map<String, ?>> propsCaptor;

	@Captor
	private ArgumentCaptor<ClientAccessTokenEntity> clientAccessTokenCaptor;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private PrefixedTextEncryptor textEncryptor = PrefixedTextEncryptor.aesTextEncryptor(randomString(),
			randomString());

	private DaoUserCloudIntegrationsBiz biz;

	@BeforeEach
	public void setup() {
		given(integrationService.getId()).willReturn(TEST_SERVICE_ID);
		given(integrationService.getSettingUid()).willReturn(TEST_SERVICE_ID);

		// provide settings to verify masking sensitive values
		List<SettingSpecifier> settings = Arrays.asList(new BasicTextFieldSettingSpecifier("foo", null),
				new BasicTextFieldSettingSpecifier(TEST_SECURE_SETTING, null, true));
		given(integrationService.getSettingSpecifiers()).willReturn(settings);

		biz = new DaoUserCloudIntegrationsBiz(clock, userSettingsDao, integrationDao, datumStreamDao,
				datumStreamSettingsDao, datumStreamMappingDao, datumStreamPropertyDao, controlDao,
				datumStreamPollTaskDao, datumStreamRakeTaskDao, clientAccessTokenDao, textEncryptor,
				Set.of(integrationService));

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		biz.setValidator(factory.getValidator());
	}

	@Test
	public void integrationConfiguration_simple() {
		// GIVEN
		final MergeMode mergeMode = MergeMode.Simple;
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(pk, now(),
				randomString(), TEST_SERVICE_ID);

		given(integrationDao.get(pk)).willReturn(conf);

		final Map<String, Object> daoMergeResult = Map.of("merged", true);
		given(integrationDao.mergeServiceProperties(eq(pk), eq(mergeMode), any()))
				.willReturn(daoMergeResult);

		// WHEN
		final Map<String, Object> props = Map.of("foo", 1, "bim", "bam");
		final Map<String, ?> result = biz.mergeConfigurationServiceProperties(pk, mergeMode, props,
				CloudIntegrationConfiguration.class);

		// THEN
		then(integrationDao).should().mergeServiceProperties(eq(pk), eq(mergeMode), eq(props));
		// @formatter:off
		and.then(result)
			.as("DAO merge result returned")
			.isSameAs(daoMergeResult)
			;
		// @formatter:on
	}

	@Test
	public void integrationConfiguration_simple_secure() {
		// GIVEN
		final MergeMode mergeMode = MergeMode.Simple;
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(pk, now(),
				randomString(), TEST_SERVICE_ID);
		conf.setServiceIdentifier(TEST_SERVICE_ID);

		given(integrationDao.get(pk)).willReturn(conf);

		final Map<String, Object> daoMergeResult = Map.of("la", "tee-da", TEST_SECURE_SETTING, "boom");
		given(integrationDao.mergeServiceProperties(eq(pk), eq(mergeMode), any()))
				.willReturn(daoMergeResult);

		// WHEN
		final Map<String, Object> props = Map.of("foo", 1, TEST_SECURE_SETTING, "bam");
		final Map<String, ?> result = biz.mergeConfigurationServiceProperties(pk, mergeMode, props,
				CloudIntegrationConfiguration.class);

		// THEN
		then(integrationDao).should().mergeServiceProperties(eq(pk), eq(mergeMode),
				propsCaptor.capture());
		// @formatter:off
		and.then(propsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("Secure props merged")
			.hasSize(2)
			.as("Plain token saved as-is")
			.containsEntry("foo", props.get("foo"))
			.as("Secure token saved encrypted")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo(props.get(TEST_SECURE_SETTING))
							;
					})
					;
			})
			;

		and.then(result)
			.asInstanceOf(map(String.class, Object.class))
			.as("DAO merge not returned directly")
			.isNotSameAs(daoMergeResult)
			.as("Service props from DAO returned")
			.hasSize(2)
			.as("Plain setting returned as-is")
			.containsEntry("la", "tee-da")
			.as("Secure setting returned as digest")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has digest prefix")
					.startsWith("{SSHA-256}")
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void integrationConfiguration_simple_withAccessToken() {
		// GIVEN
		final MergeMode mergeMode = MergeMode.Simple;
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(pk, now(),
				randomString(), TEST_SERVICE_ID);
		final String clientId = randomString();
		conf.setServiceProps(Map.of(CloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId));

		given(integrationDao.get(pk)).willReturn(conf);

		final Map<String, Object> daoMergeResult = Map.of("merged", true);
		given(integrationDao.mergeServiceProperties(eq(pk), eq(mergeMode), any()))
				.willReturn(daoMergeResult);

		final UserStringStringCompositePK accessTokenPk = new UserStringStringCompositePK(userId,
				conf.systemIdentifier(), clientId);
		given(clientAccessTokenDao.save(any())).willReturn(accessTokenPk);

		// WHEN
		final Map<String, Object> props = Map.of(CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING,
				TEST_ACCESS_TOKEN, CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING,
				TEST_REFRESH_TOKEN);
		final Map<String, ?> result = biz.mergeConfigurationServiceProperties(pk, mergeMode, props,
				CloudIntegrationConfiguration.class);

		// THEN
		then(integrationDao).should().mergeServiceProperties(eq(pk), eq(mergeMode),
				propsCaptor.capture());
		// @formatter:off
		and.then(propsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("OAuth tokens merged")
			.hasSize(2)
			.as("Access token saved")
			.containsEntry(CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING, TEST_ACCESS_TOKEN)
			.as("Refresh token saved")
			.containsEntry(CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING, TEST_REFRESH_TOKEN)
			;

		then(clientAccessTokenDao).should().save(clientAccessTokenCaptor.capture());

		and.then(clientAccessTokenCaptor.getValue())
			.as("Token entity ID assigned on save")
			.returns(accessTokenPk, from(ClientAccessTokenEntity::getId))
			.as("Access token as provided from settings")
			.returns(TEST_ACCESS_TOKEN.getBytes(UTF_8), from(ClientAccessTokenEntity::getAccessToken))
			.as("Access token expiration date decoded from JWT")
			.returns(Instant.ofEpochSecond(1741027676L), from(ClientAccessTokenEntity::getAccessTokenExpiresAt))
			.as("Access token issue date not in JWT, defaults to clock time")
			.returns(clock.instant(), from(ClientAccessTokenEntity::getAccessTokenIssuedAt))
			.as("Refresh token as provided from settings")
			.returns(TEST_REFRESH_TOKEN.getBytes(UTF_8), from(ClientAccessTokenEntity::getRefreshToken))
			.as("Refresh token issue date not in JWT, defaults to clock time")
			.returns(clock.instant(), from(ClientAccessTokenEntity::getRefreshTokenIssuedAt))
			;


		and.then(result)
			.asInstanceOf(map(String.class, Object.class))
			.as("DAO merge returned directly")
			.isSameAs(daoMergeResult)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamConfiguration_simple() {
		// GIVEN
		final MergeMode mergeMode = MergeMode.Simple;
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		final CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(pk, now(),
				randomString(), TEST_SERVICE_ID, ObjectDatumKind.Node);

		given(datumStreamDao.get(pk)).willReturn(conf);

		final Map<String, Object> daoMergeResult = Map.of("merged", true);
		given(datumStreamDao.mergeServiceProperties(eq(pk), eq(mergeMode), any()))
				.willReturn(daoMergeResult);

		// WHEN
		final Map<String, Object> props = Map.of("foo", 1, "bim", "bam");
		final Map<String, ?> result = biz.mergeConfigurationServiceProperties(pk, mergeMode, props,
				CloudDatumStreamConfiguration.class);

		// THEN
		then(datumStreamDao).should().mergeServiceProperties(eq(pk), eq(mergeMode), eq(props));
		// @formatter:off
		and.then(result)
			.as("DAO merge result returned")
			.isSameAs(daoMergeResult)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamConfiguration_simple_secure() {
		// GIVEN
		final MergeMode mergeMode = MergeMode.Simple;
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		final CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(pk, now(),
				randomString(), TEST_SERVICE_ID, ObjectDatumKind.Node);

		given(datumStreamDao.get(pk)).willReturn(conf);

		final Map<String, Object> daoMergeResult = Map.of("la", "tee-da", TEST_SECURE_SETTING, "boom");
		given(datumStreamDao.mergeServiceProperties(eq(pk), eq(mergeMode), any()))
				.willReturn(daoMergeResult);

		// WHEN
		final Map<String, Object> props = Map.of("foo", 1, TEST_SECURE_SETTING, "bam");
		final Map<String, ?> result = biz.mergeConfigurationServiceProperties(pk, mergeMode, props,
				CloudDatumStreamConfiguration.class);

		// THEN
		then(datumStreamDao).should().mergeServiceProperties(eq(pk), eq(mergeMode),
				propsCaptor.capture());
		// @formatter:off
		and.then(propsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("Secure props merged")
			.hasSize(2)
			.as("Plain token saved as-is")
			.containsEntry("foo", props.get("foo"))
			.as("Secure token saved encrypted")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo(props.get(TEST_SECURE_SETTING))
							;
					})
					;
			})
			;

		and.then(result)
			.asInstanceOf(map(String.class, Object.class))
			.as("DAO merge not returned directly")
			.isNotSameAs(daoMergeResult)
			.as("Service props from DAO returned")
			.hasSize(2)
			.as("Plain setting returned as-is")
			.containsEntry("la", "tee-da")
			.as("Secure setting returned as digest")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has digest prefix")
					.startsWith("{SSHA-256}")
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void datumStreamMappingConfiguration_simple() {
		// GIVEN
		final MergeMode mergeMode = MergeMode.Simple;
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		final CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(pk,
				now(), randomString(), randomLong());

		given(datumStreamMappingDao.get(pk)).willReturn(conf);

		final Map<String, Object> daoMergeResult = Map.of("merged", true);
		given(datumStreamMappingDao.mergeServiceProperties(eq(pk), eq(mergeMode), any()))
				.willReturn(daoMergeResult);

		// WHEN
		final Map<String, Object> props = Map.of("foo", 1, "bim", "bam");
		final Map<String, ?> result = biz.mergeConfigurationServiceProperties(pk, mergeMode, props,
				CloudDatumStreamMappingConfiguration.class);

		// THEN
		then(datumStreamMappingDao).should().mergeServiceProperties(eq(pk), eq(mergeMode), eq(props));
		// @formatter:off
		and.then(result)
			.as("DAO merge result returned")
			.isSameAs(daoMergeResult)
			;
		// @formatter:on
	}

}
