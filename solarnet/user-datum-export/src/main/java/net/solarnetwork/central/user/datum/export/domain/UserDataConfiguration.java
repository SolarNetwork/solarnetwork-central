/* ==================================================================
 * UserDataConfiguration.java - 21/03/2018 2:01:46 PM
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

package net.solarnetwork.central.user.datum.export.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.DataConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * User related entity for {@link DataConfiguration}.
 *
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "id", "created", "userId", "name", "serviceIdentifier", "serviceProps" })
@JsonIgnoreProperties("enabled")
public class UserDataConfiguration extends BaseExportConfigurationEntity<UserDataConfiguration>
		implements DataConfiguration, Serializable {

	@Serial
	private static final long serialVersionUID = 866381003784859350L;

	private @Nullable String filterJson;

	private transient @Nullable DatumFilterCommand filter;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserDataConfiguration(UserLongCompositePK id, Instant created, String name,
			String serviceIdentifier) {
		super(id, created, name, serviceIdentifier);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserDataConfiguration(Long userId, Long configId, Instant created, String name,
			String serviceIdentifier) {
		this(new UserLongCompositePK(userId, configId), created, name, serviceIdentifier);
	}

	@Override
	public UserDataConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new UserDataConfiguration(id, getCreated(), getName(), getServiceIdentifier());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserDataConfiguration entity) {
		super.copyTo(entity);
		entity.setFilterJson(filterJson);
	}

	@Override
	public boolean isSameAs(@Nullable UserDataConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return  // compare decoded JSON, as JSON key order not assumed
				Objects.equals(getFilter(), o.getFilter())
				;
		// @formatter:on
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

}
