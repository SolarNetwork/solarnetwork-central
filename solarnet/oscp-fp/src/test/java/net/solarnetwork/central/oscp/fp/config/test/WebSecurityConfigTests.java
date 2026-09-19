/* ==================================================================
 * WebSecurityConfigTests.java - 15/09/2026 8:29:22 pm
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

package net.solarnetwork.central.oscp.fp.config.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import jakarta.servlet.Filter;
import net.solarnetwork.central.oscp.fp.config.WebSecurityConfig;

/**
 * Test cases for the {@link WebSecurityConfig} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("logging-user-event-appender")
public class WebSecurityConfigTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private List<SecurityFilterChain> securityFilterChains;

	@Test
	public void securityFilterBeansNotRegisteredWithServletContainer() {
		// GIVEN
		final Collection<Filter> filterBeans = beanFactory.getBeansOfType(Filter.class).values();
		final Set<Filter> securityFilterBeans = Collections.newSetFromMap(new IdentityHashMap<>());
		for ( SecurityFilterChain chain : securityFilterChains ) {
			for ( Filter filter : chain.getFilters() ) {
				if ( filterBeans.stream().anyMatch(bean -> bean == filter) ) {
					securityFilterBeans.add(filter);
				}
			}
		}

		// WHEN
		final List<String> containerFilters = new ServletContextInitializerBeans(beanFactory).stream()
				.filter(i -> i instanceof FilterRegistrationBean<?> reg && reg.isEnabled()
						&& securityFilterBeans.contains(reg.getFilter()))
				.map(Object::toString).toList();

		// THEN
		then(securityFilterBeans).as("Security filter chains use filter beans").isNotEmpty();
		then(containerFilters)
				.as("Security filter beans are not also registered with the servlet container")
				.isEmpty();
	}

}
