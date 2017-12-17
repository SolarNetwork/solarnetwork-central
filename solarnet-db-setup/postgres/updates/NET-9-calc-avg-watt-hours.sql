CREATE OR REPLACE FUNCTION solarnet.calc_avg_watt_hours(real, real, real, real, 
	double precision, double precision, interval)
  RETURNS double precision AS
$BODY$
	SELECT CASE 
			WHEN 
				/* Wh readings available, so use difference in Wh */
				$5 IS NOT NULL AND $6 IS NOT NULL AND $5 > $6
				THEN $5 - $6
			WHEN 
				/* Assume day reset on inverter, so Wh for day reset */
				$5 IS NOT NULL AND $6 IS NOT NULL AND $5 < $6
				THEN $5
			ELSE 
				/* Wh not available, so calculate Wh using (volts * amps * dt) */
				ABS(($1 + $2) / 2) * (($3 + $4) / 2) * ((extract('epoch' from $7)) / 3600)
		END
$BODY$
  LANGUAGE sql IMMUTABLE;
