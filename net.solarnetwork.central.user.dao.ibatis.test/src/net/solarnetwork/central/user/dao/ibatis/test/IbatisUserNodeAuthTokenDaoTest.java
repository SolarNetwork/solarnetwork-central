/* ==================================================================
 * IbatisUserNodeAuthTokenDaoTest.java - Dec 12, 2012 4:41:05 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.List;
import net.solarnetwork.central.dao.ibatis.IbatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.ibatis.IbatisUserNodeAuthTokenDao;
import net.solarnetwork.central.user.dao.ibatis.IbatisUserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeAuthToken;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test cases for {@link IbatisUserNodeAuthTokenDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisUserNodeAuthTokenDaoTest extends AbstractIbatisUserDaoTestSupport {

	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_auth_token",
			"solaruser.user_node", "solaruser.user_user" };

	private static final Long TEST_NODE2_ID = TEST_NODE_ID - 1L;
	private static final String TEST_TOKEN = "public.token12345678";
	private static final String TEST_SECRET = "secret.token12345678";
	private static final String TEST_TOKEN2 = "public.token12345679";
	private static final String TEST_TOKEN3 = "public.token12345677";

	@Autowired
	private IbatisSolarNodeDao solarNodeDao;

	@Autowired
	private IbatisUserNodeDao userNodeDao;

	@Autowired
	private IbatisUserNodeAuthTokenDao userNodeAuthTokenDao;

	private User user = null;
	private SolarNode node = null;
	private UserNodeAuthToken userNodeAuthToken = null;

	@Before
	public void setUp() throws Exception {
		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);

		UserNode un = new UserNode();
		un.setUser(this.user);
		un.setNode(this.node);
		userNodeDao.store(un);

		assertNotNull(this.user);
		userNodeAuthToken = null;
	}

	@Test
	public void storeNew() {
		UserNodeAuthToken authToken = new UserNodeAuthToken();
		authToken.setCreated(new DateTime());
		authToken.setNodeId(this.node.getId());
		authToken.setAuthSecret(TEST_SECRET);
		authToken.setAuthToken(TEST_TOKEN);
		authToken.setStatus(UserAuthTokenStatus.v);
		String id = userNodeAuthTokenDao.store(authToken);
		assertNotNull(id);
		this.userNodeAuthToken = authToken;
	}

	private void validate(UserNodeAuthToken token, UserNodeAuthToken entity) {
		assertNotNull("UserNodeAuthToken should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(token.getId(), entity.getId());
		assertEquals(token.getStatus(), entity.getStatus());
		assertEquals(token.getAuthToken(), entity.getAuthToken());
		assertEquals(token.getAuthSecret(), entity.getAuthSecret());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserNodeAuthToken token = userNodeAuthTokenDao.get(userNodeAuthToken.getId());
		validate(this.userNodeAuthToken, token);
	}

	@Test
	public void update() {
		storeNew();
		UserNodeAuthToken token = userNodeAuthTokenDao.get(userNodeAuthToken.getId());
		token.setStatus(UserAuthTokenStatus.z);
		UserNodeAuthToken updated = userNodeAuthTokenDao.get(userNodeAuthTokenDao.store(token));
		validate(token, updated);
	}

	@Test
	public void delete() {
		storeNew();
		UserNodeAuthToken token = userNodeAuthTokenDao.get(userNodeAuthToken.getId());
		userNodeAuthTokenDao.delete(token);
		token = userNodeAuthTokenDao.get(token.getId());
		assertNull(token);
	}

	@Test
	public void findForNode() {
		storeNew();
		UserNodeAuthToken authToken2 = new UserNodeAuthToken(TEST_TOKEN2, this.node.getId(), TEST_SECRET);
		userNodeAuthTokenDao.store(authToken2);

		setupTestNode(TEST_NODE2_ID);
		SolarNode node2 = solarNodeDao.get(TEST_NODE2_ID);
		UserNode userNode2 = new UserNode(this.user, node2);
		userNode2 = userNodeDao.get(userNodeDao.store(userNode2));

		UserNodeAuthToken authToken3 = new UserNodeAuthToken(TEST_TOKEN3, node2.getId(), TEST_SECRET);
		userNodeAuthTokenDao.store(authToken3);

		List<UserNodeAuthToken> results = userNodeAuthTokenDao.findUserNodeAuthTokensForNode(this.node
				.getId());
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(this.userNodeAuthToken, results.get(0));
		assertEquals(authToken2, results.get(1));

		results = userNodeAuthTokenDao.findUserNodeAuthTokensForNode(node2.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(authToken3, results.get(0));
	}

}
