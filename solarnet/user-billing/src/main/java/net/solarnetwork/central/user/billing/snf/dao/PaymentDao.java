/* ==================================================================
 * PaymentDao.java - 29/07/2020 7:22:43 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.dao;

import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link Payment} entities.
 * 
 * @author matt
 * @version 2.0
 */
public interface PaymentDao
		extends GenericDao<Payment, UserUuidPK>, FilterableDao<Payment, UserUuidPK, PaymentFilter> {

}
