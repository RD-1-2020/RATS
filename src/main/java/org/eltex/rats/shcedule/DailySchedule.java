package org.eltex.rats.shcedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Service
public class DailySchedule {
    private final DateFormat redmineDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final String MAIN_VERSION_TASK = "260767";
    private final Logger logger;

    private final int redmineTimeout;
    private final String authToken;

    @Autowired
    private DailySchedule(Logger logger,
                          @Value("${auth.credentials.base64}") String authToken,
                          @Value("${redmine.timeout:10000}") int redmineTimeout) {
        this.logger = logger;
        this.redmineTimeout = redmineTimeout;
        this.authToken = authToken;
    }

    @Scheduled(cron = "0 45 13 * * MON-FRI")
    public void createDailyTimeSpent() throws UnirestException {
        logger.info("[DAILY SCHEDULE]: Start request for creating daily time spent...");

        Unirest.setTimeouts(redmineTimeout, redmineTimeout);
        HttpResponse<String> response = Unirest.post("http://red.eltex.loc/time_entries.xml")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + authToken)
                .field("utf8", "✓")
                .field("back_url", "http://red.eltex.loc/issues/260767")
                .field("issue_id", MAIN_VERSION_TASK)
                .field("time_entry[issue_id]", MAIN_VERSION_TASK)
                .field("time_entry[spent_on]", redmineDateFormat.format(Calendar.getInstance().getTime()))
                .field("time_entry[hours]", "0,3")
                .field("time_entry[comments]", "Daily")
                .field("time_entry[activity_id]", "10")
                .field("continue", "Создать+и+продолжить")
                .asString();

        if (response.getStatus() != 201) {
            logger.error("[DAILY SCHEDULE]: WTF! Can't create daily meeting time spent!");
            return;
        }

        logger.info("[DAILY SCHEDULE]: Success create daily time spent!");
    }
}
