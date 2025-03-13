/* ==================================================================
 * BaseRestOperationsCloudDatumStreamService.java - 7/10/2024 7:41:43 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.http.CachableRequestEntity;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.support.HttpOperations;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Base implementation of
 * {@link net.solarnetwork.central.c2c.biz.CloudDatumStreamService} with
 * {@link RestOperations} support.
 *
 * @author matt
 * @version 1.4
 */
public abstract class BaseRestOperationsCloudDatumStreamService extends BaseCloudDatumStreamService
		implements HttpOperations {

	/** The REST operations helper. */
	protected final RestOperationsHelper restOpsHelper;

	private Cache<CachableRequestEntity, Result<?>> httpCache;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
	 * @param clock
	 *        the clock to use
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param expressionService
	 *        the expression service
	 * @param integrationDao
	 *        the integration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamMappingDao
	 *        the datum stream mapping DAO
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param settings
	 *        the service settings
	 * @param restOpsHelper
	 *        the REST operations helper
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseRestOperationsCloudDatumStreamService(String serviceIdentifier, String displayName,
			Clock clock, UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			List<SettingSpecifier> settings, RestOperationsHelper restOpsHelper) {
		super(serviceIdentifier, displayName, clock, userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, settings);
		this.restOpsHelper = requireNonNullArgument(restOpsHelper, "restOpsHelper");
	}

	@Override
	public void setUserServiceAuditor(UserServiceAuditor userServiceAuditor) {
		super.setUserServiceAuditor(userServiceAuditor);
		restOpsHelper.setUserServiceAuditor(userServiceAuditor);
	}

	@Override
	public <I, O> ResponseEntity<O> http(HttpMethod method, URI uri, HttpHeaders headers, I body,
			Class<O> responseType, Object context) {
		RequestEntity<I> req = RequestEntity.method(method, uri).headers(headers).body(body);
		return restOpsHelper.http(req, responseType, context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O> Result<O> httpGet(String uri, Map<String, ?> parameters, Map<String, ?> headers,
			Class<O> responseType, Object context) {
		URI u = HttpOperations.uri(uri, parameters);
		HttpHeaders h = HttpOperations.headersForMap(headers);
		CachableRequestEntity req = new CachableRequestEntity(context, h, HttpMethod.GET, u);

		Result<O> result = null;
		if ( httpCache != null ) {
			result = (Result<O>) httpCache.get(req);
		}
		if ( result == null ) {
			ResponseEntity<O> res = restOpsHelper.http(req, responseType, context);
			result = Result.success(res.getBody());
			if ( httpCache != null ) {
				httpCache.put(req, result);
			}
		}

		return result;
	}

	/**
	 * Get the HTTP cache.
	 *
	 * @return the cache
	 * @since 1.4
	 */
	public final Cache<CachableRequestEntity, Result<?>> getHttpCache() {
		return httpCache;
	}

	/**
	 * Set the HTTP cache.
	 *
	 * @param httpCache
	 *        the cache to set
	 * @since 1.4
	 */
	public final void setHttpCache(Cache<CachableRequestEntity, Result<?>> httpCache) {
		this.httpCache = httpCache;
	}

}
