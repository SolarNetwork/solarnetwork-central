/* ==================================================================
 * SearchFilterUtilsTests.java - 23/09/2022 6:20:43 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.support.SearchFilterUtils;
import net.solarnetwork.util.SearchFilter;

/**
 * Test cases for the {@link SearchFilterUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SearchFilterUtilsTests {

	@Test
	public void sqlJsonPath_simple() {
		// GIVEN
		SearchFilter f = SearchFilter.forLDAPSearchFilterString("(foo=bar)");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Simple path", result, is(equalTo("$ ? (@.foo == \"bar\")")));
	}

	@Test
	public void sqlJsonPath_logicAnd() {
		// GIVEN
		SearchFilter f = SearchFilter.forLDAPSearchFilterString("(& (foo=bar) (bim>5))");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Simple path", result, is(equalTo("$ ? (@.foo == \"bar\" && @.bim > 5)")));
	}

	@Test
	public void sqlJsonPath_doubleNesting() {
		// GIVEN
		SearchFilter f = SearchFilter
				.forLDAPSearchFilterString("(& (foo=bar) (| (bim>5) (error<>1) ) )");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Double nested path", result,
				is(equalTo("$ ? (@.foo == \"bar\" && (@.bim > 5 || @.error != 1))")));
	}

	@Test
	public void sqlJsonPath_not() {
		// GIVEN
		SearchFilter f = SearchFilter.forLDAPSearchFilterString("(! (foo=bar) )");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Not path", result, is(equalTo("$ ? (!(@.foo == \"bar\"))")));
	}

	@Test
	public void sqlJsonPath_notNested() {
		// GIVEN
		SearchFilter f = SearchFilter.forLDAPSearchFilterString("(& (! (foo=bar) )(bim>5))");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Not nested path", result, is(equalTo("$ ? (!(@.foo == \"bar\") && @.bim > 5)")));
	}

	@Test
	public void sqlJsonPath_complex() {
		// GIVEN
		SearchFilter f = SearchFilter.forLDAPSearchFilterString(
				"(& (foo=bar) (| (bam.pop~=whiz) (boo.boo>0) (! (bam.ding<=9))))");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Complex path", result, is(equalTo(
				"$ ? (@.foo == \"bar\" && (@.bam.pop like_regex \"whiz\" || @.boo.boo > 0 || !(@.bam.ding <= 9)))")));
	}

	@Test
	public void sqlJsonPath_nestedMiddle() {

		// GIVEN
		SearchFilter f = SearchFilter.forLDAPSearchFilterString(
				"(& (foo=bar) (| (bam.pop~=whiz) (boo.boo>0) ) (bam.ding<=9))");

		// WHEN
		String result = SearchFilterUtils.toSqlJsonPath(f);

		// THEN
		assertThat("Complex path", result, is(equalTo(
				"$ ? (@.foo == \"bar\" && (@.bam.pop like_regex \"whiz\" || @.boo.boo > 0) && @.bam.ding <= 9)")));
	}

}
