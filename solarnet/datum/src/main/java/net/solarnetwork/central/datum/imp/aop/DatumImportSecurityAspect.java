/* ==================================================================
 * DatumImportSecurityAspect.java - 9/11/2018 4:47:15 PM
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

package net.solarnetwork.central.datum.imp.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.domain.DatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link DatumImportBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class DatumImportSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public DatumImportSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.imp.biz.DatumImportBiz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.imp.biz.DatumImportBiz.submitDatumImportRequest(..)) && args(request,..)")
	public void actionForRequest(DatumImportRequest request) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.imp.biz.DatumImportBiz.previewStagedImportRequest(..)) && args(request,..)")
	public void actionForPreviewRequest(DatumImportPreviewRequest request) {
	}

	@Before("actionForUser(userId)")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("actionForRequest(request)")
	public void requestCheck(DatumImportRequest request) {
		final Long userId = (request != null ? request.getUserId() : null);
		requireUserWriteAccess(userId);
	}

	@Before("actionForPreviewRequest(request)")
	public void previewRequestCheck(DatumImportPreviewRequest request) {
		final Long userId = (request != null ? request.getUserId() : null);
		requireUserWriteAccess(userId);
	}

}
