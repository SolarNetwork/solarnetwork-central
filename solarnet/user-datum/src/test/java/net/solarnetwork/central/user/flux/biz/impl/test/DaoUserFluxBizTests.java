/* ==================================================================
 * DaoUserFluxBizTests.java - 25/06/2024 1:14:41 pm
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

package net.solarnetwork.central.user.flux.biz.impl.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.biz.impl.DaoUserFluxBiz;
import net.solarnetwork.central.user.flux.dao.BasicFluxConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.UserFluxDefaultAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfigurationInput;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfigurationInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link DaoUserFluxBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserFluxBizTests {

	@Mock
	private UserFluxDefaultAggregatePublishConfigurationDao defaultAggPublishConfDao;

	@Mock
	private UserFluxAggregatePublishConfigurationDao aggPublishConfDao;

	@Captor
	private ArgumentCaptor<UserFluxDefaultAggregatePublishConfiguration> defaultAggPublishConfCaptor;

	@Captor
	private ArgumentCaptor<UserFluxAggregatePublishConfiguration> aggPublishConfCaptor;

	@Captor
	private ArgumentCaptor<UserLongCompositePK> idCaptor;

	@Captor
	private ArgumentCaptor<UserFluxAggregatePublishConfigurationFilter> filterCaptor;

	private Clock clock;
	private DaoUserFluxBiz service;

	@BeforeEach
	public void setup() {
		clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC);
		service = new DaoUserFluxBiz(clock, defaultAggPublishConfDao, aggPublishConfDao);
	}

	@Test
	public void saveDefaultAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();

		given(defaultAggPublishConfDao.save(any())).willReturn(userId);

		// WHEN
		var input = new UserFluxDefaultAggregatePublishConfigurationInput();
		input.setPublish(true);
		input.setRetain(true);
		UserFluxDefaultAggregatePublishConfiguration result = service
				.saveDefaultAggregatePublishConfiguration(userId, input);

		// THEN
		then(defaultAggPublishConfDao).should().save(result);

		// @formatter:off
		and.then(result)
			.as("Entity returned")
			.isNotNull()
			.as("ID is user ID")
			.returns(userId, from(UserFluxDefaultAggregatePublishConfiguration::getId))
			.as("Creation date 'now' from clock")
			.returns(clock.instant(), from(UserFluxDefaultAggregatePublishConfiguration::getCreated))
			.as("Modified date 'now' from clock")
			.returns(clock.instant(), from(UserFluxDefaultAggregatePublishConfiguration::getModified))
			.as("Publish from input")
			.returns(true, UserFluxDefaultAggregatePublishConfiguration::isPublish)
			.as("Retain from input")
			.returns(true, UserFluxDefaultAggregatePublishConfiguration::isRetain)
			;
		// @formatter:on
	}

	@Test
	public void getDefaultAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();
		final var entity = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.now());

		given(defaultAggPublishConfDao.get(userId)).willReturn(entity);

		// WHEN
		var result = service.defaultAggregatePublishConfigurationForUser(userId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void getDefaultAggregatePublishConfiguration_default() {
		// GIVEN
		final Long userId = randomLong();

		given(defaultAggPublishConfDao.get(userId)).willReturn(null);

		// WHEN
		var result = service.defaultAggregatePublishConfigurationForUser(userId);

		// THEN
		// @formatter:off
		final var expected = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.EPOCH);
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result is for given user")
			.returns(userId, from(UserFluxDefaultAggregatePublishConfiguration::getUserId))
			.as("Creation date is epoch")
			.returns(Instant.EPOCH, from(UserFluxDefaultAggregatePublishConfiguration::getCreated))
			.as("Result has default instance property values")
			.satisfies(conf -> conf.isSameAs(expected))
			;
		// @formatter:on
	}

	@Test
	public void deleteDefaultAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();

		// WHEN
		service.deleteDefaultAggregatePublishConfiguration(userId);

		// THEN
		// @formatter:off
		then(defaultAggPublishConfDao).should().delete(defaultAggPublishConfCaptor.capture());
		and.then(defaultAggPublishConfCaptor.getValue())
			.as("User ID passed to DAO as ID to delete")
			.returns(userId, from(UserFluxDefaultAggregatePublishConfiguration::getId))
			;
		// @formatter:on
	}

	@Test
	public void saveAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, randomLong());

		given(aggPublishConfDao.create(eq(userId), any())).willReturn(pk);

		// WHEN
		var input = new UserFluxAggregatePublishConfigurationInput();
		input.setNodeIds(new Long[] { 1L, 2L });
		input.setSourceIds(new String[] { "a", "b" });
		input.setPublish(true);
		input.setRetain(true);
		UserFluxAggregatePublishConfiguration result = service.saveAggregatePublishConfiguration(pk,
				input);

		// THEN
		// @formatter:off
		then(aggPublishConfDao).should().create(eq(userId), aggPublishConfCaptor.capture());

		and.then(aggPublishConfCaptor.getValue())
			.as("Entity passed to DAO is derived from input passed to service with unassigned entity ID")
			.returns(true, from(e -> e.isSameAs(input.toEntity(unassignedEntityIdKey(userId), clock.instant()))))
			;
		
		and.then(result)
			.as("Entity returned")
			.isNotNull()
			.as("Result is derived from input with primary key returned from DAO")
			.returns(true, from(e -> e.isSameAs(input.toEntity(pk, clock.instant()))))
			;
		// @formatter:on

	}

	@Test
	public void deleteAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, randomLong());

		// WHEN
		service.deleteAggregatePublishConfiguration(pk);

		// THEN
		// @formatter:off
		then(aggPublishConfDao).should().delete(aggPublishConfCaptor.capture());
		and.then(aggPublishConfCaptor.getValue())
			.as("ID passed to DAO as ID to delete")
			.returns(pk, from(UserFluxAggregatePublishConfiguration::getId))
			;
		// @formatter:on
	}

	@Test
	public void getAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();
		final Long confId = randomLong();

		final var conf = new UserFluxAggregatePublishConfiguration(userId, confId, Instant.now());
		given(aggPublishConfDao.get(any())).willReturn(conf);

		// WHEN
		UserFluxAggregatePublishConfiguration result = service
				.aggregatePublishConfigurationForUser(userId, confId);

		// THEN
		// @formatter:off
		then(aggPublishConfDao).should().get(idCaptor.capture());
		and.then(idCaptor.getValue())
			.as("Primary key passed to DAO")
			.isNotNull()
			.as("Primary key passed to DAO includes user ID passed to service")
			.returns(userId, from(UserLongCompositePK::getUserId))
			.as("Primary key passed to DAO includes configuration ID passed to service")
			.returns(confId, from(UserLongCompositePK::getEntityId))
			;
		
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(conf)
			;
		// @formatter:on
	}

	@Test
	public void filterAggregatePublishConfiguration() {
		// GIVEN
		final Long userId = randomLong();

		final var conf = new UserFluxAggregatePublishConfiguration(randomLong(), randomLong(),
				Instant.now());
		final var daoResults = new BasicFilterResults<>(asList(conf));
		given(aggPublishConfDao.findFiltered(any())).willReturn(daoResults);

		// WHEN
		final var filter = new BasicFluxConfigurationFilter();
		filter.setNodeIds(new Long[] { 1L, 2L });
		FilterResults<UserFluxAggregatePublishConfiguration, UserLongCompositePK> results = service
				.aggregatePublishConfigurationsForUser(userId, filter);

		// THEN
		// @formatter:off
		then(aggPublishConfDao).should().findFiltered(filterCaptor.capture());
		
		final var expectedDaoFilter = new BasicFluxConfigurationFilter();
		expectedDaoFilter.copyFrom(filter);
		expectedDaoFilter.setUserId(userId);
		
		and.then(filterCaptor.getValue())
			.as("Filter passed to DAO")
			.isNotNull()
			.as("Filter passed to DAO includes user ID passed to service")
			.isEqualTo(expectedDaoFilter)
			;
		
		and.then(results)
			.as("Results from DAO returned")
			.isSameAs(daoResults)
			;
		// @formatter:on
	}

	@Test
	public void filterAggregatePublishConfiguration_userIdOverridesFilter() {
		// GIVEN
		final Long userId = randomLong();

		final var conf = new UserFluxAggregatePublishConfiguration(randomLong(), randomLong(),
				Instant.now());
		final var daoResults = new BasicFilterResults<>(asList(conf));
		given(aggPublishConfDao.findFiltered(any())).willReturn(daoResults);

		// WHEN
		final var filter = new BasicFluxConfigurationFilter();
		filter.setUserIds(new Long[] { 1L, 2L }); // try to sneak in random user IDs
		FilterResults<UserFluxAggregatePublishConfiguration, UserLongCompositePK> results = service
				.aggregatePublishConfigurationsForUser(userId, filter);

		// THEN
		// @formatter:off
		then(aggPublishConfDao).should().findFiltered(filterCaptor.capture());
		
		final var expectedDaoFilter = new BasicFluxConfigurationFilter();
		expectedDaoFilter.setUserId(userId);
		
		and.then(filterCaptor.getValue())
			.as("Filter passed to DAO")
			.isNotNull()
			.as("Filter passed to DAO includes user ID passed as service argument, not filter property")
			.isEqualTo(expectedDaoFilter)
			;
		
		and.then(results)
			.as("Results from DAO returned")
			.isSameAs(daoResults)
			;
		// @formatter:on
	}

}
