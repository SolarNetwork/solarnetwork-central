/* ==================================================================
 * MyBatisUserMetadataDao_SecurityTokenTests.java - 23/09/2026 7:05:00 pm
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

package net.solarnetwork.central.dao.mybatis.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityTokenWithPolicy;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserMetadata;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.mybatis.MyBatisUserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the security token criteria support of the
 * {@link MyBatisUserMetadataDao} class.
 *
 * <p>
 * The metadata of the results is expected to be restricted to the
 * {@code userMetadataPaths} of the token's security policy, and results whose
 * metadata is restricted to nothing omitted.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class MyBatisUserMetadataDao_SecurityTokenTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisUserMetadataDao dao;

	private Long userId;

	@BeforeEach
	public void setup() {
		dao = new MyBatisUserMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		setupTestNode();
		userId = storeNewUser("token-meta-%s@localhost".formatted(randomString()));
	}

	private GeneralDatumMetadata metadata() {
		var meta = new GeneralDatumMetadata();
		meta.setInfo(Map.of("building", "Warehouse", "room", "Office"));
		meta.setPropertyInfo(Map.of("building", Map.of("floors", 3)));
		return meta;
	}

	private String tokenWithPaths(String... paths) {
		final String tokenId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		insertSecurityTokenWithPolicy(jdbcTemplate, tokenId, randomString(), userId, "Active",
				"ReadNodeData",
				BasicSecurityPolicy.builder().withUserMetadataPaths(Set.of(paths)).build());
		return tokenId;
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
		// request pagination so the count query runs too
		var results = dao.findFiltered(filter, null, 0L, 10);

		// THEN
		// @formatter:off
		then(results)
			.as("Result omitted because its metadata is restricted to nothing")
			.isEmpty()
			;
		then(results.getTotalResults())
			.as("Count query excludes the result the same way, so the total agrees")
			.isEqualTo(0L)
			;
		// @formatter:on
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
