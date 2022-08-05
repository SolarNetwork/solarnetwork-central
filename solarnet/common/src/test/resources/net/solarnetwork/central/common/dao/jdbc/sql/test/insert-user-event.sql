INSERT INTO solaruser.user_event_log (user_id,event_id,tags,message,jdata)
VALUES (?,?,?,?,?::jsonb)
ON CONFLICT (user_id,event_id) DO NOTHING