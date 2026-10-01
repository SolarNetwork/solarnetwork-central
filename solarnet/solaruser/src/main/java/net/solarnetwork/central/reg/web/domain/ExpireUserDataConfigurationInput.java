/* ==================================================================
 * ExpireUserDataConfigurationInput.java - 2 Oct 2026 8:46:31 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.domain;

import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.central.user.datum.expire.domain.ExpireUserDataConfiguration;

/**
 * Input DTO for {@link ExpireUserDataConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public final class ExpireUserDataConfigurationInput {

	private @Nullable Long id;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String name;

	@NotNull
	@NotBlank
	@Size(max = 128)
	private @Nullable String serviceIdentifier;

	private @Nullable Map<String, Object> serviceProperties;

	private boolean active = false;

	private int expireDays = ExpireUserDataConfiguration.DEFAULT_EXPIRE_DAYS;

	private @Nullable DatumFilterCommand datumFilter;

	/**
	 * Constructor.
	 */
	public ExpireUserDataConfigurationInput() {
		super();
	}

	/**
	 * Create an entity from the input properties and a given user and date.
	 *
	 * <p>
	 * If no ID is available then the entity will be assigned
	 * {@link EntityConstants#UNASSIGNED_LONG_ID}.
	 * </p>
	 *
	 * @param userId
	 *        the ID of the user to assign to the entity
	 * @param date
	 *        the creation date to use
	 * @return the new entity
	 */
	@SuppressWarnings("NullAway")
	public ExpireUserDataConfiguration toEntity(Long userId, Instant date) {
		ExpireUserDataConfiguration entity = new ExpireUserDataConfiguration(
				id != null ? id : EntityConstants.UNASSIGNED_LONG_ID, userId, date, name,
				serviceIdentifier);
		entity.setServiceProps(serviceProperties);
		entity.setActive(active);
		entity.setExpireDays(expireDays);
		entity.setFilter(datumFilter);
		return entity;
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the ID
	 */
	public final @Nullable Long getId() {
		return id;
	}

	/**
	 * Set the configuration ID.
	 *
	 * @param id
	 *        the ID to set
	 */
	public final void setId(@Nullable Long id) {
		this.id = id;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public final void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * Get the identifier of the service to use.
	 *
	 * @return the identifier
	 */
	public final @Nullable String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the identifier of the service to use.
	 *
	 * @param serviceIdentifier
	 *        the identifier to use
	 */
	public final void setServiceIdentifier(@Nullable String serviceIdentifier) {
		this.serviceIdentifier = serviceIdentifier;
	}

	/**
	 * Get the service properties.
	 *
	 * @return the service properties
	 */
	public final @Nullable Map<String, Object> getServiceProperties() {
		return serviceProperties;
	}

	/**
	 * Set the service properties to use.
	 *
	 * @param serviceProperties
	 *        the service properties to set
	 */
	public final void setServiceProperties(@Nullable Map<String, Object> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	/**
	 * Get the active flag.
	 *
	 * @return {@literal true} if the configuration should be applied
	 */
	public final boolean isActive() {
		return active;
	}

	/**
	 * Set the active flag.
	 *
	 * @param active
	 *        {@literal true} if the configuration should be applied
	 */
	public final void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Get the minimum age after which datum matching the filter should expire.
	 *
	 * @return the expire age, in days; defaults to
	 *         {@link ExpireUserDataConfiguration#DEFAULT_EXPIRE_DAYS}
	 */
	public final int getExpireDays() {
		return expireDays;
	}

	/**
	 * Set the minimum age after which datum matching the filter should expire.
	 *
	 * @param expireDays
	 *        the expire age to set, in days
	 */
	public final void setExpireDays(int expireDays) {
		this.expireDays = expireDays;
	}

	/**
	 * Get the datum filter.
	 *
	 * @return the filter
	 */
	public final @Nullable DatumFilterCommand getDatumFilter() {
		return datumFilter;
	}

	/**
	 * Set the datum filter.
	 *
	 * @param datumFilter
	 *        the filter to set
	 */
	public final void setDatumFilter(@Nullable DatumFilterCommand datumFilter) {
		this.datumFilter = datumFilter;
	}

}
