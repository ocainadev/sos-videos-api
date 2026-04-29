package com.sosvideos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configura um executor com pool fixo para requisições MVC assíncronas
 * (StreamingResponseBody dos downloads de vídeo).
 *
 * <p>O padrão do Spring — {@code SimpleAsyncTaskExecutor} — cria uma thread
 * nova para CADA requisição, sem limite. Em pico de downloads isso consome
 * toda a memória da VPS rapidamente (cada thread segura conexão Drive + NIO
 * buffers + stack ~512 KB).</p>
 *
 * <p>Com pool de 10 threads e fila de 20, o servidor aceita até 30 downloads
 * simultâneos antes de retornar 503 — comportamento controlado em vez de OOM.</p>
 */
@Configuration
public class AsyncConfig implements WebMvcConfigurer {

    /**
     * Máximo de downloads rodando em paralelo.
     * Cada thread segura ~1–2 MB em buffers de rede durante o streaming.
     * 10 × 2 MB = 20 MB de overhead controlado.
     */
    private static final int MAX_CONCURRENT_DOWNLOADS = 10;

    /** Fila de requisições aguardando thread livre. */
    private static final int QUEUE_CAPACITY = 20;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(MAX_CONCURRENT_DOWNLOADS);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("download-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // termina downloads em andamento no shutdown
        executor.initialize();
        configurer.setTaskExecutor(executor);
    }
}
