/* ==================================================================
 * HtmlToPdfInvoiceTests.java - 8/08/2020 7:32:57 AM
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

package net.solarnetwork.central.user.billing.snf.pdf.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.stringtemplate.v4.STGroupDir;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.pdf.HtmlToPdfTemplateRenderer;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;
import net.solarnetwork.service.TemplateRenderer;

/**
 * Test to render a PDF from HTML.
 * 
 * @author matt
 * @version 2.0
 */
public class HtmlToPdfInvoiceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static Address createAddress(String country, String timeZoneId) {
		final Address addr = new Address(randomUUID().getMostSignificantBits(), Instant.now());
		addr.setCountry(country);
		addr.setTimeZoneId(timeZoneId);
		return addr;
	}

	private static Account createAccount(Long userId, String locale, Address address) {
		final Account account = new Account(randomUUID().getMostSignificantBits(), userId,
				Instant.now());
		account.setLocale(locale);
		account.setAddress(address);
		return account;
	}

	private TemplateRenderer htmlInvoiceRenderer() {
		STGroupDir group = new STGroupDir("net/solarnetwork/central/user/billing/snf/pdf/test/ex", '$',
				'$');
		return ST4TemplateRenderer.html("foo", group, "invoice");
	}

	@Test
	public void render_example() throws IOException {
		// GIVEN
		final Account account = createAccount(randomUUID().getMostSignificantBits(), "en_NZ",
				createAddress("NZ", "Pacific/Auckland"));
		final SnfInvoice snfInvoice = new SnfInvoice(randomUUID().getMostSignificantBits(),
				account.getUserId(), account.getId().getId(), Instant.now());
		final SnfInvoiceItem item1 = SnfInvoiceItem.newItem(snfInvoice, InvoiceItemType.Usage,
				NodeUsage.DATUM_PROPS_IN_KEY, new BigDecimal("123456789"), new BigDecimal("12345.67"));
		final SnfInvoiceItem tax1 = SnfInvoiceItem.newItem(snfInvoice, InvoiceItemType.Tax, "GST",
				new BigDecimal("12345"), new BigDecimal("123.67"));
		snfInvoice.setItems(new LinkedHashSet<>(Arrays.asList(item1, tax1)));
		final List<InvoiceItem> invoiceItems = Arrays.asList(new InvoiceItemImpl(snfInvoice, item1),
				new InvoiceItemImpl(snfInvoice, tax1));
		final InvoiceImpl invoice = new InvoiceImpl(snfInvoice, invoiceItems);

		final Properties messages = new Properties();
		messages.put("title", "Yo!");
		messages.put("datum-props-in.item", "Metrics In");

		TemplateRenderer htmlRenderer = htmlInvoiceRenderer();
		HtmlToPdfTemplateRenderer t = new HtmlToPdfTemplateRenderer(htmlRenderer);

		// WHEN
		Path tmpFile = Files.createTempFile("HtmlToPdfInvoiceTests-", ".pdf");
		try (BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(tmpFile.toFile()))) {
			Map<String, Object> parameters = new LinkedHashMap<>(4);
			parameters.put("invoice", invoice);
			parameters.put("messages", messages);
			t.render(Locale.ENGLISH, HtmlToPdfTemplateRenderer.PDF_MIME_TYPE, parameters, out);
		}

		// THEN
		assertThat(String.format("PDF generated to temp file %s", tmpFile), Files.size(tmpFile),
				greaterThan(0L));
		log.info("PDF generated at {}", tmpFile);
	}

	@Test
	public void render_withSvg() throws IOException {
		// GIVEN
		TemplateRenderer htmlRenderer = htmlRenderer("test-01.html");
		HtmlToPdfTemplateRenderer t = new HtmlToPdfTemplateRenderer(htmlRenderer);

		// WHEN
		Path tmpFile = Files.createTempFile("HtmlToPdfInvoiceTests-SVG-", ".pdf");
		try (BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(tmpFile.toFile()))) {
			t.render(Locale.ENGLISH, HtmlToPdfTemplateRenderer.PDF_MIME_TYPE, Collections.emptyMap(),
					out);
		}

		// THEN
		assertThat(String.format("PDF generated to temp file %s", tmpFile), Files.size(tmpFile),
				greaterThan(0L));
		log.info("PDF generated at {}", tmpFile);
	}

	private TemplateRenderer htmlRenderer(String resource) throws IOException {
		final String html = FileCopyUtils.copyToString(new InputStreamReader(
				getClass().getResourceAsStream(resource), HtmlToPdfTemplateRenderer.UTF8));
		return new TemplateRenderer() {

			@Override
			public int compareTo(String o) {
				return 0;
			}

			@Override
			public String getId() {
				return "test";
			}

			@Override
			public boolean supportsMimeType(MimeType mimeType) {
				return MimeTypeUtils.TEXT_HTML.isCompatibleWith(mimeType);
			}

			@Override
			public List<MimeType> supportedMimeTypes() {
				return Collections.singletonList(MimeTypeUtils.TEXT_HTML);
			}

			@Override
			public void render(Locale locale, MimeType mimeType, Map<String, ?> parameters,
					OutputStream out) throws IOException {
				FileCopyUtils.copy(html.getBytes(HtmlToPdfTemplateRenderer.UTF8), out);
			}
		};
	}

	@Test
	public void render_withFloats() throws IOException {
		// GIVEN
		TemplateRenderer htmlRenderer = htmlRenderer("test-02.html");
		HtmlToPdfTemplateRenderer t = new HtmlToPdfTemplateRenderer(htmlRenderer);

		// WHEN
		Path tmpFile = Files.createTempFile("HtmlToPdfInvoiceTests-SVG-", ".pdf");
		try (BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(tmpFile.toFile()))) {
			t.render(Locale.ENGLISH, HtmlToPdfTemplateRenderer.PDF_MIME_TYPE, Collections.emptyMap(),
					out);
		}

		// THEN
		assertThat(String.format("PDF generated to temp file %s", tmpFile), Files.size(tmpFile),
				greaterThan(0L));
		log.info("PDF generated at {}", tmpFile);
	}

}
