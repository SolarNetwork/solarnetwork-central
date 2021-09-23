/* ==================================================================
 * FilterSupportTests.java - 8/08/2019 10:09:40 am
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

package net.solarnetwork.central.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;
import net.solarnetwork.central.support.FilterSupport;

/**
 * Test cases for the {@link FilterSupport} class.
 * 
 * @author matt
 * @version 2.0
 */
public class FilterSupportTests {

	@Test
	public void locationId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setLocationId(1L);

		// then
		assertThat("Location ID set", f.getLocationId(), equalTo(1L));
		assertThat("Location IDs set", f.getLocationIds(), arrayContaining(1L));
	}

	@Test
	public void locationIds() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setLocationIds(new Long[] { 1L, 2L });

		// then
		assertThat("Location ID set", f.getLocationId(), equalTo(1L));
		assertThat("Location IDs set", f.getLocationIds(), arrayContaining(1L, 2L));
	}

	@Test
	public void locationIdsResetByLocationId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setLocationIds(new Long[] { 1L, 2L });
		f.setLocationId(3L);

		// then
		assertThat("Location ID set", f.getLocationId(), equalTo(3L));
		assertThat("Location IDs set", f.getLocationIds(), arrayContaining(3L));
	}

	@Test
	public void nodeId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setNodeId(1L);

		// then
		assertThat("Node ID set", f.getNodeId(), equalTo(1L));
		assertThat("Node IDs set", f.getNodeIds(), arrayContaining(1L));
	}

	@Test
	public void nodeIds() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setNodeIds(new Long[] { 1L, 2L });

		// then
		assertThat("Node ID set", f.getNodeId(), equalTo(1L));
		assertThat("Node IDs set", f.getNodeIds(), arrayContaining(1L, 2L));
	}

	@Test
	public void nodeIdsResetByNodeId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setNodeIds(new Long[] { 1L, 2L });
		f.setNodeId(3L);

		// then
		assertThat("Node ID set", f.getNodeId(), equalTo(3L));
		assertThat("Node IDs set", f.getNodeIds(), arrayContaining(3L));
	}

	@Test
	public void userId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setUserId(1L);

		// then
		assertThat("User ID set", f.getUserId(), equalTo(1L));
		assertThat("User IDs set", f.getUserIds(), arrayContaining(1L));
	}

	@Test
	public void userIds() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setUserIds(new Long[] { 1L, 2L });

		// then
		assertThat("User ID set", f.getUserId(), equalTo(1L));
		assertThat("User IDs set", f.getUserIds(), arrayContaining(1L, 2L));
	}

	@Test
	public void userIdsResetByUserId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setUserIds(new Long[] { 1L, 2L });
		f.setUserId(3L);

		// then
		assertThat("User ID set", f.getUserId(), equalTo(3L));
		assertThat("User IDs set", f.getUserIds(), arrayContaining(3L));
	}

	@Test
	public void sourceId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setSourceId("foo");

		// then
		assertThat("Source ID set", f.getSourceId(), equalTo("foo"));
		assertThat("Source IDs set", f.getSourceIds(), arrayContaining("foo"));
	}

	@Test
	public void sourceIds() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setSourceIds(new String[] { "foo", "bar" });

		// then
		assertThat("Source ID set", f.getSourceId(), equalTo("foo"));
		assertThat("Source IDs set", f.getSourceIds(), arrayContaining("foo", "bar"));
	}

	@Test
	public void sourceIdsResetBySourceId() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setSourceIds(new String[] { "foo", "bar" });
		f.setSourceId("baz");

		// then
		assertThat("Source ID set", f.getSourceId(), equalTo("baz"));
		assertThat("Source IDs set", f.getSourceIds(), arrayContaining("baz"));
	}

	@Test
	public void tag() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setTag("foo");

		// then
		assertThat("Tag set", f.getTag(), equalTo("foo"));
		assertThat("Tags set", f.getTags(), arrayContaining("foo"));
	}

	@Test
	public void tags() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setTags(new String[] { "foo", "bar" });

		// then
		assertThat("Tag set", f.getTag(), equalTo("foo"));
		assertThat("Tags set", f.getTags(), arrayContaining("foo", "bar"));
	}

	@Test
	public void tagsResetByTag() {
		// given
		FilterSupport f = new FilterSupport();

		// when
		f.setTags(new String[] { "foo", "bar" });
		f.setTag("baz");

		// then
		assertThat("Source ID set", f.getTag(), equalTo("baz"));
		assertThat("Source IDs set", f.getTags(), arrayContaining("baz"));
	}

}
