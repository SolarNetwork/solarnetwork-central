/* ==================================================================
 * BasicInstructionsExpressionService.java - 19/11/2025 2:42:49 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.expression.Expression;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.dao.UserMetadataReadOnlyDao;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.user.biz.InstructionsExpressionService;
import net.solarnetwork.central.user.dao.UserSecretAccessDao;
import net.solarnetwork.central.user.domain.NodeInstructionExpressionRoot;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import net.solarnetwork.service.ExpressionService;

/**
 * Basic implementation of {@link InstructionsExpressionService}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicInstructionsExpressionService implements InstructionsExpressionService {

	private final PathMatcher sourceIdPathMatcher;
	private final ExpressionService expressionService;

	private Cache<String, Expression> expressionCache;
	private UserMetadataReadOnlyDao userMetadataDao;
	private SolarNodeMetadataReadOnlyDao nodeMetadataDao;
	private UserSecretAccessDao userSecretAccessDao;

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
	 */
	public BasicInstructionsExpressionService() {
		this(defaultSourceIdPathMatcher(), new SpelExpressionService());
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This uses a default {@link AntPathMatcher}.
	 * </p>
	 *
	 * @param expressionService
	 *        the expression service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicInstructionsExpressionService(ExpressionService expressionService) {
		this(defaultSourceIdPathMatcher(), expressionService);
	}

	/**
	 * Constructor.
	 *
	 * @param sourceIdPathMatcher
	 *        the source ID path matcher to use
	 * @param expressionService
	 *        the expression service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicInstructionsExpressionService(PathMatcher sourceIdPathMatcher,
			ExpressionService expressionService) {
		super();
		this.sourceIdPathMatcher = requireNonNullArgument(sourceIdPathMatcher, "sourceIdPathMatcher");
		this.expressionService = requireNonNullArgument(expressionService, "expressionService");
	}

	@Override
	public PathMatcher sourceIdPathMatcher() {
		return sourceIdPathMatcher;
	}

	@Override
	public Expression parseExpression(final String expression) {
		final Cache<String, Expression> cache = getExpressionCache();
		Expression result = null;
		String cacheKey = null;
		if ( cache != null ) {
			cacheKey = Base64.getUrlEncoder().encodeToString(DigestUtils.sha512_224(expression));
			result = cache.get(cacheKey);
		}
		if ( result == null ) {
			result = expressionService.parseExpression(expression);
			if ( cacheKey != null ) {
				cache.put(cacheKey, result);
			}
		}
		return result;
	}

	@Override
	public <T> T evaluateExpression(Expression expression, Object root, Map<String, Object> variables,
			Class<T> resultClass) {
		return expressionService.evaluateExpression(expression, variables, root,
				SpelExpressionService.DEFAULT_EVALUATION_CONTEXT, resultClass);
	}

	@Override
	public NodeInstructionExpressionRoot createNodeInstructionExpressionRoot(SolarNodeOwnership owner,
			NodeInstruction instruction, Map<String, ?> parameters,
			DatumStreamsAccessor datumStreamsAccessor, HttpOperations httpOperations) {
		return new NodeInstructionExpressionRoot(owner, instruction, parameters, datumStreamsAccessor,
				httpOperations, this::userMetadata, this::nodeMetadata, this::tariffSchedule,
				this::decryptUserSecret);
	}

	private DatumMetadataOperations userMetadata(Long userId) {
		final UserMetadataEntity meta = (userMetadataDao != null ? userMetadataDao.get(userId) : null);
		return (meta != null ? meta.getMetadata() : null);
	}

	private DatumMetadataOperations nodeMetadata(ObjectDatumStreamMetadataId id) {
		SolarNodeMetadata meta = (id != null && id.getKind() == ObjectDatumKind.Node
				&& id.getObjectId() != null && nodeMetadataDao != null
						? nodeMetadataDao.get(id.getObjectId())
						: null);
		return (meta != null ? meta.getMetadata() : null);
	}

	private TariffSchedule tariffSchedule(final DatumMetadataOperations meta, final String path) {
		if ( meta == null ) {
			return null;
		}
		TariffSchedule result = null;
		Object tariffData = meta.metadataAtPath(path);
		if ( tariffData != null ) {
			Locale locale = meta.resolveLocale(path);
			try {
				result = TariffUtils.parseCsvTemporalRangeSchedule(locale, true, true, null, tariffData);
			} catch ( Exception e ) {
				// ignore, continue
				String msg = "Error parsing tariff schedule at metadata path [%s]: %s".formatted(path,
						e.getMessage());
				throw new IllegalArgumentException(msg);
			}
		}
		return result;
	}

	private byte[] decryptUserSecret(final Long userId, final String key) {
		final var dao = getUserSecretAccessDao();
		if ( dao == null ) {
			return null;
		}
		var secret = dao.getUserSecret(userId, USER_SECRET_TOPIC_ID, key);
		if ( secret == null ) {
			return null;
		}
		return dao.decryptSecretValue(secret);
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
	 * Get the user secret access DAO.
	 *
	 * @return the DAO
	 */
	public UserSecretAccessDao getUserSecretAccessDao() {
		return userSecretAccessDao;
	}

	/**
	 * Set the user secret access DAO.
	 *
	 * @param userSecretAccessDao
	 *        the DAO to set
	 */
	public void setUserSecretAccessDao(UserSecretAccessDao userSecretAccessDao) {
		this.userSecretAccessDao = userSecretAccessDao;
	}

	/**
	 * Get the user metadata DAO.
	 * 
	 * @return the DAO
	 */
	public UserMetadataReadOnlyDao getUserMetadataDao() {
		return userMetadataDao;
	}

	/**
	 * Set the user metadata DAO.
	 * 
	 * @param userMetadataDao
	 *        the DAO to set
	 */
	public void setUserMetadataDao(UserMetadataReadOnlyDao userMetadataDao) {
		this.userMetadataDao = userMetadataDao;
	}

	/**
	 * Get the node metadata DAO.
	 * 
	 * @return the DAO
	 */
	public SolarNodeMetadataReadOnlyDao getNodeMetadataDao() {
		return nodeMetadataDao;
	}

	/**
	 * Set the node metadata DAO.
	 * 
	 * @param nodeMetadataDao
	 *        the DAO to set
	 */
	public void setNodeMetadataDao(SolarNodeMetadataReadOnlyDao nodeMetadataDao) {
		this.nodeMetadataDao = nodeMetadataDao;
	}

}
