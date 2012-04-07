/**************************************************************************************************
 * FUNCTION solarnet.calc_avg_watt_hours(integer, integer, double precision, 
 *                                       double precision, interval)
 * 
 * Calculate average watt hours between two watt values and a time interval. If
 * the Wh parameters are not null, the direct difference between these two values is 
 * returned instead of the average calculation.
 * 
 * @param integer			ending watt
 * @param integer			starting watt
 * @param double precision	ending Wh
 * @param double precision	starting Wh
 * @param interval			time interval
 */
CREATE OR REPLACE FUNCTION calc_avg_watt_hours(integer, integer, double precision, double precision, interval)
  RETURNS double precision AS
$BODY$
	SELECT CASE 
			WHEN 
				-- Wh readings available, so use difference in Wh if end value > start value, or
				-- end value > 10, and dt is small and the % change is less than 10%, in case of anomaly e.g. NET-19
				$3 IS NOT NULL AND $4 IS NOT NULL AND (
					$3 >= $4 OR 
					($3 > 10.0 AND $5 < interval '30 minutes' AND ($4 - $3) / $3 < 0.1))
				THEN $3 - $4
			WHEN 
				-- end Wh value less than start: assume day reset on inverter and just take end value
				$3 IS NOT NULL AND $4 IS NOT NULL
				THEN $3
			ELSE 
				-- Wh not available, so calculate Wh using (watts * dt)
				ABS(($1 + $2) / 2) * ((extract('epoch' from $5)) / 3600)
		END
$BODY$
  LANGUAGE sql IMMUTABLE;
