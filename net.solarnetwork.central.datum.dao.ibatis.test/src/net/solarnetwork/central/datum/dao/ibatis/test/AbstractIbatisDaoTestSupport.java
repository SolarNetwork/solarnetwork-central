/* ==================================================================
 * AbstractIbatisDaoTestSupport.java - Jun 3, 2011 8:22:52 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.datum.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base test class for Ibatis DRAS DAO tests.
 * 
 * @author matt
 * @version $Revision$
 */
@ContextConfiguration
public class AbstractIbatisDaoTestSupport extends AbstractCentralTransactionalTest {
	
	public static final Long TEST_USER_ID = Long.valueOf(-9999);
	public static final String TEST_USERNAME = "unittest@localhost";
	public static final Long TEST_PRICE_SOURCE_ID = Long.valueOf(-9998);
	public static final String TEST_PRICE_SOURCE_NAME = "Test Source";
	public static final Long TEST_PRICE_LOC_ID = Long.valueOf(-9997);
	public static final String TEST_PRICE_LOC_NAME = "Test Price Location";
	
	protected void validateMembers(Set<? extends Identity<?>> src, 
			Set<? extends Identity<?>> found) {
		assertNotNull(found);
		assertEquals("number of members", src.size(), found.size());
		for ( Identity<?> member : src ) {
			assertTrue("contains member " +member.getId(), found.contains(member));
		}
	}

	@Before
	public void setupInTransaction() {	
		setupTestNode();
		setupTestUser();
		setupTestPriceLocation();
	}
	
	/**
	 * Insert a test user into the solaruser.user_user table.
	 * 
	 * <p>This will use {@link #TEST_USER_ID} and {@link #TEST_USERNAME} values.</p>
	 */
	protected void setupTestUser() {
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
	}

	/**
	 * Insert a test user into the solaruser.user_user table.
	 * 
	 * @param id the user ID
	 * @param username the user username
	 */
	protected void setupTestUser(Long id, String username) {
		simpleJdbcTemplate.update(
				"insert into solaruser.user_user (id,email,password,disp_name,enabled) values (?,?,?,?,?)", 
				id, username, DigestUtils.sha256Hex("password"), "Unit Test", Boolean.TRUE);
	}
	
	/**
	 * Insert a test price source into the solarnet.sn_price_source table.
	 * 
	 * @param id the source ID
	 * @param name the source name
	 */
	protected void setupTestPriceSource(Long id, String name) {
		simpleJdbcTemplate.update(
				"insert into solarnet.sn_price_source (id,sname) values (?,?)", 
				id, name);
	}

	/**
	 * Insert a test price location into the solarnet.sn_price_loc table.
	 * 
	 * @param id the ID
	 * @param name the name
	 * @param sourceId the price source ID
	 */
	protected void setupTestPriceLocation(Long id, String name, Long sourceId) {
		simpleJdbcTemplate.update(
				"insert into solarnet.sn_price_loc (id,loc_name,source_id,currency,unit,time_zone) values (?,?,?,?,?,?)", 
				id, name, sourceId, "NZD", "kWh", "Pacific/Auckland");
	}
	
	/**
	 * Insert a test price source and name.
	 * 
	 * <p>This will use the {@link #TEST_PRICE_SOURCE_ID}, {@link #TEST_PRICE_SOURCE_NAME},
	 * {@link #TEST_PRICE_LOC_ID}, and {@link #TEST_PRICE_LOC_NAME} values.</p>
	 * 
	 */
	protected void setupTestPriceLocation() {
		setupTestPriceSource(TEST_PRICE_SOURCE_ID, TEST_PRICE_SOURCE_NAME);
		setupTestPriceLocation(TEST_PRICE_LOC_ID, TEST_PRICE_LOC_NAME, TEST_PRICE_SOURCE_ID);
	}
	
}
