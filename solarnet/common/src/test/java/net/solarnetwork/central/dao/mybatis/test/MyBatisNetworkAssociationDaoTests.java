/* ==================================================================
 * MyBatisNetworkAssociationDao.java - Nov 10, 2014 1:06:45 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisNetworkAssociationDao;
import net.solarnetwork.domain.NetworkAssociation;

/**
 * Test cases for the {@link MyBatisNetworkAssociationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisNetworkAssociationDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisNetworkAssociationDao dao;

	private static final String TEST_CONF_CODE = "test conf key";
	private static final String TEST_USERNAME = "test@localhost";
	private static final String TEST_SECURITY_PHRASE = "test phrase";

	@BeforeEach
	public void setup() {
		dao = new MyBatisNetworkAssociationDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		executeSqlScript("net/solarnetwork/central/dao/mybatis/test/create-test-network-association.sql",
				false);
	}

	@Test
	public void queryForAssociation() {
		NetworkAssociation result = dao.getNetworkAssociationForConfirmationKey(TEST_USERNAME,
				TEST_CONF_CODE);
		then(result).as("Association should not be null").isNotNull()
				.returns(TEST_CONF_CODE, from(NetworkAssociation::getConfirmationKey))
				.returns(TEST_SECURITY_PHRASE, from(NetworkAssociation::getSecurityPhrase));
	}
}
