/* ==================================================================
 * DefaultSnfInvoicingSystem_ConsolidatedTests.java - 15/10/2026 11:25:19 am
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

package net.solarnetwork.central.user.billing.snf.test;

import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.defaultOptions;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.dryRunOptions;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceDeliverer;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfConsolidatedInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link DefaultSnfInvoicingSystem} consolidated invoice
 * support.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DefaultSnfInvoicingSystem_ConsolidatedTests {

	private static final YearMonth MONTH = YearMonth.of(2026, 9);
	private static final LocalDate START_DATE = MONTH.atDay(1);
	private static final LocalDate END_DATE = MONTH.plusMonths(1).atDay(1);

	@Mock
	private AccountDao accountDao;

	@Mock
	private SnfInvoiceDao invoiceDao;

	@Mock
	private SnfInvoiceItemDao invoiceItemDao;

	@Mock
	private SnfInvoiceNodeUsageDao invoiceNodeUsageDao;

	@Mock
	private NodeUsageDao usageDao;

	@Mock
	private TaxCodeDao taxCodeDao;

	@Mock
	private VersionedMessageDao messageDao;

	@Mock
	private SnfConsolidatedInvoiceDao consolidatedInvoiceDao;

	@Mock
	private SnfInvoiceDeliverer deliverer;

	@Captor
	private ArgumentCaptor<SnfConsolidatedInvoice> invoiceCaptor;

	@Captor
	private ArgumentCaptor<SnfConsolidatedInvoiceFilter> filterCaptor;

	private DefaultSnfInvoicingSystem system;

	private Account primary;
	private Account member;
	private InvoiceConsolidationGroup group;

	@BeforeEach
	public void setup() {
		system = spy(new DefaultSnfInvoicingSystem(accountDao, invoiceDao, invoiceItemDao,
				invoiceNodeUsageDao, usageDao, taxCodeDao, messageDao));
		system.setConsolidatedInvoiceDao(consolidatedInvoiceDao);

		primary = account(1L, "Pacific/Auckland");
		member = account(2L, "America/Los_Angeles");
		group = new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false);
	}

	private static Account account(Long userId, String timeZoneId) {
		Address address = new Address(userId, "User " + userId, "user@localhost", "NZ", timeZoneId);
		Account account = new Account(userId * 10, userId, Instant.now(), "NZD", "en-NZ");
		account.setAddress(address);
		return account;
	}

	private static SnfInvoice invoice(Account account, Long invoiceId) {
		SnfInvoice invoice = new SnfInvoice(invoiceId, account.getUserId(), account.getAccountId(),
				Instant.now(), START_DATE, END_DATE, account.getCurrencyCode());
		invoice.setAddress(account.getAddress());
		return invoice;
	}

	@Test
	public void generate() {
		// GIVEN
		given(accountDao.getForUser(1L, END_DATE)).willReturn(primary);
		given(accountDao.getForUser(2L, END_DATE)).willReturn(member);
		doReturn(null).when(system).findInvoiceForPeriod(any(), any(), any());

		final SnfInvoice primaryInvoice = invoice(primary, 100L);
		final SnfInvoice memberInvoice = invoice(member, 101L);
		doReturn(primaryInvoice).when(system).generateInvoice(1L, START_DATE, END_DATE,
				defaultOptions());
		doReturn(memberInvoice).when(system).generateInvoice(2L, START_DATE, END_DATE, defaultOptions());

		final UserLongCompositePK id = new UserLongCompositePK(1L, 200L);
		given(consolidatedInvoiceDao.save(any())).willReturn(id);

		// WHEN
		SnfConsolidatedInvoice result = system.generateConsolidatedInvoice(group, MONTH,
				defaultOptions());

		// THEN
		then(accountDao).should().lockAccount(primary.getAccountId());
		then(system).should().findInvoiceForPeriod(primary.id(), START_DATE, END_DATE);
		then(system).should().findInvoiceForPeriod(member.id(), START_DATE, END_DATE);
		then(consolidatedInvoiceDao).should().save(invoiceCaptor.capture());
		// @formatter:off
		and.then(invoiceCaptor.getValue())
			.as("Billed to the primary account")
			.returns(primary.getAccountId(), from(SnfConsolidatedInvoice::getAccountId))
			.returns(primary.getUserId(), from(SnfConsolidatedInvoice::getUserId))
			.as("Billed to the primary account address")
			.returns(primary.getAddress(), from(SnfConsolidatedInvoice::getAddress))
			.as("For the month")
			.returns(START_DATE, from(SnfConsolidatedInvoice::getStartDate))
			.returns(END_DATE, from(SnfConsolidatedInvoice::getEndDate))
			.as("In the primary account currency")
			.returns(primary.getCurrencyCode(), from(SnfConsolidatedInvoice::getCurrencyCode))
			.as("Consolidates the account invoices, primary first")
			.returns(List.of(primaryInvoice.getId(), memberInvoice.getId()),
					from(SnfConsolidatedInvoice::getInvoiceIds))
			;
		and.then(result)
			.as("Saved consolidated invoice returned")
			.returns(id, from(SnfConsolidatedInvoice::getId))
			.as("Account invoices returned")
			.returns(List.of(primaryInvoice, memberInvoice), from(SnfConsolidatedInvoice::getInvoices))
			;
		// @formatter:on
	}

	@Test
	public void generate_accountAlreadyInvoiced() {
		// GIVEN
		given(accountDao.getForUser(1L, END_DATE)).willReturn(primary);
		given(accountDao.getForUser(2L, END_DATE)).willReturn(member);

		// the member account was invoiced for the month before joining the group
		doReturn(null).when(system).findInvoiceForPeriod(primary.id(), START_DATE, END_DATE);
		doReturn(invoice(member, 99L)).when(system).findInvoiceForPeriod(member.id(), START_DATE,
				END_DATE);

		final SnfInvoice primaryInvoice = invoice(primary, 100L);
		doReturn(primaryInvoice).when(system).generateInvoice(1L, START_DATE, END_DATE,
				defaultOptions());

		final UserLongCompositePK id = new UserLongCompositePK(1L, 200L);
		given(consolidatedInvoiceDao.save(any())).willReturn(id);

		// WHEN
		SnfConsolidatedInvoice result = system.generateConsolidatedInvoice(group, MONTH,
				defaultOptions());

		// THEN
		then(system).should(never()).generateInvoice(2L, START_DATE, END_DATE, defaultOptions());
		// @formatter:off
		and.then(result)
			.as("Only the account invoice generated now consolidated")
			.returns(List.of(primaryInvoice), from(SnfConsolidatedInvoice::getInvoices))
			.returns(List.of(primaryInvoice.getId()), from(SnfConsolidatedInvoice::getInvoiceIds))
			;
		// @formatter:on
	}

	@Test
	public void generate_noCharges() {
		// GIVEN
		given(accountDao.getForUser(1L, END_DATE)).willReturn(primary);
		given(accountDao.getForUser(2L, END_DATE)).willReturn(member);
		doReturn(null).when(system).findInvoiceForPeriod(any(), any(), any());
		doReturn(null).when(system).generateInvoice(any(), any(), any(), any());

		// WHEN
		SnfConsolidatedInvoice result = system.generateConsolidatedInvoice(group, MONTH,
				defaultOptions());

		// THEN
		// @formatter:off
		and.then(result)
			.as("No consolidated invoice when no account has charges")
			.isNull()
			;
		// @formatter:on
		then(consolidatedInvoiceDao).shouldHaveNoInteractions();
	}

	@Test
	public void generate_dryRun() {
		// GIVEN
		given(accountDao.getForUser(1L, END_DATE)).willReturn(primary);

		final SnfInvoice primaryInvoice = invoice(primary, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID);
		final SnfInvoice memberInvoice = invoice(member, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID);
		doReturn(primaryInvoice).when(system).generateInvoice(1L, START_DATE, END_DATE, dryRunOptions());
		doReturn(memberInvoice).when(system).generateInvoice(2L, START_DATE, END_DATE, dryRunOptions());

		// WHEN
		SnfConsolidatedInvoice result = system.generateConsolidatedInvoice(group, MONTH,
				dryRunOptions());

		// THEN
		// @formatter:off
		and.then(result)
			.as("Draft consolidated invoice returned")
			.returns(new UserLongCompositePK(1L, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID),
					from(SnfConsolidatedInvoice::getId))
			.returns(List.of(primaryInvoice, memberInvoice), from(SnfConsolidatedInvoice::getInvoices))
			;
		// @formatter:on
		then(accountDao).should(never()).lockAccount(any());
		then(system).should(never()).findInvoiceForPeriod(any(), any(), any());
		then(consolidatedInvoiceDao).shouldHaveNoInteractions();
	}

	@Test
	public void preview() {
		// GIVEN
		given(accountDao.getForUser(1L, END_DATE)).willReturn(primary);

		final SnfInvoice primaryInvoice = invoice(primary, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID);
		final SnfInvoice memberInvoice = invoice(member, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID);
		doReturn(primaryInvoice).when(system).generateInvoice(1L, START_DATE, END_DATE, dryRunOptions());
		doReturn(memberInvoice).when(system).generateInvoice(2L, START_DATE, END_DATE, dryRunOptions());
		final SnfConsolidatedInvoice generated = system.generateConsolidatedInvoice(group, MONTH,
				dryRunOptions());

		// WHEN
		SnfConsolidatedInvoice result = system.previewConsolidatedInvoice(group, MONTH,
				List.of(primaryInvoice, memberInvoice));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Preview same as dry-run generated consolidated invoice")
			.usingRecursiveComparison()
			.ignoringFields("created")
			.isEqualTo(generated)
			;
		// @formatter:on
		then(system).should(times(2)).generateInvoice(any(), any(), any(), any());
		then(consolidatedInvoiceDao).shouldHaveNoInteractions();
	}

	@Test
	public void preview_noInvoices() {
		// WHEN
		SnfConsolidatedInvoice result = system.previewConsolidatedInvoice(group, MONTH, List.of());

		// THEN
		and.then(result).as("No preview without account invoices").isNull();
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void getConsolidatedInvoice() {
		// GIVEN
		final UserLongCompositePK id = new UserLongCompositePK(1L, 200L);
		final SnfInvoice primaryInvoice = invoice(primary, 100L);
		final SnfInvoice memberInvoice = invoice(member, 101L);
		final SnfConsolidatedInvoice saved = new SnfConsolidatedInvoice(200L, 1L, primary.getAccountId(),
				Instant.now(), START_DATE, END_DATE, "NZD");
		saved.setInvoiceIds(List.of(primaryInvoice.getId(), memberInvoice.getId()));
		given(consolidatedInvoiceDao.get(id)).willReturn(saved);
		given(invoiceDao.get(primaryInvoice.getId())).willReturn(primaryInvoice);
		given(invoiceDao.get(memberInvoice.getId())).willReturn(memberInvoice);

		// WHEN
		SnfConsolidatedInvoice result = system.getConsolidatedInvoice(id);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Consolidated invoice returned")
			.isSameAs(saved)
			.as("Account invoices loaded")
			.returns(List.of(primaryInvoice, memberInvoice), from(SnfConsolidatedInvoice::getInvoices))
			;
		// @formatter:on
	}

	@Test
	public void deliverConsolidatedInvoice() {
		// GIVEN
		final UserLongCompositePK id = new UserLongCompositePK(1L, 200L);
		given(accountDao.getForUser(1L)).willReturn(primary);

		final SnfConsolidatedInvoice saved = new SnfConsolidatedInvoice(200L, 1L, primary.getAccountId(),
				Instant.now(), START_DATE, END_DATE, "NZD");
		saved.setInvoices(List.of(invoice(primary, 100L), invoice(member, 101L)));
		doReturn(saved).when(system).getConsolidatedInvoice(id);

		system.setDeliveryServices(List.of(deliverer));
		given(deliverer.deliverConsolidatedInvoice(saved, primary, null))
				.willReturn(CompletableFuture.completedFuture(Result.result(null)));

		// WHEN
		boolean result = system.deliverConsolidatedInvoice(id);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Consolidated invoice delivered to the primary account")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void findConsolidatedInvoiceForInvoice() {
		// GIVEN
		final SnfConsolidatedInvoice consolidated = new SnfConsolidatedInvoice(200L, 1L,
				primary.getAccountId(), Instant.now(), START_DATE, END_DATE, "NZD");
		given(consolidatedInvoiceDao.findFiltered(any(), isNull(), isNull(), isNull()))
				.willReturn(new BasicFilterResults<>(List.of(consolidated)));

		// WHEN
		SnfConsolidatedInvoice result = system
				.findConsolidatedInvoiceForInvoice(new UserLongCompositePK(2L, 101L));

		// THEN
		then(consolidatedInvoiceDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(),
				isNull());
		// @formatter:off
		and.then(filterCaptor.getValue())
			.as("Queried by account invoice ID")
			.returns(101L, from(SnfConsolidatedInvoiceFilter::getInvoiceId))
			;
		and.then(result)
			.as("Consolidated invoice returned")
			.isSameAs(consolidated)
			;
		// @formatter:on
	}

	@Test
	public void findConsolidatedInvoiceForInvoice_notSupported() {
		// GIVEN
		system.setConsolidatedInvoiceDao(null);

		// WHEN
		SnfConsolidatedInvoice result = system
				.findConsolidatedInvoiceForInvoice(new UserLongCompositePK(2L, 101L));

		// THEN
		// @formatter:off
		and.then(result)
			.as("No consolidated invoice without consolidated invoice support")
			.isNull()
			;
		// @formatter:on
	}

}
