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

package net.solarnetwork.central.query.config;

import static net.solarnetwork.central.query.config.ContentCachingServiceConfig.QUERY_CACHE;
import static net.solarnetwork.central.query.config.ContentCachingServiceConfig.QUERY_CACHING_SERVICE;
import static net.solarnetwork.central.query.config.RateLimitConfig.RATE_LIMIT;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.TemporalAccessorParser;
import org.springframework.format.datetime.standard.TemporalAccessorPrinter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import net.solarnetwork.central.datum.support.GeneralNodeDatumMapPropertySerializer;
import net.solarnetwork.central.support.DelegatingParser;
import net.solarnetwork.central.support.InstantFormatter;
import net.solarnetwork.central.web.PingController;
import net.solarnetwork.central.web.support.ContentCachingFilter;
import net.solarnetwork.central.web.support.ContentCachingService;
import net.solarnetwork.central.web.support.RateLimitingFilter;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.central.web.support.WebServiceErrorAttributes;
import net.solarnetwork.central.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.codec.BindingResultSerializer;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.codec.PropertySerializerRegistrar;
import net.solarnetwork.codec.TimeZonePropertySerializer;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.web.jakarta.support.SimpleCsvHttpMessageConverter;
import net.solarnetwork.web.jakarta.support.SimpleXmlHttpMessageConverter;

/**
 * Web layer configuration.
 * 
 * @author matt
 * @version 1.5
 */
@Configuration
@Import({ WebServiceErrorAttributes.class, WebServiceControllerSupport.class,
		WebServiceGlobalControllerSupport.class })
public class WebConfig implements WebMvcConfigurer {

	/** A qualifier for the source ID path matcher. */
	public static final String SOURCE_ID_PATH_MATCHER = "source-id";

	@Value("${app.query-cache.filter.lock-pool-capacity:128}")
	private int lockPoolCapacity = 128;

	@Autowired(required = false)
	@Qualifier(QUERY_CACHE)
	private ContentCachingService contentCachingService;

	@Autowired
	@Qualifier(JsonConfig.CBOR_MAPPER)
	private ObjectMapper cborObjectMapper;

	@Bean
	@Qualifier(SOURCE_ID_PATH_MATCHER)
	public PathMatcher sourceIdPathMatcher() {
		AntPathMatcher matcher = new AntPathMatcher();
		matcher.setCachePatterns(true);
		matcher.setCaseSensitive(false);
		return matcher;
	}

	@Controller
	@RequestMapping("/ping")
	static class SolarQueryPingController extends PingController {

		public SolarQueryPingController(List<PingTest> tests) {
			super(tests);
		}

	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addFormatterForFieldType(LocalDateTime.class,
				new TemporalAccessorPrinter(DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC),
				new DelegatingParser<TemporalAccessor>(
						new TemporalAccessorParser(LocalDateTime.class,
								DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC),
						new TemporalAccessorParser(LocalDateTime.class,
								DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC)));
		registry.addFormatterForFieldType(Instant.class,
				new InstantFormatter(DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC,
						DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC, DateUtils.ISO_DATE_TIME_ALT_UTC,
						DateTimeFormatter.ISO_INSTANT, DateTimeFormatter.ISO_DATE_TIME));
	}

	@Bean
	public GeneralNodeDatumMapPropertySerializer datumMapPropertySerializer() {
		return new GeneralNodeDatumMapPropertySerializer();
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

		GeneralNodeDatumMapPropertySerializer datumMapSerializer = new GeneralNodeDatumMapPropertySerializer();

		Map<String, PropertySerializer> classSerializers = new LinkedHashMap<>(4);
		classSerializers.put("sun.util.calendar.ZoneInfo", timeZonePropertySerializer());
		classSerializers.put("org.springframework.validation.BeanPropertyBindingResult",
				bindingResultSerializer());
		classSerializers.put("net.solarnetwork.central.datum.domain.GeneralNodeDatum",
				datumMapSerializer);
		classSerializers.put("net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch",
				datumMapSerializer);
		classSerializers.put("net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum",
				datumMapSerializer);
		classSerializers.put("net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumReading",
				datumMapSerializer);
		reg.setClassSerializers(classSerializers);

		return reg;
	}

	@Bean(autowireCandidate = false)
	public PropertySerializerRegistrar xmlPropertySerializerRegistrar() {
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
		// update CBOR with our standard ObjectMapper
		for ( HttpMessageConverter<?> c : converters ) {
			if ( c instanceof MappingJackson2CborHttpMessageConverter cbor ) {
				cbor.setObjectMapper(cborObjectMapper);
			}
		}

		SimpleCsvHttpMessageConverter csv = new SimpleCsvHttpMessageConverter();
		csv.setPropertySerializerRegistrar(propertySerializerRegistrar());
		converters.add(csv);

		SimpleXmlHttpMessageConverter xml = new SimpleXmlHttpMessageConverter();
		xml.setClassNamesAllowedForNesting(Collections.singleton("net.solarnetwork"));
		xml.setPropertySerializerRegistrar(xmlPropertySerializerRegistrar());
		converters.add(xml);
	}

	@Bean(autowireCandidate = false)
	@ConditionalOnBean(name = QUERY_CACHING_SERVICE)
	@ConfigurationProperties(prefix = "app.query-cache.filter")
	public ContentCachingFilter contentCachingFilter() {
		return new ContentCachingFilter(contentCachingService, lockPoolCapacity);
	}

	@Bean
	@ConditionalOnBean(name = QUERY_CACHING_SERVICE)
	public PingTest contentCachingFilterPingTest() {
		return contentCachingFilter();
	}

	@Bean
	@Order(0)
	@ConditionalOnBean(name = QUERY_CACHING_SERVICE)
	public FilterRegistrationBean<ContentCachingFilter> contentCachingFilterRegistration() {
		FilterRegistrationBean<ContentCachingFilter> reg = new FilterRegistrationBean<>();
		reg.setFilter(contentCachingFilter());
		// @formatter:off
		reg.addUrlPatterns(
				"/api/v1/pub/datum/list",
				"/api/v1/sec/datum/list",
				"/api/v1/pub/datum/reading",
				"/api/v1/sec/datum/reading",
				"/api/v1/pub/datum/mostRecent",
				"/api/v1/sec/datum/mostRecent",
				"/api/v1/pub/datum/meta/*",
				"/api/v1/sec/datum/meta/*",
				"/api/v1/pub/location/datum/*",
				"/api/v1/sec/location/datum/*",
				"/api/v1/sec/nodes",
				"/api/v1/pub/range/*",
				"/api/v1/sec/range/*"
				);
		// @formatter:on
		return reg;
	}

	@Bean
	@Order(1)
	@Profile(RATE_LIMIT)
	public FilterRegistrationBean<RateLimitingFilter> rateLimitFilterRegistration(
			@Qualifier(RATE_LIMIT) ProxyManager<Long> proxyManager,
			@Qualifier(RATE_LIMIT) Supplier<BucketConfiguration> configurationProvider,
			HandlerExceptionResolver handlerExceptionResolver) {
		var filter = new RateLimitingFilter(proxyManager, configurationProvider);
		filter.setExceptionResolver(handlerExceptionResolver);
		FilterRegistrationBean<RateLimitingFilter> reg = new FilterRegistrationBean<>();
		reg.setFilter(filter);
		// @formatter:off
		reg.addUrlPatterns(
				"/api/*"
				);
		// @formatter:on
		return reg;
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
