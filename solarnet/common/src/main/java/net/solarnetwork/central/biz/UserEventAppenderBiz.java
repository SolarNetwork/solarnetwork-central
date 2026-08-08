/* ==================================================================
 * UserEventAppenderBiz.java - 1/08/2022 3:25:32 pm
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

package net.solarnetwork.central.biz;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import com.nimbusds.jose.util.StandardCharset;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.common.mqtt.MessageSizeLimitExceeded;

/**
 * Service API for appending user events.
 * 
 * @author matt
 * @version 2.2
 */
public interface UserEventAppenderBiz {

	/**
	 * Add an event.
	 * 
	 * @param userId
	 *        the user account ID
	 * @param info
	 *        the event info to add
	 * @return the generated event
	 */
	UserEvent addEvent(Long userId, LogEventInfo info);

	/**
	 * Helper function to add an event to an optional appender.
	 * 
	 * <p>
	 * If {@code biz} is {@code null}, this method simply returns {@code null}.
	 * </p>
	 * 
	 * @param biz
	 *        the optional appender
	 * @param userId
	 *        the user account ID
	 * @param info
	 *        the event info to add
	 * @return the generated event, or {@code null} if {@code biz} is
	 *         {@code null}
	 */
	static @Nullable UserEvent addUserEvent(@Nullable UserEventAppenderBiz biz, Long userId,
			LogEventInfo info) {
		if ( biz == null ) {
			return null;
		}
		return biz.addEvent(userId, info);
	}

	/**
	 * A function to generate a SolarFlux MQTT topic from a user event.
	 *
	 * @since 2.1
	 */
	public static Function<UserEvent, String> SOLARFLUX_TOPIC_FN = (event) -> "user/" + event.getUserId()
			+ "/event";

	/**
	 * A function to generate a SolarFlux MQTT topic from a user event.
	 *
	 * @since 2.1
	 */
	public static final Function<UserEvent, @Nullable String> SOLARFLUX_TAGGED_TOPIC_FN = (event) -> {
		final StringBuilder buf = new StringBuilder("user/");
		buf.append(event.getUserId()).append("/event");

		final String[] tags = event.getTags();
		for ( String tag : tags ) {
			buf.append('/');
			buf.append(tag);
		}
		return buf.toString();
	};

	/**
	 * A function to generate a SolarFlux MQTT error topic from a user event and
	 * exception.
	 *
	 * @since 2.2
	 */
	public static final BiFunction<UserEvent, Throwable, @Nullable String> SOLARFLUX_TAGGED_ERROR_TOPIC_FN = (
			event, _) -> {
		final StringBuilder buf = new StringBuilder("user/");
		buf.append(event.getUserId()).append("/event");

		final String[] tags = event.getTags();
		for ( String tag : tags ) {
			buf.append('/');
			buf.append(tag);
		}
		if ( !buf.isEmpty() ) {
			buf.append('/');
		}
		buf.append(CommonUserEvents.ERROR_TAG);
		return buf.toString();
	};

	/**
	 * A function to generate a SolarFlux MQTT error item from a user event and
	 * exception.
	 *
	 * @since 2.2
	 */
	public static final BiFunction<UserEvent, Throwable, @Nullable UserEvent> SOLARFLUX_TAGGED_ERROR_ITEM_FN = (
			event, t) -> {
		if ( !(t instanceof MessageSizeLimitExceeded sle) ) {
			return null;
		}
		// create new error UserEvent with smaller payload to try again

		return new UserEvent(event.id(), event.getTags(),
				"Unable to publish event because the payload length %s exceeds the maximum allowed %d."
						.formatted(sle.getMessageSize(), sle.getMaximumSize()),
				limitedSizeEventData(event, (long) (sle.getMaximumSize() * 0.8)));
	};

	/**
	 * Get a reduced-size data map from an event.
	 * 
	 * @param event
	 *        the event to get a reduced size payload from
	 * @param maximumSize
	 *        the maximum size of the data payload desired; this should be very
	 *        approximate, and conservative as the size calculation is not exact
	 * @return the limited size data map
	 */
	public static String limitedSizeEventData(final UserEvent event, final long maximumSize) {
		Map<String, Object> data = JsonUtils.getStringMap(event.getData());
		if ( data == null ) {
			return "";
		}
		pruneMap(data, maximumSize);
		String result = JsonUtils.getJSONString(data);
		if ( data.isEmpty() || result.getBytes(StandardCharset.UTF_8).length > maximumSize ) {
			result = """
					{"message":"Content too large to preserve."}""";
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void pruneMap(final Map<String, Object> data, final long maximumSize) {
		long runningTotal = 0;
		for ( Iterator<Entry<String, Object>> itr = data.entrySet().iterator(); itr.hasNext(); ) {
			final Entry<String, Object> e = itr.next();
			int len = e.getKey().getBytes(StandardCharsets.UTF_8).length;
			if ( runningTotal + len > maximumSize ) {
				itr.remove();
				continue;
			}
			if ( e.getValue() instanceof Map<?, ?> map ) {
				// try to prune nested map
				pruneMap((Map) map, maximumSize - runningTotal - len);
			}
			// track individual map values, dropping any that put us over the maximumSize
			final String val = JsonUtils.getJSONString(e.getValue().toString());
			len += val.getBytes(StandardCharsets.UTF_8).length;
			if ( runningTotal + len < maximumSize ) {
				runningTotal += len;
				continue;
			}
			itr.remove();
		}
	}

}
