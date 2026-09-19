/* ==================================================================
 * ReportableIntervalControllerTests.java - 2/04/2026 10:42:58 am
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

package net.solarnetwork.central.query.web.api.test;

import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.set;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.web.api.ReportableIntervalController;
import net.solarnetwork.central.query.web.domain.GeneralReportableIntervalCommand;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link ReportableIntervalController}.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ReportableIntervalControllerTests {

	@Mock
	private QueryBiz queryBiz;

	@Mock
	private DatumMetadataBiz datumMetadataBiz;

	@Mock
	private HttpServletRequest req;

	@Captor
	private ArgumentCaptor<GeneralNodeDatumFilter> gndFilterCaptor;

	private ReportableIntervalController controller;

	@BeforeEach
	public void setup() {
		controller = new ReportableIntervalController(queryBiz, datumMetadataBiz);
		controller.setTransientExceptionRetryCount(0);
	}

	@Test
	public void findAvailableSources_forNode() {
		// GIVEN
		final Long nodeId = randomLong();

		final String sourceId = randomSourceId();
		final Set<NodeSourcePK> bizResult = new HashSet<>();
		bizResult.add(new NodeSourcePK(nodeId, sourceId));
		given(queryBiz.findAvailableSources(any())).willReturn(bizResult);

		// WHEN
		final var criteria = new GeneralReportableIntervalCommand();
		criteria.setNodeId(nodeId);
		final Result<Set<?>> result = controller.findAvailableSources(req, criteria);

		// THEN
		// @formatter:off
		then(queryBiz).should().findAvailableSources(gndFilterCaptor.capture());
		and.then(gndFilterCaptor.getValue())
			.as("Node IDs from criteria passed to QueryBiz")
			.returns(criteria.getNodeIds(), from(GeneralNodeDatumFilter::getNodeIds))
			.as("Start date from criteria passed to QueryBiz")
			.returns(criteria.getStartDate(), from(GeneralNodeDatumFilter::getStartDate))
			.as("End date from criteria passed to QueryBiz")
			.returns(criteria.getEndDate(), from(GeneralNodeDatumFilter::getEndDate))
			.as("Include alias mode from criteria passed to QueryBiz")
			.returns(criteria.getIncludeStreamAliases(), from(GeneralNodeDatumFilter::getIncludeStreamAliases))
			;
		
		and.then(result)
			.as("Result is provided")
			.isNotNull()
			.as("Successful result")
			.returns(true, from(Result<Set<?>>::getSuccess))
			.extracting(Result::getData)
			.as("Data is set of source IDs extracted from biz result")
			.asInstanceOf(set(String.class))
			.containsOnly(sourceId)
			;
		// @formatter:on
	}

	@Test
	public void findAvailableSources_forNode_withDateRange() {
		// GIVEN
		final Long nodeId = randomLong();

		final String sourceId = randomSourceId();
		final Set<NodeSourcePK> bizResult = new HashSet<>();
		bizResult.add(new NodeSourcePK(nodeId, sourceId));
		given(queryBiz.findAvailableSources(any())).willReturn(bizResult);

		// WHEN
		final var criteria = new GeneralReportableIntervalCommand();
		criteria.setNodeId(nodeId);
		criteria.setStartDate(Instant.now().truncatedTo(HOURS));
		criteria.setEndDate(criteria.getStartDate().plus(1L, HOURS));
		final Result<Set<?>> result = controller.findAvailableSources(req, criteria);

		// THEN
		// @formatter:off
		then(queryBiz).should().findAvailableSources(gndFilterCaptor.capture());
		and.then(gndFilterCaptor.getValue())
			.as("Node IDs from criteria passed to QueryBiz")
			.returns(criteria.getNodeIds(), from(GeneralNodeDatumFilter::getNodeIds))
			.as("Start date from criteria passed to QueryBiz")
			.returns(criteria.getStartDate(), from(GeneralNodeDatumFilter::getStartDate))
			.as("End date from criteria passed to QueryBiz")
			.returns(criteria.getEndDate(), from(GeneralNodeDatumFilter::getEndDate))
			.as("Include alias mode from criteria passed to QueryBiz")
			.returns(criteria.getIncludeStreamAliases(), from(GeneralNodeDatumFilter::getIncludeStreamAliases))
			;
		
		and.then(result)
			.as("Result is provided")
			.isNotNull()
			.as("Successful result")
			.returns(true, from(Result<Set<?>>::getSuccess))
			.extracting(Result::getData)
			.as("Data is set of source IDs extracted from biz result")
			.asInstanceOf(set(String.class))
			.containsOnly(sourceId)
			;
		// @formatter:on
	}

	@Test
	public void findAvailableSources_forNode_includeAliases() {
		// GIVEN
		final Long nodeId = randomLong();

		final String sourceId = randomSourceId();
		final Set<NodeSourcePK> bizResult = new HashSet<>();
		bizResult.add(new NodeSourcePK(nodeId, sourceId));
		given(queryBiz.findAvailableSources(any())).willReturn(bizResult);

		// WHEN
		final var criteria = new GeneralReportableIntervalCommand();
		criteria.setNodeId(nodeId);
		criteria.setIncludeStreamAliases(true);
		final Result<Set<?>> result = controller.findAvailableSources(req, criteria);

		// THEN
		// @formatter:off
		then(queryBiz).should().findAvailableSources(gndFilterCaptor.capture());
		and.then(gndFilterCaptor.getValue())
			.as("Node IDs from criteria passed to QueryBiz")
			.returns(criteria.getNodeIds(), from(GeneralNodeDatumFilter::getNodeIds))
			.as("Start date from criteria passed to QueryBiz")
			.returns(criteria.getStartDate(), from(GeneralNodeDatumFilter::getStartDate))
			.as("End date from criteria passed to QueryBiz")
			.returns(criteria.getEndDate(), from(GeneralNodeDatumFilter::getEndDate))
			.as("Include alias mode from criteria passed to QueryBiz")
			.returns(criteria.getIncludeStreamAliases(), from(GeneralNodeDatumFilter::getIncludeStreamAliases))
			;
		
		and.then(result)
			.as("Result is provided")
			.isNotNull()
			.as("Successful result")
			.returns(true, from(Result<Set<?>>::getSuccess))
			.extracting(Result::getData)
			.as("Data is set of source IDs extracted from biz result")
			.asInstanceOf(set(String.class))
			.containsOnly(sourceId)
			;
		// @formatter:on
	}

}
