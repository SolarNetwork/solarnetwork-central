/* ==================================================================
 * UserDnp3ControllerTests.java - 8/08/2023 6:49:46 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.dnp3.dao.ServerDataPointFilter;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.reg.web.api.v1.UserDnp3Controller;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link UserDnp3Controller} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UserDnp3ControllerTests {

	@Mock
	private UserDnp3Biz userDnp3Biz;

	@Captor
	private ArgumentCaptor<ServerFilter> filterCaptor;

	@Captor
	private ArgumentCaptor<ServerDataPointFilter> dataPointFilterCaptor;

	private UserDnp3Controller controller;

	@BeforeEach
	public void setup() {
		controller = new UserDnp3Controller(userDnp3Biz);
	}

	private void becomeUser(Long userId) {
		User userDetails = new User("tester@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", "ROLE_USER");
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Test
	public void getServer() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final Long serverId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		var conf = new ServerConfiguration(userId, serverId, now());
		var searchResults = new BasicFilterResults<>(asList(conf));
		given(userDnp3Biz.serversForUser(eq(userId), any())).willReturn(searchResults);

		// WHEN
		Result<ServerConfiguration> result = controller.getServer(serverId);

		// THEN
		// @formatter:off
		then(result.getData()).as("First query result returned").isSameAs(conf);

		verify(userDnp3Biz).serversForUser(eq(userId), filterCaptor.capture());
		
		then(filterCaptor.getValue())
			.as("Server ID provided in criteria")
			.returns(serverId, ServerFilter::getServerId)
			.as("No identifier criteria")
			.returns(false, ServerFilter::hasIdentifierCriteria)
			.as("No index criteria")
			.returns(false, ServerFilter::hasIndexCriteria)
			.as("No enabled criteria")
			.returns(false, ServerFilter::hasEnabledCriteria)
			;
		// @formatter:on
	}

	@Test
	public void getServerAuth() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final Long serverId = UUID.randomUUID().getMostSignificantBits();
		final String identifier = UUID.randomUUID().toString();
		becomeUser(userId);

		var conf = new ServerAuthConfiguration(userId, serverId, identifier, now());
		var searchResults = new BasicFilterResults<>(asList(conf));
		given(userDnp3Biz.serverAuthsForUser(eq(userId), any())).willReturn(searchResults);

		// WHEN
		Result<ServerAuthConfiguration> result = controller.getServerAuth(serverId, identifier);

		// THEN
		// @formatter:off
		then(result.getData()).as("First query result returned").isSameAs(conf);

		verify(userDnp3Biz).serverAuthsForUser(eq(userId), filterCaptor.capture());
		
		then(filterCaptor.getValue())
			.as("Server ID provided in criteria")
			.returns(serverId, ServerFilter::getServerId)
			.as("Identifier provided in criteria")
			.returns(identifier, ServerFilter::getIdentifier)
			.as("No index criteria")
			.returns(false, ServerFilter::hasIndexCriteria)
			.as("No enabled criteria")
			.returns(false, ServerFilter::hasEnabledCriteria)
			;
		// @formatter:on
	}

	@Test
	public void getServerMeasurement() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final Long serverId = UUID.randomUUID().getMostSignificantBits();
		final Integer index = (int) UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		var conf = new ServerMeasurementConfiguration(userId, serverId, index, now());
		var searchResults = new BasicFilterResults<>(asList(conf));
		given(userDnp3Biz.serverMeasurementsForUser(eq(userId), any())).willReturn(searchResults);

		// WHEN
		Result<ServerMeasurementConfiguration> result = controller.getServerMeasurement(serverId, index);

		// THEN
		// @formatter:off
		then(result.getData()).as("First query result returned").isSameAs(conf);

		verify(userDnp3Biz).serverMeasurementsForUser(eq(userId), dataPointFilterCaptor.capture());
		
		then(dataPointFilterCaptor.getValue())
			.as("Server ID provided in criteria")
			.returns(serverId, ServerDataPointFilter::getServerId)
			.as("No identifier criteria")
			.returns(false, ServerDataPointFilter::hasIdentifierCriteria)
			.as("Index provided in criteria")
			.returns(index, ServerDataPointFilter::getIndex)
			.as("No enabled criteria")
			.returns(false, ServerDataPointFilter::hasEnabledCriteria)
			;
		// @formatter:on
	}

	@Test
	public void getServerControl() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final Long serverId = UUID.randomUUID().getMostSignificantBits();
		final Integer index = (int) UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		var conf = new ServerControlConfiguration(userId, serverId, index, now());
		var searchResults = new BasicFilterResults<>(asList(conf));
		given(userDnp3Biz.serverControlsForUser(eq(userId), any())).willReturn(searchResults);

		// WHEN
		Result<ServerControlConfiguration> result = controller.getServerControl(serverId, index);

		// THEN
		// @formatter:off
		then(result.getData()).as("First query result returned").isSameAs(conf);

		verify(userDnp3Biz).serverControlsForUser(eq(userId), dataPointFilterCaptor.capture());
		
		then(dataPointFilterCaptor.getValue())
			.as("Server ID provided in criteria")
			.returns(serverId, ServerDataPointFilter::getServerId)
			.as("No identifier criteria")
			.returns(false, ServerDataPointFilter::hasIdentifierCriteria)
			.as("Index provided in criteria")
			.returns(index, ServerDataPointFilter::getIndex)
			.as("No enabled criteria")
			.returns(false, ServerDataPointFilter::hasEnabledCriteria)
			;
		// @formatter:on
	}

}
