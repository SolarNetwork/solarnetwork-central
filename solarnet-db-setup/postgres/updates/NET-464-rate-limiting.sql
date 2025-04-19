/**
 * Table for rate limiting buckets.
 *
 * Note the hash-based partition definitions below.
 *
 * @column id    		a 64-bit user key
 * @column state 		bucket internal state data
 * @column expires_at	epoch expiration date
 * @explicit_lock		the advisory lock ID
 */
CREATE TABLE IF NOT EXISTS solarcommon.bucket (
	  id 			BIGINT
	, state			BYTEA
	, expires_at 	BIGINT
	, explicit_lock BIGINT
	, CONSTRAINT bucket_pk PRIMARY KEY (id)
) PARTITION BY hash(id);

CREATE TABLE solarcommon.bucket_1
PARTITION OF solarcommon.bucket
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);

CREATE TABLE solarcommon.bucket_2
PARTITION OF solarcommon.bucket
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);

CREATE TABLE solarcommon.bucket_3
PARTITION OF solarcommon.bucket
FOR VALUES WITH (MODULUS 3, REMAINDER 2);
