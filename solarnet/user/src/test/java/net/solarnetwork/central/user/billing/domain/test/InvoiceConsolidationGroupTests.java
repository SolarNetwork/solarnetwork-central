/* ==================================================================
 * InvoiceConsolidationGroupTests.java - 15/10/2026 5:12:08 pm
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

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;

/**
 * Test cases for the {@link InvoiceConsolidationGroup} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class InvoiceConsolidationGroupTests {

	@Test
	public void valid() {
		// WHEN
		InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(1L, List.of(1L, 3L, 2L), true);

		// THEN
		// @formatter:off
		and.then(group)
			.as("Members are the users other than the primary, in order")
			.returns(List.of(3L, 2L), from(InvoiceConsolidationGroup::memberUserIds))
			.as("Group without a problem is valid")
			.returns(true, from(InvoiceConsolidationGroup::isValid))
			;
		and.then(group.problem())
			.as("No problem")
			.isNull()
			;
		and.then(group.isPrimary(1L))
			.as("Primary user")
			.isTrue()
			;
		and.then(group.isPrimary(2L))
			.as("Member user not primary")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void invalid() {
		// WHEN
		InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
				new DefaultMessageSourceResolvable("error.consolidated.noBillingAccount"));

		// THEN
		// @formatter:off
		and.then(group)
			.as("Group with a problem is invalid")
			.returns(false, from(InvoiceConsolidationGroup::isValid))
			;
		// @formatter:on
	}

	@Test
	public void userIdsCopied() {
		// GIVEN
		List<Long> userIds = new ArrayList<>(List.of(1L, 2L));

		// WHEN
		InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(1L, userIds, false);
		userIds.add(3L);

		// THEN
		// @formatter:off
		and.then(group.userIds())
			.as("User IDs copied")
			.containsExactly(1L, 2L)
			;
		and.thenThrownBy(() -> group.userIds().add(4L))
			.as("User IDs cannot be changed")
			.isInstanceOf(UnsupportedOperationException.class)
			;
		// @formatter:on
	}

	@Test
	public void userIdsNotStartingWithPrimary() {
		// @formatter:off
		and.thenThrownBy(() -> new InvoiceConsolidationGroup(1L, List.of(2L, 1L), false))
			.as("User IDs must start with the primary")
			.isInstanceOf(IllegalArgumentException.class)
			;
		and.thenThrownBy(() -> new InvoiceConsolidationGroup(1L, List.of(), false))
			.as("User IDs must include the primary")
			.isInstanceOf(IllegalArgumentException.class)
			;
		// @formatter:on
	}

}
