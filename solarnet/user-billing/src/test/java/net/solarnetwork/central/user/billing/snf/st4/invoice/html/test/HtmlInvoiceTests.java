/* ==================================================================
 * HtmlInvoiceTests.java - 25/07/2020 5:09:20 PM
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

package net.solarnetwork.central.user.billing.snf.st4.invoice.html.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;
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
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;

/**
 * Test cases for the invoice template.
 * 
 * @author matt
 * @version 1.0
 */
public class HtmlInvoiceTests {

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

		STGroupDir group = new STGroupDir(
				"net/solarnetwork/central/user/billing/snf/st4/invoice/html/test/ex", '$', '$');
		ST4TemplateRenderer t = ST4TemplateRenderer.html("foo", group, "invoice");

		// WHEN
		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		Map<String, Object> parameters = new LinkedHashMap<>(4);
		parameters.put("invoice", invoice);
		parameters.put("messages", messages);
		t.render(Locale.ENGLISH, ST4TemplateRenderer.HTML.get(0), parameters, byos);

		// THEN
		String output = new String(byos.toByteArray(), ST4TemplateRenderer.UTF8);
		// @formatter:off
		String expected = String
				.format("<html><head><title>Yo! %s</title></head><body><p>Hello, world.</p><table>"
						+ "<tr>\n\t<th>datum-props-in</th>\n\t<td>Metrics In</td>\n\t<td>%1$s</td>\n</tr>" 
						+ "<tr>\n\t<th>GST</th>\n\t<td></td>\n\t<td>%1$s</td>\n</tr>" 
						+ "</table></body></html>", invoice.getInvoiceNumber());
		// @formatter:on
		assertThat("Output generated", output, equalTo(expected));
	}

}
