/* ==================================================================
 * DefaultSnfInvoicingSystem.java - 1/11/2021 11:38:22 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.user.billing.snf.SnfBillingUtils.invoiceForSnfInvoice;
import static net.solarnetwork.central.user.billing.snf.SnfBillingUtils.usageMetadata;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Usage;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.CLOUD_INTEGRATIONS_DATA_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.DATUM_DAYS_STORED_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.DATUM_OUT_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.DATUM_PROPS_IN_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.DNP3_DATA_POINTS_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.FLUX_DATA_IN_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.FLUX_DATA_OUT_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.INSTRUCTIONS_ISSUED_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.OAUTH_CLIENT_CREDENTIALS_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.OCPP_CHARGERS_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.OSCP_CAPACITY_GROUPS_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.OSCP_CAPACITY_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.META_AVAILABLE_CREDIT;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.VersionedMessageDaoMessageSource;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.NamedCost;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsages;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceNodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.central.user.billing.snf.domain.UsageInfo;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.TemplateRenderer;

/**
 * Default implementation of {@link SnfInvoicingSystem}.
 *
 * @author matt
 * @version 1.8
 */
public class DefaultSnfInvoicingSystem implements SnfInvoicingSystem, SnfTaxCodeResolver {

	/** The message bundle name to use for versioned messages. */
	public static final String MESSAGE_BUNDLE_NAME = "snf.billing";

	/** The message bundle name to use for global versioned messages. */
	public static final String GLOBAL_MESSAGE_BUNDLE_NAME = "snf.global";

	/** The default {@code deliveryTimeoutSecs} property value. */
	public static final int DEFAULT_DELIVERY_TIMEOUT = 60;

	/** The invoice ID used for dry-run (draft) invoice generation. */
	public static final Long DRAFT_INVOICE_ID = Long.valueOf(Invoice.DRAFT_INVOICE_ID);

	/** The invoice number used for dry-run (draft) invoice generation. */
	public static final String DRAFT_INVOICE_NUMBER = SnfBillingUtils.invoiceNumForId(DRAFT_INVOICE_ID);

	private static final String[] MESSAGE_BUNDLE_NAMES = new String[] { GLOBAL_MESSAGE_BUNDLE_NAME,
			MESSAGE_BUNDLE_NAME };

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AccountDao accountDao;
	private final SnfInvoiceDao invoiceDao;
	private final SnfInvoiceItemDao invoiceItemDao;
	private final SnfInvoiceNodeUsageDao invoiceNodeUsageDao;
	private final NodeUsageDao usageDao;
	private final TaxCodeDao taxCodeDao;
	private final VersionedMessageDao messageDao;
	private SnfTaxCodeResolver taxCodeResolver;
	private List<SnfInvoiceDeliverer> deliveryServices;
	private List<SnfInvoiceRendererResolver> rendererResolvers;
	private Cache<String, VersionedMessages> messageCache;
	private String datumPropertiesInKey = DATUM_PROPS_IN_KEY;
	private String datumOutKey = DATUM_OUT_KEY;
	private String datumDaysStoredKey = DATUM_DAYS_STORED_KEY;
	private String instructionsIssuedKey = INSTRUCTIONS_ISSUED_KEY;
	private String fluxDataInKey = FLUX_DATA_IN_KEY;
	private String fluxDataOutKey = FLUX_DATA_OUT_KEY;
	private String ocppChargersKey = OCPP_CHARGERS_KEY;
	private String oscpCapacityGroupsKey = OSCP_CAPACITY_GROUPS_KEY;
	private String oscpCapacityKey = OSCP_CAPACITY_KEY;
	private String dnp3DataPointsKey = DNP3_DATA_POINTS_KEY;
	private String oauthClientCredentialsKey = OAUTH_CLIENT_CREDENTIALS_KEY;
	private String cloudIntegrationsDataKey = CLOUD_INTEGRATIONS_DATA_KEY;
	private String accountCreditKey = AccountBalance.ACCOUNT_CREDIT_KEY;
	private int deliveryTimeoutSecs = DEFAULT_DELIVERY_TIMEOUT;

