/* ==================================================================
 * ConsolidatedBillingWebTests.java - 16/10/2026 5:02:44 pm
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

package net.solarnetwork.central.reg.web.test;

import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Usage;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.money.Monetary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.reg.config.WebSecurityConfig;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfConsolidatedInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;
import net.solarnetwork.central.user.dao.UserDao;

/**
 * Web tests for showing consolidated invoices on the billing page.
 *
 * <p>
 * The primary account consolidates the invoices of one member account. Both
 * accounts have an August invoice, combined in a consolidated invoice, and the
 * primary account also has a July invoice of its own.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetUserBillingConfiguration.SNF_BILLING)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ConsolidatedBillingWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String INVOICES_URL = "/u/sec/billing/invoices/list";
	private static final String CONSOLIDATED_URL = "/u/sec/billing/consolidated-invoices";
	private static final String PREVIEW_STATUS_URL = "/u/sec/billing/invoices/preview/status";

	private static final Long PRIMARY_USER_ID = 1L;
	private static final String PRIMARY_USERNAME = "primary@localhost";
	private static final Long MEMBER_USER_ID = 2L;
	private static final String MEMBER_USERNAME = "member@localhost";

	private static final Locale NZ = Locale.forLanguageTag("en-NZ");

	private static final LocalDate JUL = LocalDate.of(2026, 7, 1);
	private static final LocalDate AUG = LocalDate.of(2026, 8, 1);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserAccountBiz userAccountBiz;

	@Autowired
	private UserDao userDao;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private SnfInvoiceDao invoiceDao;

	@Autowired
	private SnfInvoiceItemDao invoiceItemDao;

	@Autowired
	private SnfConsolidatedInvoiceDao consolidatedInvoiceDao;

	private SnfInvoice primaryJul;
	private SnfInvoice primaryAug;
	private SnfInvoice memberAug;
	private SnfConsolidatedInvoice consolidated;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		setupTestUser(PRIMARY_USER_ID, PRIMARY_USERNAME, TEST_LOC_ID);
		setupTestUser(MEMBER_USER_ID, MEMBER_USERNAME, TEST_LOC_ID);

		final Account primary = createAccount(PRIMARY_USER_ID, PRIMARY_USERNAME, "Primary Co");
		final Account member = createAccount(MEMBER_USER_ID, MEMBER_USERNAME, "Member Co");
		userDao.storeInternalData(PRIMARY_USER_ID,
				Map.of("invoiceConsolidation", Map.of("userIds", List.of(MEMBER_USER_ID))));

		primaryJul = createInvoice(primary, JUL, "1.00");
		primaryAug = createInvoice(primary, AUG, "2.00");
		memberAug = createInvoice(member, AUG, "3.00");

		final var invoice = new SnfConsolidatedInvoice(primary.getAccountId(), primary.getUserId(),
				Instant.now(), AUG, AUG.plusMonths(1), primary.getCurrencyCode());
		invoice.setAddress(primary.getAddress());
		invoice.setInvoices(List.of(primaryAug, memberAug));
		consolidated = invoice.copyWithId(consolidatedInvoiceDao.save(invoice));
	}

	private <T> T asUser(Long userId, String username, Supplier<T> action) {
		final Authentication auth = TestSecurityContextHolder.getContext().getAuthentication();
		TestSecurityContextHolder.setAuthentication(authenticationFor(userId, username));
		try {
			return action.get();
		} finally {
			if ( auth != null ) {
				TestSecurityContextHolder.setAuthentication(auth);
			} else {
				TestSecurityContextHolder.clearContext();
			}
		}
	}

	private static Authentication authenticationFor(Long userId, String username) {
		final var principal = new AuthenticatedUser(new User(username, "password",
				AuthorityUtils.createAuthorityList("ROLE_USER", WebSecurityConfig.BILLING_AUTHORITY)),
				userId, null, false);
		return new UsernamePasswordAuthenticationToken(principal, "password",
				principal.getAuthorities());
	}

	private Account createAccount(Long userId, String email, String name) {
		final var input = new SnAccountCreationInput();
		final var addr = new SnAddressInput();
		addr.setName(name);
		addr.setEmail(email);
		addr.setStreet1("Level 1");
		addr.setLocality("Wellington");
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		input.setAddress(addr);
		final var acct = new SnAccountInput();
		acct.setCurrency(Monetary.getCurrency("NZD"));
		acct.setLocale("en-NZ");
		input.setAccount(acct);
		asUser(userId, email, () -> userAccountBiz.createAccount(userId, input));
		return accountDao.getForUser(userId);
	}

	private SnfInvoice createInvoice(Account account, LocalDate start, String amount) {
		final var invoice = new SnfInvoice(account.getAccountId(), account.getUserId(), Instant.now(),
				start, start.plusMonths(1), account.getCurrencyCode());
		invoice.setAddress(account.getAddress());
		final UserLongCompositePK id = invoiceDao.save(invoice);
		invoiceItemDao.save(newItem(id.getEntityId(), Usage, "datum-props-in", BigDecimal.ONE,
				new BigDecimal(amount)));
		return invoiceDao.get(id);
	}

	private ResultActions getAs(Long userId, String username, String url) throws Exception {
		return mvc.perform(get(url).with(authentication(authenticationFor(userId, username))).locale(NZ)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
	}

	private static String num(Long invoiceId) {
		return SnfBillingUtils.invoiceNumForId(invoiceId);
	}

	@Test
	public void consolidatedInvoices_primary() throws Exception {
		// @formatter:off
		getAs(PRIMARY_USER_ID, PRIMARY_USERNAME, CONSOLIDATED_URL + "/list")
			.andExpect(jsonPath("$.data.totalResults").value(1))
			.andExpect(jsonPath("$.data.results[0].id").value(consolidated.getInvoiceId().toString()))
			.andExpect(jsonPath("$.data.results[0].invoiceNumber").value(num(consolidated.getInvoiceId())))
			.andExpect(jsonPath("$.data.results[0].consolidated").value(true))
			.andExpect(jsonPath("$.data.results[0].localizedAmount").value("$5.00"))
			;
		// @formatter:on
	}

	@Test
	public void consolidatedInvoices_member() throws Exception {
		// @formatter:off
		getAs(MEMBER_USER_ID, MEMBER_USERNAME, CONSOLIDATED_URL + "/list")
			.andExpect(jsonPath("$.data.totalResults").value(0))
			;
		// @formatter:on
	}

	@Test
	public void consolidatedInvoice_primary() throws Exception {
		// @formatter:off
		getAs(PRIMARY_USER_ID, PRIMARY_USERNAME,
				CONSOLIDATED_URL + "/" + consolidated.getInvoiceId())
			.andExpect(jsonPath("$.data.invoiceNumber").value(num(consolidated.getInvoiceId())))
			.andExpect(jsonPath("$.data.localizedAmount").value("$5.00"))
			.andExpect(jsonPath("$.data.localizedAccountInvoices", hasSize(2)))
			.andExpect(jsonPath("$.data.localizedAccountInvoices[*].name",
					contains("Primary Co", "Member Co")))
			.andExpect(jsonPath("$.data.localizedAccountInvoices[*].invoiceNumber",
					contains(num(primaryAug.getInvoiceId()), num(memberAug.getInvoiceId()))))
			.andExpect(jsonPath("$.data.localizedAccountInvoices[*].localizedAmount",
					contains("$2.00", "$3.00")))
			;
		// @formatter:on
	}

	@Test
	public void consolidatedInvoice_member() throws Exception {
		// @formatter:off
		getAs(MEMBER_USER_ID, MEMBER_USERNAME,
				CONSOLIDATED_URL + "/" + consolidated.getInvoiceId())
			.andExpect(jsonPath("$.data").doesNotExist())
			;
		// @formatter:on
	}

	@Test
	public void outstandingInvoices_primary() throws Exception {
		// @formatter:off
		getAs(PRIMARY_USER_ID, PRIMARY_USERNAME,
				INVOICES_URL + "?unpaid=true&includeConsolidated=true")
			.andExpect(jsonPath("$.data.totalResults").value(2))
			.andExpect(jsonPath("$.data.results[*].id", contains(
					consolidated.getInvoiceId().toString(), primaryJul.getInvoiceId().toString())))
			.andExpect(jsonPath("$.data.results[*].consolidated", contains(true, false)))
			.andExpect(jsonPath("$.data.localizedTotalBalance").value("$6.00"))
			;
		// @formatter:on
	}

	@Test
	public void outstandingInvoices_member() throws Exception {
		// @formatter:off
		getAs(MEMBER_USER_ID, MEMBER_USERNAME,
				INVOICES_URL + "?unpaid=true&includeConsolidated=true")
			.andExpect(jsonPath("$.data.totalResults").value(0))
			;
		// @formatter:on
	}

	@Test
	public void invoices_member() throws Exception {
		// @formatter:off
		getAs(MEMBER_USER_ID, MEMBER_USERNAME, INVOICES_URL + "?offset=0&max=10")
			.andExpect(jsonPath("$.data.results[*].id",
					contains(memberAug.getInvoiceId().toString())))
			.andExpect(jsonPath("$.data.results[0].consolidatedInvoiceNumber")
					.value(num(consolidated.getInvoiceId())))
			;
		// @formatter:on
	}

	@Test
	public void previewConsolidatedInvoiceStatus_primary() throws Exception {
		// month unique to this test, as previews are cached across tests
		// @formatter:off
		getAs(PRIMARY_USER_ID, PRIMARY_USERNAME,
				CONSOLIDATED_URL + "/preview/status?month=2026-05")
			.andExpect(jsonPath("$.data.state").value("Running"))
			.andExpect(jsonPath("$.data.accounts[*].userId",
					contains(PRIMARY_USER_ID.intValue(), MEMBER_USER_ID.intValue())))
			.andExpect(jsonPath("$.data.accounts[*].name", contains("Primary Co", "Member Co")))
			.andExpect(jsonPath("$.data.accounts[*].state", contains("Running", "Waiting")))
			.andExpect(jsonPath("$.data.invoice").doesNotExist())
			;
		// @formatter:on
	}

	@Test
	public void previewConsolidatedInvoiceStatus_member() throws Exception {
		// @formatter:off
		getAs(MEMBER_USER_ID, MEMBER_USERNAME, CONSOLIDATED_URL + "/preview/status?month=2026-04")
			.andExpect(jsonPath("$.data").doesNotExist())
			;
		// @formatter:on
	}

	@Test
	public void previewInvoiceStatus_member() throws Exception {
		// month unique to this test, as previews are cached across tests
		// @formatter:off
		getAs(MEMBER_USER_ID, MEMBER_USERNAME, PREVIEW_STATUS_URL + "?month=2026-03")
			.andExpect(jsonPath("$.data.state").value("Running"))
			.andExpect(jsonPath("$.data.invoice").doesNotExist())
			;
		// @formatter:on
	}

}
