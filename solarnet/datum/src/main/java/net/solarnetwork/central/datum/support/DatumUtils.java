/* ==================================================================
 * DatumUtils.java - Feb 13, 2012 2:52:39 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.springframework.util.PathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Utilities for Datum domain classes.
 *
 * @author matt
 * @version 2.1
 */
public final class DatumUtils {

	// can't construct me
	private DatumUtils() {
		super();
	}

	/**
	 * Convert an object to a JSON string. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and null values will not
	 * be included in the output. All exceptions while serializing the object
	 * are caught and ignored.
	 *
	 * @param o
	 *        the object to serialize to JSON
	 * @param defaultValue
	 *        a default value to use if {@code o} is <em>null</em> or if any
	 *        error occurs serializing the object to JSON
	 * @return the JSON string
	 * @since 1.1
	 * @see JsonUtils#getJSONString(Object, String)
	 */
	public static String getJSONString(final Object o, final String defaultValue) {
		return JsonUtils.getJSONString(o, defaultValue);
	}

	/**
	 * Convert a JSON string to an object. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and all floating point
	 * values will be converted to {@link BigDecimal} values to faithfully
	 * represent the data. All exceptions while deserializing the object are
	 * caught and ignored.
	 *
	 * @param <T>
	 *        the return object type
	 * @param json
	 *        the JSON string to convert
	 * @param clazz
	 *        the type of Object to map the JSON into
	 * @return the object
	 * @since 1.1
	 * @see JsonUtils#getJSONString(Object, String)
	 */
	public static <T> T getObjectFromJSON(final String json, Class<T> clazz) {
		return JsonUtils.getObjectFromJSON(json, clazz);
	}

	/**
	 * Filter a set of node sources using a source ID path pattern.
	 *
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 *
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 * @since 1.3
	 */
	public static Set<NodeSourcePK> filterNodeSources(Set<NodeSourcePK> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<NodeSourcePK> itr = sources.iterator(); itr.hasNext(); ) {
			NodeSourcePK pk = itr.next();
			if ( !pathMatcher.match(pattern, pk.getSourceId()) ) {
				itr.remove();
			}
		}
		return sources;
	}

	/**
	 * Filter a set of sources using a source ID path pattern.
	 *
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 *
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 * @since 1.3
	 */
	public static Set<String> filterSources(Set<String> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<String> itr = sources.iterator(); itr.hasNext(); ) {
			String source = itr.next();
			if ( !pathMatcher.match(pattern, source) ) {
				itr.remove();
			}
		}
		return sources;
	}

	/**
	 * Convert a {@link Datum} into either a {@link GeneralNodeDatum} or
	 * {@link GeneralLocationDatum} object.
	 *
	 * @param datum
	 *        the datum to convert
	 * @return the converted datum, or {@literal null} if {@code datum} is
	 *         {@literal null}
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object convertGeneralDatum(Datum datum) {
		if ( datum == null ) {
			return null;
		}
		DatumSamplesOperations ops = datum.asSampleOperations();
		var s = new DatumSamples();
		s.setI((Map) ops.getSampleData(DatumSamplesType.Instantaneous));
		s.setA((Map) ops.getSampleData(DatumSamplesType.Accumulating));
		s.setS((Map) ops.getSampleData(DatumSamplesType.Status));
		s.setT(ops.getTags());
		if ( datum.getKind() == ObjectDatumKind.Location ) {
			GeneralLocationDatum gld = new GeneralLocationDatum();
			gld.setCreated(datum.getTimestamp());
			gld.setSourceId(datum.getSourceId());
			gld.setLocationId(datum.getObjectId());
			gld.setSamples(s);
			return gld;
		}
		GeneralNodeDatum gnd = new GeneralNodeDatum();
		gnd.setCreated(datum.getTimestamp());
		gnd.setSourceId(datum.getSourceId());
		gnd.setNodeId(datum.getObjectId());
		gnd.setSamples(s);
		return gnd;
	}

}
