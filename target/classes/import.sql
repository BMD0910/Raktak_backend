
-- Paramètres globaux du site (équivalent de l'ancien backend PHP)
INSERT INTO site_settings (
	id,
	site_name,
	support_email,
	support_phone,
	maintenance_mode,
	maintenance_message,
	audit_retention_days,
	allow_new_registrations,
	allow_new_vendor_applications,
	created_at,
	updated_at
) VALUES (
	1,
	'Raktakk',
	'bmd09102000@gmail.com',
	'+221 77 000 00 00',
	FALSE,
	'',
	90,
	TRUE,
	TRUE,
	NOW(),
	NOW()
);

