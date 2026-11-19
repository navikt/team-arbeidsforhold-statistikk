BEGIN;

INSERT INTO repo (full_name, clone_url, last_seen) VALUES
    ('eksempel/eksempel-api', 'https://github.com/eksempel/eksempel-api.git', TIMESTAMP '2026-01-15 12:00:00'),
    ('eksempel/eksempel-web', 'https://github.com/eksempel/eksempel-web.git', TIMESTAMP '2026-01-15 12:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO workload (name, workload_type, image) VALUES
    ('eksempel-api', 'Application', 'registry.example.test/eksempel-api:local'),
    ('eksempel-web', 'Application', 'registry.example.test/eksempel-web:local')
ON CONFLICT DO NOTHING;

INSERT INTO workload_repo (workload_name, repo_full_name, last_seen) VALUES
    ('eksempel-api', 'eksempel/eksempel-api', TIMESTAMP '2026-01-15 12:00:00'),
    ('eksempel-web', 'eksempel/eksempel-web', TIMESTAMP '2026-01-15 12:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO deployment (workload_name, workload_type, environment, suffix, last_seen,
                        last_deployer, last_commit, last_deploy_time, manifest) VALUES
    ('eksempel-api', 'Application', 'prod-gcp', '', TIMESTAMP '2026-01-15 12:00:00',
     'eksempel-bruker', 'eksempel/eksempel-api@0000000000000000000000000000000000000001',
     TIMESTAMP '2026-01-15 11:00:00', 'kind: Application
spec:
  ingresses:
    - https://api.example.test
'),
    ('eksempel-api', 'Application', 'dev-gcp', 'q1', TIMESTAMP '2026-01-15 12:00:00',
     'eksempel-bruker', 'eksempel/eksempel-api@0000000000000000000000000000000000000002',
     TIMESTAMP '2026-01-15 11:30:00', 'kind: Application
spec:
  ingresses:
    - https://api-q1.example.test
'),
    ('eksempel-web', 'Application', 'prod-gcp', '', TIMESTAMP '2026-01-15 12:00:00',
     'eksempel-bruker', 'eksempel/eksempel-web@0000000000000000000000000000000000000003',
     TIMESTAMP '2026-01-15 11:45:00', 'kind: Application
spec:
  ingresses:
    - https://web.example.test
')
ON CONFLICT DO NOTHING;

INSERT INTO vulnerability (id) VALUES
    ('CVE-2099-90001'),
    ('CVE-2099-90002')
ON CONFLICT DO NOTHING;

INSERT INTO cve_nvd (cve_id, published, last_modified, cvss_version, base_score, base_severity) VALUES
    ('CVE-2099-90001', TIMESTAMP '2026-01-14 10:00:00', TIMESTAMP '2026-01-15 10:00:00', '4.0', 9.9, 'CRITICAL'),
    ('CVE-2099-90002', TIMESTAMP '2026-01-13 10:00:00', TIMESTAMP '2026-01-14 10:00:00', '4.0', 9.3, 'CRITICAL')
ON CONFLICT DO NOTHING;

INSERT INTO deployment_vulnerability (workload_name, environment, suffix, vulnerability_id) VALUES
    ('eksempel-api', 'prod-gcp', '', 'CVE-2099-90001'),
    ('eksempel-api', 'dev-gcp', 'q1', 'CVE-2099-90001'),
    ('eksempel-web', 'prod-gcp', '', 'CVE-2099-90002')
ON CONFLICT DO NOTHING;

COMMIT;
