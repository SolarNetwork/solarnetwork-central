/* ==================================================================
 * SnAddressInput.java - 26 Sept 2026 3:07:19 pm
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

package net.solarnetwork.central.user.account.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.domain.validation.ValidCountryCode;
import net.solarnetwork.central.domain.validation.ValidTimeZoneId;

/**
 * DTO for address configuration.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class SnAddressInput implements Serializable {

	@Serial
	private static final long serialVersionUID = -3131191080869369863L;

	@NotBlank
	private @Nullable String name;

	@NotNull
	@Email
	private @Nullable String email;

	@NotNull
	@ValidCountryCode
	private @Nullable String country;

	@NotNull
	@ValidTimeZoneId
	private @Nullable String timeZoneId;

	private @Nullable String region;
	private @Nullable String stateOrProvince;
	private @Nullable String locality;
	private @Nullable String postalCode;
	private @Nullable String @Nullable [] street;

	/**
	 * Constructor.
	 */
	public SnAddressInput() {
		super();
	}

	/**
	 * Create an input instance from an address entity.
	 * 
	 * @param addr
	 *        the address to create an input for
	 * @return the input
	 */
	public static SnAddressInput forAddress(@Nullable SnAddress<?, ?> addr) {
		final var result = new SnAddressInput();
		if ( addr != null ) {
			result.setName(addr.getName());
			result.setEmail(addr.getEmail());
			result.setCountry(addr.getCountry());
			result.setTimeZoneId(addr.getTimeZoneId());
			result.setRegion(addr.getRegion());
			result.setStateOrProvince(addr.getStateOrProvince());
			result.setLocality(addr.getLocality());
			result.setPostalCode(addr.getPostalCode());
			result.setStreet(addr.getStreet());
		}
		return result;
	}

	/**
	 * Get the account name.
	 * 
	 * @return the name
	 */
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Set the account name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public final void setName(@Nullable String name) {
		this.name = nonEmptyString(name);
	}

	/**
	 * Get the account email.
	 * 
	 * @return the email
	 */
	public final @Nullable String getEmail() {
		return email;
	}

	/**
	 * Set the account email.
	 * 
	 * @param email
	 *        the email to set
	 */
	public final void setEmail(@Nullable String email) {
		this.email = nonEmptyString(email);
	}

	/**
	 * Get the account country code.
	 * 
	 * @return the country
	 */
	public final @Nullable String getCountry() {
		return country;
	}

	/**
	 * Set the account country code.
	 * 
	 * @param country
	 *        the country to set
	 */
	public final void setCountry(@Nullable String country) {
		this.country = nonEmptyString(country);
	}

	/**
	 * Get the time zone ID.
	 * 
	 * @return the timeZoneId
	 */
	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID.
	 * 
	 * @param timeZoneId
	 *        the timeZoneId to set
	 */
	public final void setTimeZoneId(@Nullable String timeZoneId) {
		this.timeZoneId = nonEmptyString(timeZoneId);
	}

	/**
	 * Get the region.
	 * 
	 * @return the region
	 */
	public final @Nullable String getRegion() {
		return region;
	}

	/**
	 * Set the region.
	 * 
	 * @param region
	 *        the region to set
	 */
	public final void setRegion(@Nullable String region) {
		this.region = nonEmptyString(region);
	}

	/**
	 * Get the state or province.
	 * 
	 * @return the state or province
	 */
	public final @Nullable String getStateOrProvince() {
		return stateOrProvince;
	}

	/**
	 * Set the state or province.
	 * 
	 * @param stateOrProvince
	 *        the state or province to set
	 */
	public final void setStateOrProvince(@Nullable String stateOrProvince) {
		this.stateOrProvince = nonEmptyString(stateOrProvince);
	}

	/**
	 * Get the locality.
	 * 
	 * @return the locality
	 */
	public final @Nullable String getLocality() {
		return locality;
	}

	/**
	 * Set the locatlity.
	 * 
	 * @param locality
	 *        the locality to set
	 */
	public final void setLocality(@Nullable String locality) {
		this.locality = nonEmptyString(locality);
	}

	/**
	 * Get the postal code.
	 * 
	 * @return the postal code
	 */
	public final @Nullable String getPostalCode() {
		return postalCode;
	}

	/**
	 * Set the postal code.
	 * 
	 * @param postalCode
	 *        the postal code to set
	 */
	public final void setPostalCode(@Nullable String postalCode) {
		this.postalCode = nonEmptyString(postalCode);
	}

	/**
	 * Get a non-null street array.
	 * 
	 * @return the street array with all blanks removed
	 * @throws IllegalArgumentException
	 *         if no non-blank element is available
	 */
	public final String[] street() {
		final var streets = this.street;
		final List<String> streetsList = new ArrayList<>(streets != null ? streets.length : 0);
		if ( streets != null ) {
			for ( String s : streets ) {
				if ( s != null && !s.isBlank() ) {
					streetsList.add(s);
				}
			}
		}
		return requireNonEmptyArgument(streetsList.toArray(String[]::new), "street");
	}

	/**
	 * Set the street.
	 * 
	 * @return the street
	 */
	public final @Nullable String @Nullable [] getStreet() {
		return street;
	}

	/**
	 * Get the street.
	 * 
	 * @param street
	 *        the street to set
	 */
	public final void setStreet(@Nullable String @Nullable [] street) {
		this.street = street;
	}

	/**
	 * Set the street first element.
	 * 
	 * @return the street
	 */
	public final @Nullable String getStreet1() {
		return (street != null && street.length > 0 ? street[0] : null);
	}

	/**
	 * Get the street first element.
	 * 
	 * @param street
	 *        the street to set
	 */
	public final void setStreet1(@Nullable String street) {
		if ( this.street == null || this.street.length < 1 ) {
			this.street = new String[2];
		}
		this.street[0] = nonEmptyString(street);
	}

	/**
	 * Set the street second element.
	 * 
	 * @return the street
	 */
	public final @Nullable String getStreet2() {
		return (street != null && street.length > 1 ? street[1] : null);
	}

	/**
	 * Get the street second element.
	 * 
	 * @param street
	 *        the street to set
	 */
	public final void setStreet2(@Nullable String street) {
		if ( this.street == null || this.street.length < 2 ) {
			@Nullable
			String[] tmp = new String[2];
			if ( this.street != null && this.street.length > 0 ) {
				tmp[0] = this.street[0];
			}
			this.street = tmp;
		}
		this.street[1] = nonEmptyString(street);
	}

}
