/* ==================================================================
 * MailSnfInvoiceDelivererTests.java - 15/10/2026 3:14:51 pm
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

package net.solarnetwork.central.user.billing.snf.mail.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.mail.MailAddress;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.MessageDataSource;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.mail.MailSnfInvoiceDeliverer;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link MailSnfInvoiceDeliverer} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class MailSnfInvoiceDelivererTests {

	private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
	private static final Locale LOCALE = Locale.forLanguageTag("en-NZ");
	private static final MimeType PDF = MimeType.valueOf("application/pdf");

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Mock
	private MailService mailService;

	@Captor
	private ArgumentCaptor<MailAddress> addressCaptor;

	@Captor
	private ArgumentCaptor<MessageDataSource> messageCaptor;

	private MailSnfInvoiceDeliverer deliverer;

	private Account account;

	@BeforeEach
	public void setup() {
		deliverer = new MailSnfInvoiceDeliverer(invoicingSystem, mailService, Runnable::run);

		account = new Account(10L, 1L, Instant.now(), "NZD", "en-NZ");
		account.setAddress(
				new Address(1L, "Primary Co", "accounts@localhost", "NZ", "Pacific/Auckland"));
	}

	private static List<Resource> attachments(MessageDataSource message) {
		return StreamSupport.stream(message.getAttachments().spliterator(), false).toList();
	}

	private static Resource resource(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void deliverInvoice() throws Exception {
		// GIVEN
		final SnfInvoice invoice = new SnfInvoice(2001L, 1L, 10L, Instant.now(), START_DATE,
				START_DATE.plusMonths(1), "NZD");
		final Resource pdf = resource("PDF");
		given(invoicingSystem.renderInvoice(invoice, MimeTypeUtils.TEXT_HTML, LOCALE))
				.willReturn(resource("<p>Invoice</p>"));
		given(invoicingSystem.renderInvoice(invoice, PDF, LOCALE)).willReturn(pdf);

		final StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("invoice.mail.subject", LOCALE, "Invoice {0} ({1})");
		given(invoicingSystem.messageSourceForInvoice(invoice)).willReturn(messageSource);

		// WHEN
		CompletableFuture<Result<Object>> result = deliverer.deliverInvoice(invoice, account, null);

		// THEN
		// @formatter:off
		and.then(result.get().getSuccess()).as("Delivered").isTrue();
		then(mailService).should().sendMail(addressCaptor.capture(), messageCaptor.capture());
		and.then(addressCaptor.getValue().getTo())
			.as("Sent to the account address")
			.containsExactly("\"Primary Co\" <accounts@localhost>")
			;
		and.then(messageCaptor.getValue())
			.as("Subject from messages")
			.returns("Invoice INV-1JL (2026-10)", from(MessageDataSource::getSubject))
			.as("Body is the HTML invoice")
			.returns("<p>Invoice</p>", from(MessageDataSource::getBody))
			.as("PDF invoice attached")
			.returns(List.of(pdf), from(m -> attachments(m)))
			;
		// @formatter:on
	}

	@Test
	public void deliverConsolidatedInvoice() throws Exception {
		// GIVEN
		final SnfConsolidatedInvoice invoice = new SnfConsolidatedInvoice(2010L, 1L, 10L, Instant.now(),
				START_DATE, START_DATE.plusMonths(1), "NZD");
		final Resource pdf = resource("PDF");
		given(invoicingSystem.renderConsolidatedInvoice(invoice, MimeTypeUtils.TEXT_HTML, LOCALE, true))
				.willReturn(resource("<p>Summary</p>"));
		given(invoicingSystem.renderConsolidatedInvoice(eq(invoice), eq(PDF), eq(LOCALE), eq(false)))
				.willReturn(pdf);

		final StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("consolidatedInvoice.mail.subject", LOCALE,
				"Consolidated invoice {0} ({1})");
		given(invoicingSystem.messageSourceForConsolidatedInvoice(invoice)).willReturn(messageSource);

		// WHEN
		CompletableFuture<Result<Object>> result = deliverer.deliverConsolidatedInvoice(invoice, account,
				null);

		// THEN
		// @formatter:off
		and.then(result.get().getSuccess()).as("Delivered").isTrue();
		then(mailService).should().sendMail(addressCaptor.capture(), messageCaptor.capture());
		and.then(addressCaptor.getValue().getTo())
			.as("Sent to the primary account address")
			.containsExactly("\"Primary Co\" <accounts@localhost>")
			;
		and.then(messageCaptor.getValue())
			.as("Subject from messages")
			.returns("Consolidated invoice INV-1JU (2026-10)", from(MessageDataSource::getSubject))
			.as("Body is the HTML summary of the consolidated invoice")
			.returns("<p>Summary</p>", from(MessageDataSource::getBody))
			.as("Full PDF consolidated invoice attached")
			.returns(List.of(pdf), from(m -> attachments(m)))
			;
		// @formatter:on
	}

	@Test
	public void deliverConsolidatedInvoice_renderError() {
		// GIVEN
		final SnfConsolidatedInvoice invoice = new SnfConsolidatedInvoice(2010L, 1L, 10L, Instant.now(),
				START_DATE, START_DATE.plusMonths(1), "NZD");
		final RuntimeException err = new IllegalArgumentException("Not supported");
		given(invoicingSystem.renderConsolidatedInvoice(invoice, MimeTypeUtils.TEXT_HTML, LOCALE, true))
				.willThrow(err);

		// WHEN
		CompletableFuture<Result<Object>> result = deliverer.deliverConsolidatedInvoice(invoice, account,
				null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Delivery failed with the render error")
			.isCompletedExceptionally()
			.failsWithin(Duration.ZERO)
			.withThrowableOfType(ExecutionException.class)
			.withCause(err)
			;
		// @formatter:on
		then(mailService).shouldHaveNoInteractions();
	}

}
