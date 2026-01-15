/* ==================================================================
 * SolcastIrradianceType.java - 30/10/2024 9:47:04 am
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

package net.solarnetwork.central.c2c.biz.impl;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Solcast irradiance types.
 *
 * @author matt
 * @version 1.0
 */
public enum SolcastIrradianceType {

	/** Albedo. */
	Albedo("albedo", "Albedo daily", "%", """
			The average daytime surface reflectivity of visible light, \
			expressed as a fractional value between 0 and 1. 0 represents \
			complete absorption. 1 represents complete reflection. This is \
			an interpolated daily average value, therefore does not capture \
			any diurnal angular dependence of reflectivity."""),

	/** Surface pressure. */
	Atm("surface_pressure", "Surface pressure", "%", """
			The air pressure at surface level."""),

	/** Solar azimuth angle. */
	Azimuth("azimuth", "Solar azimuth angle", "\u00B0", """
			The angle between the horizontal direction of the sun, and due \
			north, with negative angles eastwards and positive values \
			westward . Varies from -180 to 180. A value of -90 means the sun \
			is in the east, 0 means north, and 90 means west."""),

	/** Thunderstorm potential (CAPE). */
	Cape("cape", "Thunderstorm potential (CAPE)", "J/kg", """
			Convective Available Potential Energy (CAPE) is the energy available \
			for a thunderstorm. CAPE provides an estimate of the energy that a \
			thunderstorm updraught will release as it rises and the water vapour \
			it contains condenses to liquid water, releasing latent heat. It is a \
			measure of deep atmospheric instability which is one of the necessary \
			conditions for a thunderstorm. While large values of CAPE (many 100s \
			to 1000s of J/kg) indicate a favourable environment for a thunderstorm \
			to occur, to get a thunderstorm it is also necessary to have a \
			triggering mechanism to initiate it, and other suitable environmental \
			wind and moisture conditions to support its growth. Large values of \
			CAPE, when combined with precipitation_rate output being > 0 can be \
			used as a simple proxy for a thunderstorm event."""),

	/** Clearsky diffuse horizontal irradiance. */
	ClearDHI("clearsky_dhi", "Clearsky diffuse horizontal irradiance", "W/m2", """
			The diffuse irradiance received on a horizontal surface (if there are \
			no clouds). Also referred to as Diffuse Sky Radiation. The diffuse \
			component is irradiance that is scattered by the atmosphere, in the \
			clear sky scenario, (i.e. no water or ice clouds in the sky)."""),

	/** Clearsky direct normal irradiance. */
	ClearDNI("clearsky_dni", "Clearsky direct normal irradiance", "W/m2", """
			The diffuse irradiance received on a horizontal surface (if there are \
			no clouds). Also referred to as Diffuse Sky Radiation. The diffuse \
			component is irradiance that is scattered by the atmosphere, in the \
			clear sky scenario, (i.e. no water or ice clouds in the sky)."""),

	/** Clearsky global horizontal irradiance. */
	ClearGHI("clearsky_ghi", "Clearsky global horizontal irradiance", "W/m2", """
			Total irradiance on a horizontal surface (if there are no clouds). \
			The sum of direct and diffuse irradiance components received on a \
			horizontal surface, in the clear sky scenario, (i.e. no water or \
			ice clouds in the sky)."""),

	/** Clearsky global tilted irradiance. */
	ClearGTI("clearsky_gti", "Clearsky global tilted irradiance", "W/m2", """
			Total irradiance received on a surface, if there are no clouds, \
			with defined tilt and azimuth (sum of direct, diffuse and reflected \
			components), fixed or tracking. You must also specify the array type. \
			If you specify an array type of "fixed", you can optionally specify a \
			tilt and azimuth, or else default values are assumed (equatorward-facing \
			azimuth, latitude-dependent tilt). If you specify an array type of \
			"horizontal_single_axis", you can specify the tracker axis angle as \
			azimuth, or else a default value of zero is assumed (i.e. tracker axis \
			runs north-south). Tilt is ignored if array type is "horizontal_single_axis"."""),

	/** Cloud opacity. */
	CloudOpacity("cloud_opacity", "Cloud opacity", "%", """
			The attenuation of incoming sunlight due to cloud. Varies from 0% \
			(no cloud) to 100% (full attenuation of incoming sunlight)."""),

	/** Cloud opacity (10th percentile). */
	CloudOpacity10("cloud_opacity10", "Cloud opacity (10th percentile)", "%", """
			The attenuation of incoming sunlight due to cloud. Varies from 0% \
			(no cloud) to 100% (full attenuation of incoming sunlight)."""),

