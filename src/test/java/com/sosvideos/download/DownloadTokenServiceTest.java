package com.sosvideos.download;

import com.sosvideos.download.dto.DownloadTokenEntry;
import com.sosvideos.download.enums.DownloadType;
import com.sosvideos.download.service.DownloadTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para DownloadTokenService.
 * Sem contexto Spring — execução rápida, sem I/O externo.
 */
@DisplayName("DownloadTokenService — geração, consumo e limpeza de tokens")
class DownloadTokenServiceTest {

    private DownloadTokenService service;

    @BeforeEach
    void setUp() {
        service = new DownloadTokenService();
    }

    // ── Geração ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken retorna string não vazia com 32 caracteres")
    void generateToken_retornaTokenNaoVazio() {
        String token = service.generateToken(1L, 10L, DownloadType.SINGLE);

        assertThat(token).isNotBlank();
        assertThat(token).hasSize(32); // UUID sem hífens
    }

    @Test
    @DisplayName("generateToken para vídeos diferentes produz tokens únicos")
    void generateToken_sempreRetornaTokenUnico() {
        String t1 = service.generateToken(1L, 10L, DownloadType.SINGLE);
        String t2 = service.generateToken(1L, 10L, DownloadType.SINGLE);

        assertThat(t1).isNotEqualTo(t2);
    }

    // ── Consumo válido ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateAndConsume com token válido retorna a entrada correta")
    void validateAndConsume_tokenValido_retornaEntrada() {
        String token = service.generateToken(42L, 7L, DownloadType.BATCH);

        Optional<DownloadTokenEntry> result = service.validateAndConsume(token);

        assertThat(result).isPresent();
        assertThat(result.get().videoId()).isEqualTo(42L);
        assertThat(result.get().userId()).isEqualTo(7L);
        assertThat(result.get().type()).isEqualTo(DownloadType.BATCH);
    }

    // ── Uso único ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token é de uso único — segunda chamada retorna vazio")
    void validateAndConsume_usoUnico_segundaChamadaRetornaVazio() {
        String token = service.generateToken(1L, 10L, DownloadType.SINGLE);

        service.validateAndConsume(token); // consome
        Optional<DownloadTokenEntry> second = service.validateAndConsume(token);

        assertThat(second).isEmpty();
    }

    // ── Token inválido ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateAndConsume com token inexistente retorna vazio")
    void validateAndConsume_tokenInexistente_retornaVazio() {
        Optional<DownloadTokenEntry> result = service.validateAndConsume("token-que-nao-existe");

        assertThat(result).isEmpty();
    }

    // ── Token expirado ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateAndConsume com token expirado retorna vazio")
    void validateAndConsume_tokenExpirado_retornaVazio() throws Exception {
        // Injeta uma entrada já expirada diretamente no mapa interno via reflexão
        ConcurrentHashMap<String, DownloadTokenEntry> tokens = getTokensMap(service);
        String expiredToken = "expiredtoken000000000000000000000";
        tokens.put(expiredToken, new DownloadTokenEntry(
                99L, 99L, DownloadType.SINGLE, Instant.now().minusSeconds(120)));

        Optional<DownloadTokenEntry> result = service.validateAndConsume(expiredToken);

        assertThat(result).isEmpty();
    }

    // ── Limpeza agendada ──────────────────────────────────────────────────────

    @Test
    @DisplayName("purgeExpiredTokens remove apenas os tokens expirados")
    void purgeExpiredTokens_removeApenasExpirados() throws Exception {
        String validToken = service.generateToken(1L, 1L, DownloadType.SINGLE);

        // Insere token já expirado via reflexão
        ConcurrentHashMap<String, DownloadTokenEntry> tokens = getTokensMap(service);
        tokens.put("expired-one", new DownloadTokenEntry(
                2L, 2L, DownloadType.SINGLE, Instant.now().minusSeconds(1)));

        service.purgeExpiredTokens();

        // Token válido deve permanecer
        assertThat(tokens).containsKey(validToken);
        // Token expirado deve ter sido removido
        assertThat(tokens).doesNotContainKey("expired-one");
    }

    @Test
    @DisplayName("purgeExpiredTokens com mapa vazio não lança exceção")
    void purgeExpiredTokens_mapaVazio_naoLancaExcecao() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> service.purgeExpiredTokens());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<String, DownloadTokenEntry> getTokensMap(
            DownloadTokenService svc) throws Exception {
        Field field = DownloadTokenService.class.getDeclaredField("tokens");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, DownloadTokenEntry>) field.get(svc);
    }
}
