/* ==================================================================
 * DatumExpressionRoot.java - 12/11/2024 5:26:33 pm
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

package net.solarnetwork.central.datum.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.TariffSchedule;

/**
 * Extension of {@link DatumSamplesExpressionRoot} that adds support for
 * {@link DatumMetadataOperations}.
 *
 * @author matt
 * @version 1.0
 */
public class DatumExpressionRoot extends DatumSamplesExpressionRoot {

	// a general metadata object, for example user metadata
	private final DatumMetadataOperations metadata;

	// a function to lookup metadata based on an object ID
	private final Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider;

	// a function to parse a metadata tariff schedule associated with an object ID
	private final BiFunction<DatumMetadataOperations, ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleProvider;

	/**
	 * Constructor.
	 *
	 * @param datum
	 *        the datum currently being populated
	 * @param sample
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @param metadata
	 *        the metadata
	 * @param metadataProvider
	 *        function that resolves metadata based on an ID; the
	 *        {@code sourceId} component may be {@code null} to represent node
	 *        or location wide metadata
	 * @param tariffScheduleProvider
	 *        function that resolves a {@link TariffSchedule} from metadata
	 *        located at a path specified by
	 *        {@link ObjectDatumStreamMetadataId#getSourceId()}
	 */
	public DatumExpressionRoot(Datum datum, DatumSamplesOperations sample, Map<String, ?> parameters,
			DatumMetadataOperations metadata,
			Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider,
			BiFunction<DatumMetadataOperations, ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleProvider) {
		super(datum, sample, parameters);
		this.metadata = metadata;
		this.metadataProvider = metadataProvider;
		this.tariffScheduleProvider = tariffScheduleProvider;
	}

	/**
	 * Get the general metadata.
	 *
	 * @return the general metadata, or {@code null} if none available
	 */
	public DatumMetadataOperations metadata() {
		return metadata;
	}

	/**
	 * Extract a value from the general metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object metadata(String path) {
		DatumMetadataOperations metadata = metadata();
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/**
	 * Get node metadata associated with the configured {@code Datum}.
	 *
	 * @return the metadata for {@link #getDatum()}'s object ID, or {@code null}
	 *         if none available
	 */
	public DatumMetadataOperations nodeMetadata() {
		final Datum d = getDatum();
		return (d != null && d.getKind() == ObjectDatumKind.Node && metadataProvider != null
				? metadataProvider.apply(
						new ObjectDatumStreamMetadataId(d.getKind(), d.getObjectId(), null))
				: null);
	}

	/**
	 * Extract a value from the general metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object nodeMetadata(String path) {
		DatumMetadataOperations metadata = nodeMetadata();
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/**
	 * Resolve a tariff schedule from node metadata at a given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @return the schedule, or {@code node} if none available
	 */
	public TariffSchedule nodeTariffSchedule(String path) {
		final Datum d = getDatum();
		final DatumMetadataOperations meta = nodeMetadata();
		return (d != null && meta != null && tariffScheduleProvider != null
				? tariffScheduleProvider.apply(meta,
						new ObjectDatumStreamMetadataId(d.getKind(), d.getObjectId(), path))
				: null);
	}

	/**
	 * Resolve the first available tariff schedule rate for "now" from node
	 * metadata at a given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @return the first available rate for the current time, or {@code null} if
	 *         not available
	 */
	public BigDecimal resolveNodeTariffScheduleRate(String path) {
		return resolveNodeTariffScheduleRate(path, LocalDateTime.now(), null);
	}

	/**
	 * Resolve the first available tariff schedule rate from node metadata at a
	 * given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @param date
	 *        the date to evaluate the schedule at
	 * @return the first available rate, or {@code null} if not available
	 */
	public BigDecimal resolveNodeTariffScheduleRate(String path, LocalDateTime date) {
		return resolveNodeTariffScheduleRate(path, date, null);
	}

	/**
	 * Resolve a tariff schedule rate from node metadata at a given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @param date
	 *        the date to evaluate the schedule at
	 * @param rateName
	 *        the name of the rate to return, or {@code null} to return the
	 *        first available rate
	 * @return the rate, or {@code null} if not available
	 */
	public BigDecimal resolveNodeTariffScheduleRate(String path, LocalDateTime date, String rateName) {
		BigDecimal result = null;
		TariffSchedule schedule = nodeTariffSchedule(path);
		if ( schedule != null ) {
			Tariff t = schedule.resolveTariff(date, null);
			if ( t != null ) {
				Map<String, Tariff.Rate> rates = t.getRates();
				if ( !rates.isEmpty() ) {
					Tariff.Rate r = (rateName != null ? rates.get(rateName)
							: rates.values().iterator().next());
					if ( r != null ) {
						result = r.getAmount();
					}
				}

			}
		}
		return result;
	}

}
