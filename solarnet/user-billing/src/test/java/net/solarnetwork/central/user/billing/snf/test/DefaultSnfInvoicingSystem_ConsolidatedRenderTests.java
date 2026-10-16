/* ==================================================================
 * DefaultSnfInvoicingSystem_ConsolidatedRenderTests.java - 15/10/2026 2:05:33 pm
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

package net.solarnetwork.central.user.billing.snf.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.BDDMockito.given;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.support.CsvVersionedMessageDao;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.pdf.HtmlToPdfSnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.st4.VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.st4.VersionedMessageSourceSnfInvoiceRendererResolver;

/**
 * Test cases for rendering consolidated invoices with the
 * {@link DefaultSnfInvoicingSystem} class.
 *
 * <p>
 * The consolidated invoice templates in the test messages are the same as the
 * production ones, but the account invoice templates are simplified.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DefaultSnfInvoicingSystem_ConsolidatedRenderTests {

	private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
	private static final LocalDate END_DATE = START_DATE.plusMonths(1);
	private static final Locale LOCALE = Locale.forLanguageTag("en-NZ");
	private static final MimeType PDF = MimeType.valueOf("application/pdf");

	@Mock
	private AccountDao accountDao;

	@Mock
	private SnfInvoiceDao invoiceDao;

	@Mock
	private SnfInvoiceItemDao invoiceItemDao;

	@Mock
	private SnfInvoiceNodeUsageDao invoiceNodeUsageDao;

	@Mock
	private NodeUsageDao usageDao;

	@Mock
	private TaxCodeDao taxCodeDao;

	private DefaultSnfInvoicingSystem system;

	private SnfInvoice primaryInvoice;
	private SnfInvoice memberInvoice;
	private SnfConsolidatedInvoice invoice;

	@BeforeEach
	public void setup() {
		final CsvVersionedMessageDao messageDao = new CsvVersionedMessageDao(
				List.of(new ClassPathResource("consolidated-invoice-messages.csv", getClass())));
		final SimpleCache<String, VersionedMessages> messageCache = new SimpleCache<>(
				"TestMessageCache");
		final var htmlResolver = new VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver(
				"/snf/text/html/consolidated-invoice", List.of("/snf/text/html/invoice"),
				"consolidatedInvoice", MimeTypeUtils.TEXT_HTML, messageDao, messageCache,
				new SimpleCache<>("TestTemplateCache"));
		system = new DefaultSnfInvoicingSystem(accountDao, invoiceDao, invoiceItemDao,
				invoiceNodeUsageDao, usageDao, taxCodeDao, messageDao);
		system.setMessageCache(messageCache);
		system.setConsolidatedRendererResolvers(
				List.of(htmlResolver, new HtmlToPdfSnfConsolidatedInvoiceRendererResolver(htmlResolver)));
		system.setRendererResolvers(List.of(new VersionedMessageSourceSnfInvoiceRendererResolver(
				"/snf/text/html/invoice", "invoice", MimeTypeUtils.TEXT_HTML, messageDao, messageCache,
				new SimpleCache<>("TestInvoiceTemplateCache"))));

		final Address primaryAddress = address(1L, "Primary Co");
		primaryInvoice = invoice(1L, 10L, 2001L, primaryAddress, "12.00", "1.80");
		memberInvoice = invoice(2L, 20L, 2002L, address(2L, "Member Co"), "30.00", "4.50");

		invoice = new SnfConsolidatedInvoice(2010L, 1L, 10L, Instant.now(), START_DATE, END_DATE,
				"NZD");
		invoice.setAddress(primaryAddress);
		invoice.setInvoices(List.of(primaryInvoice, memberInvoice));
	}

	private static Address address(Long userId, String name) {
		return new Address(userId, name, "user%d@localhost".formatted(userId), "NZ",
				"Pacific/Auckland");
	}

	private static SnfInvoice invoice(Long userId, Long accountId, Long invoiceId, Address address,
			String usage, String tax) {
		final SnfInvoice result = new SnfInvoice(invoiceId, userId, accountId, Instant.now(),
				START_DATE, END_DATE, "NZD");
		result.setAddress(address);
		result.setItems(new LinkedHashSet<>(List.of(
				SnfInvoiceItem.newItem(result, InvoiceItemType.Usage, NodeUsage.DATUM_PROPS_IN_KEY,
						BigDecimal.ONE, new BigDecimal(usage)),
				SnfInvoiceItem.newItem(result, InvoiceItemType.Tax, "GST", BigDecimal.ONE,
						new BigDecimal(tax)))));
		return result;
	}

	private static List<String> texts(Elements elements) {
		return elements.stream().map(Element::text).toList();
	}

	/**
	 * Group the body of a consolidated invoice into pages.
	 *
	 * <p>
	 * The pages are not nested in elements of their own, so the invoice styles that select
	 * {@code body} children apply to them. Instead the summary and each account invoice start
	 * with an empty marker element.
	 * </p>
	 *
	 * @param doc
	 *        the document
	 * @return a copy of each page's elements, in an element with the ID of the page marker
	 */
	private static List<Element> pages(Document doc) {
		final List<Element> result = new ArrayList<>(4);
		for ( Element e : doc.body().children() ) {
			if ( e.is("#summary, .account") ) {
				result.add(new Element("div").attr("id", e.id()));
			} else if ( !result.isEmpty() ) {
				result.getLast().appendChild(e.clone());
			}
		}
		return result;
	}

	private String renderHtml(boolean summaryOnly) throws IOException {
		Resource r = system.renderConsolidatedInvoice(invoice, MimeTypeUtils.TEXT_HTML, LOCALE,
				summaryOnly);
		return r.getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	public void html() throws IOException {
		// WHEN
		Document doc = Jsoup.parse(renderHtml(false));

		// THEN
		final List<Element> pages = pages(doc);
		// @formatter:off
		and.then(pages).extracting(Element::id)
			.as("Summary page, then a page for each account invoice, primary first")
			.containsExactly("summary", "account-10", "account-20")
			;
		final Element summary = pages.getFirst();
		and.then(summary.select(".info .number").text())
			.as("Summary shows the consolidated invoice number")
			.isEqualTo("INV-1JU")
			;
		and.then(summary.select(".info .name").text())
			.as("Summary billed to the primary account")
			.isEqualTo("Primary Co")
			;
		and.then(summary.select(".info .balance").text())
			.as("Summary balance is the total of the account invoices")
			.isEqualTo("$48.30")
			;
		and.then(doc.select(".accounts tbody tr"))
			.as("One account row for each account invoice, linked to the account invoice")
			.satisfiesExactly(
				tr -> and.then(texts(tr.select("td")))
					.containsExactly("Primary Co", "INV-1JL", "$13.80", ""),
				tr -> and.then(texts(tr.select("td")))
					.containsExactly("Member Co", "INV-1JM", "$34.50", ""))
			;
		and.then(doc.select(".accounts tbody a").eachAttr("href"))
			.as("Account names and page numbers link to the account invoices")
			.containsExactly("#account-10", "#account-10", "#account-20", "#account-20")
			;
		and.then(texts(doc.select(".accounts .left")))
			.as("Invoice number column aligned left, unlike the amount and page columns")
			.containsExactly("Invoice", "INV-1JL", "INV-1JM")
			;
		and.then(doc.select(".totals tr.tax").eachText())
			.as("Tax of all accounts totalled")
			.containsExactly("GST $6.30")
			;
		and.then(summary.select("footer kbd").text())
			.as("Payment reference is the consolidated invoice number")
			.isEqualTo("INV-1JU")
			;
		and.then(pages.subList(1, pages.size()))
			.as("Account invoice pages, primary first")
			.satisfiesExactly(
				p -> and.then(p)
					.returns("account-10", from(e -> e.id()))
					.returns("INV-1JL", from(e -> e.select(".info .number").text()))
					.returns("Primary Co", from(e -> e.select(".info .name").text()))
					.returns(List.of("Posted Properties $12.00"), from(e -> e.select(".cost .item").eachText()))
					.returns("Included in consolidated invoice INV-1JU", from(e -> e.select(".consolidated").text())),
				p -> and.then(p)
					.returns("account-20", from(e -> e.id()))
					.returns("INV-1JM", from(e -> e.select(".info .number").text()))
					.returns("Member Co", from(e -> e.select(".info .name").text()))
					.returns(List.of("Posted Properties $30.00"), from(e -> e.select(".cost .item").eachText()))
					.returns("Included in consolidated invoice INV-1JU", from(e -> e.select(".consolidated").text())))
			;
		and.then(doc.select("footer"))
			.as("Only the summary has a payment footer, so account invoices are not paid separately")
			.hasSize(1)
			;
		and.then(doc.select("bookmark").eachAttr("href"))
			.as("PDF bookmarks for the summary and each account invoice")
			.containsExactly("#summary", "#account-10", "#account-20")
			;
		// @formatter:on
	}

	@Test
	public void html_summaryOnly() throws IOException {
		// WHEN
		Document doc = Jsoup.parse(renderHtml(true));

		// THEN
		// @formatter:off
		and.then(doc.select(".accounts tbody tr"))
			.as("Account rows without page numbers")
			.satisfiesExactly(
				tr -> and.then(texts(tr.select("td"))).containsExactly("Primary Co", "INV-1JL", "$13.80"),
				tr -> and.then(texts(tr.select("td"))).containsExactly("Member Co", "INV-1JM", "$34.50"))
			;
		and.then(doc.select("a"))
			.as("No links to account invoices")
			.isEmpty()
			;
		and.then(pages(doc)).extracting(Element::id)
			.as("Summary page only, without the account invoices")
			.containsExactly("summary")
			;
		and.then(doc.select("bookmark"))
			.as("No PDF bookmarks")
			.isEmpty()
			;
		and.then(doc.select("footer kbd").text())
			.as("Payment reference is the consolidated invoice number")
			.isEqualTo("INV-1JU")
			;
		// @formatter:on
	}

	@Test
	public void pdf() throws IOException {
		// WHEN
		Resource r = system.renderConsolidatedInvoice(invoice, PDF, LOCALE, false);

		// THEN
		// @formatter:off
		and.then(r.getFilename())
			.as("File name from messages")
			.isEqualTo("SolarNetwork consolidated invoice INV-1JU - 2026-10.pdf")
			;
		// @formatter:on
		try (PDDocument pdf = PDDocument.load(r.getContentAsByteArray())) {
			// @formatter:off
			and.then(pdf.getNumberOfPages())
				.as("Summary page, then each account invoice on its own page")
				.isEqualTo(3)
				;
			// @formatter:on

			final List<Integer> linkPages = new ArrayList<>(4);
			for ( PDAnnotation a : pdf.getPage(0).getAnnotations() ) {
				if ( a instanceof PDAnnotationLink link ) {
					PDDestination dest = link.getDestination();
					if ( dest == null && link.getAction() instanceof PDActionGoTo goTo ) {
						dest = goTo.getDestination();
					}
					if ( dest instanceof PDPageDestination pageDest ) {
						linkPages.add(pageDest.retrievePageNumber());
					}
				}
			}
			// @formatter:off
			and.then(linkPages)
				.as("Account names and page numbers on the summary page link to the account invoice pages")
				.containsExactly(1, 1, 2, 2)
				;
			// @formatter:on

			final List<String> bookmarks = new ArrayList<>(4);
			PDOutlineItem item = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild();
			while ( item != null ) {
				bookmarks.add(item.getTitle());
				item = item.getNextSibling();
			}
			// @formatter:off
			and.then(bookmarks)
				.as("PDF bookmarks for the summary and each account invoice")
				.containsExactly("Summary", "Primary Co", "Member Co")
				;
			// @formatter:on
		}
	}

	@Test
	public void html_loadAccountInvoices() throws IOException {
		// GIVEN
		SnfConsolidatedInvoice saved = new SnfConsolidatedInvoice(2010L, 1L, 10L, Instant.now(),
				START_DATE, END_DATE, "NZD");
		saved.setAddress(invoice.getAddress());
		saved.setInvoiceIds(List.of(primaryInvoice.getId(), memberInvoice.getId()));
		given(invoiceDao.get(primaryInvoice.getId())).willReturn(primaryInvoice);
		given(invoiceDao.get(memberInvoice.getId())).willReturn(memberInvoice);

		// WHEN
		Resource r = system.renderConsolidatedInvoice(saved, MimeTypeUtils.TEXT_HTML, LOCALE, false);
		Document doc = Jsoup.parse(r.getContentAsString(StandardCharsets.UTF_8));

		// THEN
		// @formatter:off
		and.then(doc.select(".account").eachAttr("id"))
			.as("Account invoices loaded")
			.containsExactly("account-10", "account-20")
			;
		// @formatter:on
	}

	private Document renderAccountInvoiceHtml(SnfInvoice accountInvoice) throws IOException {
		Resource r = system.renderInvoice(accountInvoice, MimeTypeUtils.TEXT_HTML, LOCALE);
		return Jsoup.parse(r.getContentAsString(StandardCharsets.UTF_8));
	}

	@Test
	public void html_accountInvoice_consolidated() throws IOException {
		// GIVEN
		memberInvoice.setConsolidatedInvoiceId(invoice.getInvoiceId());

		// WHEN
		Document doc = renderAccountInvoiceHtml(memberInvoice);

		// THEN
		// @formatter:off
		and.then(texts(doc.select("body > p.consolidated")))
			.as("Account invoice of a consolidated invoice names the consolidated invoice")
			.containsExactly("Included in consolidated invoice INV-1JU")
			;
		and.then(doc.select("footer"))
			.as("No payment footer, as the account invoice is paid with the consolidated invoice")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void html_accountInvoice_notConsolidated() throws IOException {
		// WHEN
		Document doc = renderAccountInvoiceHtml(primaryInvoice);

		// THEN
		// @formatter:off
		and.then(doc.select(".consolidated"))
			.as("Invoice that is not part of a consolidated invoice has no consolidated invoice note")
			.isEmpty()
			;
		and.then(doc.select("footer kbd").text())
			.as("Payment footer with the invoice number as the payment reference")
			.isEqualTo("INV-1JL")
			;
		// @formatter:on
	}

}
