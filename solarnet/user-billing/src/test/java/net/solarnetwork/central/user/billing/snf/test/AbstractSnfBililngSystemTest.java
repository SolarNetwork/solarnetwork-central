/* ==================================================================
 * AbstractSnfBililngSystemTest.java - 24/07/2020 10:03:08 AM
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

package net.solarnetwork.central.user.billing.snf.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;

/**
 * Helper class for {@link SnfBillingSystem} tests.
 * 
 * @author matt
 * @version 1.0
 */
public class AbstractSnfBililngSystemTest {

	protected AccountDao accountDao;
	protected SnfInvoiceDao invoiceDao;
	protected SnfInvoiceItemDao invoiceItemDao;
	protected SnfInvoiceNodeUsageDao invoiceNodeUsageDao;
	protected NodeUsageDao usageDao;
	protected TaxCodeDao taxCodeDao;
	protected VersionedMessageDao messageDao;
	protected ResourceBundleMessageSource messageSource;
	protected DefaultSnfInvoicingSystem invoicingSystem;
	protected SnfBillingSystem system;

	protected Long userId;
	protected LocalDate startDate;
	protected LocalDate endDate;

	@Before
	public void setup() {
		accountDao = EasyMock.createMock(AccountDao.class);
		invoiceDao = EasyMock.createMock(SnfInvoiceDao.class);
		invoiceItemDao = EasyMock.createMock(SnfInvoiceItemDao.class);
		invoiceNodeUsageDao = EasyMock.createMock(SnfInvoiceNodeUsageDao.class);
		usageDao = EasyMock.createMock(NodeUsageDao.class);
		taxCodeDao = EasyMock.createMock(TaxCodeDao.class);
		messageDao = EasyMock.createMock(VersionedMessageDao.class);
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(SnfBillingSystem.class.getName());

		invoicingSystem = new DefaultSnfInvoicingSystem(accountDao, invoiceDao, invoiceItemDao,
				invoiceNodeUsageDao, usageDao, taxCodeDao, messageDao);

		system = new SnfBillingSystem(invoicingSystem, accountDao, invoiceDao);

		userId = UUID.randomUUID().getMostSignificantBits();
		startDate = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).minusMonths(1)
				.toLocalDate();
		endDate = startDate.plusMonths(1);
	}

	protected void replayAll() {
		EasyMock.replay(accountDao, invoiceDao, invoiceItemDao, invoiceNodeUsageDao, usageDao,
				taxCodeDao, messageDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(accountDao, invoiceDao, invoiceItemDao, invoiceNodeUsageDao, usageDao,
				taxCodeDao, messageDao);
	}

}
