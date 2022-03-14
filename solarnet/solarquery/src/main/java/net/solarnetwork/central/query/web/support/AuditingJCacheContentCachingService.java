/* ==================================================================
 * AuditingContentCachingService.java - 30/09/2018 5:58:11 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.web.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.cache.Cache;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.web.support.CachedContent;
import net.solarnetwork.central.web.support.JCacheContentCachingService;

/**
 * Extension of {@link JCacheContentCachingService} that supports query
 * auditing.
 * 
 * @author matt
 * @version 2.0
 */
public class AuditingJCacheContentCachingService extends JCacheContentCachingService {

	/**
	 * A cached content metadata key for a map returned from
	 * {@link QueryAuditor#currentAuditResults()}.
	 */
	public static final String QUERY_AUDITOR_NODE_DATUM_RESULTS_KEY = "NODE_DATUM_AUDIT_RESULTS";

	private final QueryAuditor auditor;

	/**
	 * Constructor.
	 * 
	 * @param cache
	 *        the cache
	 * @param queryAuditor
	 *        the query auditor service to use
	 */
	public AuditingJCacheContentCachingService(Cache<String, CachedContent> cache,
			QueryAuditor queryAuditor) {
		super(cache);
		this.auditor = requireNonNullArgument(queryAuditor, "queryAuditor");
	}

	@Override
	public CachedContent sendCachedResponse(String key, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		final CachedContent result = super.sendCachedResponse(key, request, response);
		if ( result != null ) {
			// cache hit: look for audit metadata to pass to QueryAuditor
			Map<String, ?> metadata = result.getMetadata();
			if ( metadata != null && metadata.containsKey(QUERY_AUDITOR_NODE_DATUM_RESULTS_KEY) ) {
				@SuppressWarnings("unchecked")
				Map<GeneralNodeDatumPK, Integer> auditResults = (Map<GeneralNodeDatumPK, Integer>) metadata
						.get(QUERY_AUDITOR_NODE_DATUM_RESULTS_KEY);
				auditor.addNodeDatumAuditResults(auditResults);
			}
		} else {
			// cache miss: reset current audit results to capture in getCacheContentMetadata() later
			auditor.resetCurrentAuditResults();
		}
		return result;
	}

	@Override
	protected Map<String, ?> getCacheContentMetadata(String key, HttpServletRequest request,
			int statusCode, HttpHeaders headers) {
		Map<GeneralNodeDatumPK, Integer> auditResults = auditor.currentAuditResults();
		if ( auditResults != null ) {
			return Collections.singletonMap(QUERY_AUDITOR_NODE_DATUM_RESULTS_KEY, auditResults);
		}
		return null;
	}

}
