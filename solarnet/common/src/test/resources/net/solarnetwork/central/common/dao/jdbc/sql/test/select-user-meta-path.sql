SELECT um.jdata #> regexp_split_to_array(ltrim(?, '/'), '/')
FROM solaruser.user_meta um
WHERE um.user_id = ?
