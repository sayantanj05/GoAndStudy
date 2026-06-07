package com.goandstudybackend.util;

public final class MemberIdGenerator {

    private MemberIdGenerator() {
    }

    public static String generate(String registrationDate, long dailySequence) {
        return "MEM" + registrationDate + String.format("%03d", dailySequence);
    }
}
