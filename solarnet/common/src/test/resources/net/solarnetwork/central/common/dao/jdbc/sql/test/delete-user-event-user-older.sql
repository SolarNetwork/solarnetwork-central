DELETE FROM solaruser.user_event_log
WHERE user_id = ?
AND ts < ?