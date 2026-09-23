/* ==================================================================
 * JdbcUserMetadataDaoTests.java - 24 Sept 2026 7:50:22 am
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.MS_CLOCK;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityTokenWithPolicy;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserMetadata;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.jdbc.JdbcUserMetadataDao;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link JdbcUserMetadataDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserMetadataDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	public static final String TEST_EMAIL = "foo@localhost.localdomain";

	private JdbcUserMetadataDao dao;

	private Long userId;
	private UserMetadataEntity lastDatum;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserMetadataDao(jdbcTemplate);
		setupTestNode();
		userId = insertUser(jdbcTemplate, TEST_EMAIL);
	}

	private UserMetadataEntity getTestInstance() {
		return getTestInstance(userId);
	}

	private UserMetadataEntity getTestInstance(Long userId) {
		UserMetadataEntity datum = new UserMetadataEntity(userId, MS_CLOCK.instant());

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		return datum;
	}

	private GeneralDatumMetadata metadata() {
		var meta = new GeneralDatumMetadata();
		meta.setInfo(Map.of("building", "Warehouse", "room", "Office"));
		meta.setPropertyInfo(Map.of("building", Map.of("floors", 3)));
		return meta;
	}

	private String tokenWithPaths(String... paths) {
		final String tokenId = CommonTestUtils.randomString(20);
		insertSecurityTokenWithPolicy(jdbcTemplate, tokenId, randomString(), userId, "Active",
				"ReadNodeData",
				BasicSecurityPolicy.builder().withUserMetadataPaths(Set.of(paths)).build());
		return tokenId;
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

		Long userId2 = insertUser(jdbcTemplate, "bar@example.com");
		UserMetadataEntity user2Meta = getTestInstance(userId2);
		dao.save(user2Meta);

		BasicUserMetadataFilter criteria = new BasicUserMetadataFilter();
		criteria.setUserId(userId);

		FilterResults<UserMetadataEntity, Long> results = dao.findFiltered(criteria, null, null, null);
		// @formatter:off
		then(results)
			.as("Non-null results returned")
			.containsExactly(lastDatum)
			.asInstanceOf(type(FilterResults.class))
			.as("Total results is returned count")
			.returns(1L, from(r -> r.getTotalResults()))
			.as("Returned results same as list size")
			.returns(1, from(r -> r.getReturnedResultCount()))
			;
		// @formatter:on
	}

	@Test
	public void jsonMetadataAtPath_noRow() {
		// GIVEN

		// WHEN
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, "/m/foo");

		// THEN
		then(result).as("No matching row returns null.").isNull();
	}

	@Test
	public void jsonMetadataAtPath_noMeta() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		dao.save(meta);

		// WHEN
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, "/pm/does/not/exist");

		// THEN
		then(result).as("No matching path returns null.").isNull();
	}

	@Test
	public void jsonMetadataAtPath_stringPropertyMatch() {
		// GIVEN
		UserMetadataEntity meta = getTestInstance();
		dao.save(meta);

		// WHEN
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, "/m/foo");

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
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, "/m/num");

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
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, "/pm/foo");

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
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, "/pm/foo/bim");

		// THEN
		String[] resultArray = JsonUtils.getObjectFromJSON(result, String[].class);
		then(resultArray).as("Array property returned as JSON array.").containsExactly("one", "two");
	}

	@Test
	public void tokenCriteria_restrictsMetadataToPolicyPaths() {
		// GIVEN
		insertUserMetadata(jdbcTemplate, userId, metadata());
		final String tokenId = tokenWithPaths("/**/building/**");

		var filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setTokenId(tokenId);

		// WHEN
		var results = dao.findFiltered(filter, null, null, null);

		// THEN
		// @formatter:off
		then(results)
			.as("One result returned")
			.hasSize(1)
			.element(0)
			.as("Metadata restricted to the policy paths")
			.extracting(UserMetadataEntity::getMeta)
			.satisfies(meta -> {
				then(meta.getInfo())
					.as("Only the info matched by the policy is kept")
					.containsOnlyKeys("building");
				then(meta.getPropertyInfo())
					.as("Only the property info matched by the policy is kept")
					.containsOnlyKeys("building");
			})
			;
		// @formatter:on
	}

	@Test
	public void tokenCriteria_omitsResultsRestrictedToNothing() {
		// GIVEN
		insertUserMetadata(jdbcTemplate, userId, metadata());
		final String tokenId = tokenWithPaths("/pm/nothing/**");

		var filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setTokenId(tokenId);

		// WHEN
		var results = dao.findFiltered(filter, null, null, null);

		// THEN
		then(results).as("Result omitted because its metadata is restricted to nothing").isEmpty();
	}

	@Test
	public void tokenCriteria_omitsNodesNotOwnedByTokenUser() {
		// GIVEN another user's node, with metadata the policy would otherwise allow
		final Long otherUserId = randomLong();
		setupTestUser(otherUserId);
		insertUserMetadata(jdbcTemplate, otherUserId, metadata());

		final String tokenId = tokenWithPaths("/**/building/**");

		var filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setTokenId(tokenId);

		// WHEN
		var results = dao.findFiltered(filter, null, null, null);

		// THEN
		then(results).as("Node owned by another user is not visible to the token").isEmpty();
	}

	@Test
	public void noTokenCriteria_metadataNotRestricted() {
		// GIVEN
		insertUserMetadata(jdbcTemplate, userId, metadata());

		var filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);

		// WHEN
		var results = dao.findFiltered(filter, null, null, null);

		// THEN
		// @formatter:off
		then(results)
			.as("One result returned")
			.hasSize(1)
			.element(0)
			.extracting(UserMetadataEntity::getMeta)
			.satisfies(meta -> {
				then(meta.getInfo())
					.as("All info returned without token criteria")
					.containsOnlyKeys("building", "room");
			})
			;
		// @formatter:on
	}

}
