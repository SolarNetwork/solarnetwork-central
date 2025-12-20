/* ==================================================================
 * DaoUserCloudIntegrationsBizTests.java - 4/10/2024 2:11:15 pm
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

package net.solarnetwork.central.user.c2c.biz.impl.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.user.c2c.biz.impl.DaoUserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudControlConfigurationInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Test cases for the {@link DaoUserCloudIntegrationsBiz} class.
 *
 * @author matt
 * @version 1.7
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserCloudIntegrationsBiz_ControlsTests {

	private static final String TEST_SECURE_SETTING = "watchout";

	private static final String TEST_SERVICE_ID = randomString();

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
	private ArgumentCaptor<Locale> localeCaptor;

	@Captor
	private ArgumentCaptor<CloudIntegrationConfiguration> integrationCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamConfiguration> datumStreamCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamMappingConfiguration> datumStreamMappingCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamPropertyConfiguration> datumStreamPropertyCaptor;

	@Captor
	private ArgumentCaptor<CloudControlConfiguration> controlCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamPollTaskEntity> datumStreamPollTaskCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamRakeTaskEntity> datumStreamRakeTaskCaptor;

	@Captor
	private ArgumentCaptor<UserSettingsEntity> userSettingsCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamSettingsEntity> datumStreamSettingsCaptor;

	@Captor
	private ArgumentCaptor<BasicFilter> filterCaptor;

	@Captor
	private ArgumentCaptor<ClientAccessTokenEntity> clientAccessTokenCaptor;

	@Captor
	private ArgumentCaptor<Map<String, ?>> propsCaptor;

	@Captor
	private ArgumentCaptor<UserLongCompositePK> userLongKeyCaptor;

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
				Collections.singleton(integrationService));

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		biz.setValidator(factory.getValidator());
	}

	@Test
	public void controlConfigurations_forUser() {
		// GIVEN
		final Long userId = randomLong();
		final Map<String, Object> sprops = Map.of("foo", "bar", TEST_SECURE_SETTING, "should be masked");
		final CloudControlConfiguration conf = new CloudControlConfiguration(userId, randomLong(),
				now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		conf.setServiceProps(sprops);
		final var daoResults = new BasicFilterResults<CloudControlConfiguration, UserLongCompositePK>(
				Arrays.asList(conf));
		given(controlDao.findFiltered(any(), isNull(), isNull(), isNull())).willReturn(daoResults);

		// WHEN
		FilterResults<CloudControlConfiguration, UserLongCompositePK> result = biz
				.listConfigurationsForUser(userId, null, CloudControlConfiguration.class);

		// THEN
		then(controlDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		// @formatter:off
		and.then(filterCaptor.getValue())
			.as("Filter has user ID set")
			.isEqualTo(expectedFilter)
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(daoResults)
			.element(0)
			.extracting(BaseIdentifiableUserModifiableEntity::getServiceProps, map(String.class, Object.class))
			.as("Service props from DAO returned")
			.hasSize(2)
			.as("Plain setting returned as-is")
			.containsEntry("foo", "bar")
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
	public void controlConfigurations_forUserAndIntegration() {
		// GIVEN
		final Long userId = randomLong();
		final Map<String, Object> sprops = Map.of("foo", "bar", TEST_SECURE_SETTING, "should be masked");
		final CloudControlConfiguration conf = new CloudControlConfiguration(userId, randomLong(),
				now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		conf.setServiceProps(sprops);
		final var daoResults = new BasicFilterResults<CloudControlConfiguration, UserLongCompositePK>(
				Arrays.asList(conf));
		given(controlDao.findFiltered(any(), isNull(), isNull(), isNull())).willReturn(daoResults);

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setUserId(randomLong()); // should be replaced
		filter.setIntegrationId(conf.getIntegrationId());
		FilterResults<CloudControlConfiguration, UserLongCompositePK> result = biz
				.listConfigurationsForUser(userId, null, CloudControlConfiguration.class);

		// THEN
		then(controlDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);
		expectedFilter.setIntegrationId(conf.getIntegrationId());

		// @formatter:off
		and.then(filterCaptor.getValue())
			.as("Filter has user ID set")
			.isEqualTo(expectedFilter)
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(daoResults)
			;
		// @formatter:on
	}

	@Test
	public void controlConfiguration_forId() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		CloudControlConfiguration conf = new CloudControlConfiguration(pk, now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		conf.setServiceProps(Map.of("foo", "bar", TEST_SECURE_SETTING, "bam"));

		given(controlDao.get(pk)).willReturn(conf);

		// WHEN
		CloudControlConfiguration result = biz.configurationForId(pk, CloudControlConfiguration.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(conf)
			.extracting(BaseIdentifiableUserModifiableEntity::getServiceProps, map(String.class, Object.class))
			.as("Service props from DAO returned")
			.hasSize(2)
			.as("Plain setting returned as-is")
			.containsEntry("foo", "bar")
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
	public void controlConfiguration_save() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final Map<String, Object> sprops = Map.of("foo", "bar", TEST_SECURE_SETTING, "should be masked");

		CloudControlConfiguration conf = new CloudControlConfiguration(pk, now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		conf.setServiceProps(sprops);

		// save and retrieve
		given(controlDao.save(any(CloudControlConfiguration.class))).willReturn(pk);
		given(controlDao.get(pk)).willReturn(conf);

		// WHEN
		CloudControlConfigurationInput input = new CloudControlConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(TEST_SERVICE_ID);
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		input.setIntegrationId(randomLong());
		input.setNodeId(randomLong());
		input.setControlId(randomString());
		input.setControlReference(randomString());
		UserLongCompositePK unassignedId = UserLongCompositePK.unassignedEntityIdKey(userId);
		CloudControlConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(controlDao).should().save(controlCaptor.capture());

		and.then(controlCaptor.getValue())
			.as("Entity ID unassigned on DAO save")
			.returns(unassignedId, from(CloudControlConfiguration::getId))
			.as("Enabled from input passed to DAO")
			.returns(input.isEnabled(), from(CloudControlConfiguration::isEnabled))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(CloudControlConfiguration::getName))
			.as("Service identifier from input passed to DAO")
			.returns(input.getServiceIdentifier(), from(CloudControlConfiguration::getServiceIdentifier))
			.as("Integration ID from input passed to DAO")
			.returns(input.getIntegrationId(), from(CloudControlConfiguration::getIntegrationId))
			.as("Node ID from input passed to DAO")
			.returns(input.getNodeId(), from(CloudControlConfiguration::getNodeId))
			.as("Control ID from input passed to DAO")
			.returns(input.getControlId(), from(CloudControlConfiguration::getControlId))
			.as("Control ref from input passed to DAO")
			.returns(input.getControlReference(), from(CloudControlConfiguration::getControlReference))
			.as("Service properties passed to DAO are changed to encrypt secrets")
			.satisfies(c -> {
				and.then(c.getServiceProps())
					.as("Has same keys provided in input")
					.containsOnlyKeys(sprops.keySet())
					.as("Non-senstive property unchanged")
					.containsEntry("foo", sprops.get("foo"))
					.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
						and.then(v)
							.asInstanceOf(InstanceOfAssertFactories.STRING)
							.as("Sensitive value has encryptor prefix")
							.startsWith(textEncryptor.getPrefix())
							.as("Can decrypt value back to original plain text")
							.satisfies(cipherText -> {
								and.then(textEncryptor.decrypt(cipherText))
									.as("Decrypted value same as original plain text")
									.isEqualTo(sprops.get(TEST_SECURE_SETTING))
									;
							})
							;
					})
					;
			})
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(conf)
			.extracting(BaseIdentifiableUserModifiableEntity::getServiceProps, map(String.class, Object.class))
			.as("Service props from DAO returned")
			.hasSize(2)
			.as("Plain setting returned as-is")
			.containsEntry("foo", "bar")
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
	public void controlConfiguration_delete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudControlConfiguration conf = new CloudControlConfiguration(pk, now());

		given(controlDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, CloudControlConfiguration.class);

		// THEN
		// @formatter:off
		then(controlDao).should().delete(controlCaptor.capture());

		and.then(controlCaptor.getValue())
			.as("Cloud control ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

}
