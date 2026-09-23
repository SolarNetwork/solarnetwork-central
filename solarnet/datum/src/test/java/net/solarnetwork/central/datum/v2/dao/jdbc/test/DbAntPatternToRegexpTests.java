/* ==================================================================
 * DbAntPatternToRegexpTests.java - 18/02/2025 9:46:25 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.PreparedStatementCallback;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;

/**
 * Test cases for the {@code solarcommon.ant_pattern_to_regexp()} stored
 * procedure.
 *
 * @author matt
 * @version 1.1
 */
public class DbAntPatternToRegexpTests extends AbstractJUnit5JdbcDaoTestSupport {

	private boolean testMatch(PreparedStatement ps, String sourceId, String pat) {
		try {
			ps.setString(1, sourceId);
			ps.setString(2, pat);
			if ( ps.execute() ) {
				try (ResultSet rs = ps.getResultSet()) {
					return rs.next() && rs.getBoolean(1);
				}
			}
			return false;
		} catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void noWildcards() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					then(testMatch(ps, "/a/b", "/a/b")).isTrue();
					then(testMatch(ps, "/a/b", "/c")).isFalse();
					return null;
				});
	}

	@Test
	public void singleAsterisk() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					then(testMatch(ps, "src/file.js", "src/*.js")).isTrue();
					then(testMatch(ps, "src/subdir/file.js", "src/*.js")).isFalse();

					then(testMatch(ps, "/src/file.js", "/src/*.js")).isTrue();
					then(testMatch(ps, "/src/subdir/file.js", "/src/*.js")).isFalse();
					return null;
				});
	}

	@Test
	public void doubleAsterisk() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "src/**/*.js";
					then(testMatch(ps, "src/file.js", pat)).isTrue();
					then(testMatch(ps, "src/subdir/file.js", pat)).isTrue();
					then(testMatch(ps, "src/subdir/nested/file.js", pat)).isTrue();
					then(testMatch(ps, "nah", pat)).isFalse();
					then(testMatch(ps, "src/file.nah", pat)).isFalse();
					then(testMatch(ps, "src/subdir/file.nah", pat)).isFalse();
					then(testMatch(ps, "src/subdir/nested/file.nah", pat)).isFalse();
					then(testMatch(ps, "nah/file.js", pat)).isFalse();
					then(testMatch(ps, "nah/subdir/file.js", pat)).isFalse();
					return null;
				});
	}

	@Test
	public void doubleAsterisk2() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "src/**/foo";
					then(testMatch(ps, "src/foo", pat)).isTrue();
					then(testMatch(ps, "src/subdir/foo", pat)).isTrue();
					then(testMatch(ps, "src/subdir/nested/foo", pat)).isTrue();
					then(testMatch(ps, "nah", pat)).isFalse();
					then(testMatch(ps, "src/foo.nah", pat)).isFalse();
					then(testMatch(ps, "src/subdir/foo.nah", pat)).isFalse();
					then(testMatch(ps, "src/subdir/nested/foo.nah", pat)).isFalse();
					then(testMatch(ps, "nah/foo", pat)).isFalse();
					then(testMatch(ps, "nah/subdir/foo", pat)).isFalse();
					then(testMatch(ps, "src/nahfoo", pat)).isFalse();
					then(testMatch(ps, "src/subdir/nahfoo", pat)).isFalse();
					return null;
				});
	}

	@Test
	public void doubleAsteriskStart() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "**/*.js";
					then(testMatch(ps, "src/file.js", pat)).isTrue();
					then(testMatch(ps, "src/subdir/file.js", pat)).isTrue();
					then(testMatch(ps, "src/subdir/nested/file.js", pat)).isTrue();
					then(testMatch(ps, "nah", pat)).isFalse();
					then(testMatch(ps, "src/file.nah", pat)).isFalse();
					then(testMatch(ps, "src/subdir/file.nah", pat)).isFalse();
					then(testMatch(ps, "src/subdir/nested/file.nah", pat)).isFalse();
					then(testMatch(ps, "nah/file.js", pat)).isTrue();
					then(testMatch(ps, "nah/subdir/file.js", pat)).isTrue();
					return null;
				});
	}

	@Test
	public void doubleAsteriskEnd() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "src/**";
					then(testMatch(ps, "src", pat)).isTrue();
					then(testMatch(ps, "src/file.js", pat)).isTrue();
					then(testMatch(ps, "src/subdir/file.js", pat)).isTrue();
					then(testMatch(ps, "src/subdir/nested/file.js", pat)).isTrue();
					then(testMatch(ps, "nah", pat)).isFalse();
					then(testMatch(ps, "src/file.nah", pat)).isTrue();
					then(testMatch(ps, "src/subdir/file.nah", pat)).isTrue();
					then(testMatch(ps, "src/subdir/nested/file.nah", pat)).isTrue();
					then(testMatch(ps, "nah/file.js", pat)).isFalse();
					then(testMatch(ps, "nah/subdir/file.js", pat)).isFalse();
					return null;
				});
	}

	@Test
	public void questionMark() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "src/?.js";
					then(testMatch(ps, "src/a.js", pat)).isTrue();
					then(testMatch(ps, "src/abc.js", pat)).isFalse();
					then(testMatch(ps, "src/nested/a.js", pat)).isFalse();
					return null;
				});
	}

	@Test
	public void escapedSpecialCharacters() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "src/[abc].js";
					then(testMatch(ps, "src/[abc].js", pat)).isTrue();
					then(testMatch(ps, "src/a.js", pat)).isFalse();
					return null;
				});
	}

	@Test
	public void mixedPatterns() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "src/*/test?.js";
					then(testMatch(ps, "src/dir/test1.js", pat)).isTrue();
					then(testMatch(ps, "src/test1.js", pat)).isFalse();
					then(testMatch(ps, "src/dir/subdir/test1.js", pat)).isFalse();
					return null;
				});
	}

	/**
	 * Test that a trailing {@literal **} matches zero trailing segments, as
	 * {@code AntPathMatcher} does.
	 *
	 * @since 1.1
	 */
	@Test
	public void doubleAsteriskEndMatchesBareSegment() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					then(testMatch(ps, "/power", "/power/**")).isTrue();
					then(testMatch(ps, "/power/meter", "/power/**")).isTrue();
					then(testMatch(ps, "/powerx", "/power/**")).isFalse();

					then(testMatch(ps, "/m/building", "/**/building/**")).isTrue();
					then(testMatch(ps, "/pm/building/floors", "/**/building/**")).isTrue();
					then(testMatch(ps, "/pm/buildingx/floors", "/**/building/**")).isFalse();
					return null;
				});
	}

	/**
	 * Test that {@literal **} only spans segments when it forms a complete
	 * segment, and otherwise behaves like {@literal *}.
	 *
	 * @since 1.1
	 */
	@Test
	public void doubleAsteriskWithinSegment() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					then(testMatch(ps, "/a/yb", "/a/**b")).isTrue();
					then(testMatch(ps, "/a/x/yb", "/a/**b")).isFalse();

					then(testMatch(ps, "/a/bx", "/a/b**")).isTrue();
					then(testMatch(ps, "/a/b/x", "/a/b**")).isFalse();
					return null;
				});
	}

	/**
	 * Test that adjacent {@literal **} segments are equivalent to a single one.
	 *
	 * @since 1.1
	 */
	@Test
	public void adjacentDoubleAsteriskSegments() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					final var pat = "/a/**/**/c";
					then(testMatch(ps, "/a/c", pat)).isTrue();
					then(testMatch(ps, "/a/b/c", pat)).isTrue();
					then(testMatch(ps, "/a/b/x/c", pat)).isTrue();
					then(testMatch(ps, "/a/b/x", pat)).isFalse();
					return null;
				});
	}

	/**
	 * Test that a pattern only matches a path that agrees on the leading
	 * separator.
	 *
	 * @since 1.1
	 */
	@Test
	public void leadingSeparatorMustAgree() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					then(testMatch(ps, "a/b/c", "**/c")).isTrue();
					then(testMatch(ps, "/a/b/c", "**/c")).isFalse();

					then(testMatch(ps, "/a/b/c", "/**/c")).isTrue();
					then(testMatch(ps, "a/b/c", "/**/c")).isFalse();
					return null;
				});
	}

	/**
	 * Test the source ID examples documented on the SolarNetwork "Wildcard
	 * patterns" wiki page.
	 *
	 * @since 1.1
	 */
	@Test
	public void documentedSourceIdExamples() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					// /power/** matches /power, /power/meter, and /power/meter/1
					then(testMatch(ps, "/power", "/power/**")).isTrue();
					then(testMatch(ps, "/power/meter", "/power/**")).isTrue();
					then(testMatch(ps, "/power/meter/1", "/power/**")).isTrue();
					then(testMatch(ps, "/switch/1", "/power/**")).isFalse();

					// /power/* matches /power/meter
					then(testMatch(ps, "/power/meter", "/power/*")).isTrue();
					then(testMatch(ps, "/power", "/power/*")).isFalse();
					then(testMatch(ps, "/power/meter/1", "/power/*")).isFalse();

					// /**/1 matches /power/meter/1 and /switch/1
					then(testMatch(ps, "/power/meter/1", "/**/1")).isTrue();
					then(testMatch(ps, "/switch/1", "/**/1")).isTrue();
					then(testMatch(ps, "/switch/3/a", "/**/1")).isFalse();

					// /basement/**/lights matches only the basement lights
					then(testMatch(ps, "/basement/bedroom/lights", "/basement/**/lights")).isTrue();
					then(testMatch(ps, "/basement/tvroom/lights", "/basement/**/lights")).isTrue();
					then(testMatch(ps, "/ground/dining/lights", "/basement/**/lights")).isFalse();
					then(testMatch(ps, "/basement/bedroom/heater", "/basement/**/lights")).isFalse();
					return null;
				});
	}

	/**
	 * Test the metadata path examples documented on the SolarNetwork "Wildcard
	 * patterns" wiki page.
	 *
	 * @since 1.1
	 */
	@Test
	public void documentedMetadataPathExamples() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					// /m/* matches /m/building and /m/room
					then(testMatch(ps, "/m/building", "/m/*")).isTrue();
					then(testMatch(ps, "/m/room", "/m/*")).isTrue();
					then(testMatch(ps, "/pm/building/floors", "/m/*")).isFalse();

					// /** matches everything
					then(testMatch(ps, "/m/building", "/**")).isTrue();
					then(testMatch(ps, "/pm/hours/holiday/M-F", "/**")).isTrue();

					// /pm/hours/* matches only the direct children of hours
					then(testMatch(ps, "/pm/hours/M-F", "/pm/hours/*")).isTrue();
					then(testMatch(ps, "/pm/hours/Sa-Su", "/pm/hours/*")).isTrue();
					then(testMatch(ps, "/pm/hours/holiday/M-F", "/pm/hours/*")).isFalse();

					// /**/building/** matches /m/building and both /pm/building children
					then(testMatch(ps, "/m/building", "/**/building/**")).isTrue();
					then(testMatch(ps, "/pm/building/floors", "/**/building/**")).isTrue();
					then(testMatch(ps, "/pm/building/employees", "/**/building/**")).isTrue();
					then(testMatch(ps, "/m/room", "/**/building/**")).isFalse();
					return null;
				});
	}

	/**
	 * Test that the internal segment marker cannot be injected via a pattern.
	 *
	 * @since 1.1
	 */
	@Test
	public void segmentMarkerNotInjectable() {
		jdbcTemplate.execute("SELECT ?::text ~ solarcommon.ant_pattern_to_regexp(?)",
				(PreparedStatementCallback<Void>) ps -> {
					then(testMatch(ps, "/a/anything/b", "/a/\001/b")).isFalse();
					then(testMatch(ps, "/a//b", "/a/\001/b")).isTrue();
					return null;
				});
	}

}
