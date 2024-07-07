package org.bcnlabs.beaconlabsproxy;

import java.time.LocalDateTime;

public class BanEntry {
    private String playerName;
    private String reason;
    private LocalDateTime banDate;
    private int banDurationDays; // Duration of the ban in days

    public BanEntry(String playerName, String reason, LocalDateTime banDate, int banDurationDays) {
        this.playerName = playerName;
        this.reason = reason;
        this.banDate = banDate;
        this.banDurationDays = banDurationDays;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getBanDate() {
        return banDate;
    }

    public int getBanDurationDays() {
        return banDurationDays;
    }
}
