/* ==================================================================
 * UsageTiers.java - 27/05/2021 12:24:45 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.domain.NamedCostTiers;

/**
 * A collection of ordered {@link UsageTier} objects.
 *
 * @author matt
 * @version 1.1
 * @since 1.1
 */
public class UsageTiers implements NamedCostTiers {

	private final List<UsageTier> tiers;
	private final @Nullable LocalDate date;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The tiers will be sorted by {@code quantity}.
	 * </p>
	 *
	 * @param tiers
	 *        the tiers; the list will be copied
	 * @throws IllegalArgumentException
	 *         if {@code tiers} is {@literal null}
	 */
	public UsageTiers(List<UsageTier> tiers) {
		this(tiers, null, UsageTier.SORT_BY_KEY_QUANTITY);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The tiers will be sorted by {@code quantity}.
	 * </p>
	 *
	 * @param tiers
	 *        the tiers; the list will be copied
	 * @param date
	 *        the date
	 * @throws IllegalArgumentException
	 *         if {@code tiers} is {@literal null}
	 */
	public UsageTiers(List<UsageTier> tiers, @Nullable LocalDate date) {
		this(tiers, date, UsageTier.SORT_BY_KEY_QUANTITY);
	}

	/**
	 * Constructor.
	 *
	 * @param tiers
	 *        the tiers; the list will be copied
	 * @param date
	 *        the date
	 * @param comparator
	 *        an optional comparator to sort the tiers with, or {@literal null}
	 *        to not sort
	 * @throws IllegalArgumentException
	 *         if {@code tiers} is {@literal null}
	 */
	public UsageTiers(List<UsageTier> tiers, @Nullable LocalDate date,
			@Nullable Comparator<UsageTier> comparator) {
		super();
		final List<UsageTier> t = requireNonNullArgument(tiers, "tiers");
		if ( comparator != null ) {
			List<UsageTier> sorted = new ArrayList<>(t);
			sorted.sort(UsageTier.SORT_BY_KEY_QUANTITY);
			this.tiers = sorted;
		} else {
			this.tiers = Collections.unmodifiableList(t);
		}
		this.date = date;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(format("| %-20s | %9s | %-12s |\n", "Key", "Quantity", "Cost"));
		buf.append("|----------------------|-----------|--------------|");
		for ( UsageTier tier : tiers ) {
			buf.append("\n");
			buf.append(format("| %-20s | %,9d | %0,9.10f |", tier.getKey(), tier.getQuantity(),
					tier.getCost()));
		}
		return buf.toString();
	}

	/**
	 * Get all tiers of a given type.
	 *
	 * @param key
	 *        the key of the tiers to get
	 * @return the matching tiers, never {@literal null}
	 */
	public final List<UsageTier> tiers(String key) {
		return tiers.stream().filter(t -> key.equals(t.getKey())).collect(Collectors.toList());
	}

	/**
	 * Get a mapping of all tier types to associated tiers.
	 *
	 * @return the tier map
	 */
	public final Map<String, List<UsageTier>> tierMap() {
		return tiers.stream().collect(Collectors.toMap(UsageTier::getKey, t -> {
			List<UsageTier> l = new ArrayList<>(4);
			l.add(t);
			return l;
		}, (l, r) -> {
			l.addAll(r);
			return l;
		}, LinkedHashMap::new));
	}

	/**
	 * Get the tiers.
	 *
	 * @return the tiers (unmodifiable)
	 */
	@Override
	public final List<UsageTier> getTiers() {
		return tiers;
	}

	/**
	 * Get the tiers date.
	 *
	 * <p>
	 * The {@code date} might be interpreted as an effective date for this
	 * collection of tiers.
	 * </p>
	 *
	 * @return the date, or {@literal null}
	 */
	@Override
	public final @Nullable LocalDate getDate() {
		return date;
	}

}
