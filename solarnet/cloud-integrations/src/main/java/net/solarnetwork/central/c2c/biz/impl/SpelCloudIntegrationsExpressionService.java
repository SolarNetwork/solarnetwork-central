/* ==================================================================
 * SpelCloudIntegrationsExpressionService.java - 8/10/2024 8:06:13 am
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

import java.util.Base64;
import java.util.Map;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.expression.Expression;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.common.expr.spel.SpelExpressionService;

/**
 * Spring Expression implementation of
 * {@link CloudIntegrationsExpressionService}.
 *
 * @author matt
 * @version 1.0
 */
public class SpelCloudIntegrationsExpressionService implements CloudIntegrationsExpressionService {

	private final SpelExpressionService spel;

	private Cache<String, Expression> expressionCache;

	/**
	 * Constructor.
	 */
	public SpelCloudIntegrationsExpressionService() {
		super();
		this.spel = new SpelExpressionService();
	}

	@Override
	public <T> T evaluateDatumPropertyExpression(CloudDatumStreamPropertyConfiguration property,
			Object root, Map<String, Object> variables, Class<T> resultClass) {
		Expression expr = expression(property);
		return spel.evaluateExpression(expr, variables, root,
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
			result = spel.parseExpression(property.getValueReference());
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

}
