/* ==================================================================
 * SnfBillingSystem.java - 20/07/2020 9:01:05 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf;

import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils.invoiceForSnfInvoice;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.NamedCost;
import net.solarnetwork.central.user.billing.domain.NamedCostTiers;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AddressDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.ConsolidatedInvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.LocalizedConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.LocalizedConsolidatedInvoiceAccount;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions;
import net.solarnetwork.central.user.billing.support.BasicBillingSystemInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;
import net.solarnetwork.central.user.billing.support.LocalizedNamedCost;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * {@link BillingSystem} implementation for SolarNetwork Foundation.
 *
 * @author matt
 * @version 2.5
 */
public class SnfBillingSystem implements BillingSystem, BillingSystemRegistrar {

	/** The {@literal accounting} billing data value for SNF. */
	public static final String ACCOUNTING_SYSTEM_KEY = "snf";

	public static final String ANY_CURRENCY_CODE = "*";

	private final InstantSource clock;
	private final AccountDao accountDao;
	private final AddressDao addressDao;
	private final SnfInvoiceDao invoiceDao;
	private final SnfInvoicingSystem invoicingSystem;
	private final NodeUsageDao nodeUsageDao;
	private @Nullable InvoiceConsolidationResolver consolidationResolver;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock
	 * @param invoicingSystem
	 *        the invoicing system
	 * @param addressDao
	 *        the address DAO
	 * @param accountDao
	 *        the account DAO
	 * @param invoiceDao
	 *        the invoice DAO
	 * @param nodeUsageDao
	 *        the node usage DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 2.3
	 */
	public SnfBillingSystem(InstantSource clock, SnfInvoicingSystem invoicingSystem,
			AddressDao addressDao, AccountDao accountDao, SnfInvoiceDao invoiceDao,
			NodeUsageDao nodeUsageDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
		this.addressDao = requireNonNullArgument(addressDao, "addressDao");
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.invoiceDao = requireNonNullArgument(invoiceDao, "invoiceDao");
		this.nodeUsageDao = requireNonNullArgument(nodeUsageDao, "nodeUsageDao");
	}

	@Override
	public String getAccountingSystemKey() {
		return ACCOUNTING_SYSTEM_KEY;
	}

