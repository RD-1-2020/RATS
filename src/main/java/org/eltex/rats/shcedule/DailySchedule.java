package org.eltex.rats.shcedule;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import javax.net.ssl.SSLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

@Component
@Lazy(false)
public class DailySchedule {
    private final DateFormat redmineDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Logger logger;
    private final WebClient webClient;

    private final String mainVersionTask;

    @Autowired
    private DailySchedule(Logger logger,
                          @Value("${auth.credentials.base64}") String authToken,
                          @Value("${redmine.timeout:10000}") int redmineTimeout,
                          @Value("${redmine.main.task}") String mainVersionTask) throws SSLException {
        this.logger = logger;
        this.mainVersionTask = mainVersionTask;

        // Настройка SSL контекста для игнорирования самоподписанных сертификатов
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        // Создание HTTP клиента с настроенным SSL
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.of(redmineTimeout, ChronoUnit.MILLIS))
                .secure(t -> t.sslContext(sslContext));

        // Создание WebClient с настроенным HTTP клиентом
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + authToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Scheduled(cron = "${daily.cron}")
    public void createDailyTimeSpent() {
        logger.info("[DAILY SCHEDULE]: Start request for creating daily time spent...");

        // Подготовка данных формы
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("utf8", "✓");
        formData.add("back_url", "http://red.eltex.loc/issues/260767");
        formData.add("issue_id", mainVersionTask);
        formData.add("time_entry[issue_id]", mainVersionTask);
        formData.add("time_entry[spent_on]", redmineDateFormat.format(Calendar.getInstance().getTime()));
        formData.add("time_entry[hours]", "0:30");
        formData.add("time_entry[comments]", "Daily");
        formData.add("time_entry[activity_id]", "10");
        formData.add("continue", "Создать+и+продолжить");

        try {
            // Выполнение POST запроса
            HttpStatusCode statusCode = webClient.post()
                    .uri("https://red.eltex.loc/time_entries.xml")
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .block()
                    .getStatusCode();

            if (statusCode != HttpStatus.CREATED) {
                logger.error("[DAILY SCHEDULE]: WTF! Can't create daily meeting time spent! Status code: {}", statusCode);
                return;
            }

            logger.info("[DAILY SCHEDULE]: Success create daily time spent!");
        } catch (Exception e) {
            logger.error("[DAILY SCHEDULE]: Error occurred while creating daily time spent", e);
        }
    }
}
