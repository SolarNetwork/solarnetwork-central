/* Cleanup functions to be run on a future date after SolarNet code updates have been
 * deployed so these functions are no longer in use.
 */
 
DROP FUNCTION IF EXISTS solaragg.find_most_recent_hourly(nodes bigint, sources text[]);

DROP FUNCTION IF EXISTS solaragg.find_most_recent_daily(nodes bigint, sources text[]);

DROP FUNCTION IF EXISTS solaragg.find_most_recent_monthly(nodes bigint, sources text[]);
