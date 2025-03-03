/* ==================================================================
 * BasicCloudIntegrationsExpressionService.java - 8/10/2024 8:06:13 am
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
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.expression.Expression;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.datum.domain.DatumExpressionRoot;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import net.solarnetwork.service.ExpressionService;

/**
 * Basic implementation of {@link CloudIntegrationsExpressionService}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicCloudIntegrationsExpressionService implements CloudIntegrationsExpressionService {

	private final PathMatcher sourceIdPathMatcher;
	private final ExpressionService expressionService;
	private final SolarNodeOwnershipDao nodeOwnershipDao;

	private Cache<String, Expression> expressionCache;
	private SolarNodeMetadataReadOnlyDao metadataDao;
	private Cache<ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleCache;

	private static PathMatcher defaultSourceIdPathMatcher() {
		var pm = new AntPathMatcher();
		pm.setCachePatterns(false);
		pm.setCaseSensitive(false);
		return pm;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This uses a default {@link AntPathMatcher} and the
	 * {@link SpelExpressionService}.
	 * </p>
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicCloudIntegrationsExpressionService(SolarNodeOwnershipDao nodeOwnershipDao) {
		this(nodeOwnershipDao, defaultSourceIdPathMatcher(), new SpelExpressionService());
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This uses a default {@link AntPathMatcher}.
	 * </p>
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 * @param expressionService
	 *        the expression service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicCloudIntegrationsExpressionService(SolarNodeOwnershipDao nodeOwnershipDao,
			ExpressionService expressionService) {
		this(nodeOwnershipDao, defaultSourceIdPathMatcher(), expressionService);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 * @param sourceIdPathMatcher
	 *        the source ID path matcher to use
	 * @param expressionService
	 *        the expression service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicCloudIntegrationsExpressionService(SolarNodeOwnershipDao nodeOwnershipDao,
			PathMatcher sourceIdPathMatcher, ExpressionService expressionService) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.sourceIdPathMatcher = requireNonNullArgument(sourceIdPathMatcher, "sourceIdPathMatcher");
		this.expressionService = requireNonNullArgument(expressionService, "expressionService");
	}

	@Override
	public PathMatcher sourceIdPathMatcher() {
		return sourceIdPathMatcher;
	}

	@Override
	public DatumExpressionRoot createDatumExpressionRoot(Datum datum, Map<String, ?> parameters,
			DatumMetadataOperations metadata, DatumStreamsAccessor datumStreamsAccessor) {
		Map<String, ?> p = parameters;

		// for node datum, lookup ownership so we have access to the node's time zone
		if ( datum != null && datum.getKind() == ObjectDatumKind.Node && datum.getObjectId() != null ) {
			SolarNodeOwnership node = nodeOwnershipDao.ownershipForNodeId(datum.getObjectId());
			if ( node != null ) {
				Map<String, Object> params = new LinkedHashMap<>(
						parameters != null ? parameters : Collections.emptyMap());
				params.put("node", node);
				p = params;
			}
		}

		return new DatumExpressionRoot(datum, datum != null ? datum.asSampleOperations() : null, p,
				metadata, datumStreamsAccessor, this::nodeMetadata, this::tariffSchedule);
	}

	private DatumMetadataOperations nodeMetadata(ObjectDatumStreamMetadataId id) {
		SolarNodeMetadata meta = (id != null && id.getKind() == ObjectDatumKind.Node
				&& id.getObjectId() != null && metadataDao != null ? metadataDao.get(id.getObjectId())
						: null);
		return (meta != null ? meta.getMetadata() : null);
	}

	private TariffSchedule tariffSchedule(DatumMetadataOperations meta, ObjectDatumStreamMetadataId id) {
		if ( meta == null || id == null || id.getSourceId() == null ) {
			return null;
		}
		final Cache<ObjectDatumStreamMetadataId, TariffSchedule> cache = getTariffScheduleCache();
		TariffSchedule result = null;
		if ( tariffScheduleCache != null ) {
			result = cache.get(id);
		}
		if ( result == null ) {
			Object tariffData = meta.metadataAtPath(id.getSourceId());
			if ( tariffData != null ) {
				Locale locale = metadataLocale(meta, id.getSourceId());
				try {
					result = TariffUtils.parseCsvTemporalRangeSchedule(locale, true, true, null,
							tariffData);
					if ( result != null && cache != null ) {
						cache.put(id, result);
					}
				} catch ( Exception e ) {
					// ignore, continue
					String msg = "Error parsing tariff schedule at %s %d metadata path [%s]: %s"
							.formatted(id.getKind(), id.getObjectId(), id.getSourceId(), e.getMessage());
					throw new IllegalArgumentException(msg);
				}
			}
		}
		return result;
	}

	private Locale metadataLocale(DatumMetadataOperations meta, String path) {
		String[] components = StringUtils.delimitedListToStringArray(path, "/");
		for ( int idx = components.length; idx > 0; idx-- ) {
			String[] cmp = new String[idx];
			System.arraycopy(components, 0, cmp, 0, cmp.length);
			cmp[idx - 1] = cmp[idx - 1] + "-locale";
			String localePath = StringUtils.arrayToDelimitedString(cmp, "/");
			String localeValue = meta.metadataAtPath(localePath, String.class);
			if ( localeValue != null ) {
				try {
					return Locale.forLanguageTag(localeValue);
				} catch ( Exception e ) {
					// ignore and continue
				}
			}
			cmp[idx - 1] = "locale";
			localePath = StringUtils.arrayToDelimitedString(cmp, "/");
			localeValue = meta.metadataAtPath(localePath, String.class);
			if ( localeValue != null ) {
				try {
					return Locale.forLanguageTag(localeValue);
				} catch ( Exception e ) {
					// ignore and continue
				}
			}
		}
		return Locale.getDefault();
	}

	@Override
	public <T> T evaluateDatumPropertyExpression(CloudDatumStreamPropertyConfiguration property,
			Object root, Map<String, Object> variables, Class<T> resultClass) {
		Expression expr = expression(property);
		return expressionService.evaluateExpression(expr, variables, root,
				SpelExpressionService.DEFAULT_EVALUATION_CONTEXT, resultClass);
	}

	private Expression expression(CloudDatumStreamPropertyConfiguration property) {
		final Cache<String, Expression> cache = getExpressionCache();
		Expression result = null;
		String cacheKey = null;
		if ( cache != null ) {
			cacheKey = Base64.getUrlEncoder()
					.encodeToString(DigestUtils.sha512_224(property.getValueReference()));
			result = cache.get(cacheKey);
		}
		if ( result == null ) {
			result = expressionService.parseExpression(property.getValueReference());
			if ( cacheKey != null ) {
				cache.put(cacheKey, result);
			}
		}
		return result;
	}

	/**
	 * Get the expression cache.
	 *
	 * <p>
	 * The keys of the cache are SHA hashes of the expression value.
	 * </p>
	 *
	 * @return the expression cache, or {@literal null}
	 */
	public final Cache<String, Expression> getExpressionCache() {
		return expressionCache;
	}

	/**
	 * Set the expression cache.
	 *
	 * @param expressionCache
	 *        the expression cache to set
	 */
	public final void setExpressionCache(Cache<String, Expression> expressionCache) {
		this.expressionCache = expressionCache;
	}

	/**
	 * Get the metadata DAO.
	 *
	 * @return the metadata DAO
	 */
	public final SolarNodeMetadataReadOnlyDao getMetadataDao() {
		return metadataDao;
	}

	/**
	 * Set the metadata DAO.
	 *
	 * @param metadataDao
	 *        the DAO to set
	 */
	public final void setMetadataDao(SolarNodeMetadataReadOnlyDao metadataDao) {
		this.metadataDao = metadataDao;
	}

	/**
	 * Get the tariff schedule cache.
	 *
	 * @return the cache
	 */
	public final Cache<ObjectDatumStreamMetadataId, TariffSchedule> getTariffScheduleCache() {
		return tariffScheduleCache;
	}

	/**
	 * Set the tariff schedule cache.
	 *
	 * @param tariffScheduleCache
	 *        the cache to set
	 */
	public final void setTariffScheduleCache(
			Cache<ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleCache) {
		this.tariffScheduleCache = tariffScheduleCache;
	}

}
