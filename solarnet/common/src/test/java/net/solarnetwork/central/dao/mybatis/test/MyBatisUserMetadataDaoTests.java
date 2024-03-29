/* ==================================================================
 * MyBatisUserMetadataDaoTests.java - 11/11/2016 5:50:19 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisUserMetadataDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilterMatch;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link MyBatisUserMetadataDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserMetadataDaoTests extends AbstractMyBatisDaoTestSupport {

	public static final String TEST_EMAIL = "foo@localhost.localdomain";

	private MyBatisUserMetadataDao dao;

	private Long testUserId;
	private UserMetadataEntity lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisUserMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		setupTestNode();
		testUserId = storeNewUser(TEST_EMAIL);
	}

	private UserMetadataEntity getTestInstance() {
		UserMetadataEntity datum = new UserMetadataEntity();
		datum.setCreated(Instant.now());
		datum.setUserId(testUserId);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		UserMetadataEntity datum = getTestInstance();
		Long id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(UserMetadataEntity src, UserMetadataEntity entity) {
		assertNotNull("GeneralNodeDatum should exist", entity);
		assertEquals(src.getUserId(), entity.getUserId());
		assertEquals(src.getCreated(), entity.getCreated());
		assertEquals(src.getMeta(), entity.getMeta());
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
		dao.store(datum);

		UserMetadataEntity entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void findFiltered() {
		storeNew();

		Long userId2 = storeNewUser("bar@example.com");
		UserMetadataEntity user2Meta = getTestInstance();
		user2Meta.setUserId(userId2);
		dao.store(user2Meta);

		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setUserId(testUserId);

		FilterResults<UserMetadataFilterMatch> results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		UserMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Match ID", testUserId, match.getId());
	}

}
