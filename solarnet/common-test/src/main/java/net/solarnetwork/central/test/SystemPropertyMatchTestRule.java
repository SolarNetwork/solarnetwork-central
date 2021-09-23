/* ==================================================================
 * SystemPropertyMatchTestRule.java - 24/05/2018 3:40:21 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

import java.util.regex.Pattern;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test rule that tests for the existence of a system property, or a system
 * property that matches a specific expression.
 * 
 * @author matt
 * @version 1.1
 * @since 1.9
 */
public class SystemPropertyMatchTestRule implements TestRule {

	private final String key;
	private final Pattern regex;

	/**
	 * Construct for an existence test.
	 * 
	 * @param key
	 *        the key to test for the existence of
	 */
	public SystemPropertyMatchTestRule(String key) {
		this(key, ".*");
	}

	/**
	 * Construct for a matching test.
	 * 
	 * @param key
	 *        the key to test the value of
	 * @param expression
	 *        the regular expression to test the value against, which will be
	 *        treated as a case-insensitive expression
	 */
	public SystemPropertyMatchTestRule(String key, String expression) {
		super();
		this.key = key;
		this.regex = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
	}

	@Override
	public Statement apply(final Statement statement, Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				String v = System.getProperty(key);
				if ( v != null && regex.matcher(v).matches() ) {
					statement.evaluate();
				} else {
					throw new org.junit.AssumptionViolatedException("System property [" + key
							+ "] not found, or value does not match [" + regex + "] Skipping test!");
				}
			}
		};
	}

}
