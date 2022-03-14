/* ==================================================================
 * BootstrapTransactionalTest.java - Jan 11, 2010 10:15:47 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

import static org.junit.Assert.assertNotNull;
import java.time.Instant;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataAccessException;

/**
 * Test case to validate unit test can connect to transactional datastore.
 * 
 * @author matt
 * @version 2.0
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BootstrapTransactionalTest extends AbstractCentralTransactionalTest {

	/**
	 * Test able to connect to the configured database.
	 */
	@Test
	public void testConnectToDatabase() throws DataAccessException {
		Instant now = jdbcTemplate.queryForObject("select CURRENT_TIMESTAMP", Instant.class,
				(Object[]) null);
		assertNotNull(now);
		log.debug("Got timestamp: " + now);
	}

}
