package org.eltex.rats.shcedule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Component
@Lazy(false)
public class DailySchedule {
    private final DateFormat redmineDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Logger logger;

    private final int redmineTimeout;
    private final String authToken;
    private final String mainVersionTask;

    @Autowired
    private DailySchedule(Logger logger,
                          @Value("${auth.credentials.base64}") String authToken,
                          @Value("${redmine.timeout:10000}") int redmineTimeout,
                          @Value("${redmine.main.task}") String mainVersionTask) {
        this.logger = logger;
        this.redmineTimeout = redmineTimeout;
        this.authToken = authToken;
        this.mainVersionTask = mainVersionTask;
    }

    @Scheduled(cron = "${daily.cron}")
    public void createDailyTimeSpent() throws Exception {
        logger.info("[DAILY SCHEDULE]: Start request for creating daily time spent...");

        SSLContext sslcontext = SSLContexts.custom()
                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                .build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
        Unirest.setHttpClient(httpclient);

        HttpResponse<String> response = Unirest.post("https://red.eltex.loc/time_entries.xml")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + authToken)
                .field("utf8", "✓")
                .field("back_url", "http://red.eltex.loc/issues/260767")
                .field("issue_id", mainVersionTask)
                .field("time_entry[issue_id]", mainVersionTask)
                .field("time_entry[spent_on]", redmineDateFormat.format(Calendar.getInstance().getTime()))
                .field("time_entry[hours]", "0:30")
                .field("time_entry[comments]", "Daily")
                .field("time_entry[activity_id]", "10")
                .field("continue", "Создать+и+продолжить")
                .asString();

        if (response.getStatus() != 201) {
            logger.error("[DAILY SCHEDULE]: WTF! Can't create daily meeting time spent! Response {}", response);
            return;
        }

        logger.info("[DAILY SCHEDULE]: Success create daily time spent!");
    }
}
