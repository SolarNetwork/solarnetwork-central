/* ==================================================================
 * SearchFilterUtils.java - 23/09/2022 5:19:20 pm
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

package net.solarnetwork.central.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.SearchFilter.CompareOperator;
import net.solarnetwork.util.SearchFilter.LogicOperator;
import net.solarnetwork.util.SearchFilter.VisitorCallback;
import net.solarnetwork.util.StringUtils;

/**
 * Utilities for working with {@link SearchFilter} objects.
 * 
 * @author matt
 * @version 1.0
 */
public final class SearchFilterUtils {

	private SearchFilterUtils() {
		// not available
	}

	private static class StackObj {

		private final LogicOperator op;
		private final SearchFilter node;
		private int count;

		private StackObj(LogicOperator op, SearchFilter node) {
			super();
			this.op = op;
			this.node = node;
			this.count = 0;
		}

		@Override
		public String toString() {
			return "StackObj{" + node + "," + count + "}";
		}
	}

	/**
	 * Convert a search filter into a JSON Path expression.
	 * 
	 * @param filter
	 *        the filter
	 * @return the JSON Path expression
	 */
	public static String toSqlJsonPath(SearchFilter filter) {
		if ( filter == null ) {
			return null;
		}
		Queue<StackObj> ancestors = new LinkedList<>();
		StringBuilder buf = new StringBuilder();
		filter.walk(new VisitorCallback() {

			@Override
			public boolean visit(SearchFilter node, SearchFilter parentNode) {
				StackObj ref = null;
				if ( parentNode == null ) {
					buf.append("$ ? (");
				}
				boolean found = false;
				for ( Iterator<StackObj> itr = ancestors.iterator(); itr.hasNext(); ) {
					StackObj so = itr.next();
					SearchFilter f = so.node;
					if ( f == parentNode ) {
						found = true;
						ref = so;
					} else if ( found ) {
						itr.remove();
						if ( so.op != LogicOperator.NOT ) {
							buf.append(")");
						}
					}
				}
				if ( !found ) {
					ref = new StackObj(
							parentNode != null ? parentNode.getLogicOperator() : node.getLogicOperator(),
							parentNode != null ? parentNode : node);
					ancestors.add(ref);
				}
				if ( node.hasNestedFilter() ) {
					if ( ref.count > 0 ) {
						switch (ref.op) {
							case AND:
								buf.append(" && ");
								break;
							case OR:
								buf.append(" || ");
								break;
							default:
								// nothing
						}
					}
					if ( node.getLogicOperator() == LogicOperator.NOT ) {
						ref.count++;
					}
					if ( node.getLogicOperator() != LogicOperator.NOT && parentNode != null ) {
						buf.append("(");
					}
				}
				if ( !node.hasNestedFilter() ) {
					LogicOperator op = ref.op;
					for ( Entry<String, ?> e : node.getFilter().entrySet() ) {
						switch (op) {
							case AND:
								if ( ref.count > 0 ) {
									buf.append(" && ");
								}
								break;
							case OR:
								if ( ref.count > 0 ) {
									buf.append(" || ");
								}
								break;
							case NOT:
								buf.append("!(");
								break;
						}
						String k = e.getKey();
						Object v = e.getValue();
						if ( k != null && v != null ) {
							String s = v.toString();
							Number n = StringUtils.numberValue(s);
							if ( node.getCompareOperator() == CompareOperator.PRESENT ) {
								buf.append("exists (@.").append(k).append(")");
							} else {
								buf.append("@.").append(k).append(" ");
								switch (node.getCompareOperator()) {
									case APPROX:
									case OVERLAP:
									case SUBSTRING:
										buf.append("like_regex");
										break;

									case EQUAL:
										buf.append("==");
										break;

									case GREATER_THAN:
										buf.append(">");
										break;

									case GREATER_THAN_EQUAL:
										buf.append(">=");
										break;

									case LESS_THAN:
										buf.append("<");
										break;

									case LESS_THAN_EQUAL:
										buf.append("<=");
										break;

									case NOT_EQUAL:
										buf.append("!=");
										break;

									case SUBSTRING_AT_START:
										buf.append("starts with");
										break;

									default:
										// ignore
								}
								buf.append(" ");
								switch (node.getCompareOperator()) {
									case APPROX:
									case OVERLAP:
									case SUBSTRING:
										appendJsonString(s);
										break;

									default:
										if ( n == null ) {
											appendJsonString(s);
										} else {
											buf.append(n);
										}
								}
							}
						}
						if ( op != LogicOperator.NOT ) {
							ref.count++;
						}
					}
					if ( op == LogicOperator.NOT ) {
						buf.append(")");
					}
				}
				return true;
			}

			private void appendJsonString(String s) {
				buf.append("\"");
				buf.append(s.replace("\"", "\\\""));
				buf.append("\"");
			}

		});
		while ( !ancestors.isEmpty() ) {
			StackObj ref = ancestors.remove();
			if ( ref.count > 0 ) {
				buf.append(")");
			}
		}
		return (!buf.isEmpty() ? buf.toString() : null);
	}

}
