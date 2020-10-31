/**
 * Find the datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 * @see the `solardatm.find_time_range(uuid[])` function
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_least(sid uuid)
	RETURNS solardatm.da_datm LANGUAGE sql STABLE AS
$$
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
	ORDER BY ts
	LIMIT 1
$$;


/**
 * Find the datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 * @see the `solardatm.find_time_range(uuid[])` function
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_greatest(sid uuid)
	RETURNS solardatm.da_datm LANGUAGE sql STABLE AS
$$
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
	ORDER BY ts DESC
	LIMIT 1
$$;


/**
 * Find the smallest and largest datum for a given stream, i.e. the "first" and "last".
 *
 * This will return two rows for each stream that has any datm available, even if there is only
 * one datm in the stream.
 *
 * @param stream_ids the stream IDs to return results for
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_range(stream_ids uuid[])
	RETURNS SETOF solardatm.da_datm LANGUAGE sql ROWS 200 STABLE AS
$$
	WITH ids AS (
		SELECT unnest(stream_ids) AS stream_id
	)
	, d AS (
		(
		SELECT d.*
		FROM ids
		INNER JOIN solardatm.find_time_least(ids.stream_id) d ON d.stream_id = ids.stream_id
		)
		UNION ALL
		(
		SELECT d.*
		FROM ids
		INNER JOIN solardatm.find_time_greatest(ids.stream_id) d ON d.stream_id = ids.stream_id
		)
	)
	SELECT * FROM d
$$;
