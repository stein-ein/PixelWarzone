package com.github.steinein.pixelwarzone;

public class WarzonePermission {
    // Op level 2 required for admin commands
    public static final int ADMIN_LEVEL = 2;
    // Op level 3 for special exemptions
    public static final int EXEMPT_LEVEL = 3;

    // Used as identifiers for hasPermission checks
    public static final String WARZONE_EXCLUDE = "pixelwarzone.exclude";
    public static final String WARZONE_FLIGHT_EXEMPT = "pixelwarzone.flightexempt";
}
