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
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.test.CommonTestUtils.randomDecimal;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
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
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.user.c2c.biz.impl.DaoUserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamMappingConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationConfigurationInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Test cases for the {@link DaoUserCloudIntegrationsBiz} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserCloudIntegrationsBizTests {

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
	private CloudDatumStreamPollTaskDao datumStreamPollTaskDao;

	@Mock
	private CloudIntegrationService integrationService;

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
	private ArgumentCaptor<CloudDatumStreamPollTaskEntity> datumStreamPollTaskCaptor;

	@Captor
	private ArgumentCaptor<BasicFilter> filterCaptor;

	private PrefixedTextEncryptor textEncryptor = PrefixedTextEncryptor.aesTextEncryptor(randomString(),
			randomString());

	private DaoUserCloudIntegrationsBiz biz;

	@BeforeEach
	public void setup() {
		given(integrationService.getId()).willReturn(TEST_SERVICE_ID);

		// provide settings to verify masking sensitive values
		List<SettingSpecifier> settings = Arrays.asList(new BasicTextFieldSettingSpecifier("foo", null),
				new BasicTextFieldSettingSpecifier("watchout", null, true));
		given(integrationService.getSettingSpecifiers()).willReturn(settings);

		biz = new DaoUserCloudIntegrationsBiz(integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, datumStreamPollTaskDao, textEncryptor,
				Collections.singleton(integrationService));

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		biz.setValidator(factory.getValidator());
	}

	@Test
	public void availableTransformServices() {
		// WHEN
		Iterable<CloudIntegrationService> result = biz.availableIntegrationServices();

		// THEN
		and.then(result).as("Services returned").containsExactly(integrationService);
	}

	@Test
	public void transformService() {
		// WHEN
		CloudIntegrationService result = biz.integrationService(TEST_SERVICE_ID);

		// THEN
		and.then(result).as("Service returned").isSameAs(integrationService);
	}

	@Test
	public void integrationConfigurations_forUser() {
		// GIVEN
		final Long userId = randomLong();
		final Map<String, Object> sprops = Map.of("foo", "bar", "watchout", "should be masked");
		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(userId,
				randomLong(), now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		conf.setServiceProps(sprops);
		final var daoResults = new BasicFilterResults<CloudIntegrationConfiguration, UserLongCompositePK>(
				Arrays.asList(conf));
		given(integrationDao.findFiltered(any(), isNull(), isNull(), isNull())).willReturn(daoResults);

		// WHEN
		FilterResults<CloudIntegrationConfiguration, UserLongCompositePK> result = biz
				.listConfigurationsForUser(userId, null, CloudIntegrationConfiguration.class);

		// THEN
		then(integrationDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

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
			.as("Service props are changed")
			;
		// @formatter:on
	}

	@Test
	public void datumStreamConfigurations_forUser() {
		// GIVEN
		final Long userId = randomLong();
		final CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(userId,
				randomLong(), now());
		final var daoResults = new BasicFilterResults<CloudDatumStreamConfiguration, UserLongCompositePK>(
				Arrays.asList(conf));
		given(datumStreamDao.findFiltered(any(), isNull(), isNull(), isNull())).willReturn(daoResults);

		// WHEN
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> result = biz
				.listConfigurationsForUser(userId, null, CloudDatumStreamConfiguration.class);

		// THEN
		then(datumStreamDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void datumStreamMappingConfigurations_forUser() {
		// GIVEN
		final Long userId = randomLong();
		final CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(
				userId, randomLong(), now());
		final var daoResults = new BasicFilterResults<CloudDatumStreamMappingConfiguration, UserLongCompositePK>(
				Arrays.asList(conf));
		given(datumStreamMappingDao.findFiltered(any(), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		FilterResults<CloudDatumStreamMappingConfiguration, UserLongCompositePK> result = biz
				.listConfigurationsForUser(userId, null, CloudDatumStreamMappingConfiguration.class);

		// THEN
		then(datumStreamMappingDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void datumStreamPropertyConfigurations_forUser() {
		// GIVEN
		final Long userId = randomLong();
		final CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(
				userId, randomLong(), randomInt(), now());
		final var daoResults = new BasicFilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>(
				Arrays.asList(conf));
		given(datumStreamPropertyDao.findFiltered(any(), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		FilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK> result = biz
				.listConfigurationsForUser(userId, null, CloudDatumStreamPropertyConfiguration.class);

		// THEN
		then(datumStreamPropertyDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void datumStreamPropertyConfigurations_forUserAndDatumStreamMapping() {
		// GIVEN
		final Long userId = randomLong();
		final CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(
				userId, randomLong(), randomInt(), now());
		final var daoResults = new BasicFilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>(
				Arrays.asList(conf));
		given(datumStreamPropertyDao.findFiltered(any(), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setUserId(randomLong()); // should be replaced
		filter.setDatumStreamMappingId(conf.getDatumStreamMappingId());
		FilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK> result = biz
				.listConfigurationsForUser(userId, filter, CloudDatumStreamPropertyConfiguration.class);

		// THEN
		then(datumStreamPropertyDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);
		expectedFilter.setDatumStreamMappingId(conf.getDatumStreamMappingId());

		and.then(filterCaptor.getValue()).as("Filter has user ID and datum stream ID set")
				.isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void integrationConfiguration_forId() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(pk, now());

		given(integrationDao.get(pk)).willReturn(conf);

		// WHEN
		CloudIntegrationConfiguration result = biz.configurationForId(pk,
				CloudIntegrationConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void datumStreamConfiguration_forId() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(pk, now());

		given(datumStreamDao.get(pk)).willReturn(conf);

		// WHEN
		CloudDatumStreamConfiguration result = biz.configurationForId(pk,
				CloudDatumStreamConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void datumStreamMappingConfiguration_forId() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(pk, now());

		given(datumStreamMappingDao.get(pk)).willReturn(conf);

		// WHEN
		CloudDatumStreamMappingConfiguration result = biz.configurationForId(pk,
				CloudDatumStreamMappingConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void datumStreamPropertyConfiguration_forId() {
		// GIVEN
		Long userId = randomLong();
		Long groupId = randomLong();
		Integer entityId = randomInt();
		UserLongIntegerCompositePK pk = new UserLongIntegerCompositePK(userId, groupId, entityId);
		CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(pk,
				now());

		given(datumStreamPropertyDao.get(pk)).willReturn(conf);

		// WHEN
		CloudDatumStreamPropertyConfiguration result = biz.configurationForId(pk,
				CloudDatumStreamPropertyConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void integrationConfiguration_save() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final Map<String, Object> sprops = Map.of("foo", "bar", "watchout", "should be masked");

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(pk, now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		conf.setServiceProps(sprops);

		// save and retrieve
		given(integrationDao.save(any(CloudIntegrationConfiguration.class))).willReturn(pk);
		given(integrationDao.get(pk)).willReturn(conf);

		// WHEN
		CloudIntegrationConfigurationInput input = new CloudIntegrationConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(TEST_SERVICE_ID);
		input.setServiceProperties(sprops);
		UserLongCompositePK unassignedId = UserLongCompositePK.unassignedEntityIdKey(userId);
		CloudIntegrationConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(integrationDao).should().save(integrationCaptor.capture());

		and.then(integrationCaptor.getValue())
			.as("Entity ID unassigned on DAO save")
			.returns(unassignedId, from(CloudIntegrationConfiguration::getId))
			.as("Enabled from input passed to DAO")
			.returns(input.isEnabled(), from(CloudIntegrationConfiguration::isEnabled))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(CloudIntegrationConfiguration::getName))
			.as("Service identifier from input passed to DAO")
			.returns(input.getServiceIdentifier(), from(CloudIntegrationConfiguration::getServiceIdentifier))
			.as("Service properties passed to DAO are changed to mask secrets")
			.satisfies(c -> {
				and.then(c.getServiceProps())
					.as("Has same keys provided in input")
					.containsOnlyKeys(sprops.keySet())
					.as("Non-senstive property unchanged")
					.containsEntry("foo", sprops.get("foo"))
					.hasEntrySatisfying("watchout", v -> {
						and.then(v)
							.asInstanceOf(InstanceOfAssertFactories.STRING)
							.as("Sensitive value has encryptor prefix")
							.startsWith(textEncryptor.getPrefix())
							.as("Can decrypt value back to original plain text")
							.satisfies(cipherText -> {
								and.then(textEncryptor.decrypt(cipherText))
									.as("Decrypted value same as original plain text")
									.isEqualTo(sprops.get("watchout"))
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
			;
		// @formatter:on
	}

	@Test
	public void datumStreamConfiguration_save() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(pk, now());

		// save and retrieve
		given(datumStreamDao.save(any(CloudDatumStreamConfiguration.class))).willReturn(pk);
		given(datumStreamDao.get(pk)).willReturn(conf);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		CloudDatumStreamConfigurationInput input = new CloudDatumStreamConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(randomString());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		input.setDatumStreamMappingId(randomLong());
		input.setSchedule(randomString());
		input.setKind(ObjectDatumKind.Node);
		input.setObjectId(randomLong());
		input.setSourceId(randomString());
		UserLongCompositePK unassignedId = UserLongCompositePK.unassignedEntityIdKey(userId);
		CloudDatumStreamConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(datumStreamDao).should().save(datumStreamCaptor.capture());

		and.then(datumStreamCaptor.getValue())
			.as("Entity ID unassigned on DAO save")
			.returns(unassignedId, from(CloudDatumStreamConfiguration::getId))
			.as("Enabled from input passed to DAO")
			.returns(input.isEnabled(), from(CloudDatumStreamConfiguration::isEnabled))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(CloudDatumStreamConfiguration::getName))
			.as("Service identifier from input passed to DAO")
			.returns(input.getServiceIdentifier(), from(CloudDatumStreamConfiguration::getServiceIdentifier))
			.as("Service properties from input passed to DAO")
			.returns(input.getServiceProperties(), from(CloudDatumStreamConfiguration::getServiceProperties))
			.as("Datum steram mapping ID from input passed to DAO")
			.returns(input.getDatumStreamMappingId(), from(CloudDatumStreamConfiguration::getDatumStreamMappingId))
			.as("Schedule from input passed to DAO")
			.returns(input.getSchedule(), from(CloudDatumStreamConfiguration::getSchedule))
			.as("Kind from input passed to DAO")
			.returns(input.getKind(), from(CloudDatumStreamConfiguration::getKind))
			.as("Object ID from input passed to DAO")
			.returns(input.getObjectId(), from(CloudDatumStreamConfiguration::getObjectId))
			.as("Source ID from input passed to DAO")
			.returns(input.getSourceId(), from(CloudDatumStreamConfiguration::getSourceId))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(conf)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamMappingConfiguration_save() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(pk, now());

		// save and retrieve
		given(datumStreamMappingDao.save(any(CloudDatumStreamMappingConfiguration.class)))
				.willReturn(pk);
		given(datumStreamMappingDao.get(pk)).willReturn(conf);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		CloudDatumStreamMappingConfigurationInput input = new CloudDatumStreamMappingConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		input.setIntegrationId(randomLong());
		UserLongCompositePK unassignedId = UserLongCompositePK.unassignedEntityIdKey(userId);
		CloudDatumStreamMappingConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(datumStreamMappingDao).should().save(datumStreamMappingCaptor.capture());

		and.then(datumStreamMappingCaptor.getValue())
			.as("Entity ID unassigned on DAO save")
			.returns(unassignedId, from(CloudDatumStreamMappingConfiguration::getId))
			.as("Enabled from input passed to DAO")
			.returns(input.isEnabled(), from(CloudDatumStreamMappingConfiguration::isEnabled))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(CloudDatumStreamMappingConfiguration::getName))
			.as("Service properties from input passed to DAO")
			.returns(input.getServiceProperties(), from(CloudDatumStreamMappingConfiguration::getServiceProperties))
			.as("Integration ID from input passed to DAO")
			.returns(input.getIntegrationId(), from(CloudDatumStreamMappingConfiguration::getIntegrationId))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(conf)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPropertyConfiguration_save() {
		// GIVEN
		Long userId = randomLong();
		Long groupId = randomLong();
		Integer entityId = randomInt();
		UserLongIntegerCompositePK pk = new UserLongIntegerCompositePK(userId, groupId, entityId);
		CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(pk,
				now());

		// save and retrieve
		given(datumStreamPropertyDao.save(any(CloudDatumStreamPropertyConfiguration.class)))
				.willReturn(pk);
		given(datumStreamPropertyDao.get(pk)).willReturn(conf);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		CloudDatumStreamPropertyConfigurationInput input = new CloudDatumStreamPropertyConfigurationInput();
		input.setEnabled(true);
		input.setPropertyType(DatumSamplesType.Instantaneous);
		input.setPropertyName(randomString());
		input.setValueReference(randomString());
		input.setMultiplier(randomDecimal());
		input.setScale(randomInt());
		CloudDatumStreamPropertyConfiguration result = biz.saveConfiguration(pk, input);

		// THEN
		// @formatter:off
		then(datumStreamPropertyDao).should().save(datumStreamPropertyCaptor.capture());

		and.then(datumStreamPropertyCaptor.getValue())
			.as("Entity ID from input passed to DAO")
			.returns(pk, from(CloudDatumStreamPropertyConfiguration::getId))
			.as("Enabled from input passed to DAO")
			.returns(input.isEnabled(), from(CloudDatumStreamPropertyConfiguration::isEnabled))
			.as("Property type from input passed to DAO")
			.returns(input.getPropertyType(), from(CloudDatumStreamPropertyConfiguration::getPropertyType))
			.as("Property name from input passed to DAO")
			.returns(input.getPropertyName(), from(CloudDatumStreamPropertyConfiguration::getPropertyName))
			.as("Value reference from input passed to DAO")
			.returns(input.getValueReference(), from(CloudDatumStreamPropertyConfiguration::getValueReference))
			.as("Multiplier from input passed to DAO")
			.returns(input.getMultiplier(), from(CloudDatumStreamPropertyConfiguration::getMultiplier))
			.as("Scale from input passed to DAO")
			.returns(input.getScale(), from(CloudDatumStreamPropertyConfiguration::getScale))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(conf)
			;
		// @formatter:on
	}

	@Test
	public void integrationConfiguration_delete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(pk, now());

		given(integrationDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, CloudIntegrationConfiguration.class);

		// THEN
		// @formatter:off
		then(integrationDao).should().delete(integrationCaptor.capture());

		and.then(integrationCaptor.getValue())
			.as("Integration ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void datumStreamConfiguration_delete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(pk, now());

		given(datumStreamDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, CloudDatumStreamConfiguration.class);

		// THEN
		// @formatter:off
		then(datumStreamDao).should().delete(datumStreamCaptor.capture());

		and.then(datumStreamCaptor.getValue())
			.as("DatumStream ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void datumStreamMappingConfiguration_delete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(pk, now());

		given(datumStreamMappingDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, CloudDatumStreamMappingConfiguration.class);

		// THEN
		// @formatter:off
		then(datumStreamMappingDao).should().delete(datumStreamMappingCaptor.capture());

		and.then(datumStreamMappingCaptor.getValue())
			.as("Datum stream mapping ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPropertyConfiguration_delete() {
		// GIVEN
		Long userId = randomLong();
		Long groupId = randomLong();
		Integer entityId = randomInt();
		UserLongIntegerCompositePK pk = new UserLongIntegerCompositePK(userId, groupId, entityId);
		CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(pk,
				now());

		given(datumStreamPropertyDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, CloudDatumStreamPropertyConfiguration.class);

		// THEN
		// @formatter:off
		then(datumStreamPropertyDao).should().delete(datumStreamPropertyCaptor.capture());

		and.then(datumStreamPropertyCaptor.getValue())
			.as("DatumStreamProperty ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPollTaskEntity_save() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamPollTaskEntity entity = new CloudDatumStreamPollTaskEntity(pk);

		// save and retrieve
		given(datumStreamPollTaskDao.save(any(CloudDatumStreamPollTaskEntity.class))).willReturn(pk);
		given(datumStreamPollTaskDao.get(pk)).willReturn(entity);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		CloudDatumStreamPollTaskEntityInput input = new CloudDatumStreamPollTaskEntityInput();
		input.setState(Queued);
		input.setExecuteAt(now());
		input.setStartAt(now().minusSeconds(1));
		input.setMessage(randomString());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		CloudDatumStreamPollTaskEntity result = biz.saveDatumStreamPollTask(pk, input);

		// THEN
		// @formatter:off
		then(datumStreamPollTaskDao).should().save(datumStreamPollTaskCaptor.capture());

		and.then(datumStreamPollTaskCaptor.getValue())
			.as("Entity ID on DAO save is argument to service")
			.returns(pk, from(CloudDatumStreamPollTaskEntity::getId))
			.as("State from input passed to DAO")
			.returns(input.getState(), from(CloudDatumStreamPollTaskEntity::getState))
			.as("Exec date input passed to DAO")
			.returns(input.getExecuteAt(), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Start date from input passed to DAO")
			.returns(input.getStartAt(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("Message from input passed to DAO")
			.returns(input.getMessage(), from(CloudDatumStreamPollTaskEntity::getMessage))
			.as("Service properties from input passed to DAO")
			.returns(input.getServiceProperties(), from(CloudDatumStreamPollTaskEntity::getServiceProperties))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPollTaskEntity_save_invalidState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		CloudDatumStreamPollTaskEntityInput input = new CloudDatumStreamPollTaskEntityInput();
		input.setExecuteAt(now());
		input.setStartAt(now().minusSeconds(1));

		// WHEN
		for ( BasicClaimableJobState state : EnumSet.complementOf(EnumSet.of(Queued, Completed)) ) {
			input.setState(state);
			ValidationException ex = catchThrowableOfType(() -> biz.saveDatumStreamPollTask(pk, input),
					ValidationException.class);

			// THEN
			// @formatter:off
			and.then(ex)
				.as("Validation exception is thrown because not allowed set %s state", state)
				.isNotNull()
				.extracting(e -> e.getErrors().getFieldError("state"))
				.as("Validation is on the state field")
				.isNotNull()
				;
			// @formatter:on
		}
	}

	@Test
	public void datumStreamPollTaskEntity_save_expectedState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamPollTaskEntity entity = new CloudDatumStreamPollTaskEntity(pk);

		// save and retrieve
		given(datumStreamPollTaskDao.updateTask(any(CloudDatumStreamPollTaskEntity.class),
				eq(Completed))).willReturn(true);
		given(datumStreamPollTaskDao.get(pk)).willReturn(entity);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		CloudDatumStreamPollTaskEntityInput input = new CloudDatumStreamPollTaskEntityInput();
		input.setState(Queued);
		input.setExecuteAt(now());
		input.setStartAt(now().minusSeconds(1));
		input.setMessage(randomString());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		CloudDatumStreamPollTaskEntity result = biz.saveDatumStreamPollTask(pk, input, Completed);

		// THEN
		// @formatter:off
		then(datumStreamPollTaskDao).should().updateTask(datumStreamPollTaskCaptor.capture(), eq(Completed));

		and.then(datumStreamPollTaskCaptor.getValue())
			.as("Entity ID on DAO save is argument to service")
			.returns(pk, from(CloudDatumStreamPollTaskEntity::getId))
			.as("State from input passed to DAO")
			.returns(input.getState(), from(CloudDatumStreamPollTaskEntity::getState))
			.as("Exec date input passed to DAO")
			.returns(input.getExecuteAt(), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Start date from input passed to DAO")
			.returns(input.getStartAt(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("Message from input passed to DAO")
			.returns(input.getMessage(), from(CloudDatumStreamPollTaskEntity::getMessage))
			.as("Service properties from input passed to DAO")
			.returns(input.getServiceProperties(), from(CloudDatumStreamPollTaskEntity::getServiceProperties))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPollTaskEntity_updateState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamPollTaskEntity entity = new CloudDatumStreamPollTaskEntity(pk);

		// save and retrieve
		given(datumStreamPollTaskDao.updateTaskState(pk, Queued)).willReturn(true);
		given(datumStreamPollTaskDao.get(pk)).willReturn(entity);

		// WHEN
		CloudDatumStreamPollTaskEntity result = biz.updateDatumStreamPollTaskState(pk, Queued);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPollTaskEntity_updateState_expectedState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamPollTaskEntity entity = new CloudDatumStreamPollTaskEntity(pk);

		// save and retrieve
		given(datumStreamPollTaskDao.updateTaskState(pk, Queued, Completed)).willReturn(true);
		given(datumStreamPollTaskDao.get(pk)).willReturn(entity);

		// WHEN
		CloudDatumStreamPollTaskEntity result = biz.updateDatumStreamPollTaskState(pk, Queued,
				Completed);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamPollTaskEntity_delete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CloudDatumStreamPollTaskEntity entity = new CloudDatumStreamPollTaskEntity(pk);

		given(datumStreamPollTaskDao.entityKey(pk)).willReturn(entity);

		// WHEN
		biz.deleteDatumStreamPollTask(pk);

		// THEN
		// @formatter:off
		then(datumStreamPollTaskDao).should().delete(datumStreamPollTaskCaptor.capture());

		and.then(datumStreamPollTaskCaptor.getValue())
			.as("DAO passed entity returned from entityKey()")
			.isSameAs(entity)
			;
		// @formatter:on
	}

}