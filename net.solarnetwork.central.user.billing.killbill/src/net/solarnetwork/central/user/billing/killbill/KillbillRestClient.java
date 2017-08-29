/* ==================================================================
 * KillbillRestClient.java - 21/08/2017 10:37:52 AM
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

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.LocalDate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.BundleSubscription;
import net.solarnetwork.central.user.billing.killbill.domain.Invoice;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.SubscriptionUsage;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.central.user.billing.killbill.domain.UsageUnitRecord;
import net.solarnetwork.web.support.LoggingHttpRequestInterceptor;

/**
 * REST implementation of {@link KillbillClient}.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillRestClient implements KillbillClient {

	/** The default base URL for the production service. */
	public static final String DEFAULT_BASE_URL = "https://billing.solarnetwork.net";

	public static final String HEADER_PAGINATION_OFFSET = "X-Killbill-Pagination-CurrentOffset";
	public static final String HEADER_PAGINATION_TOTAL_COUNT = "X-Killbill-Pagination-TotalNbRecords";
	public static final String HEADER_PAGINATION_MAX_COUNT = "X-Killbill-Pagination-MaxNbRecords";

	private static final ParameterizedTypeReference<List<Bundle>> BUNDLE_LIST_TYPE = new ParameterizedTypeReference<List<Bundle>>() {
	};

	private static final ParameterizedTypeReference<List<Invoice>> INVOICE_LIST_TYPE = new ParameterizedTypeReference<List<Invoice>>() {
	};

	private String baseUrl = DEFAULT_BASE_URL;
	private String username = "solaruser";
	private String password = "changeit";
	private String apiKey = "solarnetwork";
	private String apiSecret = "changeit";

	private final RestOperations client;

	private static RestTemplate defaultRestTemplate() {
		// note this does not handle dynamic logging level changes, but this is just for development
		ClientHttpRequestFactory reqFactory = LoggingHttpRequestInterceptor.requestFactory();
		RestTemplate restTemplate = new RestTemplate(reqFactory);
		if ( LoggingHttpRequestInterceptor.supportsLogging(reqFactory) ) {
			restTemplate.getInterceptors().add(new LoggingHttpRequestInterceptor());
		}
		return restTemplate;
	}

	/**
	 * Default constructor.
	 */
	public KillbillRestClient() {
		this(defaultRestTemplate());
	}

	/**
	 * Construct with a RestTemplate.
	 * 
	 * <p>
	 * The {@link ObjectMapper} configured on any
	 * {@link MappingJackson2HttpMessageConverter} will have the serial
	 * inclusion setting set to {@link Include#NON_NULL}.
	 * </p>
	 * 
	 * @param template
	 *        the template to use
	 */
	public KillbillRestClient(RestTemplate template) {
		super();
		client = template;

		// set to our own ObjectMapper to ensure we have Joda support and NON_NULL inclusion
		for ( HttpMessageConverter<?> converter : template.getMessageConverters() ) {
			if ( converter instanceof MappingJackson2HttpMessageConverter ) {
				MappingJackson2HttpMessageConverter messageConverter = (MappingJackson2HttpMessageConverter) converter;
				messageConverter.setObjectMapper(KillbillUtils.defaultObjectMapper());
			}
		}

		setupRestTemplateInterceptors();
	}

	private void setupRestTemplateInterceptors() {
		RestTemplate restTemplate = (RestTemplate) client;
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		if ( restTemplate.getInterceptors() != null ) {
			interceptors.addAll(restTemplate.getInterceptors().stream()
					.filter(o -> !(o instanceof KillbillAuthorizationInterceptor))
					.collect(Collectors.toList()));
		}
		interceptors.add(0, new KillbillAuthorizationInterceptor(username, password, apiKey, apiSecret));
		restTemplate.setInterceptors(interceptors);
	}

	private String kbUrl(String path) {
		return baseUrl + path;
	}

	private <T> T getForObjectOrNull(URI uri, Class<T> responseType) throws RestClientException {
		try {
			return client.getForObject(uri, responseType);
		} catch ( HttpStatusCodeException e ) {
			if ( HttpStatus.NOT_FOUND.equals(e.getStatusCode()) ) {
				return null;
			}
			throw e;
		}
	}

	private <T> T getForObjectOrNull(URI uri, ParameterizedTypeReference<T> responseType)
			throws RestClientException {
		try {
			ResponseEntity<T> result = client.exchange(uri, HttpMethod.GET, null, responseType);
			return result.getBody();
		} catch ( HttpStatusCodeException e ) {
			if ( HttpStatus.NOT_FOUND.equals(e.getStatusCode()) ) {
				return null;
			}
			throw e;
		}
	}

	@Override
	public Account accountForExternalKey(String key) {
		URI uri = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/accounts"))
				.queryParam("externalKey", key).build().toUri();
		return getForObjectOrNull(uri, Account.class);
	}

	private static String idFromLocation(URI loc) {
		String path = loc.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	@Override
	public String createAccount(Account info) {
		URI loc = client.postForLocation(kbUrl("/1.0/kb/accounts"), info);
		return idFromLocation(loc);
	}

	@Override
	public String addPaymentMethodToAccount(Account account, Map<String, Object> paymentData,
			boolean defaultMethod) {
		Map<String, Object> uriVariables = Collections.singletonMap("accountId", account.getAccountId());
		URI uri = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/accounts/{accountId}/paymentMethods"))
				.queryParam("isDefault", defaultMethod).buildAndExpand(uriVariables).toUri();
		URI loc = client.postForLocation(uri, paymentData);
		return idFromLocation(loc);
	}

	@Override
	public Bundle bundleForExternalKey(Account account, String key) {
		URI uri = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/bundles"))
				.queryParam("externalKey", key).build().toUri();
		List<Bundle> results = getForObjectOrNull(uri, BUNDLE_LIST_TYPE);
		return (results != null && !results.isEmpty() ? results.get(0) : null);
	}

	@Override
	public String createBundle(Account account, LocalDate requestedDate, Bundle info) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/subscriptions"));
		if ( requestedDate != null ) {
			builder.queryParam("requestedDate", ISO_DATE_FORMATTER.print(requestedDate));
		}
		Bundle bundle = (Bundle) info.clone();
		bundle.setAccountId(account.getAccountId());
		URI uri = builder.build().toUri();
		URI loc = client.postForLocation(uri, new BundleSubscription(bundle));
		return idFromLocation(loc);
	}

	@Override
	public void addUsage(Subscription subscription, String unit, List<UsageRecord> usage) {
		SubscriptionUsage su = new SubscriptionUsage(subscription,
				Collections.singletonList(new UsageUnitRecord(unit, usage)));
		URI uri = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/usages")).build().toUri();
		client.postForObject(uri, su, Void.class);
	}

	private void applyAccountSettings(Account account, Collection<Invoice> invoices) {
		if ( account == null || invoices == null || invoices.isEmpty() ) {
			return;
		}
		if ( account.getTimeZone() != null ) {
			final String tz = account.getTimeZone();
			invoices.parallelStream().forEach(invoice -> invoice.setTimeZoneId(tz));
		}
	}

	@Override
	public Invoice getInvoice(Account account, String invoiceId, boolean withItems,
			boolean withChildrenItems) {
		Map<String, Object> uriVariables = Collections.singletonMap("invoiceId", invoiceId);
		URI uri = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/invoices/{invoiceId}"))
				.queryParam("withItems", withItems).queryParam("withChildrenItems", withChildrenItems)
				.buildAndExpand(uriVariables).toUri();
		Invoice result = getForObjectOrNull(uri, Invoice.class);
		if ( result != null ) {
			applyAccountSettings(account, Collections.singleton(result));
		}
		return result;
	}

	@Override
	public List<Invoice> listInvoices(Account account, boolean unpaidOnly) {
		Map<String, Object> uriVariables = Collections.singletonMap("accountId", account.getAccountId());
		URI uri = UriComponentsBuilder.fromHttpUrl(kbUrl("/1.0/kb/accounts/{accountId}/invoices"))
				.queryParam("unpaidInvoicesOnly", unpaidOnly).buildAndExpand(uriVariables).toUri();
		List<Invoice> results = getForObjectOrNull(uri, INVOICE_LIST_TYPE);

		// make sure invoice time zone set to account time zone
		applyAccountSettings(account, results);

		// reverse
		if ( results != null ) {
			Collections.reverse(results);
		}

		return (results != null ? results : Collections.emptyList());
	}

	private static final BigDecimal ZERO = new BigDecimal(0);

	@Override
	public FilterResults<Invoice> findInvoices(Account account, InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {

		final int origOffset = (offset != null ? offset.intValue() : 0);
		int pageSize = (max != null ? max.intValue() : 0);

		// the /search endpoint does not return amounts; if paid flag specified then resort to
		// calling listInvoices() and just manually filtering the results
		if ( filter != null && filter.getUnpaid() != null ) {
			List<Invoice> allInvoices = listInvoices(account, filter.getUnpaid());
			if ( filter.getUnpaid() == false ) {
				// KB returns paid/unpaid mixed; filter to just paid here (balance <= 0)
				allInvoices = allInvoices.parallelStream()
						.filter(invoice -> invoice.getBalance().compareTo(ZERO) < 1)
						.collect(Collectors.toList());
			}
			List<Invoice> resultInvoices;
			if ( origOffset > allInvoices.size() ) {
				resultInvoices = Collections.emptyList();
			} else {
				resultInvoices = allInvoices.subList(origOffset,
						Math.min(origOffset + pageSize, allInvoices.size()));
			}
			return new BasicFilterResults<>(resultInvoices, (long) allInvoices.size(), origOffset,
					resultInvoices.size());
		}

		Map<String, Object> uriVariables = Collections.singletonMap("accountId", account.getAccountId());

		// we need to sort the results in reverse; so have to find total results first to flip query
		UriComponentsBuilder uriBuilder = UriComponentsBuilder
				.fromHttpUrl(kbUrl("/1.0/kb/invoices/search/{accountId}")).queryParam("withItems", false)
				.queryParam("offset", 0).queryParam("limit", 0);
		HttpHeaders headers = client.headForHeaders(uriBuilder.buildAndExpand(uriVariables).toUri());
		String paginationTotal = headers.getFirst(HEADER_PAGINATION_TOTAL_COUNT);

		long totalCount = 0;
		if ( paginationTotal != null ) {
			totalCount = Long.parseLong(paginationTotal);
		}

		long rOffset = totalCount - origOffset - pageSize;
		if ( rOffset < 0 ) {
			if ( totalCount > pageSize ) {
				pageSize = (int) (pageSize + rOffset);
			}
			rOffset = 0;
		}

		uriBuilder.replaceQueryParam("offset", rOffset).replaceQueryParam("limit", pageSize);

		ResponseEntity<List<Invoice>> response = client.exchange(
				uriBuilder.buildAndExpand(uriVariables).toUri(), HttpMethod.GET, null,
				INVOICE_LIST_TYPE);

		List<Invoice> invoices = response.getBody();

		// make sure invoice time zone set to account time zone
		applyAccountSettings(account, invoices);

		// reverse results
		Collections.reverse(invoices);

		paginationTotal = response.getHeaders().getFirst(HEADER_PAGINATION_TOTAL_COUNT);

		BasicFilterResults<Invoice> results = new BasicFilterResults<>(invoices,
				paginationTotal != null ? Long.valueOf(paginationTotal)
						: invoices != null ? invoices.size() : 0L,
				origOffset, invoices != null ? invoices.size() : 0);

		return results;
	}

	/**
	 * The Killbill base URL for REST operations.
	 * 
	 * @param baseUrl
	 *        the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * The Killbill username to use.
	 * 
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		if ( username != null && !username.equals(this.username) ) {
			this.username = username;
			setupRestTemplateInterceptors();
		}
	}

	/**
	 * The Killbill password to use.
	 * 
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		if ( password != null && !password.equals(this.password) ) {
			this.password = password;
			setupRestTemplateInterceptors();
		}
	}

	/**
	 * The Killbill API key to use.
	 * 
	 * @param apiKey
	 *        the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		if ( apiKey != null && !apiKey.equals(this.apiKey) ) {
			this.apiKey = apiKey;
			setupRestTemplateInterceptors();
		}
	}

	/**
	 * The Killbill API secret to use.
	 * 
	 * @param apiSecret
	 *        the apiSecret to set
	 */
	public void setApiSecret(String apiSecret) {
		if ( apiSecret != null && !apiSecret.equals(this.apiSecret) ) {
			this.apiSecret = apiSecret;
			setupRestTemplateInterceptors();
		}
	}

}
