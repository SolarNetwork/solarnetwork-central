/* ==================================================================
 * IbatisNetworkAssociationDaoTest.java - Nov 29, 2012 11:40:20 AM
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

package net.solarnetwork.central.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.dao.NetworkAssociationDao;
import net.solarnetwork.central.dao.ibatis.IbatisNetworkAssociationDao;
import net.solarnetwork.domain.NetworkAssociation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisNetworkAssociationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisNetworkAssociationDaoTest extends AbstractIbatisDaoTestSupport {

	private static final String TEST_CONF_CODE = "test conf key";
	private static final String TEST_USERNAME = "test@localhost";
	private static final String TEST_SECURITY_PHRASE = "test phrase";

	@Autowired
	private NetworkAssociationDao networkAssociationDao;

	@Before
	public void setup() {
		executeSqlScript("net/solarnetwork/central/dao/ibatis/test/create-test-network-association.sql",
				false);
	}

	@Test
	public void queryForAssociation() {
		NetworkAssociation result = networkAssociationDao.getNetworkAssociationForConfirmationKey(
				TEST_USERNAME, TEST_CONF_CODE);
		assertNotNull("Association should not be null", result);
		assertEquals(TEST_CONF_CODE, result.getConfirmationKey());
		assertEquals(TEST_SECURITY_PHRASE, result.getSecurityPhrase());
	}

}
