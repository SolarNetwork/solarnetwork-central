/* ==================================================================
 * JdbcSolarNodeMetadataDaoTests.java - 12/11/2024 9:29:42 pm
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertNodeMetadata;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link JdbcSolarNodeMetadataDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcSolarNodeMetadataDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcSolarNodeMetadataDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcSolarNodeMetadataDao(jdbcTemplate);
		setupTestNode();
	}

	private List<Map<String, Object>> allNodeMetadataRows() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solarnet.sn_node_meta");
		log.debug("solarnet.sn_node_meta table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	private SolarNodeMetadata lastDatum;

	private SolarNodeMetadata getTestInstance() {
		SolarNodeMetadata datum = new SolarNodeMetadata();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		// GIVEN
		SolarNodeMetadata datum = getTestInstance();

		// WHEN
		Long id = dao.save(datum);

		// THEN
		then(id).as("Primary key returned").isEqualTo(datum.getNodeId());

		var rows = allNodeMetadataRows();
		// @formatter:off
		then(rows)
			.as("Row exists in table")
			.hasSize(1)
			.element(0)
			.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
			.as("Node ID persisted")
			.containsEntry("node_id", id)
			.as("Creation persisted")
			.containsEntry("created", Timestamp.from(datum.getCreated()))
			.as("Updated populated by database")
			.containsKey("updated")
			.hasEntrySatisfying("jdata", json -> {
				then(JsonUtils.getStringMap(json.toString()))
					.as("Metadata persisted")
					.isEqualTo(JsonUtils.getStringMap(datum.getMetaJson()))
					;
			})
			;
		// @formatter:on

		lastDatum = datum;
	}

	@Test
	public void storeUpdate() {
		// GIVEN
		storeNew();

		// WHEN
		SolarNodeMetadata datum = lastDatum;
		datum.getMeta().putInfoValue("bim", "bam");
		Long id = dao.save(datum);

		// THEN
		then(id).as("Primary key returned").isEqualTo(datum.getNodeId());
	}

	private void validate(SolarNodeMetadata expected, SolarNodeMetadata entity) {
		// @formatter:off
		then(entity)
			.as("Entity exists")
			.isNotNull()
			.as("Node ID matches")
			.returns(expected.getNodeId(), from(SolarNodeMetadata::getNodeId))
			.as("Created matches")
			.returns(expected.getCreated(), from(SolarNodeMetadata::getCreated))
			.as("Metadata matches")
			.returns(expected.getMetadata(), from(SolarNodeMetadata::getMetadata))
			;
		// @formatter:on
	}

	@Test
	public void getByPrimaryKey() {
		// GIVEN
		storeNew();

		// WHEN
		SolarNodeMetadata datum = dao.get(lastDatum.getId());

		// THEN
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		// GIVEN
		SolarNodeMetadata datum = getTestInstance();
		datum.getMeta().getInfo().put("watt_hours", 39309570293789380L);
		datum.getMeta().getInfo().put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getMeta().getInfo().put("watts", 498475890235787897L);
		datum.getMeta().getInfo().put("floating",
				new BigDecimal("293487590845639845728947589237.49087"));

		// WHEN
		dao.save(datum);
		SolarNodeMetadata entity = dao.get(datum.getId());

		// THEN
		validate(datum, entity);
	}

	@Test
	public void delete() {
		// GIVEN
		storeNew();

		// WHEN
		dao.delete(lastDatum);

		// THEN
		var rows = allNodeMetadataRows();
		then(rows).as("Row removed from table").isEmpty();
	}

	@Test
	public void findFiltered_nodeId() {
		// GIVEN
		storeNew();

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeId(TEST_NODE_ID);

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("Result provided")
			.isNotNull()
			.as("Single result returned")
			.hasSize(1)
			.satisfies(l -> {
				validate(lastDatum, l.iterator().next());
			})
			;
		// formatter:on
	}

	/**
	 * Create metadata for a set of new nodes, one node per given info value.
	 * 
	 * @param infoValues
	 *        the {@code foo} info value to give each node's metadata
	 * @return the node IDs created, in the order of {@code infoValues}
	 */
	private List<Long> setupNodeMetadata(String... infoValues) {
		List<Long> nodeIds = new ArrayList<>(infoValues.length);
		for ( String infoValue : infoValues ) {
			Long nodeId = randomLong();
			setupTestNode(nodeId);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", infoValue);
			insertNodeMetadata(jdbcTemplate, nodeId, meta);
			nodeIds.add(nodeId);
		}
		return nodeIds;
	}

	private static List<Long> nodeIds(FilterResults<SolarNodeMetadata, Long> results) {
		return StreamSupport.stream(results.spliterator(), false).map(SolarNodeMetadata::getNodeId)
				.toList();
	}

	@Test
	public void findFiltered_sortByNodeDescending() {
		// GIVEN
		final List<Long> nodeIds = setupNodeMetadata("a", "b", "c");
		final List<Long> expected = nodeIds.stream().sorted(Comparator.reverseOrder()).toList();

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeIds(nodeIds.toArray(Long[]::new));
		filter.setSorts(List.of(new SimpleSortDescriptor("node", true)));

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Results sorted by node ID descending")
			.containsExactlyElementsOf(expected)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_sortArgumentOverridesFilter() {
		// GIVEN
		final List<Long> nodeIds = setupNodeMetadata("a", "b", "c");
		final List<Long> expected = nodeIds.stream().sorted(Comparator.reverseOrder()).toList();

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeIds(nodeIds.toArray(Long[]::new));

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter,
				List.of(new SimpleSortDescriptor("node", true)), null, null);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Results sorted by the given sort descriptors")
			.containsExactlyElementsOf(expected)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_searchFilter() {
		// GIVEN
		final List<Long> nodeIds = setupNodeMetadata("a", "b", "c");

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeIds(nodeIds.toArray(Long[]::new));
		filter.setSearchFilter("(/m/foo=b)");

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Only the node whose metadata matches the search filter returned")
			.containsExactly(nodeIds.get(1))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_searchFilter_singleNode() {
		// GIVEN
		final List<Long> nodeIds = setupNodeMetadata("a");

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeId(nodeIds.getFirst());
		filter.setSearchFilter("(/m/foo=nope)");

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("Search filter applied even for a single node criteria")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_paginated() {
		// GIVEN
		final List<Long> nodeIds = setupNodeMetadata("a", "b", "c");
		final List<Long> expected = nodeIds.stream().sorted().toList();

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeIds(nodeIds.toArray(Long[]::new));
		filter.setOffset(1L);
		filter.setMax(1);

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Only the requested page of results returned")
			.containsExactly(expected.get(1))
			;
		then(results.getTotalResults())
			.as("Total available result count returned")
			.isEqualTo(3L)
			;
		then(results.getStartingOffset())
			.as("Requested offset returned")
			.isEqualTo(1L)
			;
		then(results.getReturnedResultCount())
			.as("Returned result count is the page size")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_unpaginated_totalResultsIsRowCount() {
		// GIVEN
		final List<Long> nodeIds = setupNodeMetadata("a", "b", "c");

		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setNodeIds(nodeIds.toArray(Long[]::new));

		// WHEN
		FilterResults<SolarNodeMetadata, Long> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("All results returned")
			.hasSize(3)
			;
		then(results.getTotalResults())
			.as("Total result count is the returned row count")
			.isEqualTo(3L)
			;
		then(results.getStartingOffset())
			.as("Starting offset defaults to zero")
			.isEqualTo(0L)
			;
		// @formatter:on
	}

}
