/* ==================================================================
 * WebUserServiceAuditorConfig.java - 6/05/2025 9:10:40 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.USER_SERVICE_AUDITOR;
import static net.solarnetwork.central.web.config.SolarNetCommonWebConfiguration.AUDIT_API;
import java.util.regex.Pattern;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.web.support.ResponseLengthUserServiceFilter;

/**
 * Web API audit configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(USER_SERVICE_AUDITOR + " & " + AUDIT_API)
@Configuration(proxyBeanMethods = false)
public class WebUserServiceAuditorConfig {

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> datumResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Udat");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/datum/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> expireResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uexi");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/expire/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> exportResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uexo");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/export/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> importResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uimp");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/import/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> locationResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uloc");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/location/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> instructionResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uins");
		filter.setExcludes(new Pattern[] { Pattern.compile("/instr/(?:add|exec)(?:/|$)") });
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/instr/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> nodesResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Unod");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/nodes/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> authTokensResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uato");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/auth-tokens/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> billingResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Ubil");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/billing/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> c2cResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uc2c");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/c2c/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> dinResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Udin");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/din/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> dnp3ResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Udp3");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/dnp3/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> eventsResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uevt");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/events/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> fluxResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uflx");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/flux/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> ininResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uinn");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/inin/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> metaResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Umet");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/meta/*",
				"/api/v1/sec/users/meta/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> ocppResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uocp");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/ocpp/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> oscpResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Uosc");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/oscp/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	public FilterRegistrationBean<ResponseLengthUserServiceFilter> secretsResponseLengthUserServiceFilterRegistration(
			UserServiceAuditor userServiceAuditor) {
		var filter = new ResponseLengthUserServiceFilter(userServiceAuditor, "Usec");
		var reg = new FilterRegistrationBean<ResponseLengthUserServiceFilter>();
		reg.setOrder(-1);
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/sec/user/secrets/*"
				);
		// @formatter:on
		return reg;
	}

}
