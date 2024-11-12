/* ==================================================================
 * CachingSolarNodeMetadataDaoTests.java - 13/11/2024 7:56:30 am
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

package net.solarnetwork.central.common.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.List;
import java.util.concurrent.Executors;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.CachingSolarNodeMetadataDao;
import net.solarnetwork.central.common.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link CachingSolarNodeMetadataDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("static-access")
public class CachingSolarNodeMetadataDaoTests {

	@Mock
	private Cache<Long, SolarNodeMetadata> entityCache;

	@Mock
	private SolarNodeMetadataDao delegate;

	private CachingSolarNodeMetadataDao dao;

	@BeforeEach
	public void setup() {
		dao = new CachingSolarNodeMetadataDao(delegate, entityCache,
				Executors.newVirtualThreadPerTaskExecutor());
	}

	@Test
	public void get_cacheMiss() {
		// GIVEN
		final Long nodeId = randomLong();

		given(entityCache.get(nodeId)).willReturn(null);

		final var metadata = new SolarNodeMetadata(nodeId);
		given(delegate.get(nodeId)).willReturn(metadata);

		// WHEN
		SolarNodeMetadata result = dao.get(nodeId);

		// THEN
		and.then(result).as("Result from delegate").isSameAs(metadata);
	}

	@Test
	public void get_cacheHit() {
		// GIVEN
		final Long nodeId = randomLong();

		final var metadata = new SolarNodeMetadata(nodeId);
		given(entityCache.get(nodeId)).willReturn(metadata);

		// WHEN
		SolarNodeMetadata result = dao.get(nodeId);

		// THEN
		then(delegate).shouldHaveNoInteractions();
		and.then(result).as("Result from cache").isSameAs(metadata);
	}

	@Test
	public void findForNodeId_cacheMiss() {
		// GIVEN
		final Long nodeId = randomLong();

		given(entityCache.get(nodeId)).willReturn(null);

		final var delegateResult = new SolarNodeMetadata(nodeId);
		given(delegate.get(nodeId)).willReturn(delegateResult);

		// WHEN
		final var filter = new BasicCoreCriteria();
		filter.setNodeId(nodeId);

		FilterResults<SolarNodeMetadata, Long> result = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.hasSize(1)
			.element(0)
			.as("Delegate result returned")
			.isSameAs(delegateResult)
			;
		// @formatter:on
	}

	@Test
	public void findForNodeId_cacheHit() {
		// GIVEN
		final Long nodeId = randomLong();

		final var cacheResult = new SolarNodeMetadata(nodeId);
		given(entityCache.get(nodeId)).willReturn(cacheResult);

		// WHEN
		final var filter = new BasicCoreCriteria();
		filter.setNodeId(nodeId);

		FilterResults<SolarNodeMetadata, Long> result = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.hasSize(1)
			.element(0)
			.as("Cache result returned")
			.isSameAs(cacheResult)
			;
		// @formatter:on
	}

	@Test
	public void findForNodeIds() {
		// GIVEN
		final Long nodeId = randomLong();

		final var filter = new BasicCoreCriteria();
		filter.setNodeIds(new Long[] { nodeId, randomLong() });

		final var delegateResult = new BasicFilterResults<>(List.of(new SolarNodeMetadata(nodeId)));
		given(delegate.findFiltered(filter, null, null, null)).willReturn(delegateResult);

		// WHEN
		FilterResults<SolarNodeMetadata, Long> result = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(entityCache).shouldHaveNoInteractions();
		and.then(result)
			.as("Cache ignored when multiple node IDs provided in filter")
			.isSameAs(delegateResult)
			;
		// @formatter:on
	}

}
