/* ==================================================================
 * BillingSecurityAspect.java - 25/08/2017 5:09:57 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;

/**
 * Security enforcing AOP aspect for {@link BillingBiz}.
 * 
 * @author matt
 * @version 2.0
 */
@Aspect
public class BillingSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 */
	public BillingSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.billing.biz.*BillingBiz.*ForUser(..)) && args(userId, ..)")
	public void forUserAccess(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.billing.biz.*BillingBiz.getInvoice(..)) && args(userId, ..)")
	public void getInvoice(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.billing.biz.*BillingBiz.renderInvoice(..)) && args(userId, ..)")
	public void renderInvoice(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.billing.biz.*BillingBiz.previewInvoice(..)) && args(userId, ..)")
	public void previewInvoice(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.billing.biz.*BillingBiz.findFilteredInvoices(..)) && args(filter, ..)")
	public void findFilteredInvoices(InvoiceFilter filter) {
	}

	@Before("forUserAccess(userId) || getInvoice(userId) || renderInvoice(userId) || previewInvoice(userId)")
	public void checkForUserAccess(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("findFilteredInvoices(filter)")
	public void checkFindFilteredInvoices(InvoiceFilter filter) {
		Long userId = (filter != null ? filter.getUserId() : null);
		requireUserReadAccess(userId);
	}

}