	/** Cloud opacity (90th percentile). */
	CloudOpacity90("cloud_opacity90", "Cloud opacity (90th percentile)", "%", """
			The attenuation of incoming sunlight due to cloud. Varies from 0% \
			(no cloud) to 100% (full attenuation of incoming sunlight)."""),

	/** Dew point. */
	Dew("dewpoint_temp", "Dew point", "\u2103", """
			The dew point at 2 meters above surface level."""),

	DHI("dhi", "Diffuse horizontal irradiance", "W/m2", """
			The diffuse irradiance received on a horizontal surface. Also \
			referred to as Diffuse Sky Radiation. The diffuse component is \
			irradiance that is scattered by the atmosphere."""),

	/** Diffuse horizontal irradiance (10th percentile). */
	DHI10("dhi10", "Diffuse horizontal irradiance (10th percentile)", "W/m2", """
			The diffuse irradiance received on a horizontal surface. Also \
			referred to as Diffuse Sky Radiation. The diffuse component is \
			irradiance that is scattered by the atmosphere."""),

	/** Diffuse horizontal irradiance (90th percentile). */
	DHI90("dhi90", "Diffuse horizontal irradiance (90th percentile)", "W/m2", """
			The diffuse irradiance received on a horizontal surface. Also \
			referred to as Diffuse Sky Radiation. The diffuse component is \
			irradiance that is scattered by the atmosphere."""),

	/** Direct normal irradiance. */
	DNI("dni", "Direct normal irradiance", "W/m2", """
			Irradiance received from the direction of the sun. Also referred \
			to as beam radiation."""),

	/** Direct normal irradiance (10th percentile). */
	DNI10("dni10", "Direct normal irradiance (10th percentile)", "W/m2", """
			Irradiance received from the direction of the sun. Also referred \
			to as beam radiation."""),

	/** Direct normal irradiance (90th percentile). */
	DNI90("dni90", "Direct normal irradiance (90th percentile)", "W/m2", """
			Irradiance received from the direction of the sun. Also referred \
			to as beam radiation."""),

	/** Global horizontal irradiance. */
	GHI("ghi", "Global horizontal irradiance", "W/m2", """
			Total irradiance on a horizontal surface. The sum of direct and \
			diffuse irradiance components received on a horizontal surface."""),

	/** Global horizontal irradiance (10th percentile). */
	GHI10("ghi10", "Global horizontal irradiance (10th percentile)", "W/m2", """
			Total irradiance on a horizontal surface. The sum of direct and \
			diffuse irradiance components received on a horizontal surface."""),

	/** Global horizontal irradiance (90th percentile). */
	GHI90("ghi90", "Global horizontal irradiance (90th percentile)", "W/m2", """
			Total irradiance on a horizontal surface. The sum of direct and \
			diffuse irradiance components received on a horizontal surface."""),

	/** Global tilted irradiance. */
	GTI("gti", "Global tilted irradiance", "W/m2", """
			Total irradiance received on a surface with defined tilt and azimuth \
			(sum of direct, diffuse and reflected components), fixed or tracking. \
			You must also specify the array type. \
			If you specify an array type of "fixed", you can optionally specify a \
			tilt and azimuth, or else default values are assumed (equatorward-facing \
			azimuth, latitude-dependent tilt). If you specify an array type of \
			"horizontal_single_axis", you can specify the tracker axis angle as \
			azimuth, or else a default value of zero is assumed (i.e. tracker axis \
			runs north-south). Tilt is ignored if array type is "horizontal_single_axis"."""),

	/** Global tilted irradiance (10th percentile). */
	GTI10("gti10", "Global tilted irradiance (10th percentile)", "W/m2", """
			Total irradiance received on a surface with defined tilt and azimuth \
			(sum of direct, diffuse and reflected components), fixed or tracking. \
			You must also specify the array type."""),

	/** Global tilted irradiance (90th percentile). */
	GTI90("gti90", "Global tilted irradiance (90th percentile)", "W/m2", """
			Total irradiance received on a surface with defined tilt and azimuth \
			(sum of direct, diffuse and reflected components), fixed or tracking. \
			You must also specify the array type."""),

	/** Relative humidity. */
	Humidity("relative_humidity", "Relative humidity", "%", """
			The relative humidity at 2 meters above ground level. Relative humidity is \
			the amount of water vapour as a percentage of the amount needed for \
			saturation at the same temperature. A value of 50% means the air is \
			50% saturated."""),

	PercepWater("precipitable_water", "Precipitable water", "kg/m2", """
			Precipitable water of the entire atmospheric column."""),

