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
import static net.solarnetwork.central.user.billing.killbill.test.JsonObjectMatchers.jsonBodyObject;
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
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.billing.killbill.KillbillRestClient;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;

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

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setSerializationInclusion(Include.NON_NULL)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

	private MockRestServiceServer server;
	private KillbillRestClient client;

	@Before
	public void setup() {
		// setup RestTemplate's ObjectMapper so we know exactly what we are converting
		RestTemplate restTemplate = new RestTemplate();
		for ( ListIterator<HttpMessageConverter<?>> itr = restTemplate.getMessageConverters()
				.listIterator(); itr.hasNext(); ) {
			if ( itr.next() instanceof MappingJackson2HttpMessageConverter ) {
				MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
				messageConverter.setPrettyPrint(false);
				messageConverter.setObjectMapper(OBJECT_MAPPER);
				itr.set(messageConverter);
				break;
			}
		}

		server = MockRestServiceServer.createServer(restTemplate);
		client = new KillbillRestClient(restTemplate);
		client.setApiKey(TEST_API_KEY);
		client.setApiSecret(TEST_API_SECRET);
		client.setBaseUrl(TEST_BASE_URL);
		client.setUsername(TEST_USERNAME);
		client.setPassword(TEST_PASSWORD);
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
		.andExpect(jsonBodyObject(account))
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
			.andExpect(jsonBodyObject(paymentData))
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

}
