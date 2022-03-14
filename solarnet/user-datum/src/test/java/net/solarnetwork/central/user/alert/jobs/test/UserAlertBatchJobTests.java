/* ==================================================================
 * UserAlertBatchJobTests.java - 10/11/2021 3:53:50 PM
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

package net.solarnetwork.central.user.alert.jobs.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.common.dao.jdbc.JdbcAppSettingDao;
import net.solarnetwork.central.dao.AppSettingDao;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.user.alert.jobs.UserAlertBatchJob;
import net.solarnetwork.central.user.alert.jobs.UserAlertBatchProcessor;

/**
 * Test cases for the {@link UserAlertBatchJob} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UserAlertBatchJobTests extends AbstractJUnit5JdbcDaoTestSupport {

	@Autowired
	private TransactionTemplate txTemplate;

	@Mock
	private UserAlertBatchProcessor processor;

	@Captor
	private ArgumentCaptor<Instant> validToCaptor;

	private AppSettingDao appSettingDao;
	private UserAlertBatchJob job;

	@BeforeEach
	public void setup() {
		appSettingDao = new JdbcAppSettingDao(jdbcTemplate);
		job = new UserAlertBatchJob(processor, txTemplate, appSettingDao);
	}

	@Test
	public void run_firstTime() {
		// GIVEN
		final Instant now = Instant.now();
		final Long nextId = 123L;
		given(processor.processAlerts(isNull(), validToCaptor.capture())).willReturn(nextId);

		// WHEN
		job.run();

		// THEN
		then(validToCaptor.getValue()).isAfter(now);
	}

}
