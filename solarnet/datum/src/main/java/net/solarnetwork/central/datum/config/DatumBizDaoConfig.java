/* ==================================================================
 * DatumBizDaoConfig.java - 5/10/2021 8:13:06 AM
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

package net.solarnetwork.central.datum.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.common.dao.LocationRequestDao;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.biz.dao.DaoAuditDatumBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumMaintenanceBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumMetadataBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.mail.MailService;

/**
 * Datum Biz DAO configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumBizDaoConfig {

	@Autowired
	private AuditDatumDao auditDatumDao;

	@Autowired
	private DatumAuxiliaryEntityDao datumAuxiliaryDao;

	@Autowired
	private DatumMaintenanceDao datumMaintenanceDao;

	@Autowired
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Autowired
	private LocationRequestDao locationRequestDao;

	@Autowired
	private TaskExecutor taskExecutor;

	@Value("${app.location.request.alert-mail:nobody@localhost}")
	private String locationRequestAlertEmail;

	@Autowired
	private MessageSource messageSource;

	@Autowired(required = false)
	private MailService mailService;

	@Autowired
	private ObjectMapper objectMapper;

	@Bean
	public DatumAuxiliaryBiz datumAuxiliaryBiz() {
		return new DaoDatumAuxiliaryBiz(datumAuxiliaryDao, datumStreamMetadataDao);
	}

	@Bean
	public DatumMaintenanceBiz datumMaintenanceBiz() {
		return new DaoDatumMaintenanceBiz(datumMaintenanceDao, datumStreamMetadataDao);
	}

	@Bean
	public DatumMetadataBiz datumMetadataBiz() {
		DaoDatumMetadataBiz biz = new DaoDatumMetadataBiz(datumStreamMetadataDao, locationRequestDao,
				objectMapper);
		biz.setTaskExecutor(taskExecutor);
		biz.setLocationRequestSubmittedAlertEmailRecipient(locationRequestAlertEmail);
		biz.setMessageSource(messageSource);
		biz.setMailService(mailService);
		return biz;
	}

	@Bean
	public AuditDatumBiz auditDatumBiz() {
		return new DaoAuditDatumBiz(auditDatumDao);
	}

	@Bean
	public DatumStreamMetadataBiz datumStreamMetadataBiz() {
		return new DaoDatumStreamMetadataBiz(datumStreamMetadataDao);
	}

}
