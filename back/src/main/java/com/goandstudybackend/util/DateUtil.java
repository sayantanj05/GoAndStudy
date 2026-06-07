package com.goandstudybackend.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    private static final DateTimeFormatter REGISTRATION_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private DateUtil() {
    }

    public static String todayRegistrationKey() {
        return LocalDate.now().format(REGISTRATION_FORMATTER);
    }

    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    public static LocalDateTime endOfToday() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }

    public static LocalDateTime startOfYear(int year) {
        return Year.of(year).atDay(1).atStartOfDay();
    }

    public static LocalDateTime endOfYear(int year) {
        return Year.of(year).atMonth(12).atEndOfMonth().atTime(LocalTime.MAX);
    }

    public static LocalDateTime startOfCurrentYear() {
        return startOfYear(LocalDate.now().getYear());
    }

    public static LocalDateTime startOfWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    }
}