	/**
	 * Constructor.
	 *
	 * @param accountDao
	 *        the account DAO
	 * @param invoiceDao
	 *        the invoice DAO
	 * @param invoiceItemDao
	 *        the invoice item DAO
	 * @param invoiceNodeUsageDao
	 *        the invoice node usage DAO
	 * @param usageDao
	 *        the usage DAO
	 * @param taxCodeDao
	 *        the tax code DAO
	 * @param messageDao
	 *        the message DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultSnfInvoicingSystem(AccountDao accountDao, SnfInvoiceDao invoiceDao,
			SnfInvoiceItemDao invoiceItemDao, SnfInvoiceNodeUsageDao invoiceNodeUsageDao,
			NodeUsageDao usageDao, TaxCodeDao taxCodeDao, VersionedMessageDao messageDao) {
		super();
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.invoiceDao = requireNonNullArgument(invoiceDao, "invoiceDao");
		this.invoiceItemDao = requireNonNullArgument(invoiceItemDao, "invoiceItemDao");
		this.invoiceNodeUsageDao = requireNonNullArgument(invoiceNodeUsageDao, "invoiceNodeUsageDao");
		this.usageDao = requireNonNullArgument(usageDao, "usageDao");
		this.taxCodeDao = requireNonNullArgument(taxCodeDao, "taxCodeDao");
		this.messageDao = requireNonNullArgument(messageDao, "messageDao");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Account accountForUser(Long userId) {
		return accountDao.getForUser(userId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public SnfInvoice findLatestInvoiceForAccount(UserLongPK accountId) {
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(accountId.getId());
		filter.setIgnoreCreditOnly(true);
		net.solarnetwork.dao.FilterResults<SnfInvoice, UserLongPK> results = invoiceDao
				.findFiltered(filter, SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0L, 1);
		Iterator<SnfInvoice> itr = (results != null ? results.iterator() : null);
		return (itr != null && itr.hasNext() ? itr.next() : null);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public SnfInvoice generateInvoice(Long userId, LocalDate startDate, LocalDate endDate,
			SnfInvoicingSystem.InvoiceGenerationOptions options) {
		// get account
		Account account = accountDao.getForUser(userId, endDate);
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
		}

		final boolean dryRun = (options != null && options.isDryRun());
		final boolean useCredit = (options != null ? options.isUseAccountCredit() : !dryRun);

		// query for usage
		final List<NodeUsage> usages = usageDao.findUsageForAccount(userId, startDate, endDate);
		if ( usages == null || usages.isEmpty() ) {
			// no invoice necessary
			return null;
		}

		// check for no-cost usage, and remove
		usages.removeIf(usage -> usage.getTotalCost().compareTo(BigDecimal.ZERO) == 0);
		if ( usages.isEmpty() ) {
			// only zero cost usage, no invoice necessary
			return null;
		}

		// turn usage into invoice items
		final SnfInvoice invoice = new SnfInvoice(account.getId().getId(), userId, Instant.now());
		invoice.setAddress(account.getAddress());
		invoice.setCurrencyCode(account.getCurrencyCode());
		invoice.setStartDate(startDate);
		invoice.setEndDate(endDate);

		// query for node usage counts
		final List<NodeUsage> nodeUsages = usageDao.findNodeUsageForAccount(userId, startDate, endDate);

		// for dryRun support, we generate a negative invoice ID
		final UserLongPK invoiceId = (dryRun ? new UserLongPK(userId, DRAFT_INVOICE_ID)
				: invoiceDao.save(invoice));
		invoice.getId().setId(invoiceId.getId()); // for return

		List<SnfInvoiceItem> items = new ArrayList<>(usages.size());

		for ( NodeUsage usage : usages ) {
			if ( usage.getTotalCost().compareTo(BigDecimal.ZERO) < 1 ) {
				// no cost for this node
				log.debug("No usage cost for node {} invoice date {}", usage.getId(), startDate);
				continue;
			}
			final Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			final Map<String, UsageInfo> usageInfo = usage.getUsageInfo();
			if ( usage.getDatumPropertiesIn().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, datumPropertiesInKey,
						new BigDecimal(usage.getDatumPropertiesIn()), usage.getDatumPropertiesInCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, DATUM_PROPS_IN_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getDatumOut().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, datumOutKey,
						new BigDecimal(usage.getDatumOut()), usage.getDatumOutCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, DATUM_OUT_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getDatumDaysStored().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, datumDaysStoredKey,
						new BigDecimal(usage.getDatumDaysStored()), usage.getDatumDaysStoredCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, DATUM_DAYS_STORED_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getInstructionsIssued().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, instructionsIssuedKey,
						new BigDecimal(usage.getInstructionsIssued()),
						usage.getInstructionsIssuedCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, INSTRUCTIONS_ISSUED_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getFluxDataIn().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, fluxDataInKey,
						new BigDecimal(usage.getFluxDataIn()), usage.getFluxDataInCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, FLUX_DATA_IN_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getFluxDataOut().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, fluxDataOutKey,
						new BigDecimal(usage.getFluxDataOut()), usage.getFluxDataOutCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, FLUX_DATA_OUT_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getOcppChargers().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, ocppChargersKey,
						new BigDecimal(usage.getOcppChargers()), usage.getOcppChargersCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, OCPP_CHARGERS_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getOscpCapacityGroups().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, oscpCapacityGroupsKey,
						new BigDecimal(usage.getOscpCapacityGroups()),
						usage.getOscpCapacityGroupsCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, OSCP_CAPACITY_GROUPS_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getOscpCapacity().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, oscpCapacityKey,
						new BigDecimal(usage.getOscpCapacity()), usage.getOscpCapacityCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, OSCP_CAPACITY_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getDnp3DataPoints().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, dnp3DataPointsKey,
						new BigDecimal(usage.getDnp3DataPoints()), usage.getDnp3DataPointsCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, DNP3_DATA_POINTS_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getOauthClientCredentials().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, oauthClientCredentialsKey,
						new BigDecimal(usage.getOauthClientCredentials()),
						usage.getOauthClientCredentialsCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, OAUTH_CLIENT_CREDENTIALS_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
			if ( usage.getCloudIntegrationsData().compareTo(BigInteger.ZERO) > 0 ) {
				SnfInvoiceItem item = newItem(invoiceId.getId(), Usage, cloudIntegrationsDataKey,
						new BigDecimal(usage.getCloudIntegrationsData()),
						usage.getCloudIntegrationsDataCost());
				item.setMetadata(usageMetadata(usageInfo, tiersBreakdown, CLOUD_INTEGRATIONS_DATA_KEY));
				if ( !dryRun ) {
					invoiceItemDao.save(item);
				}
				items.add(item);
			}
		}

		invoice.setItems(new LinkedHashSet<>(items));

		List<SnfInvoiceItem> taxItems = computeInvoiceTaxItems(invoice);
		for ( SnfInvoiceItem taxItem : taxItems ) {
			if ( !dryRun ) {
				invoiceItemDao.save(taxItem);
			}
			invoice.getItems().add(taxItem);
		}

		// claim credit, if available
		if ( useCredit ) {
			BigDecimal invoiceTotal = invoice.getTotalAmount();
			if ( invoiceTotal.compareTo(BigDecimal.ZERO) > 0 ) {
				BigDecimal credit = accountDao.claimAccountBalanceCredit(invoice.getAccountId(),
						invoiceTotal);
				if ( credit.compareTo(BigDecimal.ZERO) > 0 ) {
					AccountBalance balance = accountDao.getBalanceForUser(invoice.getUserId());
					SnfInvoiceItem creditItem = SnfInvoiceItem.newItem(invoice, InvoiceItemType.Credit,
							accountCreditKey, BigDecimal.ONE, credit.negate());
					creditItem.setMetadata(Collections.singletonMap(META_AVAILABLE_CREDIT,
							balance.getAvailableCredit().toPlainString()));
					if ( !dryRun ) {
						invoiceItemDao.save(creditItem);
					}
					invoice.getItems().add(creditItem);
				}
			}
		}

		// populate node usages
		if ( nodeUsages != null ) {
			nodeUsages.sort(NodeUsage.SORT_BY_NODE_ID);
			List<SnfInvoiceNodeUsage> invoiceNodeUsages = new ArrayList<>(nodeUsages.size());
			for ( NodeUsage nodeUsage : nodeUsages ) {
				SnfInvoiceNodeUsage u = new SnfInvoiceNodeUsage(invoiceId.getId(), nodeUsage.getId(),
						invoice.getCreated(), nodeUsage.getDescription(),
						nodeUsage.getDatumPropertiesIn(), nodeUsage.getDatumOut(),
						nodeUsage.getDatumDaysStored(), nodeUsage.getInstructionsIssued(),
						nodeUsage.getFluxDataIn());
				invoiceNodeUsages.add(u);
				if ( !dryRun ) {
					invoiceNodeUsageDao.save(u);
				}
			}
			invoice.setUsages(new LinkedHashSet<>(invoiceNodeUsages));
		} else {
			invoice.setUsages(Collections.emptySet());
		}

		log.info("Generated invoice for user {} for date {}: {}", userId, startDate, invoice);

		return invoice;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public boolean deliverInvoice(final UserLongPK invoiceId) {
		// get account
		final Account account = accountDao.getForUser(invoiceId.getUserId());
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, invoiceId.getUserId());
		}

		final SnfInvoice invoice = invoiceDao.get(invoiceId);
		if ( invoice == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, invoiceId);
		}

		SnfInvoiceDeliverer deliverer = invoiceDeliverer();
		if ( deliverer == null ) {
			String msg = format("No invoice delivery service available to delivery invoice %d",
					invoiceId.getId());
			log.error(msg);
			throw new RepeatableTaskException(msg);
		}

		// TODO: support configuration for account
		try {
			final int maxSeconds = getDeliveryTimeoutSecs();
			CompletableFuture<Result<Object>> future = deliverer.deliverInvoice(invoice, account, null);
			Result<Object> result;
			if ( maxSeconds > 0 ) {
				result = future.get(maxSeconds, TimeUnit.SECONDS);
			} else {
				result = future.get();
			}
			return (result != null && result.getSuccess() != null && result.getSuccess());
		} catch ( TimeoutException e ) {
			throw new RepeatableTaskException("Timeout delivering invoice", e);
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( root instanceof IOException ) {
				throw new RepeatableTaskException("Communication error delivering invoice.", e);
			}
			if ( e instanceof RuntimeException re ) {
				throw re;
			}
			throw new RuntimeException(e);
		}
	}

	private SnfInvoiceDeliverer invoiceDeliverer() {
		Iterable<SnfInvoiceDeliverer> iterable = getDeliveryServices();
		Iterator<SnfInvoiceDeliverer> itr = (iterable != null ? iterable.iterator() : null);
		return (itr != null && itr.hasNext() ? itr.next() : null);
	}

	@Override
	public MessageSource messageSourceForInvoice(SnfInvoice invoice) {
		requireNonNullArgument(invoice, "invoice");
		final Instant version = invoice.getStartDate().atStartOfDay(invoice.getTimeZone()).toInstant();
		return messageSourceForDate(version);
	}

	@Override
	public MessageSource messageSourceForDate(Instant date) {
		return new VersionedMessageDaoMessageSource(messageDao, MESSAGE_BUNDLE_NAMES, date,
				messageCache);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Resource renderInvoice(SnfInvoice invoice, MimeType outputType, Locale locale) {
		TemplateRenderer renderer = renderer(invoice, outputType, locale);
		VersionedMessageDaoMessageSource messageSource = (VersionedMessageDaoMessageSource) messageSourceForInvoice(
				invoice);
		LocalizedInvoice localizedInvoice = new LocalizedInvoice(
				invoiceForSnfInvoice(invoice, messageSource, locale), locale);
		Properties messages = messageSource.propertiesForLocale(locale);
		Map<String, Object> parameters = new LinkedHashMap<>(4);
		parameters.put("invoice", localizedInvoice);
		parameters.put("address", invoice.getAddress());
		parameters.put("messages", messages);
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
			renderer.render(locale, outputType, parameters, out);
			return invoiceResource(out.toByteArray(), invoice, messageSource, outputType, locale);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private Resource invoiceResource(byte[] data, SnfInvoice invoice, MessageSource messageSource,
			MimeType outputType, Locale locale) {
		String extension;
		if ( outputType.isCompatibleWith(MimeType.valueOf("application/pdf")) ) {
			extension = ".pdf";
		} else {
			extension = "." + outputType.getSubtype();
		}
		Object[] filenameArgs = new Object[] {
				DRAFT_INVOICE_ID.equals(invoice.getId().getId())
						? messageSource.getMessage("draftInvoice", null, "DRAFT", locale)
						: SnfBillingUtils.invoiceNumForId(invoice.getId().getId()),
				YearMonth.from(invoice.getStartDate()).toString(), extension };
		String filename = messageSource.getMessage("invoice.filename", filenameArgs,
				"SolarNetwork Invoice {0} - {1}{2}", locale);
		return new ByteArrayResource(data) {

			@Override
			public String getFilename() {
				return filename;
			}

		};
	}

	/**
	 * Resolve tax codes for a given invoice.
	 *
	 * <p>
	 * This implementation creates tax zone names out of the invoice's address,
	 * with the following patterns:
	 * </p>
	 *
	 * <ol>
	 * <li><code>country</code> via {@link Address#getCountry()} (required)</li>
	 * <li><code>country.state</code> via {@link Address#getCountry()} and
	 * {@link Address#getStateOrProvince()} (only if state available)</li>
	 * </ol>
	 *
	 * <p>
	 * The tax date is resolved as the invoice's start date, or the current date
	 * if that is not available.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public TaxCodeFilter taxCodeFilterForInvoice(SnfInvoice invoice) {
		SnfTaxCodeResolver service = this.taxCodeResolver;
		if ( service != null ) {
			return service.taxCodeFilterForInvoice(invoice);
		}
		if ( invoice == null ) {
			throw new IllegalArgumentException("The invoice argument must be provided.");
		}
		Address addr = invoice.getAddress();
		if ( addr == null ) {
			throw new IllegalArgumentException("The invoice must provide an address.");
		}
		if ( addr.getCountry() == null || addr.getCountry().trim().isEmpty() ) {
			throw new IllegalArgumentException("The address must provide a country.");
		}
		List<String> zones = new ArrayList<>(2);
		zones.add(addr.getCountry());
		if ( addr.getStateOrProvince() != null && !addr.getStateOrProvince().trim().isEmpty() ) {
			zones.add(String.format("%s.%s", addr.getCountry(), addr.getStateOrProvince()));
		}

		ZoneId tz = invoice.getTimeZone();
		if ( tz == null ) {
			throw new IllegalArgumentException("The invoice must provide a time zone.");
		}

		TaxCodeFilter filter = new TaxCodeFilter();
		filter.setZones(zones.toArray(String[]::new));

		LocalDate date = invoice.getStartDate();
		if ( date == null ) {
			date = LocalDate.now(UTC);
		}
		filter.setDate(date.atStartOfDay(tz).toInstant());
		return filter;
	}

	private TemplateRenderer renderer(SnfInvoice invoice, MimeType mimeType, Locale locale) {
		if ( rendererResolvers != null ) {
			for ( SnfInvoiceRendererResolver resolver : rendererResolvers ) {
				TemplateRenderer r = resolver.rendererForInvoice(invoice, mimeType, locale);
				if ( r != null ) {
					return r;
				}
			}
		}
		String msg = String.format("MIME %s not supported for invoice rendering.", mimeType);
		throw new IllegalArgumentException(msg);
	}

	/**
	 * Compute the set of tax items for a given invoice.
	 *
	 * <p>
	 * Existing tax items are ignored in the given invoice. The invoice is not
	 * mutated in any way.
	 * </p>
	 *
	 * @param invoice
	 *        the invoice to compute tax items for
	 * @return the list of tax items, never {@literal null}
	 */
	public List<SnfInvoiceItem> computeInvoiceTaxItems(SnfInvoice invoice) {
		SnfTaxCodeResolver taxResolver = (taxCodeResolver != null ? taxCodeResolver : this);
		TaxCodeFilter taxFilter = taxResolver.taxCodeFilterForInvoice(invoice);
		List<SnfInvoiceItem> taxItems = new ArrayList<>(8);
		if ( taxFilter != null ) {
			net.solarnetwork.dao.FilterResults<TaxCode, Long> taxes = taxCodeDao.findFiltered(taxFilter,
					null, null, null);
			if ( taxes != null && taxes.getReturnedResultCount() > 0 ) {
				Map<String, BigDecimal> taxAmounts = new LinkedHashMap<>(taxes.getReturnedResultCount());
				for ( SnfInvoiceItem item : invoice.getItems() ) {
					final InvoiceItemType itemType = item.getItemType();
					if ( itemType == InvoiceItemType.Tax ) {
						continue;
					}
					final String itemKey = item.getKey();
					final BigDecimal itemAmount = item.getAmount();
					if ( itemKey == null || itemAmount == null ) {
						continue;
					}
					for ( TaxCode tax : taxes ) {
						final String taxCode = tax.getCode();
						final BigDecimal taxRate = tax.getRate();
						if ( taxCode == null || taxRate == null ) {
							continue;
						}
						if ( itemKey.equalsIgnoreCase(tax.getItemKey()) ) {
							taxAmounts.merge(taxCode, taxRate.multiply(itemAmount), BigDecimal::add);
						}
					}
				}
				for ( Map.Entry<String, BigDecimal> me : taxAmounts.entrySet() ) {
					SnfInvoiceItem taxItem = newItem(invoice, InvoiceItemType.Tax, me.getKey(),
							BigDecimal.ONE, me.getValue().setScale(2, RoundingMode.HALF_UP));
					taxItems.add(taxItem);
				}
			}
		}
		return taxItems;
	}

