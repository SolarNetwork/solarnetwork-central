/**
 * Node/source ID aliases for datum streams
 */
CREATE TABLE solardatm.da_datm_alias (
	stream_id			UUID NOT NULL DEFAULT uuid_generate_v4(),
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id				BIGINT NOT NULL,
	source_id			CHARACTER VARYING(64) NOT NULL COLLATE solarcommon.naturalsort,
	alias_node_id		BIGINT NOT NULL,
	alias_source_id		CHARACTER VARYING(64) NOT NULL COLLATE solarcommon.naturalsort,
	CONSTRAINT da_datm_alias_pk PRIMARY KEY (stream_id),
	CONSTRAINT da_datm_alias_unq UNIQUE (alias_node_id, alias_source_id),
	CONSTRAINT da_datm_alias_alias_chk CHECK (NOT(node_id = alias_node_id AND source_id = alias_source_id)),
	CONSTRAINT da_datm_alias_stream_fk FOREIGN KEY (node_id, source_id)
		REFERENCES solardatm.da_datm_meta (node_id, source_id) MATCH SIMPLE
		ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX da_datm_alias_node_source_idx ON solardatm.da_datm_alias (node_id, source_id);

/**
 * View combining da_datm_meta and da_datm_alias, to ease querying.
 */
CREATE OR REPLACE VIEW solardatm.da_datm_meta_aliased AS
SELECT s.stream_id
	, s.node_id
	, s.source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, FALSE AS is_alias
	, s.created
	, s.updated
FROM solardatm.da_datm_meta s
UNION ALL
SELECT a.stream_id
	, a.alias_node_id AS node_id
	, a.alias_source_id AS source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, TRUE AS is_alias
	, a.created
	, a.modified AS updated
FROM solardatm.da_datm_alias a
INNER JOIN solardatm.da_datm_meta s ON s.node_id = a.node_id AND s.source_id = a.source_id
;

/**
 * View combining user ID with da_datm_meta and da_datm_alias, to ease querying.
 */
CREATE OR REPLACE VIEW solaruser.da_datm_meta_aliased AS
SELECT un.user_id
	, s.stream_id
	, s.node_id
	, s.source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, FALSE AS is_alias
	, s.created
	, s.updated
FROM solardatm.da_datm_meta s
INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
UNION ALL
SELECT un.user_id
	, a.stream_id
	, a.alias_node_id AS node_id
	, a.alias_source_id AS source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, TRUE AS is_alias
	, a.created
	, a.modified AS updated
FROM solardatm.da_datm_alias a
INNER JOIN solardatm.da_datm_meta s ON s.node_id = a.node_id AND s.source_id = a.source_id
INNER JOIN solaruser.user_node un ON un.node_id = a.node_id
;


/**
 * Disallow saving node/source combo that exists as an alias.
 */
CREATE OR REPLACE FUNCTION solardatm.validate_node_source()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
DECLARE
	found BOOLEAN;
BEGIN
	SELECT TRUE
	FROM solardatm.da_datm_alias
	WHERE alias_node_id = NEW.node_id AND alias_source_id = NEW.source_id
	INTO found;

	IF FOUND THEN
		RAISE EXCEPTION 'Node % source "%" already exists as a datum stream alias.', NEW.node_id, NEW.source_id
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solardatm',
			TABLE = 'da_datm_meta',
			HINT = 'Use a different node/source combination or change/delete the datum stream alias.';
	END IF;
	RETURN NULL;
END;
$$;

CREATE TRIGGER da_datm_meta_node_source_checker
    AFTER INSERT OR UPDATE
    ON solardatm.da_datm_meta
    FOR EACH ROW
    EXECUTE PROCEDURE solardatm.validate_node_source();


-- ==========================================================================================
-- Updates
-- ==========================================================================================

/**
 * Increment the `solardatm.aud_datm_io` table `datum_q_count` for a stream.
 *
 * @param node 				the node ID to update audit datum for
 * @param source 			the source ID to update audit datum for
 * @param ts				ts the query date
 * @param dcount			the datum count to insert, or add to an existing record
 */
CREATE OR REPLACE FUNCTION solardatm.audit_increment_datum_q_count(
		node	BIGINT,
		source	TEXT,
		ts 		TIMESTAMP WITH TIME ZONE,
		dcount 	INTEGER
	) RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	sid 	UUID;
	tz		TEXT;
BEGIN
	-- with constraints and da_datm_meta_node_source_checker trigger we assume
	-- the following query can only ever return 1 row (i.e. a node/source combo
	-- is globally unique across da_datm_meta and da_datm_alias)
	SELECT m.orig_stream_id, COALESCE(l.time_zone, 'UTC')
	FROM solardatm.da_datm_meta_aliased m
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE m.node_id = node AND m.source_id = source
	INTO sid, tz;

	IF FOUND THEN
		INSERT INTO solardatm.aud_datm_io(stream_id, ts_start, datum_q_count)
		VALUES (sid, date_trunc('hour', ts), dcount)
		ON CONFLICT (stream_id, ts_start) DO UPDATE
		SET datum_q_count = aud_datm_io.datum_q_count + EXCLUDED.datum_q_count;

		INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
		VALUES (sid, date_trunc('day', ts AT TIME ZONE tz) AT TIME ZONE tz, 'd')
		ON CONFLICT DO NOTHING;
	END IF;
END
$$;


/**
 * Get the metadata associated with a node datum or location datum stream.
 *
 * The `kind` output column will be `n` if the found metadata is for a node datum stream,
 * or `l` for a location datum stream, by looking in the `da_datum_meta_aliased` and `da_loc_datm_meta`
 * tables for a matching stream ID. If the stream ID is found in both tables, the node metadata
 * will be returned.
 *
 * The `time_zone` output column will be the time zone associated with the location of the stream,
 * either via the location of the node for a node stream or the location itself for a location
 * stream. If no time zone is available, it will be returned as `UTC`.
 *
 * @param sid the stream ID to find metadata for
 */
CREATE OR REPLACE FUNCTION solardatm.find_metadata_for_stream(
		sid 		UUID
	) RETURNS TABLE(
		stream_id 	UUID,
		obj_id		BIGINT,
		source_id	CHARACTER VARYING(64),
		created		TIMESTAMP WITH TIME ZONE,
		updated		TIMESTAMP WITH TIME ZONE,
		names_i		TEXT[],
		names_a		TEXT[],
		names_s		TEXT[],
		jdata		JSONB,
		kind		CHARACTER,
		time_zone	CHARACTER VARYING(64)
	) LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY
	SELECT m.stream_id, m.node_id AS obj_id, m.source_id, m.created, m.updated
		, m.names_i, m.names_a, m.names_s, m.jdata, 'n'::CHARACTER AS kind
		, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta_aliased m
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE m.stream_id = sid
	LIMIT 1;

	IF NOT FOUND THEN
		RETURN QUERY
		SELECT m.stream_id, m.loc_id AS obj_id, m.source_id, m.created, m.updated
			, m.names_i, m.names_a, m.names_s, m.jdata, 'l'::CHARACTER AS kind
			, COALESCE(l.time_zone, 'UTC') AS time_zone
		FROM solardatm.da_loc_datm_meta m
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = m.loc_id
		WHERE m.stream_id = sid
		LIMIT 1;
	END IF;
END;
$$;
