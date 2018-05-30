package me.fahimfarook.config.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jgit.transport.http.HttpConnection;
import org.eclipse.jgit.transport.http.apache.HttpClientConnection;
import org.springframework.cloud.config.server.environment.HttpClientConfigurableHttpConnectionFactory;
import org.springframework.cloud.config.server.environment.JGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;
import org.springframework.cloud.config.server.support.HttpClientSupport;

/**
 * 
 * @author fahim
 *
 */
public class TimeoutHttpConnectionFactory extends HttpClientConfigurableHttpConnectionFactory {
	
    protected Map<String, HttpClientBuilder> httpClientsByUri = new HashMap<>();

    @Override
    public void addConfiguration(final MultipleJGitEnvironmentProperties environmentProperties) throws GeneralSecurityException {
        addHttpClient(environmentProperties);
        for (JGitEnvironmentProperties repo : environmentProperties.getRepos().values()) {
            addHttpClient(repo);
        }
    }
    
    @Override
    public HttpConnection create(URL url) throws IOException {
        return create(url, null);
    }

    @Override
    public HttpConnection create(URL url, Proxy proxy) throws IOException {
        return new HttpClientConnection(url.toString(), proxy, lookupHttpClientBuilder(url).build());
    }

    /*
     * This method caused IndexOutOfBoundException, hence re-factored.
     */
    private HttpClientBuilder lookupHttpClientBuilder(URL url) throws MalformedURLException {   	
        final Optional<HttpClientBuilder> cb = httpClientsByUri.entrySet()
                .stream()
                .filter(entry -> url.toString().startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
        return cb.get();
    }

    private void addHttpClient(JGitEnvironmentProperties properties) throws GeneralSecurityException {
        if (properties.getUri().startsWith("http")) {
            httpClientsByUri.put(properties.getUri(), builder(properties));
        }
    }

    /*
     * Enhanced HttpClientSupport.builder() to set timeouts.
     */
    protected static HttpClientBuilder builder(JGitEnvironmentProperties properties) throws GeneralSecurityException {
		final int timeout = properties.getTimeout() * 1000;
		final RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();
		return HttpClientSupport.builder(properties).setDefaultRequestConfig(config);
    }
}
