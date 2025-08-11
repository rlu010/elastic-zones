package no.fiskeridir.elasticzones.integration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Configuration
public class MyClientConfig extends ElasticsearchConfiguration {

    @Nonnull
    @Override
    public ClientConfiguration clientConfiguration() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "ApiKey NFZrRExKZ0JfUXp0YThFeGhKcVI6c1NveUJhWFFPOE0zcVdIcG9rZmdHZw==");

        return ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .usingSsl("2B:B0:B2:79:91:7C:62:38:9C:02:D9:4B:1E:8A:FD:FE:14:E6:AB:2F:1E:2F:B1:40:A5:1F:8C:F5:EF:5D:E4:7E")
                .withDefaultHeaders(httpHeaders)
                .withConnectTimeout(Duration.ofSeconds(40))
                .withSocketTimeout(Duration.ofSeconds(40))
                .withHeaders(() -> { // Denne kalles hver gang en forespørsel gjøres, kan være logref
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    return headers;
                })
                .build();
    }
}