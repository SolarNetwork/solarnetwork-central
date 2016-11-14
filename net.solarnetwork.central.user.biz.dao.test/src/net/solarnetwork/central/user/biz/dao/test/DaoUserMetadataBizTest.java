/* ==================================================================
 * DaoUserMetadataBizTest.java - 11/11/2016 5:32:34 PM
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

package net.solarnetwork.central.user.biz.dao.test;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.biz.dao.DaoUserMetadataBiz;
import net.solarnetwork.central.user.dao.UserMetadataDao;
import net.solarnetwork.central.user.domain.UserFilterCommand;
import net.solarnetwork.central.user.domain.UserMetadataEntity;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link DaoUserMetadataBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserMetadataBizTest {

	private final Long TEST_USER_ID = -11L;
	private UserMetadataDao userMetadataDao;
	private DaoUserMetadataBiz biz;

	private void replayAll() {
		replay(userMetadataDao);
	}

	private void verifyAll() {
		verify(userMetadataDao);
	}

	@Before
	public void setup() {
		userMetadataDao = EasyMock.createMock(UserMetadataDao.class);
		biz = new DaoUserMetadataBiz(userMetadataDao);
	}

	@Test
	public void addUserMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		Capture<UserMetadataEntity> metaCap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);
		EasyMock.expect(userMetadataDao.store(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		verifyAll();

		UserMetadataEntity stored = metaCap.getValue();
		assertEquals("Node", TEST_USER_ID, stored.getUserId());
		assertTrue("Tag created", stored.getMeta().hasTag("bam"));
		assertEquals("Info value", "bar", stored.getMeta().getInfoString("foo"));
	}

	@Test
	public void addUserMetadataNewWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		Capture<UserMetadataEntity> metaCap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);
		EasyMock.expect(userMetadataDao.store(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		verifyAll();

		UserMetadataEntity stored = metaCap.getValue();
		assertEquals("Node", TEST_USER_ID, stored.getUserId());
		assertTrue("Tag created", stored.getMeta().hasTag("bam"));
		assertEquals("Info value", "bar", stored.getMeta().getInfoString("foo"));
		assertEquals("Info prop value", "W", stored.getMeta().getInfoString("watts", "unit"));
	}

	@Test
	public void findUserMetadata() {
		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setUserId(TEST_USER_ID);

		EasyMock.expect(userMetadataDao.findFiltered(criteria, null, null, null)).andReturn(null);

		replayAll();
		biz.findUserMetadata(criteria, null, null, null);
		verifyAll();
	}

	@Test
	public void addUserMetadataMerge() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		final Capture<UserMetadataEntity> metaCap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);
		EasyMock.expect(userMetadataDao.store(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta.addTag("mab");

		Capture<UserMetadataEntity> meta2Cap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andAnswer(new IAnswer<UserMetadataEntity>() {

			@Override
			public UserMetadataEntity answer() throws Throwable {
				return metaCap.getValue();
			}
		});
		EasyMock.expect(userMetadataDao.store(EasyMock.capture(meta2Cap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		biz.addUserMetadata(TEST_USER_ID, meta2);
		verifyAll();

		UserMetadataEntity stored = meta2Cap.getValue();
		assertEquals("Node", TEST_USER_ID, stored.getUserId());
		assertTrue("Has original tag", stored.getMeta().hasTag("bam"));
		assertTrue("Has new tag", stored.getMeta().hasTag("mab"));
		assertEquals("Replaced info value", "bam", stored.getMeta().getInfoString("foo"));
		assertEquals("New info value", "rab", stored.getMeta().getInfoString("oof"));
	}

	@Test
	public void addUserMetadataMergeWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		final Capture<UserMetadataEntity> metaCap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);
		EasyMock.expect(userMetadataDao.store(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta2.putInfoValue("watts", "unit", "Wh"); // this should replace
		meta2.putInfoValue("watts", "unitType", "SI");
		meta2.addTag("mab");

		Capture<UserMetadataEntity> meta2Cap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andAnswer(new IAnswer<UserMetadataEntity>() {

			@Override
			public UserMetadataEntity answer() throws Throwable {
				return metaCap.getValue();
			}
		});
		EasyMock.expect(userMetadataDao.store(EasyMock.capture(meta2Cap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		biz.addUserMetadata(TEST_USER_ID, meta2);
		verifyAll();

		UserMetadataEntity stored = metaCap.getValue();
		assertEquals("Node", TEST_USER_ID, stored.getUserId());

		assertTrue("Has original tag", stored.getMeta().hasTag("bam"));
		assertTrue("Has new tag", stored.getMeta().hasTag("mab"));
		assertEquals("Replaced info value", "bam", stored.getMeta().getInfoString("foo"));
		assertEquals("New info value", "rab", stored.getMeta().getInfoString("oof"));
		assertEquals("Replaced info property value", "Wh",
				stored.getMeta().getInfoString("watts", "unit"));
		assertEquals("New info property value", "SI",
				stored.getMeta().getInfoString("watts", "unitType"));
	}

	@Test
	public void removeNode() {
		UserMetadataEntity gndm = new UserMetadataEntity();
		gndm.setUserId(TEST_USER_ID);

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(gndm);
		userMetadataDao.delete(gndm);

		replayAll();
		biz.removeUserMetadata(TEST_USER_ID);
		verifyAll();
	}

	@Test
	public void removeNodeNonExisting() {
		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);

		replayAll();
		biz.removeUserMetadata(TEST_USER_ID);
		verifyAll();
	}

}
