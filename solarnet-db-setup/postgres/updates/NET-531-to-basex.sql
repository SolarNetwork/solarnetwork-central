/* ============================================================================
 * NET-531: fix solarcommon.to_baseX(), which returned NULL for every number.
 *
 * It called CHR() with a BIGINT argument, which has no matching function, and
 * then cast the encoded string to a BIGINT; the error from either was caught
 * and turned into NULL. As a result the inv_num column of the
 * solarbill.bill_invoice_info view has always been NULL.
 * ============================================================================
 */

/**
 * Encode a non-negative integer as a string of a specific radix.
 *
 * Digits above 9 are encoded as upper case letters, so the radix can be at most 36.
 *
 * @param n the number to encode
 * @param radix the radix to use, from 2 to 36; defaults to `16`
 * @return the encoded number, or `NULL` if `n` is negative or `radix` is not supported
 */
CREATE OR REPLACE FUNCTION solarcommon.to_baseX(n BIGINT, radix INT DEFAULT 16) RETURNS TEXT LANGUAGE plpgsql IMMUTABLE AS
$$
DECLARE
	s TEXT := '';
	d INTEGER;
BEGIN
	IF n < 0 OR radix < 2 OR radix > 36 THEN
		RETURN NULL;
	ELSIF n = 0 THEN
		RETURN '0';
	END IF;
	LOOP
		EXIT WHEN n <= 0;
		d := (n % radix)::INTEGER;
		s := CHR(d + CASE WHEN d < 10 THEN 48 ELSE 55 END) || s;
		n := n / radix;
	END LOOP;
	RETURN s;
END
$$;