	/**
	 * Get the item key for datum properties input usage.
	 *
	 * @return the key; defaults to {@link NodeUsages#DATUM_PROPS_IN_KEY}
	 */
	public String getDatumPropertiesInKey() {
		return datumPropertiesInKey;
	}

	/**
	 * Set the item key for datum properties input usage.
	 *
	 * @param datumPropertiesInKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setDatumPropertiesInKey(String datumPropertiesInKey) {
		this.datumPropertiesInKey = requireNonNullArgument(datumPropertiesInKey, "datumPropertiesInKey");
	}

	/**
	 * Get the item key for datum output usage.
	 *
	 * @return the key; defaults to {@link NodeUsages#DATUM_OUT_KEY}
	 */
	public String getDatumOutKey() {
		return datumOutKey;
	}

	/**
	 * Set the item key for datum output usage.
	 *
	 * @param datumOutKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setDatumOutKey(String datumOutKey) {
		this.datumOutKey = requireNonNullArgument(datumOutKey, "datumOutKey");
	}

	/**
	 * Get the item key for datum days stored usage.
	 *
	 * @return the key; defaults to {@link NodeUsages#DATUM_DAYS_STORED_KEY}
	 */
	public String getDatumDaysStoredKey() {
		return datumDaysStoredKey;
	}

