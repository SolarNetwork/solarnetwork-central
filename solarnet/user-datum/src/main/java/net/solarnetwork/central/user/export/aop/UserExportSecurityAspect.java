/* ==================================================================
 * UserExportSecurityAspect.java - 28/03/2018 4:14:55 PM
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

package net.solarnetwork.central.user.export.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DataConfiguration;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.export.biz.UserExportBiz;

/**
 * Security enforcing AOP aspect for {@link UserExportBiz}.
 * 
 * @author matt
 * @version 2.0
 */
@Aspect
@Component
public class UserExportSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserExportSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.user.export.biz.UserExportBiz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.export.biz.UserExportBiz.save*(..)) && args(config,..)")
	public void saveConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.export.biz.UserExportBiz.delete*(..)) && args(config,..)")
	public void deleteConfiguration(UserRelatedEntity<?> config) {
	}

	@Before("actionForUser(userId)")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveConfiguration(config) || deleteConfiguration(config)")
	public void saveConfigurationCheck(UserRelatedEntity<?> config) {
		final Long userId = (config != null ? config.getUserId() : null);
		requireUserWriteAccess(userId);

		DataConfiguration dataConfiguration = null;
		if ( config instanceof Configuration ) {
			Configuration fullConfig = (Configuration) config;
			dataConfiguration = fullConfig.getDataConfiguration();
		}

		if ( dataConfiguration != null ) {
			AggregateGeneralNodeDatumFilter filter = dataConfiguration.getDatumFilter();
			if ( filter != null ) {
				Long[] nodeIds = filter.getNodeIds();
				if ( nodeIds != null ) {
					for ( Long nodeId : nodeIds ) {
						requireNodeReadAccess(nodeId);
					}
				}
			}
		}
	}

}
