/* ==================================================================
 * ConsolidatedInvoiceDelivererTests.java - 15/10/2026 3:31:40 pm
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

package net.solarnetwork.central.user.billing.snf.jobs.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.jobs.ConsolidatedInvoiceDeliverer;

/**
 * Test cases for the {@link ConsolidatedInvoiceDeliverer} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ConsolidatedInvoiceDelivererTests {

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	private ConsolidatedInvoiceDeliverer deliverer;

	@BeforeEach
	public void setup() {
		deliverer = new ConsolidatedInvoiceDeliverer(invoicingSystem);
	}

	@Test
	public void deliver() {
		// GIVEN
		final UserLongCompositePK id = new UserLongCompositePK(1L, 2010L);
		given(invoicingSystem.deliverConsolidatedInvoice(id)).willReturn(true);

		// WHEN
		boolean result = deliverer.handleTask(AccountTask.newTask(Instant.now(),
				AccountTaskType.DeliverConsolidatedInvoice, 10L, Map.of("userId", 1, "id", "2010")));

		// THEN
		// @formatter:off
		and.then(result).as("Delivery result returned").isTrue();
		// @formatter:on
	}

	@Test
	public void deliver_failed() {
		// GIVEN
		final UserLongCompositePK id = new UserLongCompositePK(1L, 2010L);
		given(invoicingSystem.deliverConsolidatedInvoice(id)).willReturn(false);

		// WHEN
		boolean result = deliverer.handleTask(AccountTask.newTask(Instant.now(),
				AccountTaskType.DeliverConsolidatedInvoice, 10L, Map.of("userId", 1L, "id", 2010L)));

		// THEN
		// @formatter:off
		and.then(result).as("Delivery result returned, so the task is tried again").isFalse();
		// @formatter:on
	}

	@Test
	public void deliver_missingInvoiceId() {
		// WHEN
		boolean result = deliverer.handleTask(AccountTask.newTask(Instant.now(),
				AccountTaskType.DeliverConsolidatedInvoice, 10L, Map.of("userId", 1L)));

		// THEN
		// @formatter:off
		and.then(result).as("Invalid task handled, so it is removed").isTrue();
		// @formatter:on
		then(invoicingSystem).shouldHaveNoInteractions();
	}

	@Test
	public void deliver_invalidUserId() {
		// WHEN
		boolean result = deliverer.handleTask(AccountTask.newTask(Instant.now(),
				AccountTaskType.DeliverConsolidatedInvoice, 10L, Map.of("userId", "abc", "id", 2010L)));

		// THEN
		// @formatter:off
		and.then(result).as("Invalid task handled, so it is removed").isTrue();
		// @formatter:on
		then(invoicingSystem).shouldHaveNoInteractions();
	}

}
