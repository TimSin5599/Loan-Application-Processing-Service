package ru.creditbank.credit.operations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "scoringExecutor")
    @Profile("!test")
    public Executor scoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("credit-scoring-");
        executor.initialize();
        return executor;
    }

    // В тестах скоринг выполняется синхронно в вызывающем потоке — так тесты
    // могут сразу после запроса проверять итоговое состояние заявки без ожиданий.
    @Bean(name = "scoringExecutor")
    @Profile("test")
    public Executor scoringExecutorForTests() {
        return new SyncTaskExecutor();
    }
}
