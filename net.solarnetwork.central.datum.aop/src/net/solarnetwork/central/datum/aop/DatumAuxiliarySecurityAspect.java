/* ==================================================================
 * DatumAuxiliarySecurityAspect.java - 4/02/2019 12:37:58 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.aop;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security AOP support for {@link DatumAuxiliaryBiz}.
 * 
 * @author matt
 * @version 1.2
 * @since 1.5
 */
@Aspect
public class DatumAuxiliarySecurityAspect extends AuthorizationSupport {

	final private DatumStreamMetadataDao metaDao;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 * @param metaDao
	 *        the metadata DAO to use
	 */
	public DatumAuxiliarySecurityAspect(UserNodeDao userNodeDao, DatumStreamMetadataDao metaDao) {
		super(userNodeDao);
		this.metaDao = metaDao;
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumAuxiliary*.getGeneralNodeDatumAuxiliary(..)) && args(id)")
	public void viewAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumAuxiliary*.storeGeneralNodeDatumAuxiliary(..)) && args(datum)")
	public void storeAuxiliary(GeneralNodeDatumAuxiliary datum) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumAuxiliary*.moveGeneralNodeDatumAuxiliary(..)) && args(from,to)")
	public void moveAuxiliary(GeneralNodeDatumAuxiliaryPK from, GeneralNodeDatumAuxiliary to) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumAuxiliary*.removeGeneralNodeDatumAuxiliary(..)) && args(id)")
	public void removeAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumAuxiliary*.findGeneralNodeDatumAuxiliary(..)) && args(filter,..)")
	public void findAuxiliary(GeneralNodeDatumAuxiliaryFilter filter) {
	}

	/**
	 * Check access to modifying datum auxiliary data.
	 * 
	 * @param datum
	 *        the datum verify
	 */
	@Before("storeAuxiliary(datum)")
	public void storeAuxiliaryCheck(GeneralNodeDatumAuxiliary datum) {
		if ( datum != null ) {
			requireNodeWriteAccess(datum.getNodeId());
		}
	}

	/**
	 * Check access to moving datum auxiliary data.
	 * 
	 * @param from
	 *        the ID of the datum to move
	 * @param to
	 *        the new datum
	 */
	@Before("moveAuxiliary(from,to)")
	public void moveAuxiliaryCheck(GeneralNodeDatumAuxiliaryPK from, GeneralNodeDatumAuxiliary to) {
		if ( from != null ) {
			requireNodeWriteAccess(from.getNodeId());
		}
		if ( to != null ) {
			requireNodeWriteAccess(to.getNodeId());
		}
	}

	@Before("removeAuxiliary(id)")
	public void removeAuxiliaryCheck(GeneralNodeDatumAuxiliaryPK id) {
		if ( id != null ) {
			requireNodeWriteAccess(id.getNodeId());
		}
	}

	@Before("viewAuxiliary(id)")
	public void viewAuxiliaryCheck(GeneralNodeDatumAuxiliaryPK id) {
		if ( id == null ) {
			return;
		}
		requireNodeReadAccess(id.getNodeId());

		final SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy != null ) {
			DatumFilterCommand filter = new DatumFilterCommand();
			filter.setNodeId(id.getNodeId());
			filter.setSourceId(id.getSourceId());
			userNodeAccessCheck(filter);
		}
	}

	@Around(value = "findAuxiliary(filter)")
	public Object userNodeFilterAccessCheck(ProceedingJoinPoint pjp,
			GeneralNodeDatumAuxiliaryFilter filter) throws Throwable {
		final SecurityPolicy policy = getActiveSecurityPolicy();

		if ( policy != null && policy.getSourceIds() != null && !policy.getSourceIds().isEmpty()
				&& filter.getSourceId() == null ) {
			// no source IDs provided, but policy restricts source IDs.
			// restrict the filter to the available source IDs if using a DatumFilterCommand,
			// and let call to userNodeAccessCheck later on filter out restricted values
			if ( filter instanceof DatumFilterCommand ) {
				DatumFilterCommand f = (DatumFilterCommand) filter;
				BasicDatumCriteria c = new BasicDatumCriteria();
				c.setNodeIds(f.getNodeIds());
				Iterable<NodeDatumStreamMetadata> metas = metaDao.findNodeDatumStreamMetadata(c);
				Set<String> availableSources = StreamSupport.stream(metas.spliterator(), false)
						.map(NodeDatumStreamMetadata::getSourceId).collect(Collectors.toSet());
				if ( availableSources != null && !availableSources.isEmpty() ) {
					f.setSourceIds(availableSources.toArray(new String[availableSources.size()]));
				}
			}
		}

		Filter f = userNodeAccessCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}

		// if an aggregate was injected (enforced) on the filter, then call the join point method with the new filter
		Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	private GeneralNodeDatumAuxiliaryFilter userNodeAccessCheck(GeneralNodeDatumAuxiliaryFilter filter) {
		Long[] nodeIds = filter.getNodeIds();
		if ( nodeIds == null || nodeIds.length < 1 ) {
			log.warn("Access DENIED; no node ID provided");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long nodeId : nodeIds ) {
			requireNodeReadAccess(nodeId);
		}

		return policyEnforcerCheck(filter);
	}

}
