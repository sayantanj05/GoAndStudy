package com.goandstudybackend.util;

public enum MembershipType {

    BASIC("Basic", 2, 14, 10.0, 1, 0.0),
    STANDARD("Standard", 3, 14, 10.0, 1, 199.0),
    PREMIUM("Premium", 5, 21, 10.0, 2, 499.0),
    STUDENT("Student", 3, 21, 5.0, 1, 99.0);

    public final String displayName;
    public final int maxActiveLoans;
    public final int loanDurationDays;
    public final double fineRatePerDay;
    public final int renewalLimit;
    public final double annualFee;

    MembershipType(String displayName, int maxActiveLoans, int loanDurationDays,
                   double fineRatePerDay, int renewalLimit, double annualFee) {
        this.displayName = displayName;
        this.maxActiveLoans = maxActiveLoans;
        this.loanDurationDays = loanDurationDays;
        this.fineRatePerDay = fineRatePerDay;
        this.renewalLimit = renewalLimit;
        this.annualFee = annualFee;
    }
}