	/**
	 * Set the item key for datum days stored usage.
	 *
	 * @param datumDaysStoredKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setDatumDaysStoredKey(String datumDaysStoredKey) {
		this.datumDaysStoredKey = requireNonNullArgument(datumDaysStoredKey, "datumDaysStoredKey");
	}

	/**
	 * Get the instructions issued key.
	 *
	 * @return the key
	 * @since 1.3
	 */
	public String getInstructionsIssuedKey() {
		return instructionsIssuedKey;
	}

	/**
	 * Set the instructions issued key.
	 *
	 * @param instructionsIssuedKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.3
	 */
	public void setInstructionsIssuedKey(String instructionsIssuedKey) {
		this.instructionsIssuedKey = requireNonNullArgument(instructionsIssuedKey,
				"instructionsIssuedKey");
	}

	/**
	 * Get the item key for SolarFlux data in.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#FLUX_DATA_IN_KEY}
	 * @since 1.5
	 */
	public final String getFluxDataInKey() {
		return fluxDataInKey;
	}

	/**
	 * Get the item key for SolarFlux data in.
	 *
	 * @param fluxDataInKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.5
	 */
	public final void setFluxDataInKey(String fluxDataInKey) {
		this.fluxDataInKey = requireNonNullArgument(fluxDataInKey, "fluxDataInKey");
	}

