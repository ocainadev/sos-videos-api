-- V3: Admin seed
-- Senha padrão: Admin@123! (BCrypt gerado no startup via DatabaseSeeder.java)
-- Este script não insere o admin diretamente — o seeder Java é responsável por isso,
-- pois utiliza BCryptPasswordEncoder para gerar o hash em runtime.
-- Aqui apenas garantimos que o primeiro termo obrigatório exista para testes iniciais.

INSERT INTO terms (title, content, version, active, created_at, updated_at)
VALUES (
    'Termos de Uso — SOS Videos',
    'Ao utilizar a plataforma SOS Videos você concorda com os termos de uso e política de privacidade. '
    'O conteúdo disponibilizado é exclusivo para assinantes e não pode ser redistribuído. '
    'O acesso é pessoal e intransferível.',
    '1.0',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;
