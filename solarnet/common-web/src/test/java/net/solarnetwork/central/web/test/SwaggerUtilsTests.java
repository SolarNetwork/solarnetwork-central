/* ==================================================================
 * SwaggerUtilsTests.java - 8/11/2026 8:50:43 am
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

package net.solarnetwork.central.web.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;
import net.solarnetwork.central.web.SwaggerUtils;

/**
 * Test cases for the {@link SwaggerUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class SwaggerUtilsTests {

	private static Entry<String, PathItem> path(String path) {
		return Map.entry(path, new PathItem());
	}

	private static Tag tag(String name) {
		return new Tag().name(name);
	}

	@Test
	public void pathsSorter_rootFirst() {
		// GIVEN
		List<Entry<String, PathItem>> paths = List.of(path("/b"), path("/"), path("/a"));

		// WHEN
		List<String> result = paths.stream().sorted(new SwaggerUtils.PathsSorter()).map(Entry::getKey)
				.toList();

		// THEN
		// @formatter:off
		then(result)
			.as("Root path sorted before all other paths")
			.containsExactly("/", "/a", "/b")
			;
		// @formatter:on
	}

	@Test
	public void pathsSorter_equal() {
		// GIVEN
		Entry<String, PathItem> a = path("/a/b");
		Entry<String, PathItem> b = path("/a/b");

		// WHEN
		var sorter = new SwaggerUtils.PathsSorter();
		int ab = sorter.compare(a, b);
		int ba = sorter.compare(b, a);

		// THEN
		then(ab).as("Equal paths compare as equal").isZero();
		then(ba).as("Equal paths compare as equal when reversed").isZero();
	}

	@Test
	public void apiTagSorter_delimiterOnlyFirst() {
		// GIVEN
		List<Tag> tags = List.of(tag("b"), tag("-"), tag("a"));

		// WHEN
		List<String> result = tags.stream().sorted(new SwaggerUtils.ApiTagSorter()).map(Tag::getName)
				.toList();

		// THEN
		// @formatter:off
		then(result)
			.as("Delimiter-only name sorted before all other names")
			.containsExactly("-", "a", "b")
			;
		// @formatter:on
	}

	@Test
	public void apiTagSorter_equal() {
		// GIVEN
		Tag a = tag("a");
		Tag b = tag("a");

		// WHEN
		var sorter = new SwaggerUtils.ApiTagSorter();
		int ab = sorter.compare(a, b);
		int ba = sorter.compare(b, a);

		// THEN
		then(ab).as("Equal names compare as equal").isZero();
		then(ba).as("Equal names compare as equal when reversed").isZero();
	}

}
