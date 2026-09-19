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

package net.solarnetwork.central.biz.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.time.Instant;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.biz.dao.DaoUserMetadataBiz;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link DaoUserMetadataBiz} class.
 * 
 * @author matt
 * @version 2.1
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

	@BeforeEach
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
		EasyMock.expect(userMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		verifyAll();

		UserMetadataEntity stored = metaCap.getValue();
		then(stored.getUserId()).as("User").isEqualTo(TEST_USER_ID);
		then(stored.getMeta().hasTag("bam")).as("Tag craeted").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Info value").isEqualTo("bar");
	}

	@Test
	public void addUserMetadataNewWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		Capture<UserMetadataEntity> metaCap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);
		EasyMock.expect(userMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		verifyAll();

		UserMetadataEntity stored = metaCap.getValue();
		then(stored.getUserId()).as("User").isEqualTo(TEST_USER_ID);
		then(stored.getMeta().hasTag("bam")).as("Tag craeted").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Info value").isEqualTo("bar");
		then(stored.getMeta().getInfoString("watts", "unit")).as("Info prop avlue").isEqualTo("W");
	}

	@Test
	public void findUserMetadata() {
		BasicUserMetadataFilter criteria = new BasicUserMetadataFilter();
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
		EasyMock.expect(userMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

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
		EasyMock.expect(userMetadataDao.save(EasyMock.capture(meta2Cap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		biz.addUserMetadata(TEST_USER_ID, meta2);
		verifyAll();

		UserMetadataEntity stored = meta2Cap.getValue();
		then(stored.getUserId()).as("User").isEqualTo(TEST_USER_ID);
		then(stored.getMeta().hasTag("bam")).as("Has original tag").isTrue();
		then(stored.getMeta().hasTag("mab")).as("Has new tag").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Replaced info value").isEqualTo("bam");
		then(stored.getMeta().getInfoString("oof")).as("New info value").isEqualTo("rab");
	}

	@Test
	public void addUserMetadataMergeWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		final Capture<UserMetadataEntity> metaCap = new Capture<UserMetadataEntity>();

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);
		EasyMock.expect(userMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_USER_ID);

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
		EasyMock.expect(userMetadataDao.save(EasyMock.capture(meta2Cap))).andReturn(TEST_USER_ID);

		replayAll();
		biz.addUserMetadata(TEST_USER_ID, meta);
		biz.addUserMetadata(TEST_USER_ID, meta2);
		verifyAll();

		UserMetadataEntity stored = metaCap.getValue();
		then(stored.getUserId()).as("User").isEqualTo(TEST_USER_ID);

		then(stored.getMeta().hasTag("bam")).as("Has original tag").isTrue();
		then(stored.getMeta().hasTag("mab")).as("Has new tag").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Replaced info value").isEqualTo("bam");
		then(stored.getMeta().getInfoString("oof")).as("New info value").isEqualTo("rab");
		then(stored.getMeta().getInfoString("watts", "unit")).as("Replaced info property value")
				.isEqualTo("Wh");
		then(stored.getMeta().getInfoString("watts", "unitType")).as("New info property value")
				.isEqualTo("SI");
	}

	@Test
	public void remove() {
		UserMetadataEntity gndm = new UserMetadataEntity(TEST_USER_ID, Instant.now());

		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(gndm);
		userMetadataDao.delete(gndm);

		replayAll();
		biz.removeUserMetadata(TEST_USER_ID);
		verifyAll();
	}

	@Test
	public void removeNonExisting() {
		EasyMock.expect(userMetadataDao.get(TEST_USER_ID)).andReturn(null);

		replayAll();
		biz.removeUserMetadata(TEST_USER_ID);
		verifyAll();
	}

}
