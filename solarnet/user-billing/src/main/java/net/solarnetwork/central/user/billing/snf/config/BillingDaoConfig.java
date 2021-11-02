/* ==================================================================
 * BillingDaoConfig.java - 29/10/2021 2:12:37 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisTaxCodeDao;

/**
 * User billing DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class BillingDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public AccountDao accountDao() {
		MyBatisAccountDao dao = new MyBatisAccountDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public SnfInvoiceDao snfInvoiceDao() {
		MyBatisSnfInvoiceDao dao = new MyBatisSnfInvoiceDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public SnfInvoiceItemDao snfInvoiceItemDao() {
		MyBatisSnfInvoiceItemDao dao = new MyBatisSnfInvoiceItemDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public SnfInvoiceNodeUsageDao invoiceNodeUsageDao() {
		MyBatisSnfInvoiceNodeUsageDao dao = new MyBatisSnfInvoiceNodeUsageDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public NodeUsageDao nodeUsageDao() {
		MyBatisNodeUsageDao dao = new MyBatisNodeUsageDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public TaxCodeDao taxCodeDao() {
		MyBatisTaxCodeDao dao = new MyBatisTaxCodeDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
