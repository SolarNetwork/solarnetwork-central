CREATE OR REPLACE FUNCTION solardatum.trigger_agg_stale_datum()
  RETURNS trigger AS
$BODY$
DECLARE
	neighbor solardatum.da_datum;
BEGIN
	IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
		-- curr hour
		BEGIN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', NEW.ts), NEW.node_id, NEW.source_id, 'h');
		EXCEPTION WHEN unique_violation THEN
			-- Nothing to do, just continue
		END;

		-- prev hour; if the previous record for this source falls on the previous hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts < NEW.ts
			AND d.ts > NEW.ts - interval '1 hour'
			AND d.node_id = NEW.node_id
			AND d.source_id = NEW.source_id
		ORDER BY d.ts DESC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts < date_trunc('hour', NEW.ts) THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
		END IF;

		-- next hour; if the next record for this source falls on the next hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts > NEW.ts
			AND d.ts < NEW.ts + interval '1 hour'
			AND d.node_id = NEW.node_id
			AND d.source_id = NEW.source_id
		ORDER BY d.ts ASC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts > date_trunc('hour', NEW.ts) THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
		END IF;
	END IF;

	IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND (OLD.source_id <> NEW.source_id OR OLD.node_id <> NEW.node_id)) THEN
		-- curr hour
		BEGIN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', OLD.ts), OLD.node_id, OLD.source_id, 'h');
		EXCEPTION WHEN unique_violation THEN
			-- Nothing to do, just continue
		END;

		-- prev hour; if the previous record for this source falls on the previous hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts < OLD.ts
			AND d.ts > OLD.ts - interval '1 hour'
			AND d.node_id = OLD.node_id
			AND d.source_id = OLD.source_id
		ORDER BY d.ts DESC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts < date_trunc('hour', OLD.ts) THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
		END IF;

		-- next hour; if the next record for this source falls on the next hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts > OLD.ts
			AND d.ts < OLD.ts + interval '1 hour'
			AND d.node_id = OLD.node_id
			AND d.source_id = OLD.source_id
		ORDER BY d.ts ASC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts > date_trunc('hour', OLD.ts) THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
		END IF;
	END IF;

	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			RETURN NEW;
		ELSE
			RETURN OLD;
	END CASE;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;