	/**
	 * Get the item key for SolarFlux data out.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#FLUX_DATA_OUT_KEY}
	 * @since 1.5
	 */
	public final String getFluxDataOutKey() {
		return fluxDataOutKey;
	}

	/**
	 * Set the item key for SolarFlux data out.
	 *
	 * @param fluxDataOutKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.5
	 */
	public final void setFluxDataOutKey(String fluxDataOutKey) {
		this.fluxDataOutKey = requireNonNullArgument(fluxDataOutKey, "fluxDataOutKey");
	}

	/**
	 * Get the item key for OCPP chargers.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#OCPP_CHARGERS_KEY}
	 * @since 1.1
	 */
	public String getOcppChargersKey() {
		return ocppChargersKey;
	}

	/**
	 * Set the item key for OCPP chargers.
	 *
	 * @param ocppChargersKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.1
	 */
	public void setOcppChargersKey(String ocppChargersKey) {
		this.ocppChargersKey = requireNonNullArgument(ocppChargersKey, "ocppChargersKey");
	}

	/**
	 * Get the item key for OSCP Capacity Groups.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#OSCP_CAPACITY_GROUPS_KEY}
	 * @since 1.1
	 */
	public String getOscpCapacityGroupsKey() {
		return oscpCapacityGroupsKey;
	}

