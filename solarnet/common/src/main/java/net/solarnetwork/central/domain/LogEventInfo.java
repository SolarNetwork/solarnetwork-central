/* ==================================================================
 * LogEventInfo.java - 4/08/2022 9:58:37 am
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

package net.solarnetwork.central.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Standard log event info.
 *
 * @author matt
 * @version 1.1
 */
public class LogEventInfo {

	private final String[] tags;
	private final @Nullable String message;
	private final @Nullable String data;

	/**
	 * Create a new event.
	 *
	 * <p>
	 * This method will merge the base and extra tags into the resulting event.
	 * </p>
	 *
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param data
	 *        the data
	 * @param extraTags
	 *        optional extra tags
	 * @return the event
	 * @throws IllegalArgumentException
	 *         if no tags are provided
	 */
	@SuppressWarnings({ "null", "NullAway" })
	public static LogEventInfo event(String @Nullable [] baseTags, @Nullable String message,
			@Nullable String data, String @Nullable... extraTags) throws IllegalStateException {
		String[] tags = null;
		boolean hasBaseTags = (baseTags != null && baseTags.length > 0);
		boolean hasExtraTags = (extraTags != null && extraTags.length > 0);
		if ( hasBaseTags && hasExtraTags ) {
			tags = new String[baseTags.length + extraTags.length];
			System.arraycopy(baseTags, 0, tags, 0, baseTags.length);
			System.arraycopy(extraTags, 0, tags, baseTags.length, extraTags.length);
		} else if ( hasBaseTags ) {
			tags = baseTags;
		} else if ( hasExtraTags ) {
			tags = extraTags;
		}
		return new LogEventInfo(tags, message, data);
	}

	/**
	 * Create a new event.
	 *
	 * <p>
	 * This method will merge the base and extra tags into the resulting event.
	 * </p>
	 *
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param data
	 *        the data
	 * @param extraTags
	 *        optional extra tags
	 * @return the event
	 * @since 1.1
	 * @throws IllegalArgumentException
	 *         if no tags are provided
	 */
	@SuppressWarnings({ "null", "NullAway" })
	public static LogEventInfo event(@Nullable List<String> baseTags, @Nullable String message,
			@Nullable String data, String @Nullable... extraTags) {
		String[] tags = null;
		boolean hasBaseTags = (baseTags != null && !baseTags.isEmpty());
		boolean hasExtraTags = (extraTags != null && extraTags.length > 0);
		if ( hasBaseTags && hasExtraTags ) {
			tags = new String[baseTags.size() + extraTags.length];
			tags = baseTags.toArray(tags);
			System.arraycopy(extraTags, 0, tags, baseTags.size(), extraTags.length);
		} else if ( hasBaseTags ) {
			tags = baseTags.toArray(String[]::new);
		} else if ( hasExtraTags ) {
			tags = extraTags;
		}
		return new LogEventInfo(tags, message, data);
	}

	/**
	 * Create a new event.
	 *
	 * <p>
	 * This method will merge the base and extra tags into the resulting event.
	 * </p>
	 *
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param data
	 *        the data
	 * @param extraTags
	 *        optional extra tags
	 * @return the event
	 * @since 1.1
	 */
	@SuppressWarnings({ "null", "NullAway" })
	public static LogEventInfo event(@Nullable List<String> baseTags, @Nullable String message,
			@Nullable String data, @Nullable List<String> extraTags) {
		String[] tags = null;
		boolean hasBaseTags = (baseTags != null && !baseTags.isEmpty());
		boolean hasExtraTags = (extraTags != null && !extraTags.isEmpty());
		if ( hasBaseTags && hasExtraTags ) {
			tags = new String[baseTags.size() + extraTags.size()];
			tags = baseTags.toArray(tags);
			System.arraycopy(extraTags.toArray(String[]::new), 0, tags, baseTags.size(),
					extraTags.size());
		} else if ( hasBaseTags ) {
			tags = baseTags.toArray(String[]::new);
		} else if ( hasExtraTags ) {
			tags = extraTags.toArray(String[]::new);
			;
		}
		return new LogEventInfo(tags, message, data);
	}

	/**
	 * Constructor.
	 *
	 * @param tags
	 *        the tags
	 * @param message
	 *        the message
	 * @param data
	 *        the data
	 * @throws IllegalArgumentException
	 *         if {@code tags} is empty
	 */
	public LogEventInfo(String[] tags, @Nullable String message, @Nullable String data) {
		super();
		this.tags = requireNonEmptyArgument(tags, "tags");
		this.message = message;
		this.data = data;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LogEventInfo{");
		if ( tags != null ) {
			builder.append("tags=");
			builder.append(Arrays.toString(tags));
			builder.append(", ");
		}
		if ( message != null ) {
			builder.append("message=");
			builder.append(message);
			builder.append(", ");
		}
		if ( data != null ) {
			builder.append("data=");
			builder.append(data);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the tags.
	 *
	 * @return the tags
	 */
	public String[] getTags() {
		return tags;
	}

	/**
	 * Get the message.
	 *
	 * @return the message
	 */
	public @Nullable String getMessage() {
		return message;
	}

	/**
	 * Get the data.
	 *
	 * @return the data
	 */
	public @Nullable String getData() {
		return data;
	}

}
