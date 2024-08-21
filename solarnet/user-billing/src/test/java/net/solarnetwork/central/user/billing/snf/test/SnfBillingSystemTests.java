/* ==================================================================
 * SnfBillingSystemTests.java - 16/08/2024 2:54:57 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.user.billing.domain.NamedCost;
import net.solarnetwork.central.user.billing.domain.NamedCostTiers;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.UsageTier;
import net.solarnetwork.central.user.billing.snf.domain.UsageTiers;
import net.solarnetwork.central.user.billing.support.LocalizedNamedCost;

/**
 * Test cases for the {@link SnfBillingSystem}.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SnfBillingSystemTests {

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Mock
	private AccountDao accountDao;

	@Mock
	private SnfInvoiceDao invoiceDao;

	@Mock
	private NodeUsageDao usageDao;

	private SnfBillingSystem system;

	@BeforeEach
	public void setup() {
		system = new SnfBillingSystem(invoicingSystem, accountDao, invoiceDao, usageDao);
	}

	@Test
	public void namedCosts() {
		// GIVEN
		final UsageTiers rates1 = new UsageTiers(asList(new UsageTier("s1", 0, new BigDecimal("1")),
				new UsageTier("s1", 10, new BigDecimal("10"))), LocalDate.of(2010, 1, 1));
		final UsageTiers rates2 = new UsageTiers(asList(new UsageTier("s1", 0, new BigDecimal("2")),
				new UsageTier("s1", 10, new BigDecimal("20"))), LocalDate.of(2011, 1, 1));
		final UsageTiers rates3 = new UsageTiers(asList(new UsageTier("s1", 0, new BigDecimal("3")),
				new UsageTier("s1", 10, new BigDecimal("30"))), LocalDate.of(2012, 1, 1));
		final List<UsageTiers> allRates = Arrays.asList(rates1, rates2, rates3);
		given(usageDao.effectiveUsageTiers()).willReturn(allRates);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasename(getClass().getName());

		for ( UsageTiers rates : allRates ) {
			given(invoicingSystem
					.messageSourceForDate(rates.getDate().atStartOfDay(ZoneOffset.UTC).toInstant()))
							.willReturn(msg);
		}

		// WHEN
		var result = system.namedCostTiers(Locale.US);

		// THEN
		then(result).as("Same number of DAO results returned").hasSize(allRates.size());
		for ( int i = 0; i < allRates.size(); i++ ) {
			NamedCostTiers actualRates = result.get(i);
			UsageTiers expectedRates = allRates.get(i);
			// @formatter:off
			then(actualRates)
				.as("Result %d has same date as DAO result".formatted(i))
				.returns(expectedRates.getDate(), from(NamedCostTiers::getDate))
				;
			then(actualRates.getTiers())
				.as("Result %d has same number of tiers as DAO result".formatted(i))
				.hasSize(expectedRates.getTiers().size())
				;
			// @formatter:on
			int j = 0;
			for ( NamedCost cost : actualRates.getTiers() ) {
				// @formatter:off
				then(cost)
					.as("Result %d cost %d has been localized".formatted(i, j))
					.asInstanceOf(InstanceOfAssertFactories.type(LocalizedNamedCost.class))
					.as("Result %d cost %d proxies DAO cost name".formatted(i, j))
					.returns(expectedRates.getTiers().get(j).getName(), from(LocalizedNamedCost::getName))
					.as("Result %d cost %d proxies DAO cost quantity".formatted(i, j))
					.returns(expectedRates.getTiers().get(j).getQuantity(), from(LocalizedNamedCost::getQuantity))
					.as("Result %d cost %d proxies DAO cost cost".formatted(i, j))
					.returns(expectedRates.getTiers().get(j).getCost(), from(LocalizedNamedCost::getCost))
					.as("Result %d cost %d has no effective rate".formatted(i, j))
					.returns(null, from(LocalizedNamedCost::getEffectiveRate))
					.as("Result %d cost %d has localized name from MessageSource".formatted(i, j))
					.returns("Service 1", from(LocalizedNamedCost::getLocalizedDescription))
					.as("Result %d cost %d has no localized quantity".formatted(i, j))
					.returns(null, from(LocalizedNamedCost::getLocalizedQuantity))
					.as("Result %d cost %d has no localized cost".formatted(i, j))
					.returns(null, from(LocalizedNamedCost::getLocalizedCost))
					.as("Result %d cost %d has no localized effective rate".formatted(i, j))
					.returns(null, from(LocalizedNamedCost::getLocalizedEffectiveRate))
					;
				// @formatter:on
				j++;
			}
		}
	}

}
