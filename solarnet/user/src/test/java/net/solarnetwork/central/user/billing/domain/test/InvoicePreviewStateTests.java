/* ==================================================================
 * InvoicePreviewStateTests.java - 22/10/2026 3:41:09 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.domain.test;

import static net.solarnetwork.central.user.billing.domain.InvoicePreviewState.Done;
import static net.solarnetwork.central.user.billing.domain.InvoicePreviewState.Failed;
import static net.solarnetwork.central.user.billing.domain.InvoicePreviewState.Running;
import static net.solarnetwork.central.user.billing.domain.InvoicePreviewState.Waiting;
import static net.solarnetwork.central.user.billing.domain.InvoicePreviewState.combined;
import static org.assertj.core.api.BDDAssertions.and;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewState;

/**
 * Test cases for the {@link InvoicePreviewState} enum.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class InvoicePreviewStateTests {

	@Test
	public void combined_empty() {
		and.then(combined(List.of())).as("No states is done").isEqualTo(Done);
	}

	@Test
	public void combined_allDone() {
		and.then(combined(List.of(Done, Done))).as("All done is done").isEqualTo(Done);
	}

	@Test
	public void combined_failed() {
		and.then(combined(List.of(Done, Running, Failed, Waiting))).as("Any failed is failed")
				.isEqualTo(Failed);
	}

	@Test
	public void combined_running() {
		and.then(combined(List.of(Done, Waiting, Running))).as("Any running is running")
				.isEqualTo(Running);
	}

	@Test
	public void combined_waiting() {
		and.then(combined(List.of(Done, Waiting))).as("Waiting without running is waiting")
				.isEqualTo(Waiting);
	}

}
