/* ==================================================================
 * DaoSolarNodeMetadataBiz_SecurityTokenTests.java - 23/09/2026 6:55:00 pm
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
import net.solarnetwork.central.biz.dao.DaoSolarNodeMetadataBiz;
import net.solarnetwork.central.common.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.common.dao.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the security token criteria support of the
 * {@link DaoSolarNodeMetadataBiz} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoSolarNodeMetadataBiz_SecurityTokenTests {

	@Mock
	private SolarNodeMetadataDao solarNodeMetadataDao;

	@Captor
	private ArgumentCaptor<SolarNodeMetadataFilter> filterCaptor;

	private DaoSolarNodeMetadataBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoSolarNodeMetadataBiz(solarNodeMetadataDao);
	}

	@AfterEach
	public void teardown() {
		SecurityUtils.removeAuthentication();
	}

	private void givenDaoReturnsNoResults() {
		given(solarNodeMetadataDao.findFiltered(any(), any(), any(), any()))
				.willReturn(new BasicFilterResults<SolarNodeMetadata, Long>(List.of()));
	}

	@Test
	public void actorToken_appliedToCriteria() {
		// GIVEN
		final Long nodeId = randomLong();
		final String tokenId = randomString();
		SecurityUtils.becomeToken(tokenId, SecurityTokenType.ReadNodeData, randomLong(), null);
		givenDaoReturnsNoResults();

		var criteria = new FilterSupport();
		criteria.setNodeId(nodeId);

		// WHEN
		biz.findSolarNodeMetadata(criteria, null, null, null);

		// THEN
		// @formatter:off
		then(solarNodeMetadataDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());

		and.then(filterCaptor.getValue())
			.as("Criteria passed to the DAO carries the actor's token")
			.returns(tokenId, SolarNodeMetadataFilter::getTokenId)
			.as("Other criteria are translated for the DAO")
			.returns(nodeId, SolarNodeMetadataFilter::getNodeId)
			;
		// @formatter:on
	}

	@Test
	public void noActorToken_noTokenCriteria() {
		// GIVEN
		givenDaoReturnsNoResults();

		var criteria = new FilterSupport();
		criteria.setNodeId(randomLong());

		// WHEN
		biz.findSolarNodeMetadata(criteria, null, null, null);

		// THEN
		// @formatter:off
		then(solarNodeMetadataDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());

		and.then(filterCaptor.getValue())
			.as("No token criteria when the actor is not a token")
			.returns(false, SolarNodeMetadataFilter::hasTokenCriteria)
			;
		// @formatter:on
	}

}
