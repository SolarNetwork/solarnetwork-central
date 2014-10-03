/* ==================================================================
 * QuerySecurityAspect.java - Dec 18, 2012 4:32:34 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.aop;

import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatumFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Security enforcing AOP aspect for {@link QueryBiz}.
 * 
 * @author matt
 * @version 1.2
 */
@Aspect
public class QuerySecurityAspect extends AuthorizationSupport {

	public static final String FILTER_KEY_NODE_ID = "nodeId";
	public static final String FILTER_KEY_NODE_IDS = "nodeIds";

	private Set<String> nodeIdNotRequiredSet;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public QuerySecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getReportableInterval(..)) && args(nodeId,..)")
	public void nodeReportableInterval(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getAvailableSources(..)) && args(nodeId,..)")
	public void nodeReportableSources(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getMostRecentWeatherConditions(..)) && args(nodeId,..)")
	public void nodeMostRecentWeatherConditions(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getAggregatedDatum(..)) && args(criteria,..)")
	public void nodeDatumQuery(DatumQueryCommand criteria) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.findFiltered*(..)) && args(filter,..)")
	public void nodeDatumFilter(Filter filter) {
	}

	/**
	 * Allow the current actor access to aggregated datum data.
	 * 
	 * @param criteria
	 */
	@Before("nodeDatumQuery(criteria)")
	public void userNodeDatumAccessCheck(DatumQueryCommand criteria) {
		userNodeAccessCheck(criteria);
	}

	@Before("nodeDatumFilter(filter)")
	public void userNodeFilterAccessCheck(Filter filter) {
		userNodeAccessCheck(filter);
	}

	private void userNodeAccessCheck(Filter filter) {
		Long[] nodeIds = null;
		boolean nodeIdRequired = true;
		if ( filter instanceof NodeDatumFilter ) {
			NodeDatumFilter cmd = (NodeDatumFilter) filter;
			nodeIdRequired = isNodeIdRequired(cmd);
			if ( nodeIdRequired ) {
				nodeIds = cmd.getNodeIds();
			}
		} else {
			nodeIdRequired = false;
			Map<String, ?> f = filter.getFilter();
			if ( f.containsKey(FILTER_KEY_NODE_IDS) ) {
				nodeIds = getLongArrayParameter(f, FILTER_KEY_NODE_IDS);
			} else if ( f.containsKey(FILTER_KEY_NODE_ID) ) {
				nodeIds = getLongArrayParameter(f, FILTER_KEY_NODE_ID);
			}
		}
		if ( !nodeIdRequired ) {
			return;
		}
		if ( nodeIds == null || nodeIds.length < 1 ) {
			log.warn("Access DENIED; no node ID provided");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long nodeId : nodeIds ) {
			userNodeAccessCheck(nodeId);
		}
	}

	/**
	 * Check if a node ID is required of a filter instance. This will return
	 * <em>true</em> unless the {@link #getNodeIdNotRequiredSet()} set contains
	 * the value returned by {@link DatumFilter#getType()}.
	 * 
	 * @param filter
	 *        the filter
	 * @return <em>true</em> if a node ID is required for the given filter
	 */
	private boolean isNodeIdRequired(DatumFilter filter) {
		final String type = (filter == null || filter.getType() == null ? null : filter.getType()
				.toLowerCase());
		return (nodeIdNotRequiredSet == null || !nodeIdNotRequiredSet.contains(type));
	}

	private Long[] getLongArrayParameter(final Map<String, ?> map, final String key) {
		Long[] result = null;
		if ( map.containsKey(key) ) {
			Object o = map.get(key);
			if ( o instanceof Long[] ) {
				result = (Long[]) o;
			} else if ( o instanceof Long ) {
				result = new Long[] { (Long) o };
			}
		}
		return result;
	}

	/**
	 * Allow the current user (or current node) access to node data.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("nodeReportableInterval(nodeId) || nodeReportableSources(nodeId) || nodeMostRecentWeatherConditions(nodeId)")
	public void userNodeAccessCheck(Long nodeId) {
		if ( nodeId == null ) {
			return;
		}
		requireNodeReadAccess(nodeId);
	}

	public Set<String> getNodeIdNotRequiredSet() {
		return nodeIdNotRequiredSet;
	}

	public void setNodeIdNotRequiredSet(Set<String> nodeIdNotRequiredSet) {
		this.nodeIdNotRequiredSet = nodeIdNotRequiredSet;
	}

}
