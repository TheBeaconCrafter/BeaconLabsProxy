package org.bcnlab.beaconlabsproxy.Utils;

import java.time.Duration;

public class DurationParser {

    public static Duration parse(String input) {
        input = input.toLowerCase().trim();

        if (input.endsWith("d")) {
            int days = Integer.parseInt(input.substring(0, input.length() - 1));
            return Duration.ofDays(days);
        } else if (input.endsWith("h")) {
            int hours = Integer.parseInt(input.substring(0, input.length() - 1));
            return Duration.ofHours(hours);
        } else if (input.endsWith("min")) {
            int minutes = Integer.parseInt(input.substring(0, input.length() - 3));
            return Duration.ofMinutes(minutes);
        } else if (input.endsWith("m")) {
            // allow just "5m" too, like shorthand
            int minutes = Integer.parseInt(input.substring(0, input.length() - 1));
            return Duration.ofMinutes(minutes);
        } else if (input.endsWith("s")) {
            int seconds = Integer.parseInt(input.substring(0, input.length() - 1));
            return Duration.ofSeconds(seconds);
        } else {
            throw new IllegalArgumentException("Invalid duration format: " + input);
        }
    }
}

