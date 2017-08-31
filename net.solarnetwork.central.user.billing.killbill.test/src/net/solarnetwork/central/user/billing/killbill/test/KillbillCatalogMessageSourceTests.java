/* ==================================================================
 * KillbillCatalogMessageSourceTests.java - 31/08/2017 10:33:00 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.NoSuchMessageException;
import net.solarnetwork.central.user.billing.killbill.KillbillCatalogMessageSource;
import net.solarnetwork.central.user.billing.killbill.KillbillClient;

/**
 * Test cases for the {@link KillbillCatalogMessageSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillCatalogMessageSourceTests {

	private KillbillClient client;
	private Cache<String, Properties> cache;

	private KillbillCatalogMessageSource messageSource;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		client = EasyMock.createMock(KillbillClient.class);
		cache = EasyMock.createMock(Cache.class);
		messageSource = new KillbillCatalogMessageSource(client, cache);
	}

	@After
	public void teardown() {
		EasyMock.verify(client, cache);
	}

	private void replayAll() {
		EasyMock.replay(client, cache);
	}

	private Properties loadProps(String locale) {
		Properties props = new Properties();
		try {
			props.load(
					getClass().getResourceAsStream("catalog-translation-01_" + locale + ".properties"));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return props;
	}

	@Test
	public void directMatchNotCached() {
		// given

		// check cache (not found)
		expect(cache.get("en_NZ")).andReturn(null);

		// get from server
		Properties props = loadProps("en_NZ");
		expect(client.invoiceCatalogTranslation("en_NZ")).andReturn(props);

		// cache result
		expect(cache.putIfAbsent("en_NZ", props)).andReturn(true);

		// when
		replayAll();
		String msg = messageSource.getMessage("greeting", null, new Locale("en", "NZ"));

		// then
		assertThat(msg, equalTo("G'day, mate!"));
	}

	@Test
	public void directMatchCached() {
		// given

		// check cache
		Properties props = loadProps("en_NZ");
		expect(cache.get("en_NZ")).andReturn(props);

		// when
		replayAll();
		String msg = messageSource.getMessage("greeting", null, new Locale("en", "NZ"));

		// then
		assertThat(msg, equalTo("G'day, mate!"));
	}

	@Test(expected = NoSuchMessageException.class)
	public void noLocaleAvailable() {
		// given

		// check cache (not found)
		expect(cache.get("foo_BAR")).andReturn(null);

		// get from server
		expect(client.invoiceCatalogTranslation("foo_BAR")).andReturn(null);

		// cache empty result
		Capture<Properties> propsCapture = new Capture<>(CaptureType.ALL);
		expect(cache.putIfAbsent(eq("foo_BAR"), capture(propsCapture))).andReturn(true);

		// fall back to just lang
		expect(cache.get("foo")).andReturn(null);
		expect(client.invoiceCatalogTranslation("foo")).andReturn(null);
		expect(cache.putIfAbsent(eq("foo"), capture(propsCapture))).andReturn(true);

		// when
		replayAll();

		try {
			messageSource.getMessage("greeting", null, new Locale("foo", "BAR"));
		} catch ( NoSuchMessageException e ) {
			// then
			for ( Properties props : propsCapture.getValues() ) {
				assertThat(props, equalTo(new Properties()));
			}
			throw e;
		}
	}

	@Test
	public void noLocaleLanguageFallback() {
		// given

		// check cache (not found)
		expect(cache.get("en_US")).andReturn(null);

		// get from server
		expect(client.invoiceCatalogTranslation("en_US")).andReturn(null);

		// cache empty result
		Capture<Properties> propsCapture = new Capture<>();
		expect(cache.putIfAbsent(eq("en_US"), capture(propsCapture))).andReturn(true);

		// fall back to just lang
		expect(cache.get("en")).andReturn(null);
		Properties props = loadProps("en");
		expect(client.invoiceCatalogTranslation("en")).andReturn(props);
		expect(cache.putIfAbsent("en", props)).andReturn(true);

		// when
		replayAll();
		String msg = messageSource.getMessage("greeting", null, new Locale("en", "US"));

		// then
		assertThat(msg, equalTo("Good day, sir!"));
		assertThat(propsCapture.getValue(), equalTo(new Properties()));
	}

}
