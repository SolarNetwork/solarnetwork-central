CREATE OR REPLACE VIEW solaruser.user_alert_info AS
 SELECT al.id AS alert_id,
    al.user_id,
    al.node_id,
    al.alert_type,
    al.status AS alert_status,
    al.alert_opt,
    un.disp_name AS node_name,
    l.time_zone AS node_tz,
    u.disp_name AS user_name,
    u.email
   FROM solaruser.user_alert al
     JOIN solaruser.user_user u ON al.user_id = u.id
     LEFT JOIN solaruser.user_node un ON un.user_id = al.user_id AND un.node_id = al.node_id
     LEFT JOIN solarnet.sn_node n ON al.node_id = n.node_id
     LEFT JOIN solarnet.sn_loc l ON l.id = n.loc_id
  ORDER BY al.user_id, al.node_id;
