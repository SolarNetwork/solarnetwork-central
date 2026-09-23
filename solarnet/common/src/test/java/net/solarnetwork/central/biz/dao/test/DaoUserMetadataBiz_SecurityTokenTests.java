/* ==================================================================
 * DaoUserMetadataBiz_SecurityTokenTests.java - 23/09/2026 6:50:00 pm
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

package net.solarnetwork.central.biz.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.biz.dao.DaoUserMetadataBiz;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the security token criteria support of the
 * {@link DaoUserMetadataBiz} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserMetadataBiz_SecurityTokenTests {

	@Mock
	private UserMetadataDao userMetadataDao;

	@Captor
	private ArgumentCaptor<UserMetadataFilter> filterCaptor;

	private DaoUserMetadataBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoUserMetadataBiz(userMetadataDao);
	}

	@AfterEach
	public void teardown() {
		SecurityUtils.removeAuthentication();
	}

	private void givenDaoReturnsNoResults() {
		given(userMetadataDao.findFiltered(any(), any(), any(), any()))
				.willReturn(new BasicFilterResults<UserMetadataEntity, Long>(List.of()));
	}

	@Test
	public void actorToken_appliedToCriteria() {
		// GIVEN
		final Long userId = randomLong();
		final String tokenId = randomString();
		SecurityUtils.becomeToken(tokenId, SecurityTokenType.ReadNodeData, userId, null);
		givenDaoReturnsNoResults();

		var criteria = new BasicUserMetadataFilter();
		criteria.setUserId(userId);

		// WHEN
		biz.findUserMetadata(criteria, null, null, null);

		// THEN
		// @formatter:off
		then(userMetadataDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		and.then(filterCaptor.getValue())
			.as("Criteria passed to the DAO carries the actor's token")
			.returns(tokenId, UserMetadataFilter::getTokenId)
			.as("Other criteria are preserved")
			.returns(userId, UserMetadataFilter::getUserId)
			;
		// @formatter:on
	}

	@Test
	public void actorToken_overridesSuppliedToken() {
		// GIVEN
		final Long userId = randomLong();
		final String actorTokenId = randomString();
		SecurityUtils.becomeToken(actorTokenId, SecurityTokenType.ReadNodeData, userId, null);
		givenDaoReturnsNoResults();

		// criteria naming some other token must not widen what the actor can see
		var criteria = new BasicUserMetadataFilter();
		criteria.setUserId(userId);
		criteria.setTokenId(randomString());

		// WHEN
		biz.findUserMetadata(criteria, null, null, null);

		// THEN
		// @formatter:off
		then(userMetadataDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		and.then(filterCaptor.getValue())
			.as("The actor's token always wins over supplied token criteria")
			.returns(actorTokenId, UserMetadataFilter::getTokenId)
			;
		// @formatter:on
	}

	@Test
	public void noActorToken_criteriaUnchanged() {
		// GIVEN
		givenDaoReturnsNoResults();

		var criteria = new BasicUserMetadataFilter();
		criteria.setUserId(randomLong());

		// WHEN
		biz.findUserMetadata(criteria, null, null, null);

		// THEN
		// @formatter:off
		then(userMetadataDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());

		and.then(filterCaptor.getValue())
			.as("Criteria passed through unchanged when the actor is not a token")
			.isSameAs(criteria)
			;
		// @formatter:on
	}

}
