/* ==================================================================
 * GeneralNodeDatumPKTests.java - 1/02/2019 4:54:16 pm
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
import static org.hamcrest.Matchers.not;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;

/**
 * Test cases for the {@link GeneralNodeDatumPK} class.
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralNodeDatumPKTests {

	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2014, 8, 22, 12, 1, 2,
			(int) TimeUnit.MILLISECONDS.toNanos(345));
	private static final Instant TEST_TIMESTAMP = TEST_DATE.toInstant(ZoneOffset.UTC);
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_SOURCE_ID_2 = "test.source.2";

	@Test
	public void stringValue() {
		GeneralNodeDatumPK pk = new GeneralNodeDatumPK(TEST_NODE_ID, TEST_TIMESTAMP, TEST_SOURCE_ID);
		assertThat("String value", pk.toString(), equalTo("GeneralNodeDatumPK{nodeId=" + TEST_NODE_ID
				+ ", sourceId=" + TEST_SOURCE_ID + ", created=" + TEST_TIMESTAMP + "}"));
	}

	@Test
	public void equalsEqual() {
		GeneralNodeDatumPK pk1 = new GeneralNodeDatumPK(TEST_NODE_ID, TEST_TIMESTAMP, TEST_SOURCE_ID);
		GeneralNodeDatumPK pk2 = new GeneralNodeDatumPK(TEST_NODE_ID, TEST_TIMESTAMP, TEST_SOURCE_ID);
		assertThat("Keys equal", pk1, equalTo(pk2));
	}

	@Test
	public void equalsDifferent() {
		GeneralNodeDatumPK pk1 = new GeneralNodeDatumPK(TEST_NODE_ID, TEST_TIMESTAMP, TEST_SOURCE_ID);
		GeneralNodeDatumPK pk2 = new GeneralNodeDatumPK(TEST_NODE_ID, TEST_TIMESTAMP, TEST_SOURCE_ID_2);
		assertThat("Keys not equal", pk1, not(equalTo(pk2)));
	}
}
