/* ==================================================================
 * GeneralObjectDatumSerializer.java - 6/05/2026 5:45:20 pm
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

package net.solarnetwork.central.datum.v2.support;

import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static net.solarnetwork.util.DateUtils.LOCAL_DATE;
import static net.solarnetwork.util.DateUtils.LOCAL_TIME;
import java.io.Serial;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.domain.DatumComponents;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.ReportingDatum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for {@link GeneralObjectDatum} instances.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("rawtypes")
public class GeneralObjectDatumSerializer extends StdSerializer<GeneralObjectDatum> {

	@Serial
	private static final long serialVersionUID = -6441167743450747008L;

	/** A default instance. */
	public static final ValueSerializer<GeneralObjectDatum> INSTANCE = new GeneralObjectDatumSerializer();

	/**
	 * Constructor.
	 */
	public GeneralObjectDatumSerializer() {
		super(GeneralObjectDatum.class);
	}

	@Override
	public void serialize(@Nullable GeneralObjectDatum value, JsonGenerator gen,
			SerializationContext ctxt) throws JacksonException {
		if ( value == null ) {
			gen.writeNull();
			return;
		}

		final boolean components = (value instanceof DatumComponents);
		final ReportingDatum reporting = (value instanceof ReportingDatum r ? r : null);
		final DatumSamples samples = value.getSamples();

		@SuppressWarnings("unchecked")
		Map<String, ?> sampleData = (components ? null : value.getSampleData());

		gen.writeStartObject(3
				+ (components
						? (samples.getI() != null && !samples.getI().isEmpty() ? 1 : 0)
								+ (samples.getA() != null && !samples.getA().isEmpty() ? 1 : 0)
								+ (samples.getS() != null && !samples.getS().isEmpty() ? 1 : 0)
								+ (samples.getT() != null && !samples.getT().isEmpty() ? 1 : 0)
						: sampleData != null ? sampleData.size() : 0)
				+ (reporting != null && reporting.getLocalDate() != null ? 1 : 0)
				+ (reporting != null && reporting.getLocalTime() != null ? 1 : 0));
		if ( value.getTimestamp() != null ) {
			gen.writeStringProperty("created", ISO_DATE_TIME_ALT_UTC.format(value.getTimestamp()));
		}
		if ( reporting != null && reporting.getLocalDate() != null ) {
			gen.writeStringProperty("localDate", LOCAL_DATE.format(reporting.getLocalDate()));
		}
		if ( reporting != null && reporting.getLocalTime() != null ) {
			gen.writeStringProperty("localTime", LOCAL_TIME.format(reporting.getLocalTime()));
		}
		if ( value.getKind() == ObjectDatumKind.Node && value.getObjectId() != null ) {
			gen.writeNumberProperty("nodeId", value.getObjectId());
		} else if ( value.getKind() == ObjectDatumKind.Location && value.getObjectId() != null ) {
			gen.writeNumberProperty("locationId", value.getObjectId());
		}
		if ( value.getSourceId() != null ) {
			gen.writeStringProperty("sourceId", value.getSourceId());
		}

		if ( components ) {
			if ( samples.getI() != null && !samples.getI().isEmpty() ) {
				gen.writePOJOProperty("i", samples.getI());
			}
			if ( samples.getA() != null && !samples.getA().isEmpty() ) {
				gen.writePOJOProperty("a", samples.getA());
			}
			if ( samples.getS() != null && !samples.getS().isEmpty() ) {
				gen.writePOJOProperty("s", samples.getS());
			}
			if ( samples.getT() != null && !samples.getT().isEmpty() ) {
				gen.writePOJOProperty("t", samples.getT());
			}
		} else if ( sampleData != null && !sampleData.isEmpty() ) {
			for ( Entry<String, ?> e : sampleData.entrySet() ) {
				gen.writePOJOProperty(e.getKey(), e.getValue());
			}
		}

		gen.writeEndObject();
	}

}
