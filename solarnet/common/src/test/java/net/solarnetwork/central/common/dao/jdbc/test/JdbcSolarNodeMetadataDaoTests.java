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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link JdbcSolarNodeMetadataDao} class.
 * 
 * @author matt
 * @version 1.0
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
	
}
