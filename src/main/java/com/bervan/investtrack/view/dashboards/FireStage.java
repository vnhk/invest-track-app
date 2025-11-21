package com.bervan.investtrack.view.dashboards;

public class FireStage {

    private final String stageName;
    private final String percent;
    private final String amount;
    private final String howMuchLeft;
    private final String howManyMonths;
    private final double progress; // 0–1

    public FireStage(String stageName, String percent, String amount,
                     String howMuchLeft, String howManyMonths, double progress) {
        this.stageName = stageName;
        this.percent = percent;
        this.amount = amount;
        this.howMuchLeft = howMuchLeft;
        this.howManyMonths = howManyMonths;
        this.progress = progress;
    }

    public String getStageName() {
        return stageName;
    }

    public String getPercent() {
        return percent;
    }

    public String getAmount() {
        return amount;
    }

    public String getHowMuchLeft() {
        return howMuchLeft;
    }

    public String getHowManyMonths() {
        return howManyMonths;
    }

    public double getProgress() {
        return progress;
    }
}