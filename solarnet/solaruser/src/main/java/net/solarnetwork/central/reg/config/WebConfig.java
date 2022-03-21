/* ==================================================================
 * WebConfig.java - 9/10/2021 3:20:51 PM
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

package net.solarnetwork.central.reg.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.TemporalAccessorParser;
import org.springframework.format.datetime.standard.TemporalAccessorPrinter;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.unit.DataSize;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import net.solarnetwork.central.support.InstantFormatter;
import net.solarnetwork.central.web.PingController;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.central.web.support.WebServiceErrorAttributes;
import net.solarnetwork.central.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.codec.BindingResultSerializer;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.codec.PropertySerializerRegistrar;
import net.solarnetwork.codec.TimeZonePropertySerializer;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.web.support.JSONView;
import net.solarnetwork.web.support.SimpleCsvHttpMessageConverter;
import net.solarnetwork.web.support.SimpleXmlHttpMessageConverter;
import net.solarnetwork.web.support.SimpleXmlView;

/**
 * Web layer configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Import({ WebServiceErrorAttributes.class, WebServiceControllerSupport.class,
		WebServiceGlobalControllerSupport.class })
public class WebConfig implements WebMvcConfigurer {

	@Autowired(required = false)
	private List<PingTest> pingTests;

	@Bean
	public CommonsMultipartResolver multipartResolver(
			@Value("${spring.servlet.multipart.max-file-size:10MB}") DataSize maxFileSize,
			@Value("${spring.servlet.multipart.max-request-size:10MB}") DataSize maxRequestSize,
			@Value("${spring.servlet.multipart.file-size-threshold:1MB}") DataSize fileSizeThreshold) {
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(maxRequestSize.toBytes());
		multipartResolver.setMaxUploadSizePerFile(maxFileSize.toBytes());
		multipartResolver.setMaxInMemorySize((int) fileSizeThreshold.toBytes());
		return multipartResolver;
	}

	@Bean
	public PingController pingController() {
		PingController controller = new PingController();
		controller.setTests(pingTests);
		return controller;
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addFormatterForFieldType(LocalDateTime.class,
				new TemporalAccessorPrinter(DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC),
				new TemporalAccessorParser(LocalDateTime.class,
						DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC));
		registry.addFormatterForFieldType(Instant.class,
				new InstantFormatter(DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC));
	}

	@Bean
	public SimpleXmlView xml() {
		SimpleXmlView view = new SimpleXmlView();
		view.setContentType("text/xml;charset=UTF-8");
		view.setPropertySerializerRegistrar(propertySerializerRegistrar());
		view.setClassNamesAllowedForNesting(Collections.singleton("net.solarnetwork"));
		return view;
	}

	@Bean
	public JSONView json() {
		JSONView view = new JSONView();
		view.setContentType("application/json;charset=UTF-8");
		view.setPropertySerializerRegistrar(propertySerializerRegistrar());
		view.setIncludeParentheses(false);
		return view;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// enable client caching of static resources
		// @formatter:off
		for ( String dir : new String[] {"css", "fonts", "img", "js", "js-lib"} ) {
			registry.addResourceHandler(String.format("/*/%s/", dir))
					.addResourceLocations("classpath:/static/")
					.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
		}
		// @formatter:on
	}

	@Bean
	public TimeZonePropertySerializer timeZonePropertySerializer() {
		return new TimeZonePropertySerializer();
	}

	@Bean
	public BindingResultSerializer bindingResultSerializer() {
		return new BindingResultSerializer();
	}

	@Bean
	public PropertySerializerRegistrar propertySerializerRegistrar() {
		PropertySerializerRegistrar reg = new PropertySerializerRegistrar();

		Map<String, PropertySerializer> classSerializers = new LinkedHashMap<>(4);
		classSerializers.put("sun.util.calendar.ZoneInfo", timeZonePropertySerializer());
		classSerializers.put("org.springframework.validation.BeanPropertyBindingResult",
				bindingResultSerializer());
		reg.setClassSerializers(classSerializers);

		return reg;
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		SimpleCsvHttpMessageConverter csv = new SimpleCsvHttpMessageConverter();
		csv.setPropertySerializerRegistrar(propertySerializerRegistrar());
		converters.add(csv);

		SimpleXmlHttpMessageConverter xml = new SimpleXmlHttpMessageConverter();
		xml.setClassNamesAllowedForNesting(Collections.singleton("net.solarnetwork"));
		xml.setPropertySerializerRegistrar(propertySerializerRegistrar());
		converters.add(xml);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// @formatter:off
		registry.addMapping("/**")
			.allowCredentials(true)
			.allowedOriginPatterns(CorsConfiguration.ALL)
			.maxAge(TimeUnit.HOURS.toSeconds(24))
			.allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
			.allowedHeaders("Authorization", "Content-MD5", "Content-Type", "Digest", "X-SN-Date")
			;
		// @formatter:on
	}

}