	/** Precipitation rate. */
	PrecipRate("precipitation_rate", "Precipitation rate", "mm/h", """
			Precipitation rate. An estimate of the average precipitation rate during \
			the selected period, expressed in millimetres per hour, not an \
			accumulated value."""),

	/** Snow depth. */
	SnowDepth("", "Snow depth", "cm", """
			The physical snow pack on the ground."""),

	/** Snowfall rate. */
	SnowfallRate("snowfall_rate", "Snowfall rate", "cm/h", """
			The depth of snow falling per unit time. The snowfall rate is based on the \
			water-equivalent amount of frozen precipitation falling per hour, converted \
			to a solid snow depth using an estimate of the density of newly fallen snow. \
			The snowfall rate will generally not match the change in the snow_depth output \
			parameter over an hour. This is because snow_depth is also affected by other \
			processes, such as melting, evaporation and changes in density of the total \
			snow pack."""),

	/** Snow soiling loss - ground. */
	SnowSoilGround("snow_soiling_ground", "Snow soiling loss - ground", "%", """
			Loss in ground mounted PV module (DC) production loss due to snow soiling. 0% \
			means no snow soiling losses. 100% means snow is fully covering all modules. \
			Soiling values are estimated using Solcast snowfall, irradiance and temperature \
			data, following the method of Ryberg and Freeman (NREL, 2017), with Solcast \
			proprietary extensions."""),

	/** Snow soiling loss - rooftop. */
	SnowSoilRoof("snow_soiling_rooftop", "Snow soiling loss - rooftop", "%", """
			Loss in rooftop PV module (DC) production loss due to snow soiling. 0% \
			means no snow soiling losses. 100% means snow is fully covering all modules. \
			Soiling values are estimated using Solcast snowfall, irradiance and temperature \
			data, following the method of Ryberg and Freeman (NREL, 2017), with Solcast \
			proprietary extensions."""),

	/** Snow water equivalent. */
	SnowWaterEquiv("snow_water_equivalent", "Snow water equivalent", "cm", """
			The snow depth liquid equivalent."""),

	/** Air temperature. */
	Temp("air_temp", "Air temperature", "\u2103", """
			The air temperature at 2 meters above surface level."""),

	/** Wind direction - 10m. */
	WindDir10("wind_direction_10m", "Wind direction - 10m", "\u00B0", """
			Wind direction at 10m above ground level. Zero means true north. Varies from \
			0 to 360. A value of 270 means the wind is coming from the west."""),

	/** Wind direction - 100m. */
	WindDir100("wind_direction_100m", "Wind direction - 100m", "\u00B0", """
			Wind direction at 100m above ground level. Zero means true north. Varies from \
			0 to 360. A value of 270 means the wind is coming from the west."""),

	/** Wind gust. */
	WindGust("wind_gust", "Wind gust", "m/s", """
			The maximum wind gust speed at 10m above ground level. It is based on the \
			maximum 10m wind speed when averaged over 3 seconds. The wind gust can give a \
			better indication of the damage potential of wind than the wind speed output \
			parameter does."""),

	/** Wind speed - 10m. */
	WindSpeed10("wind_speed_10m", "Wind speed - 10m", "m/s", """
			Wind speed at 10m above ground level."""),

	/** Wind speed - 100m. */
	WindSpeed100("wind_speed_100m", "Wind speed - 100m", "m/s", """
			Wind speed at 100m above ground level."""),

	/** Solar zenith angle. */
	Zenith("zenith", "Solar zenith angle", "\u00B0", """
			The angle between the direction of the sun, and the zenith (directly overhead). \
			The zenith angle is 90 degrees at sunrise and sunset, and 0 degrees when the \
			sun is directly overhead."""),

	;

	private final String key;
	private final String name;
	private final String unit;
	private final String description;

	SolcastIrradianceType(String key, String name, String unit, String description) {
		this.key = key;
		this.name = name;
		this.unit = unit;
		this.description = description;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Get the unit of measure.
	 *
	 * @return the unit
	 */
	public final String getUnit() {
		return unit;
	}

	/**
	 * Get the description.
	 *
	 * @return the description
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name (case insensitive) or key value
	 *        (case-sensitve)
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static SolcastIrradianceType fromValue(String value) {
		if ( value != null ) {
			for ( SolcastIrradianceType e : SolcastIrradianceType.values() ) {
				if ( value.equals(e.key) || value.equalsIgnoreCase(e.name()) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Unknown SolcastIrradianceType value [" + value + "]");
	}

}