	@Override
	public boolean supportsAccountingSystemKey(String key) {
		return ACCOUNTING_SYSTEM_KEY.equals(key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public SnAccount<?, ?, ?> createAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {

		// save address
		var addr = createAddress(userId, requireNonNullArgument(input.getAddress(), "input.address"));
		var addrId = addressDao.save(addr);
		addr = addr.copyWithId(addrId);

		// save account
		return nonnull(accountDao.get(accountDao.save(createAccount(userId,
				requireNonNullArgument(input.getAccount(), "input.account"), addr))), "Entity");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public SnAccount<?, ?, ?> getAccountForUser(User user)
			throws IllegalArgumentException, AuthorizationException {
		return requireNonNullObject(accountDao.getForUser(requireNonNullArgument(user, "user").id()),
				user.getId());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public SnAccount<?, ?, ?> updateAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {
		Account acct = requireNonNullObject(
				accountDao.getForUser(requireNonNullArgument(userId, "userId")), userId);
		Address addr = acct.getAddress();

		// save new address if the input differs from existing
		final Address updateAddr = createAddress(userId,
				requireNonNullArgument(input.getAddress(), "input.address"));
		if ( updateAddr.differsFrom(addr) ) {
			var addrId = addressDao.save(updateAddr);
			addr = updateAddr.copyWithId(addrId);
		}

		// update account if the input differs from existing
		final Account updateAcct = createAccount(userId,
				requireNonNullArgument(input.getAccount(), "input.account"), addr);
		if ( updateAcct.differsFrom(acct) ) {
			acct = updateAcct.copyWithId(acct.getId());
			accountDao.save(acct);
		}

		return acct;
	}

	private Address createAddress(Long userId, SnAddressInput input) {
		final var addr = new Address(
		// @formatter:off
				UserLongCompositePK.unassignedEntityIdKey(userId), clock.instant(),
				requireNonNullArgument(input.getName(), "input.name"),
				requireNonNullArgument(input.getEmail(), "input.email"),
				requireNonNullArgument(input.getCountry(), "input.country"),
				requireNonNullArgument(input.getTimeZoneId(), "input.timeZoneId")
				// @formatter:on
		);
		addr.setStreet(input.street());
		addr.setLocality(input.getLocality());
		addr.setRegion(input.getRegion());
		addr.setStateOrProvince(input.getStateOrProvince());
		addr.setPostalCode(input.getPostalCode());
		return addr;
	}

	private Account createAccount(Long userId, SnAccountInput input, Address address) {
		final var acct = new Account(UserLongCompositePK.unassignedEntityIdKey(userId), clock.instant(),
				requireNonNullArgument(input.getCurrency(), "input.currency").getCurrencyCode(),
				requireNonNullArgument(input.getLocale(), "input.locale"));
		acct.setAddress(address);
		return acct;
	}

	@Override
	public BillingSystemInfo getInfo(@Nullable Locale locale) {
		return new BasicBillingSystemInfo(getAccountingSystemKey());
	}

	@Override
	public @Nullable List<? extends NamedCostTiers> namedCostTiers(@Nullable Locale locale) {
		final List<? extends NamedCostTiers> tiers = nodeUsageDao.effectiveUsageTiers();
		if ( tiers == null || tiers.isEmpty() ) {
			return tiers;
		}

		final Locale loc = (locale != null ? locale : Locale.getDefault());

		// localize tiers
		final List<NamedCostTiers> result = new ArrayList<>(tiers.size());
		for ( NamedCostTiers t : tiers ) {
			MessageSource msg = invoicingSystem
					.messageSourceForDate(t.getDate().atStartOfDay(ZoneOffset.UTC).toInstant());
			List<LocalizedNamedCost> locTiers = new ArrayList<>(t.getTiers().size());
			for ( NamedCost c : t.getTiers() ) {
				String desc = msg.getMessage(c.getName() + ".item", null, c.getName(), loc);
				@SuppressWarnings("NullAway")
				LocalizedNamedCost locCost = new LocalizedNamedCost(new NamedCost() {

					@Override
					public String getName() {
						return c.getName();
					}

					@Override
					public BigInteger getQuantity() {
						return c.getQuantity();
					}

					@Override
					public BigDecimal getCost() {
						return c.getCost();
					}

					@Override
					public BigDecimal getEffectiveRate() {
						// does not make sense here
						return null;
					}

				}, null, desc, null) {

					@Override
					public String getLocalizedCost() {
						// does not make sense here
						return null;
					}

					@Override
					public String getLocalizedEffectiveRate() {
						// does not make sense here
						return null;
					}

					@Override
					public String getLocalizedQuantity() {
						// does not make sense here
						return null;
					}

				};
				locTiers.add(locCost);
			}
			result.add(new NamedCostTiers() {

				@Override
				public List<? extends NamedCost> getTiers() {
					return locTiers;
				}

				@Override
				public LocalDate getDate() {
					return t.getDate();
				}

			});
		}

		return result;

	}

	@Override
	public FilterResults<InvoiceMatch, String> findFilteredInvoices(InvoiceFilter filter,
			@Nullable List<SortDescriptor> sortDescriptors, @Nullable Long offset,
			@Nullable Integer max) {
		// get account
		final Account account = accountDao.getForUser(filter.getUserId());
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, filter.getUserId());
		}
		SnfInvoiceFilter invoiceFilter = SnfInvoiceFilter.forAccount(account);
		if ( filter.getUnpaid() != null ) {
			invoiceFilter.setUnpaidOnly(filter.getUnpaid());
		}
		if ( Boolean.TRUE.equals(filter.getIncludeConsolidated()) ) {
			return findFilteredInvoicesWithConsolidated(account, invoiceFilter, offset, max);
		}
		net.solarnetwork.dao.FilterResults<SnfInvoice, UserLongCompositePK> results = invoiceDao
				.findFiltered(invoiceFilter, SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, offset, max);
		List<InvoiceMatch> matches = StreamSupport.stream(results.spliterator(), false)
				.map(InvoiceImpl::new).collect(toList());
		return new BasicFilterResults<>(matches, results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());
	}

	private @Nullable SnfInvoice getSnfInvoice(final Long userId, final String invoiceId)
			throws AuthorizationException {
		final UserLongCompositePK id = new UserLongCompositePK(userId, Long.valueOf(invoiceId));
		return invoiceDao.get(id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public @Nullable Invoice getInvoice(Long userId, String invoiceId, Locale locale) {
		final SnfInvoice invoice = getSnfInvoice(userId, invoiceId);
		if ( invoice == null ) {
			return null;
		}
		final MessageSource messageSource = invoicingSystem.messageSourceForInvoice(invoice);
		return invoiceForSnfInvoice(invoice, messageSource, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public @Nullable Resource renderInvoice(Long userId, String invoiceId, MimeType outputType,
			Locale locale) {
		SnfInvoice invoice = getSnfInvoice(userId, invoiceId);
		if ( invoice == null ) {
			return null;
		}
		return invoicingSystem.renderInvoice(invoice, outputType, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public @Nullable Invoice getPreviewInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			Locale locale) {
		SnfInvoice invoice = createPreviewInvoice(userId, options);
		if ( invoice == null ) {
			return null;
		}
		final MessageSource messageSource = invoicingSystem.messageSourceForInvoice(invoice);
		return invoiceForSnfInvoice(invoice, messageSource, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public @Nullable Resource previewInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			MimeType outputType, Locale locale) {
		SnfInvoice invoice = createPreviewInvoice(userId, options);
		if ( invoice == null ) {
			return null;
		}
		return invoicingSystem.renderInvoice(invoice, outputType, locale);
	}

	private @Nullable SnfInvoice createPreviewInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options) {
		Account account = accountDao.getForUser(userId);
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
		}

		final LocalDate start = previewStartDate(account, options);
		final LocalDate end = start.plusMonths(1);

		log.debug("Generating preview invoice for account {} (user {}) month {}", account.getId(),
				account.getUserId(), start);

		SnfInvoicingOptions opts = new SnfInvoicingOptions(true,
				options != null && options.isUseAccountCredit());
		return invoicingSystem.generateInvoice(userId, start, end, opts);
	}

	private static LocalDate previewStartDate(Account account,
			net.solarnetwork.central.user.billing.domain.@Nullable InvoiceGenerationOptions options) {
		if ( options != null && options.getMonth() != null ) {
			return options.getMonth().atDay(1);
		}
		// find current month in account's time zone
		ZoneId zone = account.getTimeZone();
		if ( zone == null ) {
			zone = ZoneId.systemDefault();
		}
		return LocalDate.now(zone).withDayOfMonth(1);
	}

	/**
	 * An invoice match with the start date and ID to sort it by.
	 */
	private record DatedInvoiceMatch(LocalDate startDate, Long invoiceId, InvoiceMatch match) {

		private static final Comparator<DatedInvoiceMatch> NEWEST_FIRST = Comparator
				.comparing(DatedInvoiceMatch::startDate).reversed()
				.thenComparing(DatedInvoiceMatch::invoiceId);

	}

	private FilterResults<InvoiceMatch, String> findFilteredInvoicesWithConsolidated(
			Account account, SnfInvoiceFilter invoiceFilter, @Nullable Long offset,
			@Nullable Integer max) {
		// both kinds of invoice are found in full and merged newest first, then paged here
		invoiceFilter.setExcludeConsolidated(true);
		final net.solarnetwork.dao.FilterResults<SnfInvoice, UserLongCompositePK> invoices = invoiceDao
				.findFiltered(invoiceFilter, SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, null, null);
		final SnfConsolidatedInvoiceFilter consolidatedFilter = SnfConsolidatedInvoiceFilter
				.forAccount(account.getAccountId());
		consolidatedFilter.setUnpaidOnly(invoiceFilter.getUnpaidOnly());
		final net.solarnetwork.dao.FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> consolidated = invoicingSystem
				.findFilteredConsolidatedInvoices(consolidatedFilter, null, null, null);

		final List<DatedInvoiceMatch> all = new ArrayList<>();
		for ( SnfInvoice invoice : invoices ) {
			all.add(new DatedInvoiceMatch(invoice.getStartDate(), invoice.getInvoiceId(),
					new InvoiceImpl(invoice)));
		}
		for ( SnfConsolidatedInvoice invoice : consolidated ) {
			all.add(new DatedInvoiceMatch(invoice.getStartDate(), invoice.getInvoiceId(),
					consolidatedInvoiceMatch(invoice)));
		}
		all.sort(DatedInvoiceMatch.NEWEST_FIRST);

		final int start = (offset != null ? (int) Math.min(Math.max(offset, 0L), all.size()) : 0);
		final int end = (max != null ? Math.min(start + max, all.size()) : all.size());
		final List<InvoiceMatch> matches = all.subList(start, end).stream()
				.map(DatedInvoiceMatch::match).toList();
		return new BasicFilterResults<>(matches, (long) all.size(), start, matches.size());
	}

	private static ConsolidatedInvoiceImpl consolidatedInvoiceMatch(SnfConsolidatedInvoice invoice) {
		final List<SnfInvoice> accountInvoices = invoice.getInvoices();
		return new ConsolidatedInvoiceImpl(invoice, (accountInvoices != null
				? accountInvoices.stream().map(InvoiceImpl::new).toList()
				: List.of()));
	}

	/**
	 * Get the invoice consolidation resolver.
	 *
	 * @return the resolver, or {@code null} if consolidated invoice previews are
	 *         not supported
	 * @since 2.5
	 */
	public final @Nullable InvoiceConsolidationResolver getConsolidationResolver() {
		return consolidationResolver;
	}

	/**
	 * Set the invoice consolidation resolver.
	 *
	 * @param consolidationResolver
	 *        the resolver to set, to support consolidated invoice previews
	 * @since 2.5
	 */
	public final void setConsolidationResolver(
			@Nullable InvoiceConsolidationResolver consolidationResolver) {
		this.consolidationResolver = consolidationResolver;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<InvoiceMatch, String> findFilteredConsolidatedInvoices(
			InvoiceFilter filter, @Nullable List<SortDescriptor> sortDescriptors,
			@Nullable Long offset, @Nullable Integer max) {
		final Account account = accountDao.getForUser(filter.getUserId());
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, filter.getUserId());
		}
		final SnfConsolidatedInvoiceFilter consolidatedFilter = SnfConsolidatedInvoiceFilter
				.forAccount(account.getAccountId());
		consolidatedFilter.setUnpaidOnly(Boolean.TRUE.equals(filter.getUnpaid()));
		final net.solarnetwork.dao.FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> results = invoicingSystem
				.findFilteredConsolidatedInvoices(consolidatedFilter, null, offset, max);
		final List<InvoiceMatch> matches = StreamSupport.stream(results.spliterator(), false)
				.map(SnfBillingSystem::consolidatedInvoiceMatch).collect(toList());
		return new BasicFilterResults<>(matches, results.getTotalResults(),
				results.getStartingOffset(), results.getReturnedResultCount());
	}

	private @Nullable SnfConsolidatedInvoice getSnfConsolidatedInvoice(final Long userId,
			final String invoiceId) {
		return invoicingSystem
				.getConsolidatedInvoice(new UserLongCompositePK(userId, Long.valueOf(invoiceId)));
	}

	/**
	 * Localize a consolidated invoice, with each account invoice localized
	 * with its own messages, as when rendered.
	 */
	private LocalizedConsolidatedInvoice localizedConsolidatedInvoice(
			SnfConsolidatedInvoice invoice, Locale locale) {
		final List<SnfInvoice> accountInvoices = invoice.getInvoices();
		final int count = (accountInvoices != null ? accountInvoices.size() : 0);
		final List<Invoice> invoices = new ArrayList<>(count);
		final List<LocalizedConsolidatedInvoiceAccount> accounts = new ArrayList<>(count);
		if ( accountInvoices != null ) {
			for ( SnfInvoice accountInvoice : accountInvoices ) {
				final InvoiceImpl accountInvoiceImpl = invoiceForSnfInvoice(accountInvoice,
						invoicingSystem.messageSourceForInvoice(accountInvoice), locale);
				invoices.add(accountInvoiceImpl);
				final Address address = accountInvoice.getAddress();
				accounts.add(new LocalizedConsolidatedInvoiceAccount(accountInvoice.getUserId(),
						(address != null ? address.getName() : ""),
						accountInvoiceImpl.getInvoiceNumber(),
						new LocalizedInvoice(accountInvoiceImpl, locale).getLocalizedAmount()));
			}
		}
		return new LocalizedConsolidatedInvoice(new ConsolidatedInvoiceImpl(invoice, invoices),
				accounts, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public @Nullable Invoice getConsolidatedInvoice(Long userId, String invoiceId, Locale locale) {
		final SnfConsolidatedInvoice invoice = getSnfConsolidatedInvoice(userId, invoiceId);
		return (invoice != null ? localizedConsolidatedInvoice(invoice, locale) : null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public @Nullable Resource renderConsolidatedInvoice(Long userId, String invoiceId,
			MimeType outputType, Locale locale) {
		final SnfConsolidatedInvoice invoice = getSnfConsolidatedInvoice(userId, invoiceId);
		return (invoice != null
				? invoicingSystem.renderConsolidatedInvoice(invoice, outputType, locale, false)
				: null);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public @Nullable Invoice getPreviewConsolidatedInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			Locale locale) {
		final SnfConsolidatedInvoice invoice = createPreviewConsolidatedInvoice(userId, options);
		return (invoice != null ? localizedConsolidatedInvoice(invoice, locale) : null);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public @Nullable Resource previewConsolidatedInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			MimeType outputType, Locale locale) {
		final SnfConsolidatedInvoice invoice = createPreviewConsolidatedInvoice(userId, options);
		return (invoice != null
				? invoicingSystem.renderConsolidatedInvoice(invoice, outputType, locale, false)
				: null);
	}

	private @Nullable SnfConsolidatedInvoice createPreviewConsolidatedInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.@Nullable InvoiceGenerationOptions options) {
		final InvoiceConsolidationResolver resolver = this.consolidationResolver;
		if ( resolver == null ) {
			return null;
		}
		final InvoiceConsolidationGroup group = resolver.groupForUser(userId);
		if ( group == null || !group.isPrimary(userId) || !group.isValid() ) {
			return null;
		}
		final Account account = accountDao.getForUser(userId);
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
		}
		final LocalDate start = previewStartDate(account, options);

		log.debug("Generating preview consolidated invoice for account {} (user {}) month {}",
				account.getId(), account.getUserId(), start);

		final SnfInvoicingOptions opts = new SnfInvoicingOptions(true,
				options != null && options.isUseAccountCredit());
		return invoicingSystem.generateConsolidatedInvoice(group, YearMonth.from(start), opts);
	}

}
