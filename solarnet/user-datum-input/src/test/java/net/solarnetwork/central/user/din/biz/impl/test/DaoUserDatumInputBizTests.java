/* ==================================================================
 * DaoUserDatumInputBizTests.java - 26/02/2024 7:34:00 am
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

package net.solarnetwork.central.user.din.biz.impl.test;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.dao.BasicFilter;
import net.solarnetwork.central.din.dao.CredentialConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.user.din.biz.impl.DaoUserDatumInputBiz;
import net.solarnetwork.central.user.din.domain.CredentialConfigurationInput;
import net.solarnetwork.central.user.din.domain.EndpointAuthConfigurationInput;
import net.solarnetwork.central.user.din.domain.EndpointConfigurationInput;
import net.solarnetwork.central.user.din.domain.TransformConfigurationInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Test cases for the {@link DaoUserDatumInputBiz} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserDatumInputBizTests {

	@Mock
	private CredentialConfigurationDao credentialDao;

	@Mock
	private TransformConfigurationDao transformDao;

	@Mock
	private EndpointConfigurationDao endpointDao;

	@Mock
	private EndpointAuthConfigurationDao endpointAuthDao;

	@Mock
	private TransformService transformService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Captor
	private ArgumentCaptor<Locale> localeCaptor;

	@Captor
	private ArgumentCaptor<CredentialConfiguration> credentialCaptor;

	@Captor
	private ArgumentCaptor<TransformConfiguration> transformCaptor;

	@Captor
	private ArgumentCaptor<EndpointConfiguration> endpointCaptor;

	@Captor
	private ArgumentCaptor<EndpointAuthConfiguration> endpointAuthCaptor;

	@Captor
	private ArgumentCaptor<BasicFilter> filterCaptor;

	private DaoUserDatumInputBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoUserDatumInputBiz(credentialDao, transformDao, endpointDao, endpointAuthDao,
				Collections.singleton(transformService));

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		biz.setValidator(factory.getValidator());
		biz.setPasswordEncoder(passwordEncoder);
	}

	@Test
	public void availableTransformServices() {
		// GIVEN
		BasicLocalizedServiceInfo info = new BasicLocalizedServiceInfo(randomString(), Locale.ENGLISH,
				null, null, null);
		given(transformService.getLocalizedServiceInfo(any(Locale.class))).willReturn(info);

		// WHEN
		Locale locale = Locale.getDefault();
		Iterable<LocalizedServiceInfo> result = biz.availableTransformServices(locale);

		// THEN
		then(transformService).should().getLocalizedServiceInfo(localeCaptor.capture());

		and.then(localeCaptor.getValue()).as("Provided locale used").isSameAs(locale);

		and.then(result).as("Service info returned").containsExactly(info);
	}

	@Test
	public void credentialConfigurationsForUser() {
		// GIVEN
		Long userId = randomLong();
		CredentialConfiguration conf = new CredentialConfiguration(userId, randomLong(), now());
		final BasicFilterResults<CredentialConfiguration, UserLongCompositePK> daoResults = new BasicFilterResults<>(
				Arrays.asList(conf));
		given(credentialDao.findFiltered(any(BasicFilter.class), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		FilterResults<CredentialConfiguration, UserLongCompositePK> result = biz
				.configurationsForUser(userId, null, CredentialConfiguration.class);

		// THEN
		then(credentialDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void transformConfigurationsForUser() {
		// GIVEN
		Long userId = randomLong();
		TransformConfiguration conf = new TransformConfiguration(userId, randomLong(), now());
		final BasicFilterResults<TransformConfiguration, UserLongCompositePK> daoResults = new BasicFilterResults<>(
				Arrays.asList(conf));
		given(transformDao.findFiltered(any(BasicFilter.class), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		FilterResults<TransformConfiguration, UserLongCompositePK> result = biz
				.configurationsForUser(userId, null, TransformConfiguration.class);

		// THEN
		then(transformDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void endpointConfigurationsForUser() {
		// GIVEN
		Long userId = randomLong();
		EndpointConfiguration conf = new EndpointConfiguration(userId, randomUUID(), now());
		final BasicFilterResults<EndpointConfiguration, UserUuidPK> daoResults = new BasicFilterResults<>(
				Arrays.asList(conf));
		given(endpointDao.findFiltered(any(BasicFilter.class), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		FilterResults<EndpointConfiguration, UserUuidPK> result = biz.configurationsForUser(userId, null,
				EndpointConfiguration.class);

		// THEN
		then(endpointDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void endpointAuthConfigurationsForUser() {
		// GIVEN
		Long userId = randomLong();
		EndpointAuthConfiguration conf = new EndpointAuthConfiguration(userId, randomUUID(),
				randomLong(), now());
		final BasicFilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK> daoResults = new BasicFilterResults<>(
				Arrays.asList(conf));
		given(endpointAuthDao.findFiltered(any(BasicFilter.class), isNull(), isNull(), isNull()))
				.willReturn(daoResults);

		// WHEN
		FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK> result = biz
				.configurationsForUser(userId, null, EndpointAuthConfiguration.class);

		// THEN
		then(endpointAuthDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());

		BasicFilter expectedFilter = new BasicFilter();
		expectedFilter.setUserId(userId);

		and.then(filterCaptor.getValue()).as("Filter has user ID set").isEqualTo(expectedFilter);

		and.then(result).as("Result provided from DAO").isSameAs(daoResults);
	}

	@Test
	public void credentialConfigurationForId() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CredentialConfiguration conf = new CredentialConfiguration(pk, now());

		given(credentialDao.get(pk)).willReturn(conf);

		// WHEN
		CredentialConfiguration result = biz.configurationForId(pk, CredentialConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void transformConfigurationForId() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		TransformConfiguration conf = new TransformConfiguration(pk, now());

		given(transformDao.get(pk)).willReturn(conf);

		// WHEN
		TransformConfiguration result = biz.configurationForId(pk, TransformConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void endpointConfigurationForId() {
		// GIVEN
		Long userId = randomLong();
		UUID entityId = randomUUID();
		UserUuidPK pk = new UserUuidPK(userId, entityId);
		EndpointConfiguration conf = new EndpointConfiguration(pk, now());

		given(endpointDao.get(pk)).willReturn(conf);

		// WHEN
		EndpointConfiguration result = biz.configurationForId(pk, EndpointConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void endpointAuthConfigurationForId() {
		// GIVEN
		Long userId = randomLong();
		UUID endpointId = randomUUID();
		Long entityId = randomLong();
		UserUuidLongCompositePK pk = new UserUuidLongCompositePK(userId, endpointId, entityId);
		EndpointAuthConfiguration conf = new EndpointAuthConfiguration(pk, now());

		given(endpointAuthDao.get(pk)).willReturn(conf);

		// WHEN
		EndpointAuthConfiguration result = biz.configurationForId(pk, EndpointAuthConfiguration.class);

		// THEN
		and.then(result).as("Result provided from DAO").isSameAs(conf);
	}

	@Test
	public void credentialConfigurationSave() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CredentialConfiguration conf = new CredentialConfiguration(pk, now());
		conf.setPassword("this should be erased");

		// encode password
		final String password = randomString();
		given(passwordEncoder.isPasswordEncrypted(password)).willReturn(false);

		final String encodedPassword = randomString();
		given(passwordEncoder.encode(password)).willReturn(encodedPassword);

		// save and retrieve
		given(credentialDao.save(any(CredentialConfiguration.class))).willReturn(pk);
		given(credentialDao.get(pk)).willReturn(conf);

		// WHEN
		CredentialConfigurationInput input = new CredentialConfigurationInput();
		input.setEnabled(true);
		input.setUsername(randomString());
		input.setPassword(password);
		UserLongCompositePK unassignedId = UserLongCompositePK.unassignedEntityIdKey(userId);
		CredentialConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(credentialDao).should().save(credentialCaptor.capture());

		and.then(credentialCaptor.getValue())
			.as("Credential ID unassigned on DAO save")
			.returns(unassignedId, from(CredentialConfiguration::getId))
			.as("Enabled from input")
			.returns(input.isEnabled(), from(CredentialConfiguration::isEnabled))
			.as("Username from input")
			.returns(input.getUsername(), from(CredentialConfiguration::getUsername))
			.as("Password encoded from input")
			.returns(encodedPassword, from(CredentialConfiguration::getPassword))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(conf)
			.as("Password is erased")
			.returns(null, from(CredentialConfiguration::getPassword))
			;
		// @formatter:on
	}

	@Test
	public void transformConfigurationSave() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		TransformConfiguration conf = new TransformConfiguration(pk, now());

		// save and retrieve
		given(transformDao.save(any(TransformConfiguration.class))).willReturn(pk);
		given(transformDao.get(pk)).willReturn(conf);

		// WHEN
		TransformConfigurationInput input = new TransformConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(randomString());

		input.setServiceProperties(Map.of("foo", "bar"));
		UserLongCompositePK unassignedId = UserLongCompositePK.unassignedEntityIdKey(userId);
		TransformConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(transformDao).should().save(transformCaptor.capture());

		and.then(transformCaptor.getValue())
			.as("Credential ID unassigned on DAO save")
			.returns(unassignedId, from(TransformConfiguration::getId))
			.as("Enabled from input")
			.returns(input.isEnabled(), from(TransformConfiguration::isEnabled))
			.as("Name from input")
			.returns(input.getName(), from(TransformConfiguration::getName))
			.as("Service ID from input")
			.returns(input.getServiceIdentifier(), from(TransformConfiguration::getServiceIdentifier))
			.as("Service props from input")
			.returns(input.getServiceProperties(), from(TransformConfiguration::getServiceProperties))
			;

		and.then(result).as("Result provided from DAO").isSameAs(conf);
		// @formatter:on
	}

	@Test
	public void endpointConfigurationSave() {
		// GIVEN
		Long userId = randomLong();
		UUID entityId = randomUUID();
		UserUuidPK pk = new UserUuidPK(userId, entityId);
		EndpointConfiguration conf = new EndpointConfiguration(pk, now());

		// save and retrieve
		given(endpointDao.save(any(EndpointConfiguration.class))).willReturn(pk);
		given(endpointDao.get(pk)).willReturn(conf);

		// WHEN
		EndpointConfigurationInput input = new EndpointConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setNodeId(randomLong());
		input.setSourceId(randomString());
		input.setTransformId(randomLong());
		UserUuidPK unassignedId = UserUuidPK.unassignedUuidKey(userId);
		EndpointConfiguration result = biz.saveConfiguration(unassignedId, input);

		// THEN
		// @formatter:off
		then(endpointDao).should().save(endpointCaptor.capture());

		and.then(endpointCaptor.getValue())
			.as("Credential ID unassigned on DAO save")
			.returns(unassignedId, from(EndpointConfiguration::getId))
			.as("Enabled from input")
			.returns(input.isEnabled(), from(EndpointConfiguration::isEnabled))
			.as("Name from input")
			.returns(input.getName(), from(EndpointConfiguration::getName))
			.as("Node ID from input")
			.returns(input.getNodeId(), from(EndpointConfiguration::getNodeId))
			.as("Source ID from input")
			.returns(input.getSourceId(), from(EndpointConfiguration::getSourceId))
			.as("Transform ID from input")
			.returns(input.getTransformId(), from(EndpointConfiguration::getTransformId))
			;

		and.then(result).as("Result provided from DAO").isSameAs(conf);
		// @formatter:on
	}

	@Test
	public void endpointAuthConfigurationSave() {
		// GIVEN
		Long userId = randomLong();
		UUID endpointId = randomUUID();
		Long credentialId = randomLong();
		UserUuidLongCompositePK pk = new UserUuidLongCompositePK(userId, endpointId, credentialId);
		EndpointAuthConfiguration conf = new EndpointAuthConfiguration(pk, now());

		// save and retrieve
		given(endpointAuthDao.save(any(EndpointAuthConfiguration.class))).willReturn(pk);
		given(endpointAuthDao.get(pk)).willReturn(conf);

		// WHEN
		EndpointAuthConfigurationInput input = new EndpointAuthConfigurationInput();
		input.setEnabled(true);
		UserUuidLongCompositePK saveId = new UserUuidLongCompositePK(userId, endpointId, credentialId);
		EndpointAuthConfiguration result = biz.saveConfiguration(saveId, input);

		// THEN
		// @formatter:off
		then(endpointAuthDao).should().save(endpointAuthCaptor.capture());

		and.then(endpointAuthCaptor.getValue())
			.as("Credential ID unassigned on DAO save")
			.returns(saveId, from(EndpointAuthConfiguration::getId))
			.as("Enabled from input")
			.returns(input.isEnabled(), from(EndpointAuthConfiguration::isEnabled))
			;

		and.then(result).as("Result provided from DAO").isSameAs(conf);
		// @formatter:on
	}

	@Test
	public void credentialConfigurationDelete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		CredentialConfiguration conf = new CredentialConfiguration(pk, now());

		given(credentialDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, CredentialConfiguration.class);

		// THEN
		// @formatter:off
		then(credentialDao).should().delete(credentialCaptor.capture());

		and.then(credentialCaptor.getValue())
			.as("Credential ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void transformConfigurationDelete() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		TransformConfiguration conf = new TransformConfiguration(pk, now());

		given(transformDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, TransformConfiguration.class);

		// THEN
		// @formatter:off
		then(transformDao).should().delete(transformCaptor.capture());

		and.then(transformCaptor.getValue())
			.as("Transform ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void endpointConfigurationDelete() {
		// GIVEN
		Long userId = randomLong();
		UUID entityId = randomUUID();
		UserUuidPK pk = new UserUuidPK(userId, entityId);
		EndpointConfiguration conf = new EndpointConfiguration(pk, now());

		given(endpointDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, EndpointConfiguration.class);

		// THEN
		// @formatter:off
		then(endpointDao).should().delete(endpointCaptor.capture());

		and.then(endpointCaptor.getValue())
			.as("Endpoint ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

	@Test
	public void endpointAuthConfigurationDelete() {
		// GIVEN
		Long userId = randomLong();
		UUID endpointId = randomUUID();
		Long credentialId = randomLong();
		UserUuidLongCompositePK pk = new UserUuidLongCompositePK(userId, endpointId, credentialId);
		EndpointAuthConfiguration conf = new EndpointAuthConfiguration(pk, now());

		given(endpointAuthDao.entityKey(pk)).willReturn(conf);

		// WHEN
		biz.deleteConfiguration(pk, EndpointAuthConfiguration.class);

		// THEN
		// @formatter:off
		then(endpointAuthDao).should().delete(endpointAuthCaptor.capture());

		and.then(endpointAuthCaptor.getValue())
			.as("EndpointAuth ID as provided")
			.returns(pk, from(Entity::getId))
			;
		// @formatter:on
	}

}
