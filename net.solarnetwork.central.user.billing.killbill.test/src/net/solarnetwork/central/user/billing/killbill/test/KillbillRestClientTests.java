/* ==================================================================
 * KillbillRestClientTests.java - 23/08/2017 9:10:20 AM
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

package net.solarnetwork.central.user.billing.killbill.test;

import static net.solarnetwork.central.user.billing.killbill.KillbillAuthorizationInterceptor.API_KEY_HEADER_NAME;
import static net.solarnetwork.central.user.billing.killbill.KillbillAuthorizationInterceptor.API_SECRET_HEADER_NAME;
import static net.solarnetwork.central.user.billing.killbill.KillbillAuthorizationInterceptor.CREATED_BY_HEADER_NAME;
import static net.solarnetwork.central.user.billing.killbill.KillbillAuthorizationInterceptor.DEFAULT_CREATED_BY;
import static net.solarnetwork.central.user.billing.killbill.test.JsonObjectMatchers.json;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.user.billing.killbill.KillbillRestClient;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.BundleSubscription;
import net.solarnetwork.central.user.billing.killbill.domain.CustomField;
import net.solarnetwork.central.user.billing.killbill.domain.Invoice;
import net.solarnetwork.central.user.billing.killbill.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.SubscriptionUsageRecords;
import net.solarnetwork.central.user.billing.killbill.domain.UnitRecord;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link KillbillRestClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillRestClientTests {

	private static final String TEST_ACCOUNT_KEY = "test.key";
	private static final String TEST_ACCOUNT_ID = "e7be50d7-63f1-429b-a5ce-5f950733c2a7";
	private static final String TEST_PAYMENT_METHOD_ID = "a25e4f73-ac51-45d0-a150-3bb7eec30ade";
	private static final String TEST_BUNDLE_KEY = "test.bundle.key";
	private static final String TEST_BUNDLE_ID = "db4fde26-9094-4a2c-8520-96455ab35201";
	private static final String TEST_SUBSCRIPTION_ID = "e791cb82-2363-4c77-86a7-770dd7aaf3c9";
	private static final String TEST_SUBSCRIPTION_PLAN_NAME = "api-posted-datum-metric-monthly-usage";
	private static final String TEST_BASE_URL = "http://localhost";
	private static final String TEST_API_SECRET = "test.secret";
	private static final String TEST_API_KEY = TEST_ACCOUNT_KEY;
	private static final String TEST_USERNAME = "test.user";
	private static final String TEST_PASSWORD = "test.password";
	private static final String TEST_BASIC_AUTH_HEADER = "Basic " + Base64Utils
			.encodeToString((TEST_USERNAME + ":" + TEST_PASSWORD).getBytes(Charset.forName("UTF-8")));
	private static final String TEST_TIME_ZONE = "Pacific/Auckland";
	private static final String TEST_INVOICE_ID = "2b717636-56b0-4967-aaab-68d83ba1a455";

	private MockRestServiceServer server;
	private KillbillRestClient client;
	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		// setup RestTemplate's ObjectMapper so we know exactly what we are converting
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.createServer(restTemplate);
		client = new KillbillRestClient(restTemplate);
		client.setApiKey(TEST_API_KEY);
		client.setApiSecret(TEST_API_SECRET);
		client.setBaseUrl(TEST_BASE_URL);
		client.setUsername(TEST_USERNAME);
		client.setPassword(TEST_PASSWORD);

		// snag the ObjectMapper out of RestTemplate so we have the same one for our tests
		for ( HttpMessageConverter<?> converter : restTemplate.getMessageConverters() ) {
			if ( converter instanceof MappingJackson2HttpMessageConverter ) {
				MappingJackson2HttpMessageConverter messageConverter = (MappingJackson2HttpMessageConverter) converter;
				this.objectMapper = messageConverter.getObjectMapper();
				break;
			}
		}
	}

	private ResponseActions serverExpect(String url, HttpMethod method) {
		ResponseActions actions = server.expect(requestTo(equalTo(TEST_BASE_URL + url)))
				.andExpect(method(method)).andExpect(header(API_KEY_HEADER_NAME, TEST_API_KEY))
				.andExpect(header(API_SECRET_HEADER_NAME, TEST_API_SECRET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, TEST_BASIC_AUTH_HEADER));
		if ( !HttpMethod.GET.equals(method) ) {
			actions = actions.andExpect(header(CREATED_BY_HEADER_NAME, DEFAULT_CREATED_BY));
		}
		return actions;
	}

	@Test
	public void accountForExternalKeyNotFound() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/accounts?externalKey="+TEST_ACCOUNT_KEY, HttpMethod.GET)
			.andRespond(withStatus(HttpStatus.NOT_FOUND)
				.body(new ClassPathResource("account-02.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = client.accountForExternalKey(TEST_ACCOUNT_KEY);

		// then
		assertThat("Account not found", account, nullValue());
	}

	@Test
	public void accountForExternalKey() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/accounts?externalKey="+TEST_ACCOUNT_KEY, HttpMethod.GET)
			.andRespond(withSuccess(new ClassPathResource("account-01.json", getClass()),
					MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = client.accountForExternalKey(TEST_ACCOUNT_KEY);

		// then
		assertThat("Account found", account, notNullValue());
		assertThat("Account ID", account.getAccountId(), equalTo(TEST_ACCOUNT_ID));
	}

	@Test
	public void createAccount() throws Exception {
		// given
		Account account = new Account();
		account.setAccountId(TEST_ACCOUNT_ID);
		account.setBillCycleDayLocal(1);
		account.setCountry("NZ");
		account.setCurrency("NZD");
		account.setEmail("foo@localhost");
		account.setExternalKey("bar");
		account.setName("John Doe");
		account.setTimeZone("+00:00");

		// @formatter:off
		serverExpect("/1.0/kb/accounts", HttpMethod.POST)
		.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
		.andExpect(json(objectMapper).bodyObject(account))
		.andRespond(withCreatedEntity(new URI("http://localhost:8080/1.0/kb/accounts/" 
				+TEST_ACCOUNT_ID)));
	    // @formatter:on

		// when
		String accountId = client.createAccount(account);

		// then
		assertThat("Account ID", accountId, equalTo(TEST_ACCOUNT_ID));
	}

	@Test
	public void addPaymentMethod() throws Exception {
		// given
		Map<String, Object> paymentData = Collections.singletonMap("foo", "bar");

		// @formatter:off
		serverExpect("/1.0/kb/accounts/"+TEST_ACCOUNT_ID+"/paymentMethods?isDefault=true", HttpMethod.POST)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
			.andExpect(json(objectMapper).bodyObject(paymentData))
			.andRespond(withCreatedEntity(new URI("http://localhost:8080/1.0/kb/paymentMethods/" 
					+TEST_PAYMENT_METHOD_ID)));
	    // @formatter:on

		String paymentId = client.addPaymentMethodToAccount(new Account(TEST_ACCOUNT_ID), paymentData,
				true);
		assertThat("Payment ID", paymentId, equalTo(TEST_PAYMENT_METHOD_ID));
	}

	@Test
	public void bundleForExternalKeyNotFound() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/bundles?externalKey="+TEST_BUNDLE_KEY, HttpMethod.GET)
			.andRespond(withStatus(HttpStatus.NOT_FOUND)
				.body(new ClassPathResource("bundle-02.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = new Account(TEST_ACCOUNT_ID);
		Bundle bundle = client.bundleForExternalKey(account, TEST_BUNDLE_KEY);

		// then
		assertThat("Bundle not found", bundle, nullValue());
	}

	@Test
	public void bundleForExternalKey() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/bundles?externalKey="+TEST_BUNDLE_KEY, HttpMethod.GET)
			.andRespond(withSuccess(new ClassPathResource("bundle-01.json", getClass()),
					MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = new Account(TEST_ACCOUNT_ID);
		Bundle bundle = client.bundleForExternalKey(account, TEST_BUNDLE_KEY);

		// then
		assertThat("Bundle found", bundle, notNullValue());
		assertThat("Account ID", bundle.getAccountId(), equalTo(TEST_ACCOUNT_ID));
		assertThat("Bundle ID", bundle.getBundleId(), equalTo(TEST_BUNDLE_ID));
		assertThat("Bundle subscriptions", bundle.getSubscriptions(), hasSize(1));

		Subscription subscription = bundle.getSubscriptions().get(0);
		assertThat("Subscription ID", subscription.getSubscriptionId(), equalTo(TEST_SUBSCRIPTION_ID));
		assertThat("Plan name", subscription.getPlanName(), equalTo(TEST_SUBSCRIPTION_PLAN_NAME));
	}

	@Test
	public void createBundle() throws Exception {
		// given
		Bundle bundle = new Bundle();
		bundle.setExternalKey(TEST_BUNDLE_KEY);

		Subscription subscription = new Subscription();
		subscription.setBillCycleDayLocal(1);
		subscription.setPlanName(TEST_SUBSCRIPTION_PLAN_NAME);
		subscription.setProductCategory(Subscription.BASE_PRODUCT_CATEGORY);

		bundle.setSubscriptions(Collections.singletonList(subscription));

		// using the BundleSubscription to verify the request JSON structure
		BundleSubscription bs = new BundleSubscription((Bundle) bundle.clone());
		bs.getBundle().setAccountId(TEST_ACCOUNT_ID); // should be copied from Account

		// @formatter:off
		serverExpect("/1.0/kb/subscriptions?requestedDate=2017-01-01", HttpMethod.POST)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
			.andExpect(json(objectMapper).bodyObject(bs))
			.andRespond(withCreatedEntity(new URI("http://localhost:8080/1.0/kb/subscriptions/" 
					+TEST_SUBSCRIPTION_ID)));
	    // @formatter:on

		// when
		Account account = new Account(TEST_ACCOUNT_ID);
		LocalDate date = new LocalDate(2017, 1, 1);
		String subscriptionId = client.createBundle(account, date, bundle);

		// then
		assertThat("Subscription ID", subscriptionId, equalTo(TEST_SUBSCRIPTION_ID));
	}

	@Test
	public void createCustomFields() throws Exception {
		// given
		CustomField field = new CustomField("foo", "bar");

		// using the SubscriptionUsage to verify the request JSON structure
		String expected = "[{\"name\":\"foo\",\"value\":\"bar\"}]";

		URI loc = new URI("http://localhost/custom-field-list-id");

		// @formatter:off
		serverExpect("/1.0/kb/subscriptions/"+TEST_SUBSCRIPTION_ID+"/customFields", HttpMethod.POST)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
			.andExpect(content().string(expected))
			.andRespond(withStatus(HttpStatus.CREATED).location(loc));
	    // @formatter:on

		// when
		String id = client.createSubscriptionCustomFields(TEST_SUBSCRIPTION_ID,
				Collections.singletonList(field));

		// then
		assertThat(id, equalTo("custom-field-list-id"));
	}

	@Test
	public void getCustomFields() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/subscriptions/"+TEST_SUBSCRIPTION_ID+"/customFields", HttpMethod.GET)
			.andRespond(withStatus(HttpStatus.CREATED)
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body("[{\"name\":\"foo\",\"value\":\"bar\"}]"));
	    // @formatter:on

		// when
		List<CustomField> fields = client.customFieldsForSubscription(TEST_SUBSCRIPTION_ID);

		// then
		assertThat(fields, hasSize(1));
		CustomField field = fields.get(0);
		assertThat("Name", field.getName(), equalTo("foo"));
		assertThat("Value", field.getValue(), equalTo("bar"));
	}

	@Test
	public void addUsage() {
		// given
		Subscription subscription = new Subscription(TEST_SUBSCRIPTION_ID);

		LocalDate date = new LocalDate(2017, 1, 1);
		List<UsageRecord> usageRecords = Arrays.asList(new UsageRecord(date, new BigDecimal(1)),
				new UsageRecord(date.plusDays(1), new BigDecimal(2)),
				new UsageRecord(date.plusDays(2), new BigDecimal(3)));

		// using the SubscriptionUsage to verify the request JSON structure
		Map<String, Object> expected = JsonUtils.getStringMap("{\"subscriptionId\":\""
				+ TEST_SUBSCRIPTION_ID + "\",\"trackingId\":\"test-tracking-id\""
				+ ",\"unitUsageRecords\":[{\"unitType\":\"test-unit\",\"usageRecords\":["
				+ "{\"recordDate\":\"2017-01-01\",\"amount\":\"1\"},"
				+ "{\"recordDate\":\"2017-01-02\",\"amount\":\"2\"},"
				+ "{\"recordDate\":\"2017-01-03\",\"amount\":\"3\"}]}]}");

		// @formatter:off
		serverExpect("/1.0/kb/usages", HttpMethod.POST)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
			.andExpect(json(objectMapper).bodyObject(expected))
			.andRespond(withStatus(HttpStatus.CREATED));
	    // @formatter:on

		// when
		client.addUsage(subscription, "test-tracking-id", "test-unit", usageRecords);

		// then
		// no exception thrown
	}

	@Test
	public void unpaidInvoices() {
		// given
		Account account = new Account(TEST_ACCOUNT_ID);
		account.setTimeZone(TEST_TIME_ZONE);

		// @formatter:off
		serverExpect("/1.0/kb/accounts/"+TEST_ACCOUNT_ID+"/invoices?unpaidInvoicesOnly=true", HttpMethod.GET)
			.andRespond(withSuccess()
				.body(new ClassPathResource("account-invoices-01.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		//when
		List<Invoice> results = client.listInvoices(account, true);

		// then
		assertThat("Result count", results, hasSize(1));

		Invoice invoice = results.get(0);
		assertThat("Invoice ID", invoice.getId(), equalTo("31b3c76a-3b6a-46ad-b30b-98f21a4e76cd"));
		assertThat("Invoice balance", invoice.getBalance(), equalTo(new BigDecimal("18.59")));
		assertThat("Invoice amount", invoice.getAmount(), equalTo(new BigDecimal("18.59")));
		assertThat("Invoice currency", invoice.getCurrencyCode(), equalTo("NZD"));
		assertThat("Invoice date", invoice.getInvoiceDate(), equalTo(new LocalDate(2017, 12, 6)));
		assertThat("Invoice created", invoice.getCreated(), equalTo(
				invoice.getInvoiceDate().toDateTimeAtStartOfDay(DateTimeZone.forID(TEST_TIME_ZONE))));
		assertThat("Invoice time zone", invoice.getTimeZoneId(), equalTo(TEST_TIME_ZONE));

	}

	@Test
	public void findInvoicesPaginated() {
		// given
		Account account = new Account(TEST_ACCOUNT_ID);
		account.setTimeZone(TEST_TIME_ZONE);

		for ( int queryNum = 0; queryNum < 3; queryNum++ ) {
			// HEAD query to get total result count
			HttpHeaders paginationHeaders = new HttpHeaders();
			paginationHeaders.set(KillbillRestClient.HEADER_PAGINATION_OFFSET, "0");
			paginationHeaders.set(KillbillRestClient.HEADER_PAGINATION_MAX_COUNT, "12");
			paginationHeaders.set(KillbillRestClient.HEADER_PAGINATION_TOTAL_COUNT, "5");
			serverExpect(
					"/1.0/kb/invoices/search/" + TEST_ACCOUNT_ID + "?withItems=false&offset=0&limit=0",
					HttpMethod.HEAD).andRespond(withSuccess().headers(paginationHeaders));

			// GET query to get results
			int offset = Math.max(0, (5 - ((queryNum + 1) * 2)));
			HttpHeaders paginationHeaders2 = new HttpHeaders();
			paginationHeaders2.set(KillbillRestClient.HEADER_PAGINATION_OFFSET, String.valueOf(offset));
			paginationHeaders2.set(KillbillRestClient.HEADER_PAGINATION_MAX_COUNT, "12");
			paginationHeaders2.set(KillbillRestClient.HEADER_PAGINATION_TOTAL_COUNT, "5");

			// @formatter:off
			serverExpect("/1.0/kb/invoices/search/" + TEST_ACCOUNT_ID 
					+ "?withItems=false&offset="+ offset +"&limit=" +(queryNum == 2 ? 1 : 2),
					HttpMethod.GET).andRespond(withSuccess()
							.headers(paginationHeaders2)
							.body(new ClassPathResource("invoices-0" +(queryNum + 1) +".json", getClass()))
							.contentType(MediaType.APPLICATION_JSON_UTF8));
			// @formatter:on
		}

		//when
		List<FilterResults<Invoice>> results = new ArrayList<>(3);
		results.add(client.findInvoices(account, null, null, 0, 2));
		results.add(client.findInvoices(account, null, null, 2, 2));
		results.add(client.findInvoices(account, null, null, 4, 2));

		// then
		int invoiceNum = 5; // invoices should be in reverse order
		for ( int queryNum = 0; queryNum < 3; queryNum++ ) {
			FilterResults<Invoice> result = results.get(queryNum);
			assertThat("Total count " + queryNum, result.getTotalResults(), equalTo(5L));
			assertThat("Returned count " + queryNum, result.getReturnedResultCount(),
					equalTo(queryNum == 2 ? 1 : 2));
			assertThat("Starting offset " + queryNum, result.getStartingOffset(), equalTo(queryNum * 2));

			for ( Invoice invoice : result ) {
				assertThat("Invoice " + invoiceNum, invoice.getInvoiceNumber(),
						equalTo(String.valueOf(invoiceNum)));
				assertThat("Invoice time zone " + invoiceNum, invoice.getTimeZoneId(),
						equalTo(TEST_TIME_ZONE));
				invoiceNum--;
			}
		}
	}

	@Test
	public void findInvoicesPaginatedOnePageTotal() {
		// given
		Account account = new Account(TEST_ACCOUNT_ID);
		account.setTimeZone(TEST_TIME_ZONE);

		// HEAD query to get total result count
		HttpHeaders paginationHeaders = new HttpHeaders();
		paginationHeaders.set(KillbillRestClient.HEADER_PAGINATION_OFFSET, "0");
		paginationHeaders.set(KillbillRestClient.HEADER_PAGINATION_MAX_COUNT, "12");
		paginationHeaders.set(KillbillRestClient.HEADER_PAGINATION_TOTAL_COUNT, "2");
		serverExpect("/1.0/kb/invoices/search/" + TEST_ACCOUNT_ID + "?withItems=false&offset=0&limit=0",
				HttpMethod.HEAD).andRespond(withSuccess().headers(paginationHeaders));

		// GET query to get results
		HttpHeaders paginationHeaders2 = new HttpHeaders();
		paginationHeaders2.set(KillbillRestClient.HEADER_PAGINATION_OFFSET, "0");
		paginationHeaders2.set(KillbillRestClient.HEADER_PAGINATION_MAX_COUNT, "12");
		paginationHeaders2.set(KillbillRestClient.HEADER_PAGINATION_TOTAL_COUNT, "2");

		// @formatter:off
			serverExpect(
					"/1.0/kb/invoices/search/" + TEST_ACCOUNT_ID + "?withItems=false&offset=0&limit=10",
					HttpMethod.GET).andRespond(withSuccess()
							.headers(paginationHeaders2)
							.body(new ClassPathResource("invoices-01.json", getClass()))
							.contentType(MediaType.APPLICATION_JSON_UTF8));
			// @formatter:on

		//when
		FilterResults<Invoice> result = client.findInvoices(account, null, null, 0, 10);

		// then
		assertThat("Total count", result.getTotalResults(), equalTo(2L));
		assertThat("Returned count", result.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", result.getStartingOffset(), equalTo(0));

		int invoiceNum = 5; // invoice numbers in data are 4,5
		for ( Invoice invoice : result ) {
			assertThat("Invoice " + invoiceNum, invoice.getInvoiceNumber(),
					equalTo(String.valueOf(invoiceNum)));
			assertThat("Invoice time zone " + invoiceNum, invoice.getTimeZoneId(),
					equalTo(TEST_TIME_ZONE));
			invoiceNum--;
		}
	}

	// TODO: pagination with unpaidOnly=(true|false)

	@Test
	public void invoiceForIdNotFound() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/invoices/" +TEST_INVOICE_ID +"?withItems=false&withChildrenItems=false",
				HttpMethod.GET)
			.andRespond(withStatus(HttpStatus.NOT_FOUND)
				.body(new ClassPathResource("invoice-00.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = new Account(TEST_ACCOUNT_ID, TEST_TIME_ZONE);
		Invoice invoice = client.getInvoice(account, TEST_INVOICE_ID, false, false);

		// then
		assertThat("Invoice not found", invoice, nullValue());
	}

	@Test
	public void invoiceForId() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/invoices/" +TEST_INVOICE_ID +"?withItems=false&withChildrenItems=false",
				HttpMethod.GET)
			.andRespond(withSuccess()
				.body(new ClassPathResource("invoice-01.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = new Account(TEST_ACCOUNT_ID, TEST_TIME_ZONE);
		Invoice invoice = client.getInvoice(account, TEST_INVOICE_ID, false, false);

		// then
		assertThat("Invoice found", invoice, notNullValue());
		assertThat("Invoice ID", invoice.getId(), equalTo(TEST_INVOICE_ID));
		assertThat("Invoice created", invoice.getCreated(), equalTo(
				new LocalDate(2017, 8, 28).toDateTimeAtStartOfDay(DateTimeZone.forID(TEST_TIME_ZONE))));
		assertThat("Invoice amount", invoice.getAmount(), equalTo(new BigDecimal("0.14")));
		assertThat("Invoice balance", invoice.getBalance(), equalTo(new BigDecimal("0.14")));
		assertThat("Invoice currency code", invoice.getCurrencyCode(), equalTo("NZD"));
		assertThat("Invoice date", invoice.getInvoiceDate(), equalTo(new LocalDate(2017, 8, 28)));
		assertThat("Invoice number", invoice.getInvoiceNumber(), equalTo("3"));
		assertThat("Invoice time zone", invoice.getTimeZoneId(), equalTo(TEST_TIME_ZONE));

		assertThat("Invoice items empty", invoice.getItems(), emptyCollectionOf(InvoiceItem.class));
	}

	@Test
	public void invoiceForIdWithItems() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/invoices/" +TEST_INVOICE_ID +"?withItems=true&withChildrenItems=false",
				HttpMethod.GET)
			.andRespond(withSuccess()
				.body(new ClassPathResource("invoice-02.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Account account = new Account(TEST_ACCOUNT_ID, TEST_TIME_ZONE);
		Invoice invoice = client.getInvoice(account, TEST_INVOICE_ID, true, false);

		// then
		assertThat("Invoice found", invoice, notNullValue());
		assertThat("Invoice ID", invoice.getId(), equalTo(TEST_INVOICE_ID));
		assertThat("Invoice created", invoice.getCreated(), equalTo(
				new LocalDate(2017, 8, 28).toDateTimeAtStartOfDay(DateTimeZone.forID(TEST_TIME_ZONE))));
		assertThat("Invoice amount", invoice.getAmount(), equalTo(new BigDecimal("0.14")));
		assertThat("Invoice balance", invoice.getBalance(), equalTo(new BigDecimal("0.14")));
		assertThat("Invoice currency code", invoice.getCurrencyCode(), equalTo("NZD"));
		assertThat("Invoice date", invoice.getInvoiceDate(), equalTo(new LocalDate(2017, 8, 28)));
		assertThat("Invoice number", invoice.getInvoiceNumber(), equalTo("3"));
		assertThat("Invoice time zone", invoice.getTimeZoneId(), equalTo(TEST_TIME_ZONE));

		assertThat("Invoice items count", invoice.getItems(), hasSize(1));

		InvoiceItem item = invoice.getItems().get(0);
		assertThat("Item ID", item.getId(), equalTo("6ddf02a0-729a-4922-bfb5-571138d1eec1"));
		assertThat("Item created", item.getCreated(), equalTo(
				new LocalDate(2017, 3, 5).toDateTimeAtStartOfDay(DateTimeZone.forID(TEST_TIME_ZONE))));
		assertThat("Item ended", item.getEnded(), equalTo(
				new LocalDate(2017, 4, 5).toDateTimeAtStartOfDay(DateTimeZone.forID(TEST_TIME_ZONE))));
		assertThat("Item start date", item.getStartDate(), equalTo(new LocalDate(2017, 3, 5)));
		assertThat("Item end date", item.getEndDate(), equalTo(new LocalDate(2017, 4, 5)));

		assertThat("Item amount", item.getAmount(), equalTo(new BigDecimal("0.14")));
		assertThat("Item bundle ID", item.getBundleId(),
				equalTo("f5632c7d-bc89-4c88-aef2-ee52571e6793"));
		assertThat("Item currency code", item.getCurrencyCode(), equalTo("NZD"));
		assertThat("Item description", item.getDescription(),
				equalTo("posted-datum-metric-daily-usage"));
		assertThat("Item type", item.getItemType(), equalTo("USAGE"));
		assertThat("Item phase name", item.getPhaseName(),
				equalTo("api-posted-datum-metric-monthly-usage-evergreen"));
		assertThat("Item plan name", item.getPlanName(),
				equalTo("api-posted-datum-metric-monthly-usage"));
		assertThat("Item subscription ID", item.getSubscriptionId(),
				equalTo("f7ec79f7-02ff-4f91-b6ef-f0571ac69baf"));
		assertThat("Item subscription ID", item.getTimeZoneId(), equalTo(TEST_TIME_ZONE));
		assertThat("Item usage name", item.getUsageName(), equalTo("posted-datum-metric-daily-usage"));
	}

	@Test
	public void usagesForSubscription() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/usages/" +TEST_SUBSCRIPTION_ID +"?startDate=2017-01-01&endDate=2017-02-01",
				HttpMethod.GET)
			.andRespond(withSuccess()
				.body(new ClassPathResource("usages-01.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		SubscriptionUsageRecords records = client.usageRecordsForSubscription(TEST_SUBSCRIPTION_ID,
				new LocalDate(2017, 1, 1), new LocalDate(2017, 2, 1));

		// then
		assertThat("Records found", records, notNullValue());
		assertThat("Subscription ID", records.getSubscriptionId(),
				equalTo("f7ec79f7-02ff-4f91-b6ef-f0571ac69baf"));
		assertThat("Start date", records.getStartDate(), equalTo(new LocalDate(2017, 3, 5)));
		assertThat("End date", records.getEndDate(), equalTo(new LocalDate(2017, 4, 5)));

		assertThat("Rolled up units count", records.getRolledUpUnits(), hasSize(1));

		UnitRecord unitRec = records.getRolledUpUnits().get(0);
		assertThat("Unit type", unitRec.getUnitType(), equalTo("DatumMetrics"));
		assertThat("Amount", unitRec.getAmount(), equalTo(new BigDecimal("14079")));
	}

	@Test
	public void getSubscription() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/subscriptions/" +TEST_SUBSCRIPTION_ID, HttpMethod.GET)
			.andRespond(withSuccess()
				.body(new ClassPathResource("subscription-01.json", getClass()))
				.contentType(MediaType.APPLICATION_JSON_UTF8));
	    // @formatter:on

		// when
		Subscription result = client.getSubscription(TEST_SUBSCRIPTION_ID);

		// then
		assertThat("Subscription", result, notNullValue());
		assertThat("Subscription BCD", result.getBillCycleDayLocal(), equalTo(5));
		assertThat("Subscription BCD", result.getPhaseType(), equalTo("EVERGREEN"));
		assertThat("Subscription BCD", result.getPlanName(),
				equalTo("api-posted-datum-metric-monthly-usage"));
		assertThat("Subscription BCD", result.getProductCategory(), equalTo("BASE"));
		assertThat("Subscription BCD", result.getProductName(), equalTo("PostedDatumMetrics"));
		assertThat("Subscription ID", result.getSubscriptionId(),
				equalTo("4d944d7c-43fb-4ae5-9eb0-8774e904c943"));
	}

	@Test
	public void catalogTranslation_en_NZ() {
		// given
		// @formatter:off
		serverExpect("/1.0/kb/invoices/catalogTranslation/en_NZ", HttpMethod.GET)
			.andRespond(withSuccess()
				.body(new ClassPathResource("catalog-translation-01_en_NZ.properties", getClass()))
				.contentType(MediaType.TEXT_PLAIN));
	    // @formatter:on

		// when
		Properties props = client.invoiceCatalogTranslation("en_NZ");

		// then
		assertThat(props, equalTo(Collections.singletonMap("greeting", "G''day, mate!")));
	}

}
