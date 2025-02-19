/* ==================================================================
 * BaseCloudDatumStreamServiceTests.java - 13/12/2024 3:04:27 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;

/**
 * Test cases for the {@link BaseCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class BaseCloudDatumStreamServiceTests {

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private TextEncryptor encryptor;

	@Mock
	private CloudIntegrationsExpressionService expressionService;

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Mock
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	private TestCloudDatumStreamService service(List<String> supportedPlaceholders) {
		return new TestCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao,
				supportedPlaceholders);
	}

	private class TestCloudDatumStreamService extends BaseCloudDatumStreamService {

		private final List<String> supportedPlaceholders;

		public TestCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
				TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
				CloudIntegrationConfigurationDao integrationDao,
				CloudDatumStreamConfigurationDao datumStreamDao,
				CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
				CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
				List<String> supportedPlaceholders) {
			super("test", "Test", Clock.systemUTC(), userEventAppenderBiz, encryptor, expressionService,
					integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao,
					Collections.emptyList());
			this.supportedPlaceholders = supportedPlaceholders;
		}

		@Override
		protected Iterable<String> supportedPlaceholders() {
			return supportedPlaceholders;
		}

		@Override
		public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
			return null;
		}

		@Override
		public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
				Map<String, ?> filters) {
			return null;
		}

		@Override
		public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
			return null;
		}

		@Override
		public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
				CloudDatumStreamQueryFilter filter) {
			return null;
		}

		@Override
		public List<Map<String, ?>> resolvePlaceholderSets(Map<String, ?> placeholders,
				Collection<String> sourceValueRefs) {
			return super.resolvePlaceholderSets(placeholders, sourceValueRefs);
		}

	}

	@Test
	public void resolvePlaceholderSets_noPlaceholders_noRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(null, null);

		// THEN
		// @formatter:off
		then(result)
			.as("Result contains single map")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholderSets_noPlaceholders_withRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		var refs = List.of("/123/abc", "/123/def", "/234/geh");

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(null, refs);

		// THEN
		// @formatter:off
		then(result)
			.as("Result contains one map for each value ref input")
			.hasSize(refs.size())
			.contains(
					Map.of("a", "123", "b", "abc"),
					Map.of("a", "123", "b", "def"),
					Map.of("a", "234", "b", "geh")
					)
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholderSets_withPlaceholders_noRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		var placeholders = Map.of("a", "123", "b", "abc");

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(placeholders, null);

		// THEN
		// @formatter:off
		then(result)
			.as("Result contains one map of given placeholders")
			.hasSize(1)
			.contains(Map.of("a", "123", "b", "abc"))
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholderSets_withPlaceholders_withRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		var placeholders = Map.of("a", "123", "b", "abc", "c", "---");

		var refs = List.of("/123/abc", "/123/def", "/234/geh");

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(placeholders, refs);

		// THEN
		// @formatter:off
		then(result)
			.as("Result contains one map for each value ref input, each merged with given placeholders")
			.hasSize(refs.size())
			.contains(
					Map.of("a", "123", "b", "abc", "c", "---"),
					Map.of("a", "123", "b", "def", "c", "---"),
					Map.of("a", "234", "b", "geh", "c", "---")
					)
			;
		// @formatter:on
	}

}
