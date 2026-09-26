/* ==================================================================
 * MyBatisAddressDao.java - 20/07/2020 4:21:54 PM
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.dao.AddressDao;
import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * MyBatis implementation of {@link AddressDao}.
 *
 * @author matt
 * @version 2.0
 */
public class MyBatisAddressDao extends BaseMyBatisGenericDaoSupport<Address, UserLongCompositePK>
		implements AddressDao {

	/**
	 * Constructor.
	 */
	public MyBatisAddressDao() {
		super(Address.class, UserLongCompositePK.class);
	}

	@Override
	protected UserLongCompositePK handleInsert(Address entity) {
		UserLongCompositePK id = super.handleInsert(entity);
		if ( !id.entityIdIsAssigned() && entity.getConfigId() != null ) {
			id = new UserLongCompositePK(id.getUserId(), entity.getConfigId());
		}
		return id;
	}

}