	/**
	 * Set the item key for OSCP Capacity Groups.
	 *
	 * @param oscpCapacityGroupsKey
	 *        the oscpCapacityGroupsKey to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.1
	 */
	public void setOscpCapacityGroupsKey(String oscpCapacityGroupsKey) {
		this.oscpCapacityGroupsKey = requireNonNullArgument(oscpCapacityGroupsKey,
				"oscpCapacityGroupsKey");
	}

	/**
	 * Get the item key for OSCP Capacity.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#OSCP_CAPACITY_KEY}
	 * @since 1.4
	 */
	public String getOscpCapacityKey() {
		return oscpCapacityKey;
	}

	/**
	 * Set the item key for OSCP Capacity.
	 *
	 * @param oscpCapacityKey
	 *        the oscpCapacityKey to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.4
	 */
	public void setOscpCapacityKey(String oscpCapacityKey) {
		this.oscpCapacityKey = requireNonNullArgument(oscpCapacityKey, "oscpCapacityKey");
	}

	/**
	 * Get the item key for DNP3 Data Points.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#DNP3_DATA_POINTS_KEY}
	 * @since 1.2
	 */
	public String getDnp3DataPointsKey() {
		return dnp3DataPointsKey;
	}

	/**
	 * Set the item key for DNP3 Data Points.
	 *
	 * @param dnp3DataPointsKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.2
	 */
	public void setDnp3DataPointsKey(String dnp3DataPointsKey) {
		this.dnp3DataPointsKey = requireNonNullArgument(dnp3DataPointsKey, "dnp3DataPointsKey");
	}

