/* ==================================================================
 * CloudDatumStreamPropertyConfigurationTests.java - 4/10/2024 7:15:28 am
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

package net.solarnetwork.central.c2c.domain.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link CloudDatumStreamPropertyConfiguration} class.
 *
 * @author matt
 * @version 1.1
 */
public class CloudDatumStreamPropertyConfigurationTests {

	@Test
	public void toJson() {
		// GIVEN
		CloudDatumStreamPropertyConfiguration entity = new CloudDatumStreamPropertyConfiguration(
				randomLong(), randomLong(), randomInt(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		entity.setModified(entity.getCreated().plusSeconds(1));
		entity.setEnabled(true);
		entity.setPropertyType(DatumSamplesType.Accumulating);
		entity.setPropertyName(randomString());
		entity.setValueType(CloudDatumStreamValueType.Reference);
		entity.setValueReference(randomString());
		entity.setMultiplier(new BigDecimal("1.23"));
		entity.setScale(6);

		// WHEN
		String json = JsonUtils.getJSONString(entity);

		// THEN
		// @formatter:off
		then(json)
			.as("JSON formatted")
			.isEqualToIgnoringWhitespace("""
				{
					"userId":%d,
					"datumStreamMappingId":%d,
					"index":%d,
					"created":"%s",
					"modified":"%s",
					"enabled":true,
					"propertyType":"a",
					"propertyName":"%s",
					"valueType":"r",
					"valueReference":"%s",
					"multiplier":1.23,
					"scale":6
				}
				""".formatted(
						entity.getUserId(),
						entity.getDatumStreamMappingId(),
						entity.getIndex(),
						DateUtils.ISO_DATE_TIME_ALT_UTC.format(entity.getCreated()),
						DateUtils.ISO_DATE_TIME_ALT_UTC.format(entity.getModified()),
						entity.getPropertyName(),
						entity.getValueReference()
					))
			;
		// @formatter:on
	}

	@Test
	public void applyXform_mult() {
		// GIVEN
		CloudDatumStreamPropertyConfiguration entity = new CloudDatumStreamPropertyConfiguration(
				randomLong(), randomLong(), randomInt(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		entity.setMultiplier(new BigDecimal("1.5"));

		// WHEN
		BigDecimal input = new BigDecimal("2.5");
		Object result = entity.applyValueTransforms(input);

		then(result).as("Multiplication applied")
				.isEqualTo(input.multiply(entity.getMultiplier()).floatValue());
	}

	@Test
	public void applyXform_scale() {
		// GIVEN
		CloudDatumStreamPropertyConfiguration entity = new CloudDatumStreamPropertyConfiguration(
				randomLong(), randomLong(), randomInt(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		entity.setScale(1);

		// WHEN
		BigDecimal input = new BigDecimal("1.234567");
		Object result = entity.applyValueTransforms(input);

		then(result).as("Scale applied")
				.isEqualTo(input.setScale(entity.getScale(), RoundingMode.HALF_UP).floatValue());
	}

	@Test
	public void applyXform_multAndScale() {
		// GIVEN
		CloudDatumStreamPropertyConfiguration entity = new CloudDatumStreamPropertyConfiguration(
				randomLong(), randomLong(), randomInt(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		entity.setMultiplier(new BigDecimal("1.5"));
		entity.setScale(1);

		// WHEN
		BigDecimal input = new BigDecimal("1.234567");
		Object result = entity.applyValueTransforms(input);

		then(result).as("Multiplication then scale applied")
				.isEqualTo(input.multiply(entity.getMultiplier())
						.setScale(entity.getScale(), RoundingMode.HALF_UP).floatValue());
	}

}
