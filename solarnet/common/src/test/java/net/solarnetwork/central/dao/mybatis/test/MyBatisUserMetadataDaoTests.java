/* ==================================================================
 * MyBatisUserMetadataDaoTests.java - 11/11/2016 5:50:19 PM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.mybatis.MyBatisUserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link MyBatisUserMetadataDao} class.
 *
 * @author matt
 * @version 2.1
 */
public class MyBatisUserMetadataDaoTests extends AbstractMyBatisDaoTestSupport {

	public static final String TEST_EMAIL = "foo@localhost.localdomain";

	private MyBatisUserMetadataDao dao;

	private Long testUserId;
	private UserMetadataEntity lastDatum;

	@BeforeEach
	public void setup() {
		dao = new MyBatisUserMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		setupTestNode();
		testUserId = storeNewUser(TEST_EMAIL);
	}

	private UserMetadataEntity getTestInstance() {
		return getTestInstance(testUserId);
	}

	private UserMetadataEntity getTestInstance(Long userId) {
		UserMetadataEntity datum = new UserMetadataEntity(userId, Instant.now());

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		UserMetadataEntity datum = getTestInstance();
		Long id = dao.save(datum);
		then(id).isNotNull();
		lastDatum = datum;
	}

	private void validate(UserMetadataEntity src, UserMetadataEntity entity) {
		// @formatter:off
		then(entity).as("UserMetadataEntity should exist").isNotNull()
			.returns(src.getUserId(), from(UserMetadataEntity::getUserId))
			.returns(src.getCreated(), from(UserMetadataEntity::getCreated))
			.returns(src.getMeta(), from(UserMetadataEntity::getMeta))
			;
		// @formatter:on
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserMetadataEntity datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		UserMetadataEntity datum = getTestInstance();
		datum.getMeta().getInfo().put("watt_hours", 39309570293789380L);
		datum.getMeta().getInfo().put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getMeta().getInfo().put("watts", 498475890235787897L);
		datum.getMeta().getInfo().put("floating",
				new BigDecimal("293487590845639845728947589237.49087"));
		dao.save(datum);

		UserMetadataEntity entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void findFiltered() {
		storeNew();

		Long userId2 = storeNewUser("bar@example.com");
		UserMetadataEntity user2Meta = getTestInstance(userId2);
		dao.save(user2Meta);

		BasicUserMetadataFilter criteria = new BasicUserMetadataFilter();
		criteria.setUserId(testUserId);

		FilterResults<UserMetadataEntity, Long> results = dao.findFiltered(criteria, null, null, null);
		// @formatter:off
		then(results)
			.as("Non-null results returned")
			.containsExactly(lastDatum)
			.asInstanceOf(type(FilterResults.class))
			.as("Total results not returned")
			.returns(null, from(r -> r.getTotalResults()))
			.as("Returned results same as list size")
			.returns(1, from(r -> r.getReturnedResultCount()))
			;
		// @formatter:on
	}

	@Test
	public void jsonMetadataAtPath_noRow() {
		// GIVEN

		// WHEN
		String result = dao.jsonMetadataAtPath(testUserId, "/m/foo");

		// THEN
		then(result).as("No matching row returns null.").isNull();
	}

	@Test
	public void jsonMetadataAtPath_noMeta() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		dao.save(meta);

		// WHEN
		String result = dao.jsonMetadataAtPath(testUserId, "/pm/does/not/exist");

		// THEN
		then(result).as("No matching path returns null.").isNull();
	}

	@Test
	public void jsonMetadataAtPath_stringPropertyMatch() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		dao.save(meta);

		// WHEN
		String result = dao.jsonMetadataAtPath(testUserId, "/m/foo");

		// THEN
		then(result).as("String property returned as JSON string.").isEqualTo("\"bar\"");
	}

	@Test
	public void jsonMetadataAtPath_numberPropertyMatch() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		meta.getMeta().putInfoValue("num", 12345);
		dao.save(meta);

		// WHEN
		String result = dao.jsonMetadataAtPath(testUserId, "/m/num");

		// THEN
		then(result).as("Number property returned as JSON string.").isEqualTo("12345");
	}

	@Test
	public void jsonMetadataAtPath_treeMatch() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		meta.getMeta().putInfoValue("foo", "bim", "bam");
		meta.getMeta().putInfoValue("foo", "whiz", "pop");
		dao.save(meta);

		// WHEN
		String result = dao.jsonMetadataAtPath(testUserId, "/pm/foo");

		// THEN
		Map<String, Object> resultMap = JsonUtils.getStringMap(result);
		Map<String, ?> expectedMap = meta.getMeta().getPropertyInfo("foo");
		then(resultMap).as("Tree property returned as JSON object.").isEqualTo(expectedMap);
	}

	@Test
	public void jsonMetadataAtPath_arrayMatch() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		meta.getMeta().putInfoValue("foo", "bim", new String[] { "one", "two" });
		dao.save(meta);

		// WHEN
		String result = dao.jsonMetadataAtPath(testUserId, "/pm/foo/bim");

		// THEN
		String[] resultArray = JsonUtils.getObjectFromJSON(result, String[].class);
		then(resultArray).as("Array property returned as JSON array.").containsExactly("one", "two");
	}

}
