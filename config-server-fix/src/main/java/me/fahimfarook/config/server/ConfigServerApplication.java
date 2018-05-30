package me.fahimfarook.config.server;

import org.apache.http.client.HttpClient;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.environment.ConfigurableHttpConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
	
    @Configuration
    @ConditionalOnClass({ HttpClient.class, TransportConfigCallback.class })
    static class JGitHttpClientConfig {

    	@Primary
        @Bean
        public ConfigurableHttpConnectionFactory timeoutConnectionFactory() {
            return new TimeoutHttpConnectionFactory();
        }
    }

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}
}
