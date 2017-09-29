/* ==================================================================
 * HealthCheckResultTests.java - 29/09/2017 5:17:49 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.central.user.billing.killbill.domain.HealthCheckResult;

/**
 * Test cases for the {@link HealthCheckResult} class.
 * 
 * @author matt
 * @version 1.0
 */
public class HealthCheckResultTests {

	@Test
	public void equals() {
		HealthCheckResult a = new HealthCheckResult("foo", true);
		HealthCheckResult b = new HealthCheckResult("foo", true);
		assertThat(a, equalTo(b));
	}

	@Test
	public void equalsWithDifferentMessages() {
		HealthCheckResult a = new HealthCheckResult("foo", true);
		HealthCheckResult b = new HealthCheckResult("foo", true, "blah");
		assertThat(a, equalTo(b));
	}

	@Test
	public void differWithDifferentHealthy() {
		HealthCheckResult a = new HealthCheckResult("foo", true);
		HealthCheckResult b = new HealthCheckResult("foo", false);
		assertThat(a, not(equalTo(b)));
	}

	@Test
	public void differWithDifferentName() {
		HealthCheckResult a = new HealthCheckResult("foo", true);
		HealthCheckResult b = new HealthCheckResult("bar", true);
		assertThat(a, not(equalTo(b)));
	}
}
