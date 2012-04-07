INSERT INTO solardras.dras_role VALUES ('SYSTEM');
INSERT INTO solardras.dras_role VALUES ('SUPPLIER');
INSERT INTO solardras.dras_role VALUES ('ANALYST');
INSERT INTO solardras.dras_role VALUES ('OPERATOR');
INSERT INTO solardras.dras_role VALUES ('PROGRAM_ADMIN');
INSERT INTO solardras.dras_role VALUES ('USER_ADMIN');

INSERT INTO solardras.dras_user (id, username, disp_name)
  VALUES (0, 'system', 'System');

INSERT INTO solardras.dras_user_role VALUES (0, 'SYSTEM');

INSERT INTO solardras.loc (id, loc_name) values (0, 'Unknown');
