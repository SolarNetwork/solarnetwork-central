/* ==================================================================
 * UserAccountBizConfig.java - 27 Sept 2026 8:49:47 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.config;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.Validator;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.biz.impl.DefaultUserAccountBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.dao.UserDao;

/**
 * Configuration for the User account service.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("NullAway.Init")
@Configuration(proxyBeanMethods = false)
public class UserAccountBizConfig {

	@Autowired
	private UserDao userDao;

	@Autowired(required = false)
	private @Nullable List<BillingSystemRegistrar> billingSystemRegistrars;

	@Autowired
	private Validator validator;

	@Bean
	public UserAccountBiz userAccountBiz() {
		var biz = new DefaultUserAccountBiz(
				billingSystemRegistrars != null ? billingSystemRegistrars : List.of(), userDao);
		biz.setValidator(validator);
		return biz;
	}

}
