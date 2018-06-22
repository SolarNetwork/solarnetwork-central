/* ==================================================================
 * KillbillClient.java - 21/08/2017 3:44:17 PM
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

package net.solarnetwork.central.user.billing.killbill;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.CustomField;
import net.solarnetwork.central.user.billing.killbill.domain.HealthCheckResult;
import net.solarnetwork.central.user.billing.killbill.domain.Invoice;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.SubscriptionUsageRecords;
import net.solarnetwork.central.user.billing.killbill.domain.Tag;
import net.solarnetwork.central.user.billing.killbill.domain.TagDefinition;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;

/**
 * API for interaction with Killbill.
 * 
 * @author matt
 * @version 1.3
 */
public interface KillbillClient {

	/** A date formatter suitable for Killbill. */
	static DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.date();

	/**
	 * Get a unique ID for the Kill Bill system this client is configured to
	 * integrate with.
	 * 
	 * @return the unique ID
	 * @since 1.2
	 */
	String getUniqueId();

	/**
	 * Get a health check status report.
	 * 
	 * @return a collection of health check results, never {@literal null}
	 * @since 1.2
	 */
	Collection<HealthCheckResult> healthCheck();

	/**
	 * Get the account associated with an external key.
	 * 
	 * @param key
	 *        the key to find
	 * @return the account, or {@literal null} if not available
	 */
	Account accountForExternalKey(String key);

	/**
	 * Create a new account.
	 * 
	 * @param info
	 *        the account info to persist
	 * @return the account ID for the new account
	 */
	String createAccount(Account info);

	/**
	 * Add a payment method to an account.
	 * 
	 * @param account
	 *        the account to update
	 * @param paymentData
	 *        the payment data to add
	 * @param defaultMethod
	 *        {@literal true} to treat this as the default payment method
	 * @return the payment method ID
	 */
	String addPaymentMethodToAccount(Account account, Map<String, Object> paymentData,
			boolean defaultMethod);

	/**
	 * Get a bundle for an external key.
	 * 
	 * @param account
	 *        the account that owns the bundle
	 * @param key
	 *        the external key of the bundle to get
	 * @return the bundle, or {@literal null} if not available
	 */
	Bundle bundleForExternalKey(Account account, String key);

	/**
	 * Create a bundle for an account.
	 * 
	 * <p>
	 * The bundle is expected to contain a {@link Subscription} to define the
	 * plan to add to the bundle. The desired {@code externalKey} should be
	 * defined on the bundle.
	 * </p>
	 * 
	 * <p>
	 * <b>Note</b> that the <em>subscription ID</em> is returned by this method,
	 * not the bundle ID.
	 * </p>
	 * 
	 * @param account
	 *        the account to update
	 * @param requestedDate
	 *        the request date, or {@code null} for the current date
	 * @param info
	 *        the bundle info to create
	 * @return the subscription ID
	 */
	String createBundle(Account account, LocalDate requestedDate, Bundle info);

	/**
	 * Add a new subscription to a bundle.
	 * 
	 * @param account
	 *        the account to update
	 * @param bundleId
	 *        the bundle ID to add the subscription to
	 * @param requestedDate
	 *        the request date, or {@code null} for the current date
	 * @param subscription
	 *        the subscription to add
	 * @return the subscription ID
	 * @since 1.3
	 */
	String addSubscriptionToBundle(Account account, String bundleId, LocalDate requestedDate,
			Subscription subscription);

	/**
	 * Create custom field values associated with a subscription.
	 * 
	 * @param subscriptionId
	 *        the subscription ID to attach the custom field to
	 * @param fields
	 *        the fields to create
	 * @return the custom field list ID
	 */
	String createSubscriptionCustomFields(String subscriptionId, List<CustomField> fields);

	/**
	 * Get the custom fields associated with a subscription.
	 * 
	 * @param subscriptionId
	 *        the subscription ID to get the custom fields for
	 * @return the custom fields, never {@literal null}
	 */
	List<CustomField> customFieldsForSubscription(String subscriptionId);

	/**
	 * Get a subscription.
	 * 
	 * @param subscriptionId
	 *        the ID of the subscription to get
	 * @return the subscription, or {@literal null} if not available
	 */
	Subscription getSubscription(String subscriptionId);

	/**
	 * Get all available tag definitions.
	 * 
	 * @return the available tag definitions
	 */
	List<TagDefinition> getTagDefinitions();

	/**
	 * Get all available tags set on an account.
	 * 
	 * @param account
	 *        the account to get the tags for
	 * @return the tags, never {@literal null}
	 */
	List<Tag> tagsForAccount(Account account);

	/**
	 * Add a set of tags to an account.
	 * 
	 * @param account
	 *        the account to add the tags to
	 * @param tagIds
	 *        the IDs of the tags to add
	 */
	void addTagsToAccount(Account account, Set<String> tagIds);

	/**
	 * Remove a set of tags from an account.
	 * 
	 * @param account
	 *        the account to remove the tags from
	 * @param tagIds
	 *        the IDs of the tags to remove
	 */
	void removeTagsFromAccount(Account account, Set<String> tagIds);

	/**
	 * Add usage records to a subscription.
	 * 
	 * @param subscription
	 *        the subscription to add usage to
	 * @param trackingId
	 *        a unique key to prevent duplicate usage records from being stored
	 * @param unit
	 *        the usage unit name
	 * @param usage
	 *        the usage records
	 */
	void addUsage(Subscription subscription, String trackingId, String unit, List<UsageRecord> usage);

	/**
	 * Get an invoice by ID.
	 * 
	 * @param account
	 *        the account to get invoices for
	 * @param invoiceId
	 *        the ID of the invoice to get
	 * @param withItems
	 *        {@literal true} to return invoice items as well
	 * @param withChildrenItems
	 *        {@literal true} to return child invoice items as well
	 * @return the invoice, or {@literal null} if not available
	 */
	Invoice getInvoice(Account account, String invoiceId, boolean withItems, boolean withChildrenItems);

	/**
	 * Get usage records for a subscription time range.
	 * 
	 * @param subscriptionId
	 *        the ID of the subscription
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @return the usage records
	 */
	SubscriptionUsageRecords usageRecordsForSubscription(String subscriptionId, LocalDate startDate,
			LocalDate endDate);

	/**
	 * Get all account invoices, optionally limited to just unpaid ones.
	 * 
	 * @param account
	 *        the account to get invoices for
	 * @param unpaidOnly
	 *        {@literal true} for just invoices with an outstanding balance;
	 *        {@code false} for all invoices
	 * @return the invoices, never {@literal null}
	 */
	List<Invoice> listInvoices(Account account, boolean unpaidOnly);

	/**
	 * Search for invoices.
	 * 
	 * @param account
	 *        the account to get invoices for
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 */
	FilterResults<Invoice> findInvoices(Account account, InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max);

	/**
	 * Get the invoice catalog translation properties for a locale.
	 * 
	 * @param locale
	 *        the locale to get
	 * @return the properties, or {@literal null} if not available
	 */
	Properties invoiceCatalogTranslation(String locale);

	/**
	 * Render an invoice.
	 * 
	 * @param invoiceId
	 *        the ID of the invoice to render
	 * @param outputType
	 *        the desired output type, e.g. {@literal text/html}
	 * @param locale
	 *        the desired output locale
	 * @return a resource with the result data, or {@literal null} if the
	 *         invoice is not available
	 * @throws IllegalArgumentException
	 *         if {@code outputType} is not supported
	 * @since 1.1
	 */
	Resource renderInvoice(String invoiceId, MimeType outputType, Locale locale);

}
