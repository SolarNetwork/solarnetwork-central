/* ==================================================================
 * MyBatisAccountTaskDao.java - 21/07/2020 6:34:53 AM
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

import java.util.UUID;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;

/**
 * MyBatis implementation of {@link AccountTaskDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisAccountTaskDao extends BaseMyBatisGenericDaoSupport<AccountTask, UUID>
		implements AccountTaskDao {

	/**
	 * Constructor.
	 */
	public MyBatisAccountTaskDao() {
		super(AccountTask.class, UUID.class);
	}

	@Override
	public AccountTask claimAccountTask() {
		return selectFirst("claim-queued-account-task", null);
	}

	@Override
	public void taskCompleted(AccountTask task) {
		delete(task);
	}

}