	/**
	 * Get the item key for OAuth client credentials.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#OAUTH_CLIENT_CREDENTIALS_KEY}
	 * @since 1.5
	 */
	public final String getOauthClientCredentialsKey() {
		return oauthClientCredentialsKey;
	}

	/**
	 * Set the item key for OAuth client credentials.
	 *
	 * @param oauthClientCredentialsKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.5
	 */
	public final void setOauthClientCredentialsKey(String oauthClientCredentialsKey) {
		this.oauthClientCredentialsKey = requireNonNullArgument(oauthClientCredentialsKey,
				"oauthClientCredentialsKey");
	}

	/**
	 * Get the item key for Cloud Integrations data.
	 *
	 * @return the key, never {@literal null}; defaults to
	 *         {@link NodeUsages#CLOUD_INTEGRATIONS_DATA_KEY}
	 * @since 1.6
	 */
	public final String getCloudIntegrationsDataKey() {
		return cloudIntegrationsDataKey;
	}

	/**
	 * Set the item key for Cloud Integrations data.
	 *
	 * @param cloudIntegrationsDataKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 * @since 1.6
	 */
	public final void setCloudIntegrationsDataKey(String cloudIntegrationsDataKey) {
		this.cloudIntegrationsDataKey = requireNonNullArgument(cloudIntegrationsDataKey,
				"cloudIntegrationsDataKey");
	}

