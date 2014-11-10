INSERT INTO solaruser.user_user (id, disp_name, email, password) 
VALUES (-1,'Test User', 'test@localhost', 'password');

INSERT INTO solaruser.user_node_conf (user_id, node_id, conf_key, sec_phrase, country, time_zone) 
VALUES (-1, -1, 'test conf key', 'test phrase', 'NZ', 'Pacific/Auckland');
