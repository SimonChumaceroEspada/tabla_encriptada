-- Habilitar la extensión pgcrypto
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE encrypted_logs_table (
    id TEXT PRIMARY KEY,
    main_id TEXT,
    encrypted_action TEXT,
    encrypted_action_date TEXT
);

CREATE OR REPLACE FUNCTION encrypt_log_data() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO encrypted_logs_table (id, main_id, encrypted_action, encrypted_action_date)
    VALUES (
        pgp_sym_encrypt(NEW.id::text, 'maquina'),
        pgp_sym_encrypt(NEW.main_id::text, 'maquina'),
        pgp_sym_encrypt(NEW.action::text, 'maquina'),
        pgp_sym_encrypt(NEW.action_date::text, 'maquina')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER encrypt_log_trigger
AFTER INSERT ON log_table
FOR EACH ROW
EXECUTE FUNCTION encrypt_log_data();