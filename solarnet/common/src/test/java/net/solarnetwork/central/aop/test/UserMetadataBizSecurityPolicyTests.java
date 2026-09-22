/* ==================================================================
 * UserMetadataBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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

package net.solarnetwork.central.aop.test;

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserMetadata;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.aop.UserMetadataSecurityAspect;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.biz.dao.DaoUserMetadataBiz;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.mybatis.MyBatisUserMetadataDao;
import net.solarnetwork.central.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.test.tenant.TestToken;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Verify security policies are enforced on {@link UserMetadataBiz} with the
 * {@code UserMetadataSecurityAspect} aspect applied, using the database.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class UserMetadataBizSecurityPolicyTests extends AbstractMyBatisDaoTestSupport {

	private TestTenants tenants;
	private TestTenant a;
	private UserMetadataBiz biz;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		tenants.insert(jdbcTemplate);
		for ( TestTenant t : List.of(a, tenants.b()) ) {
			insertUserMetadata(jdbcTemplate, t.userId(), metadata());
		}
		final MyBatisUserMetadataDao dao = new MyBatisUserMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		biz = securedProxy((UserMetadataBiz) new DaoUserMetadataBiz(dao),
				new UserMetadataSecurityAspect(new JdbcSolarNodeOwnershipDao(jdbcTemplate))).proxy();
	}

	private static GeneralDatumMetadata metadata() {
		final GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", 2);
		return meta;
	}

	private static BasicUserMetadataFilter users(Long... userIds) {
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserIds(userIds);
		return filter;
	}

	private static Set<Long> userIds(Iterable<UserMetadataEntity> results) {
		return StreamSupport.stream(results.spliterator(), false).map(UserMetadataEntity::getUserId)
				.collect(toSet());
	}

	@Test
	public void user_ownMetadata() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findUserMetadata(users(a.userId()), null, null, null);

		// THEN
		// @formatter:off
		then(userIds(results))
			.as("Only the user's metadata returned")
			.containsExactly(a.userId())
			;
		// @formatter:on
	}

	@Test
	public void node_ownerMetadata() {
		// GIVEN
		a.nodeActor().become();

		// WHEN
		var results = biz.findUserMetadata(users(a.userId()), null, null, null);

		// THEN
		// @formatter:off
		then(userIds(results))
			.as("Node can read its owner's metadata")
			.containsExactly(a.userId())
			;
		// @formatter:on
	}

	@Disabled("Policy user metadata paths are not applied to user metadata")
	@Test
	public void userMetadataPathsPolicy() {
		// GIVEN
		final TestToken token = a.newToken(SecurityTokenType.ReadNodeData,
				BasicSecurityPolicy.builder().withUserMetadataPaths(Set.of("/m/a")).build());
		token.insert(jdbcTemplate);
		TestActor.token("A metadata token", token).become();

		// WHEN
		var results = biz.findUserMetadata(users(a.userId()), null, null, null);

		// THEN
		// @formatter:off
		then(results)
			.as("User metadata returned")
			.hasSize(1)
			.first()
			.extracting(m -> nonnull(m.getMeta(), "Metadata").getInfo())
			.as("Only the policy metadata paths returned")
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsOnlyKeys("a")
			;
		// @formatter:on
	}

}
