package org.a5calls.android.a5calls.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping state abbreviations to full state names.
 */
public class StateMapping {
    
    private static final Map<String, String> STATE_MAP = new HashMap<String, String>() {{
        put("AL", "Alabama");
        put("AK", "Alaska");
        put("AZ", "Arizona");
        put("AR", "Arkansas");
        put("CA", "California");
        put("CO", "Colorado");
        put("CT", "Connecticut");
        put("DE", "Delaware");
        put("FL", "Florida");
        put("GA", "Georgia");
        put("HI", "Hawaii");
        put("ID", "Idaho");
        put("IL", "Illinois");
        put("IN", "Indiana");
        put("IA", "Iowa");
        put("KS", "Kansas");
        put("KY", "Kentucky");
        put("LA", "Louisiana");
        put("ME", "Maine");
        put("MD", "Maryland");
        put("MA", "Massachusetts");
        put("MI", "Michigan");
        put("MN", "Minnesota");
        put("MS", "Mississippi");
        put("MO", "Missouri");
        put("MT", "Montana");
        put("NE", "Nebraska");
        put("NV", "Nevada");
        put("NH", "New Hampshire");
        put("NJ", "New Jersey");
        put("NM", "New Mexico");
        put("NY", "New York");
        put("NC", "North Carolina");
        put("ND", "North Dakota");
        put("OH", "Ohio");
        put("OK", "Oklahoma");
        put("OR", "Oregon");
        put("PA", "Pennsylvania");
        put("RI", "Rhode Island");
        put("SC", "South Carolina");
        put("SD", "South Dakota");
        put("TN", "Tennessee");
        put("TX", "Texas");
        put("UT", "Utah");
        put("VT", "Vermont");
        put("VA", "Virginia");
        put("WA", "Washington");
        put("WV", "West Virginia");
        put("WI", "Wisconsin");
        put("WY", "Wyoming");
        put("DC", "Washington D.C.");
        put("PR", "Puerto Rico");
        put("VI", "U.S. Virgin Islands");
        put("AS", "American Samoa");
        put("GU", "Guam");
        put("MP", "Northern Mariana Islands");
    }};
    
    /**
     * Get the full state name from a state abbreviation.
     * @param abbreviation The state abbreviation (e.g., "CA", "NY")
     * @return The full state name (e.g., "California", "New York") or null if not found
     */
    public static String getStateName(String abbreviation) {
        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            return null;
        }
        return STATE_MAP.get(abbreviation.trim().toUpperCase());
    }
}