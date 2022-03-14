/* ==================================================================
 * GeneralNodeDatumKindPKTests.java - 11/04/2019 9:17:46 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumKindPK;

/**
 * Test cases for the {@link GeneralNodeDatumKindPK} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumKindPKTests {

	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2014, 8, 22, 12, 1, 2,
			(int) TimeUnit.MILLISECONDS.toNanos(345));
	private static final Instant TEST_TIMESTAMP = TEST_DATE.toInstant(ZoneOffset.UTC);
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_KIND = "test.kind";

	@Test
	public void stringValue() {
		GeneralNodeDatumKindPK pk = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		assertThat("String value", pk.toString(),
				equalTo("GeneralNodeDatumKindPK{nodeId=" + TEST_NODE_ID + ", sourceId=" + TEST_SOURCE_ID
						+ ", created=" + TEST_TIMESTAMP + ", kind=" + TEST_KIND + "}"));
	}

	@Test
	public void idValue() {
		GeneralNodeDatumKindPK pk = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		assertThat("ID value", pk.getId(), equalTo(DigestUtils.sha1Hex("n=" + TEST_NODE_ID + ";s="
				+ TEST_SOURCE_ID + ";c=" + TEST_TIMESTAMP + ";k=" + TEST_KIND)));
	}

	@Test
	public void equalsEqual() {
		GeneralNodeDatumKindPK pk1 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		GeneralNodeDatumKindPK pk2 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		assertThat("Keys equal", pk1, equalTo(pk2));
	}

	@Test
	public void equalsDifferent() {
		GeneralNodeDatumKindPK pk1 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		GeneralNodeDatumKindPK pk2 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, null);
		assertThat("Keys not equal", pk1, not(equalTo(pk2)));
	}

	@Test
	public void compareKindsDescending() {
		GeneralNodeDatumKindPK pk1 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		GeneralNodeDatumKindPK pk2 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, "a.kind");
		assertThat("Comparison", pk1.compareTo(pk2), greaterThanOrEqualTo(1));
	}

	@Test
	public void compareKindsAscending() {
		GeneralNodeDatumKindPK pk1 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		GeneralNodeDatumKindPK pk2 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, "z.kind");
		assertThat("Comparison", pk1.compareTo(pk2), lessThanOrEqualTo(-1));
	}

	@Test
	public void compareKindsEqual() {
		GeneralNodeDatumKindPK pk1 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		GeneralNodeDatumKindPK pk2 = new GeneralNodeDatumKindPK(TEST_NODE_ID, TEST_TIMESTAMP,
				TEST_SOURCE_ID, TEST_KIND);
		assertThat("Comparison", pk1.compareTo(pk2), equalTo(0));
	}

}
