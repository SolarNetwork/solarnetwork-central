/* Found that the tsvector_update_trigger() function in 9.6 is throwing an error when passing in a character(2)
 * column, such as sn_loc.country. Changing to character varying(2) works around the issue.
 */
ALTER TABLE solarnet.sn_loc ALTER COLUMN country TYPE character varying(2);
