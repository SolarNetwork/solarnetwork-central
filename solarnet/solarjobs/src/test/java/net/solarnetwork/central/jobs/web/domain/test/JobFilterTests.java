/* ==================================================================
 * JobFilterTests.java - 25/01/2023 7:55:59 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.jobs.web.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.jobs.web.domain.JobFilter;

/**
 * Test cases for the {@link JobFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JobFilterTests {

	@Test
	public void accessors() {
		// GIVEN
		final String groupId = UUID.randomUUID().toString();
		final String id = UUID.randomUUID().toString();

		// WHEN
		JobFilter f = new JobFilter();
		f.setExecuting(true);
		f.setGroupId(groupId);
		f.setId(id);

		// THEN
		assertThat("Executing configured", f.getExecuting(), is(equalTo(Boolean.TRUE)));
		assertThat("Group ID configured", f.getGroupId(), is(equalTo(groupId)));
		assertThat("ID configured", f.getId(), is(equalTo(id)));
	}

}
