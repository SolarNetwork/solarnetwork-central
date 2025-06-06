/* ==================================================================
 * DaoUserEventHookBizTests.java - 11/06/2020 10:55:36 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.biz.dao.test;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.domain.CompositeKey2;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.biz.dao.DaoUserEventHookBiz;
import net.solarnetwork.central.user.event.dao.UserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Test cases for the {@link DaoUserEventHookBiz} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DaoUserEventHookBizTests {

	private UserNodeEventHookConfigurationDao nodeEventHookConfigurationDao;
	private DaoUserEventHookBiz biz;

	private List<Object> methodMocks;

	@BeforeEach
	public void setup() {
		nodeEventHookConfigurationDao = EasyMock.createMock(UserNodeEventHookConfigurationDao.class);
		biz = new DaoUserEventHookBiz(nodeEventHookConfigurationDao);

		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("net/solarnetwork/central/user/event/biz/dao/test/test-messages-01");
		biz.setMessageSource(ms);

		methodMocks = new ArrayList<>(4);
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(nodeEventHookConfigurationDao);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
			methodMocks.addAll(Arrays.asList(mocks));
		}
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(nodeEventHookConfigurationDao);
		for ( Object m : methodMocks ) {
			EasyMock.verify(m);
		}
	}

	@Test
	public void availableTopics() {
		// GIVEN
		DatumAppEventProducer producer = EasyMock.createMock(DatumAppEventProducer.class);
		biz.setDatumEventProducers(singletonList(producer));

		Set<String> topics = Collections.singleton(AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC);
		expect(producer.getProducedDatumAppEventTopics()).andReturn(topics);

		// WHEN
		replayAll(producer);
		Iterable<LocalizedServiceInfo> itr = biz.availableDatumEventTopics(Locale.ENGLISH);

		// THEN
		assertThat("Service info iterable returned", itr, notNullValue());
		List<LocalizedServiceInfo> infos = StreamSupport.stream(itr.spliterator(), false)
				.collect(toList());
		assertThat("Service info count", infos, hasSize(1));
		LocalizedServiceInfo info = infos.get(0);
		assertThat("Service info ID is topic", info.getId(),
				equalTo(AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC));
		assertThat("Service name from i18n bundle", info.getLocalizedName(), equalTo("TEST-01-TITLE"));
		assertThat("Service description from i18n bundle", info.getLocalizedDescription(),
				equalTo("TEST-01-DESC"));
	}

	@Test
	public void availableTopics_localizedByService() {
		// GIVEN
		DatumAppEventProducer producer = EasyMock.createMock(DatumAppEventProducer.class);
		biz.setDatumEventProducers(singletonList(producer));

		String topic = "some/topic";
		Set<String> topics = Collections.singleton(topic);
		expect(producer.getProducedDatumAppEventTopics()).andReturn(topics);

		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("net/solarnetwork/central/user/event/biz/dao/test/test-messages-02");
		expect(producer.getMessageSource()).andReturn(ms);

		// WHEN
		replayAll(producer);
		Iterable<LocalizedServiceInfo> itr = biz.availableDatumEventTopics(Locale.ENGLISH);

		// THEN
		assertThat("Service info iterable returned", itr, notNullValue());
		List<LocalizedServiceInfo> infos = StreamSupport.stream(itr.spliterator(), false)
				.collect(toList());
		assertThat("Service info count", infos, hasSize(1));
		LocalizedServiceInfo info = infos.get(0);
		assertThat("Service info ID is topic", info.getId(), equalTo(topic));
		assertThat("Service name from service's i18n bundle", info.getLocalizedName(),
				equalTo("TEST-02-TITLE"));
		assertThat("Service description from service's i18n bundle", info.getLocalizedDescription(),
				equalTo("TEST-02-DESC"));
	}

	@Test
	public void availableTopics_multipleProducers() {
		// GIVEN
		DatumAppEventProducer producer1 = EasyMock.createMock(DatumAppEventProducer.class);
		DatumAppEventProducer producer2 = EasyMock.createMock(DatumAppEventProducer.class);
		biz.setDatumEventProducers(asList(producer1, producer2));

		String topic1 = "topic/1";
		String topic2 = "topic/2";
		expect(producer1.getProducedDatumAppEventTopics()).andReturn(singleton(topic1));
		expect(producer2.getProducedDatumAppEventTopics()).andReturn(singleton(topic2));

		// WHEN
		replayAll(producer1, producer2);
		Iterable<LocalizedServiceInfo> itr = biz.availableDatumEventTopics(Locale.ENGLISH);

		// THEN
		assertThat("Service info iterable returned", itr, notNullValue());
		List<LocalizedServiceInfo> infos = StreamSupport.stream(itr.spliterator(), false)
				.collect(toList());
		assertThat("Service info count", infos, hasSize(2));
		LocalizedServiceInfo info;

		info = infos.get(0);
		assertThat("Service info ID is topic", info.getId(), equalTo(topic1));
		assertThat("Service name from i18n bundle", info.getLocalizedName(), equalTo("TOPIC-01-1"));
		assertThat("Service description from i18n bundle", info.getLocalizedDescription(),
				equalTo("DESC-01-1"));

		info = infos.get(1);
		assertThat("Service info ID is topic", info.getId(), equalTo(topic2));
		assertThat("Service name from i18n bundle", info.getLocalizedName(), equalTo("TOPIC-01-2"));
		assertThat("Service description from i18n bundle", info.getLocalizedDescription(),
				equalTo("DESC-01-2"));
	}

	@Test
	public void availableTopics_multipleProducers_overlapTopic() {
		// GIVEN
		DatumAppEventProducer producer1 = EasyMock.createMock(DatumAppEventProducer.class);
		DatumAppEventProducer producer2 = EasyMock.createMock(DatumAppEventProducer.class);
		biz.setDatumEventProducers(asList(producer2, producer1));

		String topic = "topic/3";
		expect(producer2.getProducedDatumAppEventTopics()).andReturn(singleton(topic));
		expect(producer1.getProducedDatumAppEventTopics()).andReturn(singleton(topic));

		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("net/solarnetwork/central/user/event/biz/dao/test/test-messages-03");
		expect(producer2.getMessageSource()).andReturn(ms);

		// WHEN
		replayAll(producer1, producer2);
		Iterable<LocalizedServiceInfo> itr = biz.availableDatumEventTopics(Locale.ENGLISH);

		// THEN
		assertThat("Service info iterable returned", itr, notNullValue());
		List<LocalizedServiceInfo> infos = StreamSupport.stream(itr.spliterator(), false)
				.collect(toList());
		assertThat("Service info count", infos, hasSize(1));
		LocalizedServiceInfo info;

		info = infos.get(0);
		assertThat("Service info ID is topic", info.getId(), equalTo(topic));
		assertThat("Service name from i18n bundle", info.getLocalizedName(), equalTo("TOPIC-03-3"));
		assertThat("Service description from i18n bundle", info.getLocalizedDescription(),
				equalTo("DESC-03-3"));
	}

	@Test
	public void listConfigurationsForUser() {
		// GIVEN
		final Long userId = 1L;
		List<UserNodeEventHookConfiguration> confs = new ArrayList<>();
		expect(nodeEventHookConfigurationDao.findConfigurationsForUser(userId)).andReturn(confs);

		// WHEN
		replayAll();
		List<UserNodeEventHookConfiguration> result = biz.configurationsForUser(userId,
				UserNodeEventHookConfiguration.class);

		// THEN
		assertThat("DAO result returned", result, sameInstance(confs));
	}

	@Test
	public void configurationForId() {
		// GIVEN
		final Long userId = 1L;
		final Long confId = 2L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(confId, userId, now());
		expect(nodeEventHookConfigurationDao.get(eq(new UserLongPK(userId, confId)))).andReturn(conf);

		// WHEN
		replayAll();
		UserNodeEventHookConfiguration result = biz.configurationForUser(userId,
				UserNodeEventHookConfiguration.class, confId);

		// THEN
		assertThat("DAO result returned", result, sameInstance(conf));
	}

	@Test
	public void saveConfiguration() {
		// GIVEN
		final Long userId = 1L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(null, userId, now());
		final UserLongPK pk = new UserLongPK(userId, 2L);
		expect(nodeEventHookConfigurationDao.save(conf)).andReturn(pk);

		// WHEN
		replayAll();
		CompositeKey2<Long, Long> result = biz.saveConfiguration(conf);

		// THEN
		assertThat("PK result matches", result, sameInstance(pk));
	}

	@Test
	public void updateConfiguration_withSecureServiceProperty_unchanged() {
		// GIVEN
		final String serviceId = "test.service";
		final Long userId = 1L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(2L, userId, now());
		Map<String, Object> confProps = new LinkedHashMap<>(2);
		confProps.put("name", "foo");
		confProps.put("password", "");
		conf.setServiceProps(confProps);
		conf.setServiceIdentifier(serviceId);

		UserNodeEventHookConfiguration existing = new UserNodeEventHookConfiguration(conf.getId(),
				conf.getCreated());
		Map<String, Object> existingProps = new LinkedHashMap<>(2);
		existingProps.put("name", "bar");
		existingProps.put("password", "existing.secret");
		existing.setServiceProps(existingProps);

		// query for the hook service settings to look for secure fields
		UserNodeEventHookService hookService = EasyMock.createMock(UserNodeEventHookService.class);
		biz.setNodeEventHookServices(singletonList(hookService));
		expect(hookService.getSettingUid()).andReturn(serviceId);
		expect(hookService.getSettingSpecifiers()).andReturn(
				asList((SettingSpecifier) new BasicTextFieldSettingSpecifier("name", "", false),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("password", "", true)));

		// get the existing entity, to pull out current secure field values
		expect(nodeEventHookConfigurationDao.get(conf.getId())).andReturn(existing);

		// the conf service properties map should have been populated with the existing password value
		expect(nodeEventHookConfigurationDao.save(conf)).andReturn(conf.getId());

		// WHEN
		replayAll(hookService);
		CompositeKey2<Long, Long> result = biz.saveConfiguration(conf);

		// THEN
		assertThat("PK result matches", result, sameInstance(conf.getId()));
		assertThat("Non-secure setting value preserved in update entity", confProps,
				hasEntry("name", "foo"));
		assertThat("Secure setting value extracted from existing entity", confProps,
				hasEntry("password", "existing.secret"));
	}

	@Test
	public void deleteConfiguration() {
		// GIVEN
		final Long userId = 1L;
		final Long confId = 2L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(confId, userId, now());
		nodeEventHookConfigurationDao.delete(conf);

		// WHEN
		replayAll();
		biz.deleteConfiguration(conf);

		// THEN
	}
}
