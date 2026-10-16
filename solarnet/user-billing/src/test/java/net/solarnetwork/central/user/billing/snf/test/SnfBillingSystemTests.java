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
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.billing.snf.domain.UsageTier.tier;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.money.Monetary;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.BasicInvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.domain.InvoiceFilterCommand;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.NamedCost;
import net.solarnetwork.central.user.billing.domain.NamedCostTiers;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AddressDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.LocalizedConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.LocalizedConsolidatedInvoiceAccount;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions;
import net.solarnetwork.central.user.billing.snf.domain.UsageTiers;
import net.solarnetwork.central.user.billing.support.LocalizedNamedCost;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link SnfBillingSystem}.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SnfBillingSystemTests {

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Mock
	private AddressDao addressDao;

	@Mock
	private AccountDao accountDao;

	@Mock
	private SnfInvoiceDao invoiceDao;

	@Mock
	private NodeUsageDao usageDao;

	@Mock
	private InvoiceConsolidationResolver consolidationResolver;

	@Captor
	private ArgumentCaptor<SnfInvoiceFilter> invoiceFilterCaptor;

	@Captor
	private ArgumentCaptor<SnfConsolidatedInvoiceFilter> consolidatedFilterCaptor;

	@Captor
	private ArgumentCaptor<SnfInvoicingOptions> invoicingOptionsCaptor;

	@Captor
	private ArgumentCaptor<Address> addressCaptor;

	@Captor
	private ArgumentCaptor<Account> accountCaptor;

	private MutableClock clock;
	private SnfBillingSystem system;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneOffset.UTC);
		system = new SnfBillingSystem(clock, invoicingSystem, addressDao, accountDao, invoiceDao,
				usageDao);
	}

	@Test
	public void namedCosts() {
		// GIVEN
		final LocalDate date1 = LocalDate.of(2010, 1, 1);
		final LocalDate date2 = LocalDate.of(2011, 1, 1);
		final LocalDate date3 = LocalDate.of(2012, 1, 1);
		final UsageTiers rates1 = new UsageTiers(
				asList(tier("s1", 0, "1", date1), tier("s1", 10, "10", date1)), date1);
		final UsageTiers rates2 = new UsageTiers(
				asList(tier("s1", 0, "2", date2), tier("s1", 10, "20", date2)), date2);
		final UsageTiers rates3 = new UsageTiers(
				asList(tier("s1", 0, "3", date3), tier("s1", 10, "30", date3)), date3);
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
		and.then(result).as("Same number of DAO results returned").hasSize(allRates.size());
		for ( int i = 0; i < allRates.size(); i++ ) {
			NamedCostTiers actualRates = result.get(i);
			UsageTiers expectedRates = allRates.get(i);
			// @formatter:off
			and.then(actualRates)
				.as("Result %d has same date as DAO result".formatted(i))
				.returns(expectedRates.getDate(), from(NamedCostTiers::getDate))
				;
			and.then(actualRates.getTiers())
				.as("Result %d has same number of tiers as DAO result".formatted(i))
				.hasSize(expectedRates.getTiers().size())
				;
			// @formatter:on
			int j = 0;
			for ( NamedCost cost : actualRates.getTiers() ) {
				// @formatter:off
				and.then(cost)
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

	private static SnAccountCreationInput creationInput(String currencyCode, String localeTag) {
		final var input = new SnAccountCreationInput();

		final var acct = new SnAccountInput();
		acct.setCurrency(Monetary.getCurrency(currencyCode));
		acct.setLocale(localeTag);
		input.setAccount(acct);

		final var addr = new SnAddressInput();
		addr.setName(randomString());
		addr.setEmail(randomEmail());
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		addr.setStreet1("Level 1");
		addr.setStreet2("123 Main Street");
		addr.setLocality("Wellington");
		addr.setRegion("Region");
		addr.setStateOrProvince("State");
		addr.setPostalCode("1001");
		input.setAddress(addr);

		return input;
	}

	private static Address addressFor(SnAccountCreationInput input, UserLongCompositePK id,
			Instant created) {
		final SnAddressInput in = input.getAddress();
		final var addr = new Address(id, created, in.getName(), in.getEmail(), in.getCountry(),
				in.getTimeZoneId());
		addr.setStreet(in.street());
		addr.setLocality(in.getLocality());
		addr.setRegion(in.getRegion());
		addr.setStateOrProvince(in.getStateOrProvince());
		addr.setPostalCode(in.getPostalCode());
		return addr;
	}

	@Test
	public void accountingSystemKey() {
		// THEN
		// @formatter:off
		and.then(system.getAccountingSystemKey())
			.as("SNF accounting system key provided")
			.isEqualTo(SnfBillingSystem.ACCOUNTING_SYSTEM_KEY)
			;
		and.then(system.supportsAccountingSystemKey(SnfBillingSystem.ACCOUNTING_SYSTEM_KEY))
			.as("SNF accounting system key supported")
			.isTrue()
			;
		and.then(system.supportsAccountingSystemKey(randomString()))
			.as("Other accounting system key not supported")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void createAccount() {
		// GIVEN
		final Long userId = randomLong();
		final var input = creationInput("NZD", "en-NZ");

		final UserLongCompositePK addrId = new UserLongCompositePK(userId, randomLong());
		given(addressDao.save(any())).willReturn(addrId);

		final UserLongCompositePK acctId = new UserLongCompositePK(userId, randomLong());
		given(accountDao.save(any())).willReturn(acctId);

		final var daoAccount = new Account(acctId, clock.instant(), "NZD", "en-NZ");
		daoAccount.setAddress(addressFor(input, addrId, clock.instant()));
		given(accountDao.get(acctId)).willReturn(daoAccount);

		// WHEN
		SnAccount<?, ?, ?> result = system.createAccount(userId, input);

		// THEN
		// @formatter:off
		then(addressDao).should()
			.save(addressCaptor.capture())
			;
		and.then(addressCaptor.getValue())
			.as("Address inserted with an unassigned entity ID, for the given user")
			.returns(UserLongCompositePK.unassignedEntityIdKey(userId), from(Address::getId))
			.as("Address created date from clock")
			.returns(clock.instant(), from(Address::getCreated))
			.as("Address name from input")
			.returns(input.getAddress().getName(), from(Address::getName))
			.as("Address email from input")
			.returns(input.getAddress().getEmail(), from(Address::getEmail))
			.as("Address country from input")
			.returns("NZ", from(Address::getCountry))
			.as("Address time zone from input")
			.returns("Pacific/Auckland", from(Address::getTimeZoneId))
			.as("Address locality from input")
			.returns("Wellington", from(Address::getLocality))
			.as("Address region from input")
			.returns("Region", from(Address::getRegion))
			.as("Address state from input")
			.returns("State", from(Address::getStateOrProvince))
			.as("Address postal code from input")
			.returns("1001", from(Address::getPostalCode))
			;
		and.then(addressCaptor.getValue().getStreet())
			.as("Blank street elements removed from input")
			.containsExactly("Level 1", "123 Main Street")
			;

		then(accountDao).should()
			.save(accountCaptor.capture())
			;
		and.then(accountCaptor.getValue())
			.as("Account inserted with an unassigned entity ID, for the given user")
			.returns(UserLongCompositePK.unassignedEntityIdKey(userId), from(Account::getId))
			.as("Account created date from clock")
			.returns(clock.instant(), from(Account::getCreated))
			.as("Account currency code from input currency")
			.returns("NZD", from(Account::getCurrencyCode))
			.as("Account locale from input")
			.returns("en-NZ", from(Account::getLocale))
			.as("Account refers to the address just saved, via the key returned by the DAO")
			.extracting(Account::getAddress)
			.returns(addrId, from(Address::getId))
			;

		and.then(result)
			.as("Account re-read from DAO returned")
			.isSameAs(daoAccount)
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser() {
		// GIVEN
		final Long userId = randomLong();
		final var user = new User(userId, randomEmail());

		final var daoAccount = new Account(new UserLongCompositePK(userId, randomLong()),
				clock.instant(), "NZD", "en-NZ");
		given(accountDao.getForUser(userId)).willReturn(daoAccount);

		// WHEN
		SnAccount<?, ?, ?> result = system.getAccountForUser(user);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO account returned")
			.isSameAs(daoAccount)
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser_noAccount() {
		// GIVEN
		final Long userId = randomLong();
		final var user = new User(userId, randomEmail());

		given(accountDao.getForUser(userId)).willReturn(null);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown when no account exists for user")
			.isThrownBy(() -> system.getAccountForUser(user))
			.as("Unknown object reason given")
			.returns(Reason.UNKNOWN_OBJECT, from(AuthorizationException::getReason))
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_addressChanged() {
		// GIVEN
		final Long userId = randomLong();
		final var input = creationInput("NZD", "en-NZ");

		final UserLongCompositePK existingAddrId = new UserLongCompositePK(userId, randomLong());
		final UserLongCompositePK acctId = new UserLongCompositePK(userId, randomLong());

		final var existingAddr = new Address(existingAddrId, clock.instant().minusSeconds(60),
				randomString(), randomEmail(), "NZ", "Pacific/Auckland");
		final var existingAcct = new Account(acctId, clock.instant().minusSeconds(60), "NZD", "en-NZ");
		existingAcct.setAddress(existingAddr);
		given(accountDao.getForUser(userId)).willReturn(existingAcct);

		final UserLongCompositePK newAddrId = new UserLongCompositePK(userId, randomLong());
		given(addressDao.save(any())).willReturn(newAddrId);

		// WHEN
		SnAccount<?, ?, ?> result = system.updateAccount(userId, input);

		// THEN
		// @formatter:off
		then(addressDao).should()
			.save(addressCaptor.capture())
			;
		and.then(addressCaptor.getValue())
			.as("Changed address saved as a new row, not an update of the existing address")
			.returns(UserLongCompositePK.unassignedEntityIdKey(userId), from(Address::getId))
			.as("New address created date from clock")
			.returns(clock.instant(), from(Address::getCreated))
			.as("New address name from input")
			.returns(input.getAddress().getName(), from(Address::getName))
			;

		then(accountDao).should()
			.save(accountCaptor.capture())
			;
		and.then(accountCaptor.getValue())
			.as("Existing account key preserved, so the account row is updated")
			.returns(acctId, from(Account::getId))
			.as("Account moved to the newly saved address")
			.extracting(Account::getAddress)
			.returns(newAddrId, from(Address::getId))
			;

		and.then(result)
			.as("Updated account returned")
			.isSameAs(accountCaptor.getValue())
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_addressUnchanged() {
		// GIVEN
		final Long userId = randomLong();
		final var input = creationInput("USD", "en-US");

		final UserLongCompositePK existingAddrId = new UserLongCompositePK(userId, randomLong());
		final UserLongCompositePK acctId = new UserLongCompositePK(userId, randomLong());

		final var existingAddr = addressFor(input, existingAddrId, clock.instant().minusSeconds(60));
		final var existingAcct = new Account(acctId, clock.instant().minusSeconds(60), "NZD", "en-NZ");
		existingAcct.setAddress(existingAddr);
		given(accountDao.getForUser(userId)).willReturn(existingAcct);

		// WHEN
		SnAccount<?, ?, ?> result = system.updateAccount(userId, input);

		// THEN
		// @formatter:off
		then(addressDao).should(never())
			.save(any())
			;

		then(accountDao).should()
			.save(accountCaptor.capture())
			;
		and.then(accountCaptor.getValue())
			.as("Existing account key preserved, so the account row is updated")
			.returns(acctId, from(Account::getId))
			.as("Account currency updated from input")
			.returns("USD", from(Account::getCurrencyCode))
			.as("Account locale updated from input")
			.returns("en-US", from(Account::getLocale))
			.as("Account still refers to the existing address row")
			.extracting(Account::getAddress)
			.returns(existingAddrId, from(Address::getId))
			;

		and.then(result)
			.as("Updated account returned")
			.isSameAs(accountCaptor.getValue())
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_noChange() {
		// GIVEN
		final Long userId = randomLong();
		final var input = creationInput("NZD", "en-NZ");

		final UserLongCompositePK existingAddrId = new UserLongCompositePK(userId, randomLong());
		final UserLongCompositePK acctId = new UserLongCompositePK(userId, randomLong());

		final var existingAddr = addressFor(input, existingAddrId, clock.instant().minusSeconds(60));
		final var existingAcct = new Account(acctId, clock.instant().minusSeconds(60), "NZD", "en-NZ");
		existingAcct.setAddress(existingAddr);
		given(accountDao.getForUser(userId)).willReturn(existingAcct);

		// WHEN
		SnAccount<?, ?, ?> result = system.updateAccount(userId, input);

		// THEN
		// @formatter:off
		then(addressDao).should(never())
			.save(any())
			;
		then(accountDao).should(never())
			.save(any())
			;
		and.then(result)
			.as("Existing account returned unchanged")
			.isSameAs(existingAcct)
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_noAccount() {
		// GIVEN
		final Long userId = randomLong();
		final var input = creationInput("NZD", "en-NZ");

		given(accountDao.getForUser(userId)).willReturn(null);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown when no account exists for user")
			.isThrownBy(() -> system.updateAccount(userId, input))
			.as("Unknown object reason given")
			.returns(Reason.UNKNOWN_OBJECT, from(AuthorizationException::getReason))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(addressDao).should(never())
			.save(any())
			;
		then(accountDao).should(never())
			.save(same(null))
			;
		// @formatter:on
	}

	private static final LocalDate JUL = LocalDate.of(2026, 7, 1);
	private static final LocalDate AUG = LocalDate.of(2026, 8, 1);
	private static final LocalDate SEP = LocalDate.of(2026, 9, 1);

	private Account givenAccount(Long userId) {
		final var account = new Account(new UserLongCompositePK(userId, randomLong()),
				clock.instant(), "NZD", "en-NZ");
		account.setAddress(new Address(userId, "Tester " + userId, randomEmail(), "NZ",
				"Pacific/Auckland"));
		given(accountDao.getForUser(userId)).willReturn(account);
		return account;
	}

	private static SnfInvoice invoice(Long id, Long userId, LocalDate start, String amount) {
		final var invoice = new SnfInvoice(id, userId, randomLong(), Instant.now(), start,
				start.plusMonths(1), "NZD");
		invoice.setAddress(
				new Address(userId, "Tester " + userId, randomEmail(), "NZ", "Pacific/Auckland"));
		invoice.setItems(Set.of(SnfInvoiceItem.newItem(id, InvoiceItemType.Usage,
				"datum-props-in", BigDecimal.ONE, new BigDecimal(amount))));
		return invoice;
	}

	private static SnfConsolidatedInvoice consolidatedInvoice(Long id, Account primary,
			LocalDate start, SnfInvoice... invoices) {
		final var invoice = new SnfConsolidatedInvoice(id, primary.getUserId(),
				primary.getAccountId(), Instant.now(), start, start.plusMonths(1), "NZD");
		invoice.setAddress(primary.getAddress());
		invoice.setInvoices(List.of(invoices));
		return invoice;
	}

	@Test
	public void findFilteredInvoices_includeConsolidated() {
		// GIVEN
		final Long userId = randomLong();
		final Account account = givenAccount(userId);
		final SnfInvoice jul = invoice(1L, userId, JUL, "1.00");
		final SnfInvoice sep = invoice(5L, userId, SEP, "2.00");
		given(invoiceDao.findFiltered(invoiceFilterCaptor.capture(), any(), isNull(), isNull()))
				.willReturn(new BasicFilterResults<>(List.of(sep, jul)));

		final SnfConsolidatedInvoice aug = consolidatedInvoice(4L, account, AUG,
				invoice(2L, userId, AUG, "3.00"), invoice(3L, randomLong(), AUG, "4.00"));
		given(invoicingSystem.findFilteredConsolidatedInvoices(consolidatedFilterCaptor.capture(),
				isNull(), isNull(), isNull())).willReturn(new BasicFilterResults<>(List.of(aug)));

		final var filter = new InvoiceFilterCommand();
		filter.setUserId(userId);
		filter.setUnpaid(true);
		filter.setIncludeConsolidated(true);

		// WHEN
		FilterResults<InvoiceMatch, String> results = system.findFilteredInvoices(filter, null,
				null, null);

		// THEN
		final List<InvoiceMatch> matches = new ArrayList<>();
		results.forEach(matches::add);
		// @formatter:off
		and.then(invoiceFilterCaptor.getValue())
			.as("Account invoices of the user's account")
			.returns(account.getAccountId(), from(SnfInvoiceFilter::getAccountId))
			.as("Unpaid account invoices")
			.returns(true, from(SnfInvoiceFilter::getUnpaidOnly))
			.as("Account invoices that are part of a consolidated invoice left out")
			.returns(true, from(SnfInvoiceFilter::isExcludeConsolidated))
			;
		and.then(consolidatedFilterCaptor.getValue())
			.as("Consolidated invoices billed to the user's account")
			.returns(account.getAccountId(), from(SnfConsolidatedInvoiceFilter::getAccountId))
			.as("Unpaid consolidated invoices")
			.returns(true, from(SnfConsolidatedInvoiceFilter::isUnpaidOnly))
			;
		and.then(matches)
			.as("Account and consolidated invoices merged, newest first")
			.extracting(InvoiceMatch::getId)
			.containsExactly("5", "4", "1")
			;
		and.then(matches)
			.as("Consolidated invoice marked")
			.extracting(InvoiceMatch::isConsolidated)
			.containsExactly(false, true, false)
			;
		and.then(matches.get(1).getAmount())
			.as("Consolidated invoice amount is the total of its account invoices")
			.isEqualByComparingTo("7.00")
			;
		and.then(results.getTotalResults()).as("Total of both").isEqualTo(3L);
		// @formatter:on
	}

	@Test
	public void findFilteredInvoices_includeConsolidated_paged() {
		// GIVEN
		final Long userId = randomLong();
		final Account account = givenAccount(userId);
		given(invoiceDao.findFiltered(any(), any(), isNull(), isNull()))
				.willReturn(new BasicFilterResults<>(List.of(invoice(5L, userId, SEP, "2.00"),
						invoice(1L, userId, JUL, "1.00"))));
		given(invoicingSystem.findFilteredConsolidatedInvoices(any(), isNull(), isNull(), isNull()))
				.willReturn(new BasicFilterResults<>(List.of(consolidatedInvoice(4L, account, AUG,
						invoice(2L, userId, AUG, "3.00")))));

		final var filter = new InvoiceFilterCommand();
		filter.setUserId(userId);
		filter.setIncludeConsolidated(true);

		// WHEN
		FilterResults<InvoiceMatch, String> results = system.findFilteredInvoices(filter, null, 1L,
				1);

		// THEN
		// @formatter:off
		and.then(results.getResults())
			.as("Second page of one")
			.extracting(InvoiceMatch::getId)
			.containsExactly("4")
			;
		and.then(results.getTotalResults()).as("Total of all pages").isEqualTo(3L);
		and.then(results.getStartingOffset()).as("Starting offset").isEqualTo(1L);
		// @formatter:on
	}

	@Test
	public void findFilteredConsolidatedInvoices() {
		// GIVEN
		final Long userId = randomLong();
		final Account account = givenAccount(userId);
		final SnfConsolidatedInvoice aug = consolidatedInvoice(4L, account, AUG,
				invoice(2L, userId, AUG, "3.00"), invoice(3L, randomLong(), AUG, "4.00"));
		given(invoicingSystem.findFilteredConsolidatedInvoices(consolidatedFilterCaptor.capture(),
				isNull(), isNull(), isNull())).willReturn(new BasicFilterResults<>(List.of(aug)));

		final var filter = new InvoiceFilterCommand();
		filter.setUserId(userId);

		// WHEN
		FilterResults<InvoiceMatch, String> results = system.findFilteredConsolidatedInvoices(filter,
				null, null, null);

		// THEN
		// @formatter:off
		and.then(consolidatedFilterCaptor.getValue())
			.as("Consolidated invoices billed to the user's account")
			.returns(account.getAccountId(), from(SnfConsolidatedInvoiceFilter::getAccountId))
			.as("Paid and unpaid")
			.returns(false, from(SnfConsolidatedInvoiceFilter::isUnpaidOnly))
			;
		and.then(results.getResults())
			.as("Consolidated invoice returned")
			.singleElement()
			.returns("4", from(InvoiceMatch::getId))
			.returns(true, from(InvoiceMatch::isConsolidated))
			.satisfies(m -> and.then(m.getAmount()).isEqualByComparingTo("7.00"))
			;
		// @formatter:on
	}

	@Test
	public void getConsolidatedInvoice() {
		// GIVEN
		final Long userId = randomLong();
		final Account account = new Account(new UserLongCompositePK(userId, randomLong()),
				clock.instant(), "NZD", "en-NZ");
		account.setAddress(new Address(userId, "Primary", randomEmail(), "NZ", "Pacific/Auckland"));
		final Long memberUserId = randomLong();
		final SnfConsolidatedInvoice aug = consolidatedInvoice(4L, account, AUG,
				invoice(2L, userId, AUG, "3.00"), invoice(3L, memberUserId, AUG, "4.00"));
		given(invoicingSystem.getConsolidatedInvoice(new UserLongCompositePK(userId, 4L)))
				.willReturn(aug);
		given(invoicingSystem.messageSourceForInvoice(any())).willReturn(new StaticMessageSource());

		// WHEN
		Invoice result = system.getConsolidatedInvoice(userId, "4", Locale.ENGLISH);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Localized consolidated invoice returned")
			.asInstanceOf(InstanceOfAssertFactories.type(LocalizedConsolidatedInvoice.class))
			.returns("4", from(Invoice::getId))
			.returns(true, from(Invoice::isConsolidated))
			.satisfies(i -> and.then(i.getAmount()).isEqualByComparingTo("7.00"))
			.extracting(LocalizedConsolidatedInvoice::getLocalizedAccountInvoices,
					InstanceOfAssertFactories.list(LocalizedConsolidatedInvoiceAccount.class))
			.as("Summary of each account invoice, in order")
			.satisfiesExactly(
				a -> and.then(a)
					.returns(userId, from(LocalizedConsolidatedInvoiceAccount::userId))
					.returns("Tester " + userId, from(LocalizedConsolidatedInvoiceAccount::name))
					.returns("INV-2", from(LocalizedConsolidatedInvoiceAccount::invoiceNumber))
					.returns("NZ$3.00", from(LocalizedConsolidatedInvoiceAccount::localizedAmount)),
				a -> and.then(a)
					.returns(memberUserId, from(LocalizedConsolidatedInvoiceAccount::userId))
					.returns("INV-3", from(LocalizedConsolidatedInvoiceAccount::invoiceNumber))
					.returns("NZ$4.00", from(LocalizedConsolidatedInvoiceAccount::localizedAmount)))
			;
		// @formatter:on
	}

	@Test
	public void getConsolidatedInvoice_notFound() {
		// GIVEN
		final Long userId = randomLong();

		// WHEN
		Invoice result = system.getConsolidatedInvoice(userId, "4", Locale.ENGLISH);

		// THEN
		and.then(result).as("No consolidated invoice").isNull();
	}

	@Test
	public void getPreviewConsolidatedInvoice() {
		// GIVEN
		system.setConsolidationResolver(consolidationResolver);
		final Long userId = randomLong();
		final Long memberUserId = randomLong();
		final Account account = givenAccount(userId);
		final var group = new InvoiceConsolidationGroup(userId, List.of(userId, memberUserId),
				false);
		given(consolidationResolver.groupForUser(userId)).willReturn(group);
		final SnfConsolidatedInvoice preview = consolidatedInvoice(-1L, account, AUG,
				invoice(-1L, userId, AUG, "3.00"), invoice(-1L, memberUserId, AUG, "4.00"));
		given(invoicingSystem.generateConsolidatedInvoice(same(group), any(),
				invoicingOptionsCaptor.capture())).willReturn(preview);
		given(invoicingSystem.messageSourceForInvoice(any())).willReturn(new StaticMessageSource());

		// WHEN
		Invoice result = system.getPreviewConsolidatedInvoice(userId,
				new BasicInvoiceGenerationOptions(YearMonth.from(AUG), false), Locale.ENGLISH);

		// THEN
		then(invoicingSystem).should().generateConsolidatedInvoice(same(group),
				eq(YearMonth.from(AUG)), any());
		// @formatter:off
		and.then(invoicingOptionsCaptor.getValue())
			.as("Dry run, so nothing is saved")
			.returns(true, from(SnfInvoicingOptions::isDryRun))
			;
		and.then(result)
			.as("Localized preview returned")
			.asInstanceOf(InstanceOfAssertFactories.type(LocalizedConsolidatedInvoice.class))
			.satisfies(i -> and.then(i.getAmount()).isEqualByComparingTo("7.00"))
			.extracting(LocalizedConsolidatedInvoice::getLocalizedAccountInvoices,
					InstanceOfAssertFactories.list(LocalizedConsolidatedInvoiceAccount.class))
			.as("Each account of the group")
			.extracting(LocalizedConsolidatedInvoiceAccount::userId)
			.containsExactly(userId, memberUserId)
			;
		// @formatter:on
	}

	@Test
	public void getPreviewConsolidatedInvoice_member() {
		// GIVEN
		system.setConsolidationResolver(consolidationResolver);
		final Long userId = randomLong();
		final Long primaryUserId = randomLong();
		given(consolidationResolver.groupForUser(userId)).willReturn(
				new InvoiceConsolidationGroup(primaryUserId, List.of(primaryUserId, userId), false));

		// WHEN
		Invoice result = system.getPreviewConsolidatedInvoice(userId,
				new BasicInvoiceGenerationOptions(false), Locale.ENGLISH);

		// THEN
		and.then(result).as("No preview for a member of a group").isNull();
		then(invoicingSystem).should(never()).generateConsolidatedInvoice(any(), any(), any());
	}

	@Test
	public void getPreviewConsolidatedInvoice_invalidGroup() {
		// GIVEN
		system.setConsolidationResolver(consolidationResolver);
		final Long userId = randomLong();
		given(consolidationResolver.groupForUser(userId))
				.willReturn(new InvoiceConsolidationGroup(userId, List.of(userId, randomLong()),
						false, new DefaultMessageSourceResolvable(
								"error.consolidated.currencyMismatch")));

		// WHEN
		Invoice result = system.getPreviewConsolidatedInvoice(userId,
				new BasicInvoiceGenerationOptions(false), Locale.ENGLISH);

		// THEN
		and.then(result).as("No preview for an invalid group").isNull();
		then(invoicingSystem).should(never()).generateConsolidatedInvoice(any(), any(), any());
	}

	@Test
	public void getPreviewConsolidatedInvoice_noResolver() {
		// WHEN
		Invoice result = system.getPreviewConsolidatedInvoice(randomLong(),
				new BasicInvoiceGenerationOptions(false), Locale.ENGLISH);

		// THEN
		and.then(result).as("No preview without consolidation support").isNull();
	}

}
