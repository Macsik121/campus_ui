package com.sfedu.campus.helpers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Возвращает относительное время в текстовом формате (на русском языке).
     * Логика:
     * - меньше минуты: "только что"
     * - меньше часа: "N минут назад" (с правильными окончаниями)
     * - сегодня: "сегодня в HH:mm"
     * - вчера: "вчера в HH:mm"
     * - ранее: "dd.MM.yyyy"
     */
    public static String getRelativeTime(OffsetDateTime sentAt) {
        if (sentAt == null) return "";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime then = sentAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        long minutes = ChronoUnit.MINUTES.between(then, now);
        
        // Будущее время (на случай рассинхрона часов)
        if (minutes < 0) return "только что";

        // Меньше минуты
        if (minutes < 1) return "только что";

        // Меньше часа - минуты назад
        if (minutes < 60) {
            return minutes + " " + getRussianPlural(minutes, "минуту", "минуты", "минут") + " назад";
        }

        // Сегодня
        if (then.toLocalDate().equals(now.toLocalDate())) {
            return "сегодня в " + then.format(TIME_FORMAT);
        }

        // Вчера
        if (then.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            return "вчера в " + then.format(TIME_FORMAT);
        }

        // Более ранние даты
        return then.format(DATE_FORMAT);
    }

    /**
     * Вспомогательный метод для склонения существительных после числительных
     */
    private static String getRussianPlural(long n, String one, String two, String five) {
        n = Math.abs(n) % 100;
        long n1 = n % 10;
        if (n > 10 && n < 20) return five;
        if (n1 > 1 && n1 < 5) return two;
        if (n1 == 1) return one;
        return five;
    }
}
