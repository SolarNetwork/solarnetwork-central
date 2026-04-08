/* ==================================================================
 * ExpireUserDataConfiguration.java - 9/07/2018 10:10:11 AM
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

package net.solarnetwork.central.user.datum.expire.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * User related entity for {@link DataConfiguration}.
 *
 * @author matt
 * @version 1.2
 */
@JsonPropertyOrder({ "id", "created", "userId", "name", "serviceIdentifier", "serviceProps", "active",
		"expireDays", "datumFilter" })
public class ExpireUserDataConfiguration extends BaseExpireConfigurationEntity
		implements DataConfiguration, Serializable {

	@Serial
	private static final long serialVersionUID = 1098542708120529779L;

	/** The default value for the {@code expireDays} property (5 years). */
	public static final int DEFAULT_EXPIRE_DAYS = 1825;

	private boolean active = false;
	private int expireDays = DEFAULT_EXPIRE_DAYS;
	private @Nullable DatumFilterCommand filter;
	private @Nullable String filterJson;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param serviceIdentifier
	 *        the service identifier
	 * 
	 * @since 1.2
	 */
	public ExpireUserDataConfiguration(Long id, Long userId, Instant created, String name,
			String serviceIdentifier) {
		super(id, userId, created, name, serviceIdentifier);
	}

	@Override
	public final @Nullable AggregateGeneralNodeDatumFilter getDatumFilter() {
		return getFilter();
	}

	@JsonIgnore
	public final @Nullable String getFilterJson() {
		if ( filterJson == null ) {
			filterJson = JsonUtils.getJSONString(filter, null);
		}
		return filterJson;
	}

	public final void setFilterJson(@Nullable String filterJson) {
		this.filterJson = filterJson;
		filter = null;
	}

	@JsonIgnore
	public final @Nullable DatumFilterCommand getFilter() {
		if ( filter == null && filterJson != null ) {
			filter = JsonUtils.getObjectFromJSON(filterJson, DatumFilterCommand.class);
		}
		return filter;
	}

	@JsonSetter("datumFilter")
	public final void setFilter(@Nullable DatumFilterCommand filter) {
		this.filter = filter;
		filterJson = null;
	}

	@Override
	public final int getExpireDays() {
		return expireDays;
	}

	public final void setExpireDays(int expireDays) {
		this.expireDays = expireDays;
	}

	@Override
	public final boolean isActive() {
		return active;
	}

	public final void setActive(boolean enabled) {
		this.active = enabled;
	}

}
