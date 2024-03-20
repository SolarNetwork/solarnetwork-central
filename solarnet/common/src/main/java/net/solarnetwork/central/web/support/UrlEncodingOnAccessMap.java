/* ==================================================================
 * UrlEncodingOnAccessMap.java - 21/03/2024 6:20:55 am
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

package net.solarnetwork.central.web.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.util.ObjectUtils;

/**
 * Map that encodes values from a delegate {@link Map} instance with
 * {@link URLEncoder} when accessed via {@link #get(Object)}.
 * 
 * @author matt
 * @version 1.0
 */
public class UrlEncodingOnAccessMap<K> implements Map<K, String> {

	private final Map<K, String> delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate map
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UrlEncodingOnAccessMap(Map<K, String> delegate) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public String get(Object key) {
		String val = delegate.get(key);
		if ( val != null ) {
			return URLEncoder.encode(val.toString(), StandardCharsets.UTF_8).replace("+", "%20");
		}
		return val;
	}

	@Override
	public String put(K key, String value) {
		return delegate.put(key, value);
	}

	@Override
	public String remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends String> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<String> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<K, String>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

}