	/**
	 * Get the item key for account credit.
	 *
	 * @return the key; defaults to {@link AccountBalance#ACCOUNT_CREDIT_KEY}
	 */
	public String getAccountCreditKey() {
		return accountCreditKey;
	}

	/**
	 * Set the item key for account credit.
	 *
	 * @param accountCreditKey
	 *        the accountCreditKey to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setAccountCreditKey(String accountCreditKey) {
		this.accountCreditKey = requireNonNullArgument(accountCreditKey, "accountCreditKey");
	}

	/**
	 * Get the optional tax code resolver service to use.
	 *
	 * <p>
	 * If this property is not configured or the service is not available at
	 * runtime, this class will act as its own resolver.
	 * </p>
	 *
	 * @return the service
	 */
	public SnfTaxCodeResolver getTaxCodeResolver() {
		return taxCodeResolver;
	}

	/**
	 * Set the optional tax code resolver service to use.
	 *
	 * @param taxCodeResolver
	 *        the service to set
	 */
	public void setTaxCodeResolver(SnfTaxCodeResolver taxCodeResolver) {
		this.taxCodeResolver = taxCodeResolver;
	}

	/**
	 * Get the optional message cache.
	 *
	 * @return the cache
	 */
	public Cache<String, VersionedMessages> getMessageCache() {
		return messageCache;
	}

	/**
	 * Set the optional message cache.
	 *
	 * @param messageCache
	 *        the cache to set
	 */
	public void setMessageCache(Cache<String, VersionedMessages> messageCache) {
		this.messageCache = messageCache;
	}

	/**
	 * Get the invoice delivery services.
	 *
	 * @return the services
	 */
	public List<SnfInvoiceDeliverer> getDeliveryServices() {
		return deliveryServices;
	}

	/**
	 * Set the invoice delivery services.
	 *
	 * @param deliveryServices
	 *        the services to set
	 */
	public void setDeliveryServices(List<SnfInvoiceDeliverer> deliveryServices) {
		this.deliveryServices = deliveryServices;
	}

	/**
	 * Get the invoice renderer resolver services.
	 *
	 * @return the services
	 */
	public List<SnfInvoiceRendererResolver> getRendererResolvers() {
		return rendererResolvers;
	}

	/**
	 * Set the invoice renderer resolver services.
	 *
	 * @param rendererResolvers
	 *        the services to set
	 */
	public void setRendererResolvers(List<SnfInvoiceRendererResolver> rendererResolvers) {
		this.rendererResolvers = rendererResolvers;
	}

	/**
	 * Get the maximum amount of time to wait for invoice delivery to complete,
	 * in seconds.
	 *
	 * @return the timeout, in seconds
	 */
	public int getDeliveryTimeoutSecs() {
		return deliveryTimeoutSecs;
	}

	/**
	 * Set the maximum amount of time to wait for invoice delivery to complete.
	 *
	 * @param deliveryTimeoutSecs
	 *        the timeout to set, in seconds, or {@literal 0} for no timeout
	 */
	public void setDeliveryTimeoutSecs(int deliveryTimeoutSecs) {
		this.deliveryTimeoutSecs = deliveryTimeoutSecs;
	}

}
