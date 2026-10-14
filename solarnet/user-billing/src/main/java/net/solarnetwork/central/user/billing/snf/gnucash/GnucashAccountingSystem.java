/* ==================================================================
 * GnucashAccountingSystem.java - 11/10/2026 2:56:07 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.user.billing.snf.domain.AccountBalance.ACCOUNT_CREDIT_ADD_KEY;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfTaxCodeResolver;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.InvoicePayment;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashSettings.CurrencySettings;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.GnucashDao;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAccount;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAddress;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCommodity;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCreditLot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCustomer;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncEntry;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncInvoice;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncInvoiceRef;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncLot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncLotSplit;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncNumeric;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSlot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSplit;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTable;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTableEntry;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTransaction;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;

/**
 * Record SNF invoices and payments in a GnuCash book stored in a SQL database.
 *
 * <p>
 * An SNF invoice is added as a posted GnuCash invoice, with the rows GnuCash
 * itself writes when posting: the invoice, with one line per invoice item or a
 * summary line, a transaction with splits to the receivable, income, and tax
 * accounts, and a lot that payments are later matched against. GnuCash
 * computes tax from the invoice lines and its tax tables, rounding each line's
 * value and then each tax account's total, and the result must match the SNF
 * invoice's tax items exactly or nothing is written.
 * </p>
 *
 * <p>
 * Account credit applied to an SNF invoice is taken from the customer's open
 * credit in GnuCash, oldest first: an unapplied payment is moved onto the new
 * invoice, splitting it if only part is needed, and a credit note is linked to
 * it with a lot link transaction, as GnuCash does when it applies credit. An
 * SNF invoice that grants credit to an account is added as a GnuCash credit
 * note.
 * </p>
 *
 * <p>
 * The GnuCash invoice ID is the SNF invoice ID, and an invoice GnuCash already
 * has is skipped, so a failed attempt can be repeated safely.
 * </p>
 *
 * <p>
 * An SNF payment is added as a GnuCash customer payment, deposited to the
 * configured payment account and applied to the invoices the SNF payment was
 * applied to. Any amount left over stays open as credit for the customer, as
 * GnuCash does with an overpayment. The SNF payment ID is saved in the
 * {@link #PAYMENT_ID_SLOT} slot of the payment transaction, and a payment
 * GnuCash already has is skipped.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class GnucashAccountingSystem implements SnfAccountingSystem {

	/** The versions of the GnuCash tables this class works with. */
	public static final Map<String, Integer> SUPPORTED_TABLE_VERSIONS = Map.ofEntries(
			Map.entry("accounts", 1), Map.entry("books", 1), Map.entry("commodities", 1),
			Map.entry("customers", 2), Map.entry("entries", 4), Map.entry("invoices", 4),
			Map.entry("lots", 2), Map.entry("slots", 4), Map.entry("splits", 5),
			Map.entry("taxtable_entries", 3), Map.entry("taxtables", 2),
			Map.entry("transactions", 4));

	/** The GnuCash type name of an invoice. */
	public static final String INVOICE_TYPE = "Invoice";

	/** The GnuCash type name of a credit note. */
	public static final String CREDIT_NOTE_TYPE = "Credit Note";

	/** The action of lot link splits. */
	public static final String LOT_LINK_ACTION = "Lot Link";

	/** The action of payment splits. */
	public static final String PAYMENT_ACTION = "Payment";

	/** The name of the transaction slot that holds the SNF payment ID. */
	public static final String PAYMENT_ID_SLOT = "snf-payment-id";

	/** The memo prefix of lot link splits, as written by GnuCash 5.10. */
	public static final String LOT_LINK_MEMO_PREFIX = "Offset between documents: ";

	/** The read-only reason of a posted invoice's transaction. */
	public static final String POSTED_READ_ONLY_REASON = "Generated from an invoice. Try unposting the invoice.";

	/** The description of a granted credit line, when no other is available. */
	public static final String DEFAULT_CREDIT_DESCRIPTION = "Account credit";

	/** The SNF invoice item metadata key for a description. */
	public static final String META_DESCRIPTION = "description";

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private static final GncNumeric NEGATIVE_ONE = new GncNumeric(-1, 1);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Clock clock;
	private final TransactionTemplate transactionTemplate;
	private final GnucashDao dao;
	private final GnucashSettings settings;
	private final SnfInvoicingSystem invoicingSystem;
	private final SnfTaxCodeResolver taxCodeResolver;
	private final TaxCodeDao taxCodeDao;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param transactionTemplate
	 *        the transaction template for the GnuCash database
	 * @param dao
	 *        the GnuCash DAO
	 * @param settings
	 *        the settings
	 * @param invoicingSystem
	 *        the invoicing system, for invoice item descriptions
	 * @param taxCodeResolver
	 *        the tax code resolver, to find the items taxed on an invoice
	 * @param taxCodeDao
	 *        the tax code DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GnucashAccountingSystem(Clock clock, TransactionTemplate transactionTemplate,
			GnucashDao dao, GnucashSettings settings, SnfInvoicingSystem invoicingSystem,
			SnfTaxCodeResolver taxCodeResolver, TaxCodeDao taxCodeDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.transactionTemplate = requireNonNullArgument(transactionTemplate,
				"transactionTemplate");
		this.dao = requireNonNullArgument(dao, "dao");
		this.settings = requireNonNullArgument(settings, "settings");
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
		this.taxCodeResolver = requireNonNullArgument(taxCodeResolver, "taxCodeResolver");
		this.taxCodeDao = requireNonNullArgument(taxCodeDao, "taxCodeDao");
	}

	/**
	 * The GnuCash customer, currency, and receivable account to record SNF
	 * invoices or payments for one account with.
	 */
	private record Ledger(GncCustomer customer, GncCommodity currency,
			CurrencySettings currencySettings, GncAccount receivable) {

	}

	/**
	 * The details of an SNF invoice being recorded.
	 */
	private record Doc(SnfInvoice invoice, Account account, String id, Instant now,
			GncCustomer customer, GncCommodity currency, CurrencySettings currencySettings,
			GncAccount receivable) {

	}

	/**
	 * A line to add to a GnuCash invoice.
	 *
	 * @param amount
	 *        the line amount: the quantity multiplied by the price, as a
	 *        positive amount
	 */
	private record Line(String description, GncNumeric quantity, GncNumeric price,
			BigDecimal amount, boolean taxable, boolean taxIncluded, String accountGuid) {

	}

	/**
	 * The totals of a GnuCash invoice, computed the way GnuCash does, as
	 * positive amounts.
	 */
	private record Totals(Map<String, BigDecimal> netByAccount, Map<String, BigDecimal> taxByAccount,
			BigDecimal net, BigDecimal tax) {

		private BigDecimal total() {
			return net.add(tax);
		}

	}

	/**
	 * A posted GnuCash invoice or credit note.
	 */
	private record Posting(String lotGuid, String title, Instant postDate) {

	}

	@Override
	public boolean syncInvoice(SnfInvoice invoice, Account account) {
		requireNonNullArgument(invoice, "invoice");
		requireNonNullArgument(account, "account");
		final Boolean result = transactionTemplate.execute(_ -> sync(invoice, account));
		return (result != null && result);
	}

	@Override
	public boolean syncPayment(Payment payment, Collection<InvoicePayment> invoicePayments,
			Account account) {
		requireNonNullArgument(payment, "payment");
		requireNonNullArgument(invoicePayments, "invoicePayments");
		requireNonNullArgument(account, "account");
		final Boolean result = transactionTemplate
				.execute(_ -> recordPayment(payment, invoicePayments, account));
		return (result != null && result);
	}

	private boolean sync(SnfInvoice invoice, Account account) {
		dao.lockBook();
		verifyTableVersions();

		final String id = invoice.getInvoiceId().toString();
		final String customerId = settings.customerId(invoice.getUserId());
		final GncCustomer existingCustomer = existingCustomer(customerId);

		// skip an invoice that is already in GnuCash, such as one added by an earlier attempt
		final List<GncInvoiceRef> existing = dao.invoices(id);
		if ( !existing.isEmpty() ) {
			if ( existingCustomer != null && existing.stream()
					.anyMatch(e -> existingCustomer.guid().equals(e.ownerGuid())) ) {
				log.info("SNF invoice {} for user {} is already in GnuCash, owned by customer {}",
						id, invoice.getUserId(), customerId);
				return false;
			}
			throw new GnucashSyncException(
					"GnuCash already has a document with ID %s that does not belong to customer %s."
							.formatted(id, customerId));
		}

		final Ledger ledger = ledger(existingCustomer, customerId, account,
				invoice.getCurrencyCode());
		final Doc doc = new Doc(invoice, account, id, clock.instant(), ledger.customer(),
				ledger.currency(), ledger.currencySettings(), ledger.receivable());
		if ( isCreditGrant(invoice) ) {
			addCreditNote(doc);
		} else {
			addInvoice(doc);
		}
		return true;
	}

	private @Nullable GncCustomer existingCustomer(String customerId) {
		final List<GncCustomer> customers = dao.customers(customerId);
		if ( customers.size() > 1 ) {
			throw new GnucashSyncException(
					"GnuCash has %d customers with ID %s.".formatted(customers.size(), customerId));
		}
		return (customers.isEmpty() ? null : customers.getFirst());
	}

	private Ledger ledger(@Nullable GncCustomer existingCustomer, String customerId,
			Account account, String currencyCode) {
		final CurrencySettings cs = settings.currency(currencyCode);
		if ( cs == null ) {
			throw new GnucashSyncException(
					"No GnuCash settings are configured for currency %s.".formatted(currencyCode));
		}
		final GncCommodity currency = dao.currency(currencyCode);
		if ( currency == null ) {
			throw new GnucashSyncException("GnuCash has no currency %s.".formatted(currencyCode));
		}
		final GncAccount receivable = account(cs.getReceivableAccount(), "receivable", currency);
		if ( !GncAccount.TYPE_RECEIVABLE.equals(receivable.type()) ) {
			throw new GnucashSyncException("GnuCash account %s is not a receivable account."
					.formatted(receivable.path()));
		}

		final GncCustomer customer;
		if ( existingCustomer != null ) {
			if ( !existingCustomer.currencyGuid().equals(currency.guid()) ) {
				throw new GnucashSyncException("GnuCash customer %s does not use currency %s."
						.formatted(customerId, currencyCode));
			}
			customer = existingCustomer;
		} else if ( settings.isCreateCustomers() ) {
			customer = createCustomer(customerId, account, currency, cs);
		} else {
			throw new GnucashSyncException("GnuCash has no customer %s.".formatted(customerId));
		}
		return new Ledger(customer, currency, cs, receivable);
	}

	private void verifyTableVersions() {
		final Map<String, Integer> versions = dao.tableVersions();
		for ( Map.Entry<String, Integer> e : SUPPORTED_TABLE_VERSIONS.entrySet() ) {
			final Integer version = versions.get(e.getKey());
			if ( !e.getValue().equals(version) ) {
				throw new GnucashSyncException(
						"GnuCash table %s is version %s, but only version %d is supported."
								.formatted(e.getKey(), version, e.getValue()));
			}
		}
	}

	private GncAccount account(@Nullable String path, String role, GncCommodity currency) {
		if ( path == null ) {
			throw new GnucashSyncException("No GnuCash %s account is configured for currency %s."
					.formatted(role, currency.mnemonic()));
		}
		final GncAccount account = dao.account(path);
		if ( account == null ) {
			throw new GnucashSyncException("GnuCash has no account %s.".formatted(path));
		}
		if ( !currency.guid().equals(account.commodityGuid()) ) {
			throw new GnucashSyncException("GnuCash account %s does not use currency %s."
					.formatted(path, currency.mnemonic()));
		}
		return account;
	}

	private GncCustomer createCustomer(String customerId, Account account, GncCommodity currency,
			CurrencySettings cs) {
		final String taxTableName = cs.getTaxTable();
		final GncTaxTable taxTable = (taxTableName != null ? dao.taxTable(taxTableName) : null);
		if ( taxTableName != null && taxTable == null ) {
			throw new GnucashSyncException("GnuCash has no tax table %s.".formatted(taxTableName));
		}
		final Address addr = account.getAddress();
		final String name = (addr != null && addr.getName() != null && !addr.getName().isBlank()
				? addr.getName().trim()
				: customerId);
		final GncCustomer customer = new GncCustomer(guid(), customerId, name, "", true,
				currency.guid(), taxTable != null, GncCustomer.TAX_INCLUDED_USE_GLOBAL,
				(taxTable != null ? taxTable.guid() : null), gncAddress(addr, name),
				GncAddress.EMPTY);
		dao.saveCustomer(customer);
		if ( taxTable != null ) {
			dao.incrementTaxTableRefcount(taxTable.guid());
		}
		log.info("Created GnuCash customer {} [{}] for billing account {}", customerId, name,
				account.getAccountId());
		return customer;
	}

	private static GncAddress gncAddress(@Nullable Address addr, String name) {
		if ( addr == null ) {
			return new GncAddress(name, "", "", "", "", "", "", "");
		}
		final List<String> lines = new ArrayList<>(6);
		final String[] street = addr.getStreet();
		if ( street != null ) {
			for ( String s : street ) {
				if ( s != null && !s.isBlank() ) {
					lines.add(s.trim());
				}
			}
		}
		final String city = Stream.of(addr.getLocality(), addr.getStateOrProvince(), addr.getPostalCode())
				.filter(s -> s != null && !s.isBlank()).map(String::trim).collect(joining(" "));
		if ( !city.isEmpty() ) {
			lines.add(city);
		}
		if ( addr.getCountry() != null && !addr.getCountry().isBlank() ) {
			lines.add(Locale.of("", addr.getCountry().trim()).getDisplayCountry(Locale.ENGLISH));
		}
		// GnuCash has four address lines, so join any extra lines onto the last one
		while ( lines.size() > 4 ) {
			lines.set(3, lines.get(3) + ", " + lines.remove(4));
		}
		while ( lines.size() < 4 ) {
			lines.add("");
		}
		return new GncAddress(name, lines.get(0), lines.get(1), lines.get(2), lines.get(3), "", "",
				(addr.getEmail() != null ? addr.getEmail() : ""));
	}

	private static boolean isCreditGrant(SnfInvoice invoice) {
		final Set<SnfInvoiceItem> items = invoice.getItems();
		if ( items == null || items.isEmpty() ) {
			return false;
		}
		final long grants = items.stream().filter(i -> ACCOUNT_CREDIT_ADD_KEY.equals(i.getKey()))
				.count();
		if ( grants == 0 ) {
			return false;
		}
		if ( grants != items.size() ) {
			throw new GnucashSyncException(
					"SNF invoice %d grants account credit and also has other items."
							.formatted(invoice.getInvoiceId()));
		}
		return true;
	}

	private static List<SnfInvoiceItem> sortedItems(SnfInvoice invoice) {
		final Set<SnfInvoiceItem> items = invoice.getItems();
		return (items != null ? items.stream().sorted(SnfInvoiceItem.DEFAULT_ITEM_ORDER).toList()
				: List.of());
	}

	private static BigDecimal sum(List<SnfInvoiceItem> items, @Nullable InvoiceItemType type) {
		return items.stream().filter(i -> type == null || i.getItemType() == type)
				.map(SnfInvoiceItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private void addInvoice(Doc doc) {
		final SnfInvoice invoice = doc.invoice();
		final List<SnfInvoiceItem> items = sortedItems(invoice);
		final List<SnfInvoiceItem> charges = items.stream()
				.filter(i -> i.getItemType() != InvoiceItemType.Tax
						&& i.getItemType() != InvoiceItemType.Credit)
				.toList();
		if ( charges.isEmpty() ) {
			throw new GnucashSyncException(
					"SNF invoice %s has no charges to record.".formatted(doc.id()));
		}
		final BigDecimal snfTax = sum(items, InvoiceItemType.Tax);
		final BigDecimal credit = sum(items, InvoiceItemType.Credit).negate();
		if ( credit.signum() < 0 ) {
			throw new GnucashSyncException(
					"SNF invoice %s has a negative account credit.".formatted(doc.id()));
		}

		final GncAccount income = account(doc.currencySettings().getIncomeAccount(), "income",
				doc.currency());
		final GncTaxTable taxTable = postedTaxTable(doc);
		final List<Line> lines = invoiceLines(doc, charges, income, taxTable);
		final Totals totals = totals(doc, lines, taxTable);
		if ( totals.tax().compareTo(snfTax) != 0 ) {
			throw new GnucashSyncException(
					"GnuCash would compute %s tax on SNF invoice %s, but the invoice has %s tax."
							.formatted(totals.tax().toPlainString(), doc.id(),
									snfTax.toPlainString()));
		}
		if ( totals.total().subtract(credit).compareTo(invoice.getTotalAmount()) != 0 ) {
			throw new GnucashSyncException(
					"GnuCash would compute a total of %s less %s credit for SNF invoice %s, but the invoice total is %s."
							.formatted(totals.total().toPlainString(), credit.toPlainString(),
									doc.id(), invoice.getTotalAmount().toPlainString()));
		}

		final List<GncCreditLot> creditLots = (credit.signum() > 0
				? dao.openCreditLots(doc.customer().guid(), doc.receivable().guid())
				: List.of());
		final BigDecimal available = creditLots.stream().map(GncCreditLot::credit)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if ( credit.compareTo(available) > 0 ) {
			throw new GnucashSyncException(
					"SNF invoice %s applies %s of account credit, but GnuCash customer %s has only %s of open credit."
							.formatted(doc.id(), credit.toPlainString(), doc.customer().id(),
									available.toPlainString()));
		}

		final ZoneId zone = settings.getTimeZone();
		final LocalDate date = LocalDate.ofInstant(nonnull(invoice.getCreated(), "Invoice created"),
				zone);
		final Posting posting = post(doc, false, lines, totals, taxTable, date,
				dayNeutral(date.plus(settings.getDuePeriod()), zone), invoice.getStartDate());
		if ( credit.signum() > 0 ) {
			applyCredit(doc, posting, credit, creditLots);
		}
		log.info(
				"Added SNF invoice {} for user {} to GnuCash for customer {}: {} lines, total {} {} including {} tax, less {} credit",
				doc.id(), invoice.getUserId(), doc.customer().id(), lines.size(),
				totals.total().toPlainString(), doc.currency().mnemonic(),
				totals.tax().toPlainString(), credit.toPlainString());
	}

	private List<Line> invoiceLines(Doc doc, List<SnfInvoiceItem> charges, GncAccount income,
			@Nullable GncTaxTable taxTable) {
		// lines are taxable when SNF taxed the item, or when the tax table charges no tax, as for
		// zero-rated supplies, so the zero tax is still recorded
		final boolean zeroRated = (taxTable != null
				&& taxTable.entries().stream().allMatch(e -> e.amount().isZero()));
		final Set<String> taxedKeys = (taxTable != null && !zeroRated
				? snfTaxedItemKeys(doc.invoice())
				: Set.of());
		final List<Line> lines = new ArrayList<>(charges.size());
		if ( settings.isItemized() ) {
			final MessageSource messages = invoicingSystem.messageSourceForInvoice(doc.invoice());
			final Locale locale = doc.account().locale();
			for ( SnfInvoiceItem item : charges ) {
				final boolean taxable = taxTable != null
						&& (zeroRated || taxedKeys.contains(item.getKey().toLowerCase(Locale.ROOT)));
				lines.add(new Line(itemDescription(item, messages, locale), GncNumeric.ONE,
						GncNumeric.of(item.getAmount()), item.getAmount(), taxable, false,
						income.guid()));
			}
		} else {
			// one line for the taxable items, and one for the rest
			final Map<Boolean, BigDecimal> amounts = new LinkedHashMap<>(2);
			for ( SnfInvoiceItem item : charges ) {
				final boolean taxable = taxTable != null
						&& (zeroRated || taxedKeys.contains(item.getKey().toLowerCase(Locale.ROOT)));
				amounts.merge(taxable, item.getAmount(), BigDecimal::add);
			}
			final String description = Objects.requireNonNullElse(
					doc.currencySettings().getSummaryDescription(), "SolarNetwork usage");
			for ( Map.Entry<Boolean, BigDecimal> e : amounts.entrySet() ) {
				lines.add(new Line(description, GncNumeric.ONE, GncNumeric.of(e.getValue()),
						e.getValue(), e.getKey(), false, income.guid()));
			}
		}
		return lines;
	}

	private static String itemDescription(SnfInvoiceItem item, MessageSource messages,
			Locale locale) {
		// the GnuCash book is for SolarNetwork itself, so fall back to English for an account
		// locale without messages
		String desc = messages.getMessage(item.getKey() + ".item", null, null, locale);
		if ( desc == null && !Locale.ENGLISH.equals(locale) ) {
			desc = messages.getMessage(item.getKey() + ".item", null, null, Locale.ENGLISH);
		}
		if ( desc == null ) {
			desc = item.getKey();
		}
		final BigDecimal qty = item.getQuantity();
		if ( qty.compareTo(BigDecimal.ONE) == 0 ) {
			return desc;
		}
		return desc + " (" + NumberFormat.getNumberInstance(locale).format(qty) + ")";
	}

	private Set<String> snfTaxedItemKeys(SnfInvoice invoice) {
		final Set<String> keys = new HashSet<>(8);
		for ( TaxCode code : taxCodeDao.findFiltered(taxCodeResolver.taxCodeFilterForInvoice(invoice)) ) {
			keys.add(code.getItemKey().toLowerCase(Locale.ROOT));
		}
		return keys;
	}

	private @Nullable GncTaxTable postedTaxTable(Doc doc) {
		final GncCustomer customer = doc.customer();
		GncTaxTable table = null;
		if ( customer.taxOverride() && customer.taxTableGuid() != null ) {
			table = dao.taxTableForGuid(customer.taxTableGuid());
		}
		if ( table == null ) {
			final String name = doc.currencySettings().getTaxTable();
			if ( name == null ) {
				return null;
			}
			table = dao.taxTable(name);
			if ( table == null ) {
				throw new GnucashSyncException("GnuCash has no tax table %s.".formatted(name));
			}
		}
		if ( table.invisible() || table.parentGuid() != null ) {
			// already a posted copy
			return table;
		}
		// GnuCash points posted lines at a hidden copy of the table, so later changes to the table do
		// not change posted invoices
		for ( GncTaxTable copy : dao.postedTaxTableCopies(table.guid()) ) {
			if ( copy.hasSameEntries(table) ) {
				return copy;
			}
		}
		final GncTaxTable copy = table.postedCopy(guid());
		dao.saveTaxTable(copy);
		return copy;
	}

	private Totals totals(Doc doc, List<Line> lines, @Nullable GncTaxTable taxTable) {
		final int scale = doc.currency().scale();
		final Map<String, BigDecimal> net = new LinkedHashMap<>(2);
		final Map<String, BigDecimal> tax = new LinkedHashMap<>(2);
		for ( Line line : lines ) {
			BigDecimal pretax = line.amount();
			if ( line.taxable() && taxTable != null ) {
				BigDecimal percent = BigDecimal.ZERO;
				for ( GncTaxTableEntry entry : taxTable.entries() ) {
					if ( !entry.isPercent() ) {
						throw new GnucashSyncException(
								"GnuCash tax table %s has a fixed amount tax, which is not supported."
										.formatted(taxTable.name()));
					}
					percent = percent.add(entry.amount().toBigDecimal());
				}
				if ( line.taxIncluded() ) {
					pretax = pretax.divide(
							BigDecimal.ONE.add(percent.divide(ONE_HUNDRED, MathContext.DECIMAL128)),
							MathContext.DECIMAL128);
				}
				for ( GncTaxTableEntry entry : taxTable.entries() ) {
					tax.merge(entry.accountGuid(),
							pretax.multiply(entry.amount().toBigDecimal()).divide(ONE_HUNDRED,
									MathContext.DECIMAL128),
							BigDecimal::add);
				}
			}
			// GnuCash rounds each line's value, but only the total of each tax account
			net.merge(line.accountGuid(), pretax.setScale(scale, RoundingMode.HALF_UP),
					BigDecimal::add);
		}
		tax.replaceAll((_, v) -> v.setScale(scale, RoundingMode.HALF_UP));
		return new Totals(net, tax, net.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
				tax.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	private Posting post(Doc doc, boolean creditNote, List<Line> lines, Totals totals,
			@Nullable GncTaxTable taxTable, LocalDate date, Instant dueDate, LocalDate lineDate) {
		final ZoneId zone = settings.getTimeZone();
		final String type = (creditNote ? CREDIT_NOTE_TYPE : INVOICE_TYPE);
		final String title = type + " " + doc.id();
		final long fraction = doc.currency().fraction();
		final String invoiceGuid = guid();
		final String txnGuid = guid();
		final String lotGuid = guid();
		final Instant postDate = dayNeutral(date, zone);
		final Instant lineInstant = localNoon(lineDate, zone);

		dao.saveInvoice(new GncInvoice(invoiceGuid, doc.id(), postDate, postDate, "", true,
				doc.currency().guid(), GncInvoice.OWNER_TYPE_CUSTOMER, doc.customer().guid(),
				Objects.requireNonNullElse(
						SnfBillingUtils.invoiceNumForId(doc.invoice().getInvoiceId()), ""),
				txnGuid, lotGuid, doc.receivable().guid()));
		dao.saveSlot(GncSlot.int64(invoiceGuid, "credit-note", creditNote ? 1 : 0));
		for ( Line line : lines ) {
			dao.saveEntry(new GncEntry(guid(), lineInstant, doc.now(), line.description(), "", "",
					line.quantity(), line.accountGuid(), line.price(), line.taxable(),
					line.taxIncluded(), (line.taxable() && taxTable != null ? taxTable.guid() : null),
					invoiceGuid));
		}

		dao.saveTransaction(new GncTransaction(txnGuid, doc.currency().guid(), doc.id(), postDate,
				doc.now(), doc.customer().name()));
		dao.saveSlot(GncSlot.gdate(txnGuid, "date-posted", date));
		saveFrameGuid(txnGuid, "gncInvoice", "invoice-guid", invoiceGuid);
		dao.saveSlot(GncSlot.timespec(txnGuid, "trans-date-due", dueDate));
		dao.saveSlot(GncSlot.string(txnGuid, "trans-read-only", POSTED_READ_ONLY_REASON));
		dao.saveSlot(GncSlot.string(txnGuid, "trans-txn-type", GncTransaction.TXN_TYPE_INVOICE));

		// an invoice credits income and tax, and a credit note debits them
		final BigDecimal sign = (creditNote ? BigDecimal.ONE : BigDecimal.ONE.negate());
		for ( Map.Entry<String, BigDecimal> e : totals.netByAccount().entrySet() ) {
			final GncNumeric value = GncNumeric.of(e.getValue().multiply(sign), fraction);
			dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, e.getKey(), "", type, value, value,
					null));
		}
		for ( Map.Entry<String, BigDecimal> e : totals.taxByAccount().entrySet() ) {
			final GncNumeric value = GncNumeric.of(e.getValue().multiply(sign), fraction);
			dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, e.getKey(), "", type, value,
					taxAmount(doc, e.getKey(), value), null));
		}
		final GncNumeric total = GncNumeric.of(totals.total().multiply(sign.negate()), fraction);
		dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, doc.receivable().guid(), "", type,
				total, total, lotGuid));

		dao.saveLot(new GncLot(lotGuid, doc.receivable().guid(), GncLot.CLOSED_UNKNOWN));
		saveFrameGuid(lotGuid, "gncInvoice", "invoice-guid", invoiceGuid);
		dao.saveSlot(GncSlot.string(lotGuid, "title", title));
		return new Posting(lotGuid, title, postDate);
	}

	/**
	 * Get the amount of a tax split, in the tax account's commodity.
	 *
	 * <p>
	 * A tax account can use a different currency from the invoice, like an NZD
	 * tax account used for the zero-rated tax of a USD invoice. A zero tax is
	 * zero in any currency, so no exchange rate is needed for it.
	 * </p>
	 */
	private GncNumeric taxAmount(Doc doc, String accountGuid, GncNumeric value) {
		final GncAccount account = dao.accountForGuid(accountGuid);
		if ( account == null ) {
			throw new GnucashSyncException("GnuCash has no tax account %s.".formatted(accountGuid));
		}
		if ( doc.currency().guid().equals(account.commodityGuid()) ) {
			return value;
		}
		if ( value.isZero() ) {
			return new GncNumeric(0, Math.max(account.commodityScu(), 1));
		}
		throw new GnucashSyncException(
				"GnuCash tax account %s does not use currency %s, so its tax of %s needs an exchange rate."
						.formatted(account.path(), doc.currency().mnemonic(),
								value.toBigDecimal().toPlainString()));
	}

	private void saveFrameGuid(String objGuid, String frame, String key, String value) {
		final String frameGuid = guid();
		dao.saveSlot(GncSlot.frame(objGuid, frame, frameGuid));
		dao.saveSlot(GncSlot.guid(frameGuid, frame + "/" + key, value));
	}

	private void applyCredit(Doc doc, Posting posting, BigDecimal credit, List<GncCreditLot> lots) {
		BigDecimal remaining = credit;
		for ( GncCreditLot lot : lots ) {
			if ( remaining.signum() < 1 ) {
				break;
			}
			if ( lot.isDocument() ) {
				final BigDecimal amount = remaining.min(lot.credit());
				linkLots(doc, posting, lot, amount);
				remaining = remaining.subtract(amount);
			} else {
				remaining = applyPayment(posting, lot, remaining);
			}
		}
		if ( remaining.signum() > 0 ) {
			throw new GnucashSyncException(
					"Could not apply %s of the account credit on SNF invoice %s from GnuCash customer %s."
							.formatted(remaining.toPlainString(), doc.id(), doc.customer().id()));
		}
	}

	/**
	 * Move a payment's splits from its lot onto an invoice lot, as GnuCash
	 * does when it applies a payment to an invoice.
	 *
	 * @return the amount still to apply
	 */
	private BigDecimal applyPayment(Posting posting, GncCreditLot lot, BigDecimal amount) {
		final List<GncLotSplit> candidates = new ArrayList<>(
				lot.splits().stream().filter(s -> s.split().quantity().num() < 0).toList());
		BigDecimal remaining = amount;
		int moved = 0;
		boolean reduced = false;
		while ( remaining.signum() > 0 && !candidates.isEmpty() ) {
			final GncLotSplit best = bestOffsettingSplit(candidates, remaining);
			candidates.remove(best);
			final GncSplit split = best.split();
			final BigDecimal splitCredit = split.quantity().toBigDecimal().negate();
			if ( splitCredit.compareTo(remaining) <= 0 ) {
				dao.updateSplit(split.guid(), split.value(), split.quantity(), posting.lotGuid());
				remaining = remaining.subtract(splitCredit);
				moved++;
			} else {
				// reduce the split to what is needed, leaving the rest of the payment in its lot
				final GncNumeric quantity = GncNumeric.of(remaining.negate(),
						split.quantity().denom());
				final GncNumeric value = reducedValue(split, quantity);
				dao.updateSplit(split.guid(), value, quantity, posting.lotGuid());
				dao.saveSplit(GncSplit.unreconciled(guid(), split.txGuid(), split.accountGuid(),
						split.memo(), split.action(), subtract(split.value(), value),
						subtract(split.quantity(), quantity), lot.guid()));
				remaining = BigDecimal.ZERO;
				reduced = true;
			}
		}
		if ( !reduced && moved == lot.splits().size() ) {
			// GnuCash deletes a lot left with no splits
			dao.deleteLot(lot.guid());
		}
		return remaining;
	}

	/**
	 * Choose the split to apply next, as GnuCash does: prefer a split that
	 * exactly matches the amount, then a bigger one, then the biggest smaller
	 * one, and prefer payments over lot links.
	 */
	private static GncLotSplit bestOffsettingSplit(List<GncLotSplit> candidates,
			BigDecimal target) {
		GncLotSplit best = candidates.getFirst();
		int bestFlags = -1;
		BigDecimal bestAmount = BigDecimal.ZERO;
		for ( GncLotSplit candidate : candidates ) {
			final BigDecimal amount = candidate.split().quantity().toBigDecimal().negate();
			final int cmp = amount.compareTo(target);
			final int flags = (cmp == 0 ? 8 : cmp > 0 ? 4 : 2)
					+ (GncTransaction.TXN_TYPE_LINK.equals(candidate.txnType()) ? 0 : 1);
			if ( flags > bestFlags || (flags == bestFlags && amount.compareTo(bestAmount) > 0) ) {
				best = candidate;
				bestFlags = flags;
				bestAmount = amount;
			}
		}
		return best;
	}

	private static GncNumeric reducedValue(GncSplit split, GncNumeric quantity) {
		final BigDecimal amount = split.quantity().toBigDecimal();
		final BigDecimal value = split.value().toBigDecimal();
		if ( amount.compareTo(value) == 0 ) {
			return GncNumeric.of(quantity.toBigDecimal(), split.value().denom());
		}
		// the payment was in another currency, so scale the value by the same exchange rate
		final BigDecimal reduced = quantity.toBigDecimal().multiply(value)
				.divide(amount, MathContext.DECIMAL128);
		return GncNumeric.of(reduced.setScale(scale(split.value().denom()), RoundingMode.HALF_UP),
				split.value().denom());
	}

	private static int scale(long denom) {
		return BigDecimal.valueOf(denom).precision() - 1;
	}

	private static GncNumeric subtract(GncNumeric a, GncNumeric b) {
		return GncNumeric.of(a.toBigDecimal().subtract(b.toBigDecimal()), a.denom());
	}

	private void linkLots(Doc doc, Posting posting, GncCreditLot creditLot, BigDecimal amount) {
		final String txnGuid = guid();
		final Instant postDate = (creditLot.lastPostDate().isAfter(posting.postDate())
				? creditLot.lastPostDate()
				: posting.postDate());
		dao.saveTransaction(new GncTransaction(txnGuid, doc.currency().guid(), "", postDate,
				doc.now(), doc.customer().name()));
		dao.saveSlot(GncSlot.string(txnGuid, "trans-txn-type", GncTransaction.TXN_TYPE_LINK));
		final String memo = LOT_LINK_MEMO_PREFIX
				+ Stream.of(posting.title(), Objects.requireNonNullElse(creditLot.documentTitle(), ""))
						.sorted().collect(joining(" - "));
		final GncNumeric value = GncNumeric.of(amount, doc.currency().fraction());
		dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, doc.receivable().guid(), memo,
				LOT_LINK_ACTION, value.negate(), value.negate(), posting.lotGuid()));
		dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, doc.receivable().guid(), memo,
				LOT_LINK_ACTION, value, value, creditLot.guid()));
	}

	private void addCreditNote(Doc doc) {
		final SnfInvoice invoice = doc.invoice();
		final List<SnfInvoiceItem> items = sortedItems(invoice);
		final BigDecimal amount = sum(items, null).negate();
		if ( amount.signum() < 1 ) {
			throw new GnucashSyncException(
					"SNF invoice %s grants no account credit.".formatted(doc.id()));
		}
		final CurrencySettings cs = doc.currencySettings();
		final GncAccount account = account(
				(cs.getCreditAccount() != null ? cs.getCreditAccount() : cs.getIncomeAccount()),
				"credit", doc.currency());
		final GncTaxTable taxTable = (settings.isCreditNoteTaxable() ? postedTaxTable(doc) : null);
		final boolean taxable = (taxTable != null);
		final Line line = new Line(creditDescription(invoice, items.getFirst()), NEGATIVE_ONE,
				GncNumeric.of(amount), amount, taxable, taxable, account.guid());
		final Totals totals = totals(doc, List.of(line), taxTable);
		if ( totals.total().compareTo(amount) != 0 ) {
			throw new GnucashSyncException(
					"GnuCash would compute a total of %s for the credit note for SNF invoice %s, not %s."
							.formatted(totals.total().toPlainString(), doc.id(),
									amount.toPlainString()));
		}
		final LocalDate date = invoice.getStartDate();
		post(doc, true, List.of(line), totals, taxTable, date,
				dayNeutral(date, settings.getTimeZone()), date);
		log.info("Added SNF credit invoice {} for user {} to GnuCash as a credit note for {} {}",
				doc.id(), invoice.getUserId(), amount.toPlainString(), doc.currency().mnemonic());
	}

	private String creditDescription(SnfInvoice invoice, SnfInvoiceItem item) {
		final Map<String, Object> meta = item.getMetadata();
		final Object desc = (meta != null ? meta.get(META_DESCRIPTION) : null);
		if ( desc != null && !desc.toString().isBlank() ) {
			return desc.toString().trim();
		}
		final MessageSource messages = invoicingSystem.messageSourceForInvoice(invoice);
		return Objects.requireNonNullElse(
				messages.getMessage(item.getKey() + ".item", null, null, Locale.ENGLISH),
				DEFAULT_CREDIT_DESCRIPTION);
	}

	private boolean recordPayment(Payment payment, Collection<InvoicePayment> invoicePayments,
			Account account) {
		dao.lockBook();
		verifyTableVersions();

		// skip a payment that is already in GnuCash, such as one added by an earlier attempt
		final String id = nonnull(payment.getId(), "Payment ID").getUuid().toString();
		if ( !dao.objectsWithSlot(PAYMENT_ID_SLOT, id).isEmpty() ) {
			log.info("SNF payment {} for user {} is already in GnuCash", id, payment.getUserId());
			return false;
		}
		if ( payment.getPaymentType() != PaymentType.Payment ) {
			throw new GnucashSyncException("SNF payment %s is a %s, which is not supported."
					.formatted(id, payment.getPaymentType()));
		}
		if ( payment.getAmount().signum() < 1 ) {
			throw new GnucashSyncException(
					"SNF payment %s has no amount to record.".formatted(id));
		}

		final String customerId = settings.customerId(payment.getUserId());
		final Ledger ledger = ledger(existingCustomer(customerId), customerId, account,
				payment.getCurrencyCode());
		final GncCommodity currency = ledger.currency();
		final GncAccount deposit = account(ledger.currencySettings().getPaymentAccount(),
				"payment", currency);

		// the amount paid on each invoice, by invoice ID, and then by the GUID of the invoice's lot
		final Map<Long, BigDecimal> invoiceAmounts = new TreeMap<>();
		for ( InvoicePayment ip : invoicePayments ) {
			final BigDecimal amount = ip.getAmount();
			if ( amount != null && amount.signum() > 0 ) {
				invoiceAmounts.merge(ip.getInvoiceId(), amount, BigDecimal::add);
			}
		}
		final Map<String, BigDecimal> lotAmounts = new LinkedHashMap<>(invoiceAmounts.size());
		for ( Map.Entry<Long, BigDecimal> e : invoiceAmounts.entrySet() ) {
			lotAmounts.put(paidInvoiceLot(id, ledger, e.getKey(), e.getValue()), e.getValue());
		}
		final BigDecimal applied = invoiceAmounts.values().stream().reduce(BigDecimal.ZERO,
				BigDecimal::add);
		final BigDecimal unapplied = payment.getAmount().subtract(applied);
		if ( unapplied.signum() < 0 ) {
			throw new GnucashSyncException(
					"SNF payment %s applies %s to invoices, more than its amount of %s."
							.formatted(id, applied.toPlainString(),
									payment.getAmount().toPlainString()));
		}

		// GnuCash dates the payment on the day it was made, in the account's time zone
		final ZoneId zone = Objects.requireNonNullElse(account.getTimeZone(),
				settings.getTimeZone());
		final LocalDate date = LocalDate.ofInstant(nonnull(payment.getCreated(), "Payment created"),
				zone);
		final long fraction = currency.fraction();
		final String txnGuid = guid();
		final String ref = payment.getReference();
		dao.saveTransaction(new GncTransaction(txnGuid, currency.guid(),
				(ref != null ? ref.trim() : ""), dayNeutral(date, settings.getTimeZone()),
				clock.instant(), ledger.customer().name()));
		dao.saveSlot(GncSlot.string(txnGuid, "trans-txn-type", GncTransaction.TXN_TYPE_PAYMENT));
		dao.saveSlot(GncSlot.string(txnGuid, PAYMENT_ID_SLOT, id));

		final GncNumeric total = GncNumeric.of(payment.getAmount(), fraction);
		dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, deposit.guid(), "", PAYMENT_ACTION,
				total, total, null));
		for ( Map.Entry<String, BigDecimal> e : lotAmounts.entrySet() ) {
			final GncNumeric value = GncNumeric.of(e.getValue().negate(), fraction);
			dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, ledger.receivable().guid(), "",
					PAYMENT_ACTION, value, value, e.getKey()));
		}
		if ( unapplied.signum() > 0 ) {
			// GnuCash keeps the part of a payment not applied to an invoice in a lot of its own,
			// owned by the customer, which is the customer's open credit
			final String lotGuid = guid();
			dao.saveLot(new GncLot(lotGuid, ledger.receivable().guid(), GncLot.CLOSED_UNKNOWN));
			final String frameGuid = guid();
			dao.saveSlot(GncSlot.frame(lotGuid, "gncOwner", frameGuid));
			dao.saveSlot(GncSlot.int64(frameGuid, "gncOwner/owner-type",
					GncInvoice.OWNER_TYPE_CUSTOMER));
			dao.saveSlot(GncSlot.guid(frameGuid, "gncOwner/owner-guid", ledger.customer().guid()));
			final GncNumeric value = GncNumeric.of(unapplied.negate(), fraction);
			dao.saveSplit(GncSplit.unreconciled(guid(), txnGuid, ledger.receivable().guid(), "",
					PAYMENT_ACTION, value, value, lotGuid));
		}
		log.info(
				"Added SNF payment {} for user {} to GnuCash for customer {}: {} {} deposited to {}, {} applied to invoices {}, {} left as credit",
				id, payment.getUserId(), ledger.customer().id(),
				payment.getAmount().toPlainString(), currency.mnemonic(), deposit.path(),
				applied.toPlainString(), invoiceAmounts.keySet(), unapplied.toPlainString());
		return true;
	}

	/**
	 * Get the lot of an invoice a payment is applied to, verifying the invoice
	 * is posted and still has at least the amount paid due.
	 *
	 * @return the GUID of the invoice's lot
	 */
	private String paidInvoiceLot(String paymentId, Ledger ledger, Long invoiceId,
			BigDecimal amount) {
		final String id = invoiceId.toString();
		final GncInvoiceRef inv = dao.invoices(id).stream()
				.filter(i -> ledger.customer().guid().equals(i.ownerGuid())).findFirst()
				.orElse(null);
		if ( inv == null ) {
			throw new GnucashSyncException(
					"SNF payment %s pays invoice %s, but GnuCash customer %s has no invoice %s."
							.formatted(paymentId, id, ledger.customer().id(), id));
		}
		final String lotGuid = inv.lotGuid();
		final BigDecimal due = inv.balance();
		if ( lotGuid == null || due == null ) {
			throw new GnucashSyncException(
					"SNF payment %s pays invoice %s, but GnuCash invoice %s is not posted."
							.formatted(paymentId, id, id));
		}
		if ( !ledger.receivable().guid().equals(inv.accountGuid()) ) {
			throw new GnucashSyncException(
					"SNF payment %s pays invoice %s, but GnuCash invoice %s is not posted to %s."
							.formatted(paymentId, id, id, ledger.receivable().path()));
		}
		if ( due.compareTo(amount) < 0 ) {
			throw new GnucashSyncException(
					"SNF payment %s pays %s of invoice %s, but GnuCash invoice %s has only %s due."
							.formatted(paymentId, amount.toPlainString(), id, id,
									due.setScale(ledger.currency().scale(), RoundingMode.HALF_UP)
											.toPlainString()));
		}
		return lotGuid;
	}

	/**
	 * Get the time GnuCash uses for a date when only the date matters, such as
	 * an invoice's posted date.
	 *
	 * <p>
	 * This is 10:59 UTC, moved for time zones more than 10 hours behind or 13
	 * hours ahead of UTC, so the date is the same in nearly every time zone.
	 * </p>
	 *
	 * @param date
	 *        the date
	 * @param zone
	 *        the time zone
	 * @return the time
	 */
	public static Instant dayNeutral(LocalDate date, ZoneId zone) {
		Instant ts = date.atTime(10, 59).toInstant(ZoneOffset.UTC);
		final Duration offset = Duration.ofSeconds(zone.getRules().getOffset(ts).getTotalSeconds());
		if ( offset.compareTo(Duration.ofHours(-10)) < 0 ) {
			ts = ts.minus(offset.toHours() + 10, ChronoUnit.HOURS);
		} else if ( offset.compareTo(Duration.ofHours(13)) > 0 ) {
			ts = ts.plus(13 - offset.toHours(), ChronoUnit.HOURS);
		}
		return ts;
	}

	/**
	 * Get the time GnuCash uses for the date of an invoice line: noon in the
	 * book's time zone.
	 *
	 * @param date
	 *        the date
	 * @param zone
	 *        the time zone
	 * @return the time
	 */
	public static Instant localNoon(LocalDate date, ZoneId zone) {
		return date.atTime(12, 0).atZone(zone).toInstant();
	}

	private static String guid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
