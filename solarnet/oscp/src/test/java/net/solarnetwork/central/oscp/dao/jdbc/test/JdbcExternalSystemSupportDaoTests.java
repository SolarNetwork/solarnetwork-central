/* ==================================================================
 * JdbcExternalSystemSupportDaoTests.java - 21/08/2022 6:12:28 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcExternalSystemSupportDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcExternalSystemSupportDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcExternalSystemSupportDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	@Autowired
	private PlatformTransactionManager txManager;

	private JdbcFlexibilityProviderDao flexibilityProviderDao;
	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityOptimizerConfigurationDao capacityOptimizerDao;
	private JdbcExternalSystemSupportDao dao;
	private Long userId;
	private Long flexibilityProviderId;

	private CapacityProviderConfiguration lastProvider;

	@BeforeEach
	public void setup() {
		flexibilityProviderDao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityOptimizerDao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		dao = new JdbcExternalSystemSupportDao(capacityProviderDao, capacityOptimizerDao);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		flexibilityProviderId = flexibilityProviderDao
				.idForToken(flexibilityProviderDao.createAuthToken(unassignedEntityIdKey(userId)), false)
				.getEntityId();
	}

	// TODO

}
