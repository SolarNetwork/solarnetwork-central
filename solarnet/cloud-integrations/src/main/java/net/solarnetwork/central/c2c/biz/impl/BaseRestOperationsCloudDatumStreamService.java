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

import static net.solarnetwork.central.domain.CommonUserEvents.eventForUserRelatedKey;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.springframework.core.retry.RetryOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.common.http.HttpExchange;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Base implementation of
 * {@link net.solarnetwork.central.c2c.biz.CloudDatumStreamService} with
 * {@link RestOperations} support.
 *
 * @author matt
 * @version 2.1
 */
public abstract class BaseRestOperationsCloudDatumStreamService extends BaseCloudDatumStreamService
		implements HttpOperations {

	/** The REST operations helper. */
	protected final RestOperationsHelper restOpsHelper;

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
	 *         if any argument is {@code null}
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
	public void didSetUserServiceAuditor(@Nullable UserServiceAuditor userServiceAuditor) {
		restOpsHelper.setUserServiceAuditor(userServiceAuditor);
	}

	@Override
	protected void didSetRetryOps(@Nullable RetryOperations retryOps) {
		restOpsHelper.setRetryOps(retryOps);
	}

	@Override
	public <I, O> HttpExchange<I, O> http(HttpMethod method, URI uri, @Nullable HttpHeaders headers,
			@Nullable I body, Class<O> responseType, @Nullable Object context,
			@Nullable Map<String, ?> runtimeData) {
		return restOpsHelper.http(method, uri, headers, body, responseType, context, runtimeData);
	}

	@Override
	public <O> Result<O> httpGet(String uri, @Nullable Map<String, ?> parameters,
			@Nullable Map<String, ?> headers, Class<O> responseType, @Nullable Object context,
			@Nullable Map<String, ?> runtimeData) {
		return restOpsHelper.httpGet(uri, parameters, headers, responseType, context, runtimeData);
	}

	/**
	 * Validate an energy data value is within a maximum threshold factor.
	 *
	 * @param datumStream
	 *        the datum stream
	 * @param request
	 *        the HTTP request that generated the data value
	 * @param valueReference
	 *        the reference to the data value being validated
	 * @param refParameters
	 *        parameters to resolve placeholders in {@code valueReference} with
	 * @param energyValue
	 *        the energy value to validate
	 * @param ratedPower
	 *        the rated power of the device
	 * @param energyValidationThreshold
	 *        the energy validation threshold to use
	 * @param prevTs
	 *        the previous datum timestamp for the same device
	 * @param datumIdent
	 *        the current datum ID with the energy value being validated
	 * @return list of validation records, or empty list if no issues found
	 * @since 2.1
	 */
	protected List<DatumAuxiliaryRecord> validateEnergyDataValue(
			CloudDatumStreamConfiguration datumStream, RequestEntity<?> request, String valueReference,
			Map<String, ?> refParameters, Number energyValue, Integer ratedPower,
			double energyValidationThreshold, Instant prevTs, DatumIdentity datumIdent) {
		final long secondsDiff = ChronoUnit.SECONDS.between(prevTs, datumIdent.getTimestamp());
		final double expectedMaxEnergy = ratedPower.doubleValue() * secondsDiff / 3600.0
				* energyValidationThreshold;
		if ( Math.abs(energyValue.doubleValue()) <= expectedMaxEnergy ) {
			return List.of();
		}
		// generate validation event
		final String sourceRef = nonnull(StringUtils.expandTemplateString(valueReference, refParameters),
				"Source ref");

		final String errMsg = "Source [%s] %s energy reading [%.1f] @ %s more than %.1fx larger than expected max [%.1f] from device rating [%d]."
				.formatted(datumIdent.getSourceId(), sourceRef, energyValue.doubleValue(),
						datumIdent.getTimestamp(), energyValidationThreshold,
						expectedMaxEnergy / energyValidationThreshold, ratedPower);
		log.warn(errMsg);

		final URI reqUri = restOpsHelper.maskedUri(request.getUrl());

		final var eventParams = new LinkedHashMap<String, Object>(8);
		eventParams.put(SOURCE_DATA_KEY, sourceRef);
		eventParams.put(SOURCE_ID_DATA_KEY, datumIdent.getSourceId());
		eventParams.put(DatumAuxiliary.TIMESTAMP_META_KEY, datumIdent.getTimestamp());
		eventParams.put("dataValue", energyValue);
		eventParams.put(DURATION_DATA_KEY, secondsDiff * 1000);
		eventParams.put("validationThreshold", energyValidationThreshold);
		eventParams.put(DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY, expectedMaxEnergy);
		eventParams.put(DatumAuxiliary.RATED_POWER_META_KEY, ratedPower);

		final var reqParams = new LinkedHashMap<>(2);
		reqParams.put(HTTP_URI_DATA_KEY, reqUri);
		if ( request.getBody() != null ) {
			reqParams.put(HTTP_BODY_DATA_KEY, request.getBody());
		}
		eventParams.put(REQUEST_TAG, reqParams);

		// generate validation event
		userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
				datumStream.getId(), DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS, errMsg, eventParams));

		// generate validation Mark
		return DatumAuxiliary.createDataValueOverThresholdFactorValidationRecords("Energy reading",
				DatumValidationType.EnergySpike.getKey(), sourceRef, reqUri, request.getBody(),
				energyValue, energyValidationThreshold, expectedMaxEnergy, ratedPower,
				secondsDiff * 1000L, datumIdent);
	}

	/**
	 * Validate the time difference between two datum.
	 *
	 * @param datumStream
	 *        the datum stream
	 * @param request
	 *        the HTTP request that generated the data value
	 * @param valueReference
	 *        the reference to the data value being validated
	 * @param refParameters
	 *        parameters to resolve placeholders in {@code valueReference} with
	 * @param timeGapThreshold
	 *        the maximum amount of time between {@code prevTs} and
	 *        {@code datum}'s timestamp to allow
	 * @param prevTs
	 *        the previous datum timestamp for the same device
	 * @param datumIdent
	 *        the current datum ID with the energy value being validated
	 * @return list of validation records, or empty list if no issues found
	 * @since 2.1
	 */
	@SuppressWarnings("JavaDurationGetSecondsToToSeconds")
	protected List<DatumAuxiliaryRecord> validateTimeGap(CloudDatumStreamConfiguration datumStream,
			RequestEntity<?> request, String valueReference, @Nullable Map<String, ?> refParameters,
			Duration timeGapThreshold, Instant prevTs, DatumIdentity datumIdent) {
		final long timeDiffSecs = ChronoUnit.SECONDS.between(prevTs, datumIdent.getTimestamp());
		if ( timeGapThreshold.getSeconds() >= timeDiffSecs ) {
			return List.of();
		}

		final String sourceRef = nonnull(StringUtils.expandTemplateString(valueReference, refParameters),
				"Source ref");

		final String errMsg = "Source [%s] %s time gap %ds @ %s not within threshold %s.".formatted(
				datumIdent.getSourceId(), sourceRef, timeDiffSecs, datumIdent.getTimestamp(),
				timeGapThreshold.toString());
		log.warn(errMsg);

		final URI reqUri = restOpsHelper.maskedUri(request.getUrl());

		final var eventParams = new LinkedHashMap<String, Object>(8);
		eventParams.put(SOURCE_DATA_KEY, sourceRef);
		eventParams.put(SOURCE_ID_DATA_KEY, datumIdent.getSourceId());
		eventParams.put(DatumAuxiliary.TIMESTAMP_META_KEY, datumIdent.getTimestamp());
		eventParams.put(DURATION_DATA_KEY, timeDiffSecs * 1000);
		eventParams.put(DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY, timeGapThreshold.toString());

		final var reqParams = new LinkedHashMap<>(2);
		reqParams.put(HTTP_URI_DATA_KEY, reqUri);
		if ( request.getBody() != null ) {
			reqParams.put(HTTP_BODY_DATA_KEY, request.getBody());
		}
		eventParams.put(REQUEST_TAG, reqParams);

		// generate validation event
		userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
				datumStream.getId(), DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS, errMsg, eventParams));

		// generate validation Mark
		return DatumAuxiliary.createTimeGapValidationRecords(DatumValidationType.TimeGap.getKey(),
				sourceRef, reqUri, request.getBody(), prevTs, timeGapThreshold, datumIdent);
	}

	/**
	 * Get the REST helper.
	 *
	 * @return the REST helper
	 */
	public final RestOperationsHelper getRestOpsHelper() {
		return restOpsHelper;
	}

	/**
	 * Get the HTTP cache.
	 *
	 * @return the cache
	 * @since 1.4
	 */
	public final @Nullable Cache<CachableRequestEntity, Result<?>> getHttpCache() {
		return restOpsHelper.getHttpCache();
	}

	/**
	 * Set the HTTP cache.
	 *
	 * @param httpCache
	 *        the cache to set
	 * @since 1.4
	 */
	public final void setHttpCache(@Nullable Cache<CachableRequestEntity, Result<?>> httpCache) {
		restOpsHelper.setHttpCache(httpCache);
	}

	/**
	 * Get the "allow local hosts" mode.
	 *
	 * @return {@code true} to allow HTTP requests to local hosts; defaults to
	 *         {@code false}
	 * @since 1.4
	 */
	public final boolean isAllowLocalHosts() {
		return restOpsHelper.isAllowLocalHosts();
	}

	/**
	 * Set the "allow local hosts" mode.
	 *
	 * @param allowLocalHosts
	 *        {@code true} to allow HTTP requests to local hosts
	 * @since 1.4
	 */
	public final void setAllowLocalHosts(boolean allowLocalHosts) {
		restOpsHelper.setAllowLocalHosts(allowLocalHosts);
	}

}
