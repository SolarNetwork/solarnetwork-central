INSERT INTO solaruser.user_event_log (user_id,ts,id,kind,message,jdata)
VALUES (?,?,?,?,?,?::jsonb)
ON CONFLICT (user_id,ts,id,kind) DO NOTHING