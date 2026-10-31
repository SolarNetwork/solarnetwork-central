/* ==================================================================
 * JdbcObjectDatumStreamAliasGrantEntityDaoTests.java - 31/10/2026 5:04:18 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Test cases for the {@link JdbcObjectDatumStreamAliasGrantEntityDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcObjectDatumStreamAliasGrantEntityDaoTests extends BaseDatumJdbcTestSupport {

	private static final Long TEST_USER_1 = -9101L;
	private static final Long TEST_USER_2 = -9102L;
	private static final Long TEST_USER_3 = -9103L;

	private JdbcObjectDatumStreamAliasGrantEntityDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate);
		for ( Long userId : List.of(TEST_USER_1, TEST_USER_2, TEST_USER_3) ) {
			setupTestUser(userId, "test%d@localhost".formatted(-userId));
		}
	}

	private ObjectDatumStreamAliasGrantEntity grant(Long userId, Long granteeId) {
		final Instant now = Instant.now().truncatedTo(MILLIS);
		final var grant = new ObjectDatumStreamAliasGrantEntity(
				UserLongCompositePK.unassignedEntityIdKey(userId), now, granteeId);
		grant.setModified(now);
		grant.setEnabled(true);
		grant.setAllowPublic(true);
		grant.setNodeIds(new Long[] { 1L, 2L });
		grant.setSourceIds(new String[] { "/a/**", "/b/*" });
		return grant;
	}

	@Test
	public void insert() {
		// GIVEN
		final ObjectDatumStreamAliasGrantEntity grant = grant(TEST_USER_1, TEST_USER_2);

		// WHEN
		final UserLongCompositePK result = dao.save(grant);

		// THEN
		// @formatter:off
		then(result)
			.as("Generated ID returned")
			.returns(TEST_USER_1, from(UserLongCompositePK::getUserId))
			.returns(true, from(UserLongCompositePK::entityIdIsAssigned))
			;
		then(dao.get(result))
			.as("Saved entity has the same properties")
			.matches(grant::isSameAs)
			.as("Created saved")
			.returns(grant.getCreated(), from(ObjectDatumStreamAliasGrantEntity::getCreated))
			.as("Modified saved")
			.returns(grant.getModified(), from(ObjectDatumStreamAliasGrantEntity::getModified))
			;
		then(jdbcTemplate.queryForObject("""
				SELECT array_length(source_regexps, 1) FROM solaruser.da_datm_alias_grant WHERE id = ?
				""", Integer.class, result.getEntityId()))
			.as("Source patterns compiled to regular expressions")
			.isEqualTo(2)
			;
		// @formatter:on
	}

	@Test
	public void insert_nullNodes() {
		// GIVEN
		final ObjectDatumStreamAliasGrantEntity grant = grant(TEST_USER_1, TEST_USER_2);
		grant.setNodeIds(null);

		// WHEN
		final UserLongCompositePK result = dao.save(grant);

		// THEN
		then(dao.get(result).getNodeIds()).as("Null node IDs saved").isNull();
	}

	@Test
	public void insert_nullSources() {
		// GIVEN
		final ObjectDatumStreamAliasGrantEntity grant = grant(TEST_USER_1, TEST_USER_2);
		grant.setSourceIds(null);

		// WHEN
		final UserLongCompositePK result = dao.save(grant);

		// THEN
		// @formatter:off
		then(dao.get(result).getSourceIds())
			.as("Null source IDs saved")
			.isNull()
			;
		then(jdbcTemplate.queryForObject(
				"SELECT source_regexps IS NULL FROM solaruser.da_datm_alias_grant WHERE id = ?",
				Boolean.class, result.getEntityId()))
			.as("No compiled regular expressions")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void update() {
		// GIVEN
		final UserLongCompositePK id = dao.save(grant(TEST_USER_1, TEST_USER_2));

		// WHEN
		final ObjectDatumStreamAliasGrantEntity update = grant(TEST_USER_1, TEST_USER_3).copyWithId(id);
		update.setEnabled(false);
		update.setAllowPublic(false);
		update.setNodeIds(new Long[] { 3L });
		update.setSourceIds(new String[] { "/c" });
		final UserLongCompositePK result = dao.save(update);

		// THEN
		// @formatter:off
		then(result)
			.as("ID preserved")
			.isEqualTo(id)
			;
		then(dao.get(id))
			.as("Saved entity has the updated properties")
			.matches(update::isSameAs)
			;
		then(jdbcTemplate.queryForObject(
				"SELECT '/c' ~ source_regexps[1] FROM solaruser.da_datm_alias_grant WHERE id = ?",
				Boolean.class, id.getEntityId()))
			.as("Regular expressions follow updated patterns")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void update_otherUser() {
		// GIVEN
		final ObjectDatumStreamAliasGrantEntity grant = grant(TEST_USER_1, TEST_USER_2);
		final UserLongCompositePK id = dao.save(grant);

		// WHEN
		final ObjectDatumStreamAliasGrantEntity update = grant(TEST_USER_3, TEST_USER_2)
				.copyWithId(new UserLongCompositePK(TEST_USER_3, id.getEntityId()));
		update.setNodeIds(new Long[] { 3L });
		dao.save(update);

		// THEN
		then(dao.get(id)).as("Grant of other user not changed").matches(grant::isSameAs);
	}

	@Test
	public void findFiltered() {
		// GIVEN
		final UserLongCompositePK id1 = dao.save(grant(TEST_USER_1, TEST_USER_2));
		final UserLongCompositePK id2 = dao.save(grant(TEST_USER_1, TEST_USER_3));
		dao.save(grant(TEST_USER_2, TEST_USER_1));

		// WHEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(TEST_USER_1);
		final var results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("Grants of user found")
			.extracting(ObjectDatumStreamAliasGrantEntity::getId)
			.containsExactly(id1, id2)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_grantee() {
		// GIVEN
		dao.save(grant(TEST_USER_1, TEST_USER_2));
		final UserLongCompositePK id2 = dao.save(grant(TEST_USER_1, TEST_USER_3));

		// WHEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(TEST_USER_1);
		filter.setGranteeId(TEST_USER_3);
		final var results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("Grants for grantee found")
			.extracting(ObjectDatumStreamAliasGrantEntity::getId)
			.containsExactly(id2)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_granteeOnly() {
		// GIVEN
		final UserLongCompositePK id1 = dao.save(grant(TEST_USER_1, TEST_USER_3));
		final UserLongCompositePK id2 = dao.save(grant(TEST_USER_2, TEST_USER_3));
		dao.save(grant(TEST_USER_1, TEST_USER_2));

		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setGranteeId(TEST_USER_3);

		// WHEN
		final var result = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(result.getResults())
			.as("Grants from every owner to grantee returned")
			.extracting(ObjectDatumStreamAliasGrantEntity::getId)
			.containsExactly(id2, id1)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_noUserOrGrantee() {
		// GIVEN
		dao.save(grant(TEST_USER_1, TEST_USER_2));

		// THEN
		// @formatter:off
		thenIllegalArgumentException()
			.as("Cannot list every grant")
			.isThrownBy(() -> dao.findFiltered(new BasicObjectDatumStreamAliasGrantFilter()))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_enabled() {
		// GIVEN
		final UserLongCompositePK id1 = dao.save(grant(TEST_USER_1, TEST_USER_2));
		final ObjectDatumStreamAliasGrantEntity disabled = grant(TEST_USER_1, TEST_USER_3);
		disabled.setEnabled(false);
		dao.save(disabled);

		// WHEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(TEST_USER_1);
		filter.setEnabled(true);
		final var results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("Enabled grants found")
			.extracting(ObjectDatumStreamAliasGrantEntity::getId)
			.containsExactly(id1)
			;
		// @formatter:on
	}

	@Test
	public void updateEnabledStatus() {
		// GIVEN
		final UserLongCompositePK id1 = dao.save(grant(TEST_USER_1, TEST_USER_2));
		final UserLongCompositePK id2 = dao.save(grant(TEST_USER_1, TEST_USER_3));

		// WHEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setGrantId(id1.getEntityId());
		final int result = dao.updateEnabledStatus(TEST_USER_1, filter, false);

		// THEN
		// @formatter:off
		then(result)
			.as("One grant updated")
			.isOne()
			;
		then(dao.get(id1))
			.as("Grant disabled")
			.returns(false, from(ObjectDatumStreamAliasGrantEntity::isEnabled))
			;
		then(dao.get(id2))
			.as("Other grant not changed")
			.returns(true, from(ObjectDatumStreamAliasGrantEntity::isEnabled))
			;
		// @formatter:on
	}

	@Test
	public void updateEnabledStatus_otherUser() {
		// GIVEN
		final UserLongCompositePK id = dao.save(grant(TEST_USER_1, TEST_USER_2));

		// WHEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setGrantId(id.getEntityId());
		final int result = dao.updateEnabledStatus(TEST_USER_3, filter, false);

		// THEN
		// @formatter:off
		then(result)
			.as("Grant of other user not matched")
			.isZero()
			;
		then(dao.get(id))
			.as("Grant not changed")
			.returns(true, from(ObjectDatumStreamAliasGrantEntity::isEnabled))
			;
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		final UserLongCompositePK id1 = dao.save(grant(TEST_USER_1, TEST_USER_2));
		final UserLongCompositePK id2 = dao.save(grant(TEST_USER_1, TEST_USER_3));

		// WHEN
		dao.delete(dao.entityKey(id1));

		// THEN
		// @formatter:off
		then(dao.get(id1))
			.as("Grant deleted")
			.isNull()
			;
		then(dao.get(id2))
			.as("Other grant not deleted")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void delete_userCascade() {
		// GIVEN
		final UserLongCompositePK id = dao.save(grant(TEST_USER_1, TEST_USER_2));

		// WHEN
		jdbcTemplate.update("DELETE FROM solaruser.user_user WHERE id = ?", TEST_USER_2);

		// THEN
		then(dao.get(id)).as("Grant deleted with grantee").isNull();
	}

}
