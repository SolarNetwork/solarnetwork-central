SELECT solarcommon.jsonb_prune_ant_paths(um.jdata, t.jpolicy -> 'userMetadataPaths') #> regexp_split_to_array(ltrim(?, '/'), '/')
FROM solaruser.user_meta um
INNER JOIN solaruser.user_auth_token_login t ON t.user_id = um.user_id
WHERE um.user_id = ?
	AND t.username = ANY(?)
	AND solarcommon.jsonb_prune_ant_paths(um.jdata, t.jpolicy -> 'userMetadataPaths') IS NOT NULL
