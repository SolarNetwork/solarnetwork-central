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
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.domain.DatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link DatumImportBiz}.
 *
 * @author matt
 * @version 2.0
 */
@Aspect
@Component
public class DatumImportSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 */
	public DatumImportSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.imp.biz.DatumImportBiz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.imp.biz.DatumImportBiz.submitDatumImportRequest(..)) && args(request,..)")
	public void actionForRequest(DatumImportRequest request) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.imp.biz.DatumImportBiz.previewStagedImportRequest(..)) && args(request,..)")
	public void actionForPreviewRequest(DatumImportPreviewRequest request) {
	}

	@Before(value = "actionForUser(userId)", argNames = "userId")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "actionForRequest(request)", argNames = "request")
	public void requestCheck(DatumImportRequest request) {
		final Long userId = (request != null ? request.getUserId() : null);
		requireUserWriteAccess(userId);
	}

	@Before(value = "actionForPreviewRequest(request)", argNames = "request")
	public void previewRequestCheck(DatumImportPreviewRequest request) {
		final Long userId = (request != null ? request.getUserId() : null);
		requireUserWriteAccess(userId);
	}

}
