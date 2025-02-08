package org.a5calls.android.a5calls.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.Contact;

import java.util.regex.Pattern;

public class ScriptReplacements {
    private static final Pattern HOUSE_INTRO_PATTERN = Pattern.compile("\\*{2}WHEN CALLING HOUSE:\\*{2}\\n");
    private static final Pattern SENATE_INTRO_PATTERN = Pattern.compile("\\*{2}WHEN CALLING SENATE:\\*{2}\\n");
    private static final Pattern CONTACT_NAME_PATTERN = Pattern.compile("\\[REP/SEN NAME]|\\[SENATOR/REP NAME]|\\[SENATOR NAME]|\\[REPRESENTATIVE NAME]");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("\\[CITY,\\s?ZIP]|\\[CITY,\\s?STATE]");

    private static final String US_HOUSE = "US House";
    private static final String HOUSE = "House";
    private static final String US_SENATE = "US Senate";
    private static final String SENATE = "Senate";
    private static final String STATE_UPPER = "StateUpper";
    private static final String STATE_LOWER = "StateLower";
    private static final String GOVERNOR = "Governor";
    private static final String ATTORNEY_GENERAL = "AttorneyGeneral";
    private static final String SECRETARY_OF_STATE = "SecretaryOfState";

    public static String replacing(Context context, String script, Contact contact, @Nullable String location) {
        String replacedScript = chooseSubscript(script, contact);
        replacedScript = replacingContact(context, script, contact);

        if (!TextUtils.isEmpty(location)) {
            replacedScript = replacingLocation(replacedScript, location);
        }

        return replacedScript;
    }

    private static String chooseSubscript(String script, Contact contact) {
        String area = contact.area;

        if (area.equals(US_HOUSE) || area.equals(HOUSE)) {
            String replacedScript = HOUSE_INTRO_PATTERN.matcher(script).replaceAll("");
            return wholeRegex(SENATE_INTRO_PATTERN).matcher(replacedScript).replaceAll("");
        } else if (area.equals(US_SENATE) || area.equals(SENATE)) {
            String replacedScript = SENATE_INTRO_PATTERN.matcher(script).replaceAll("");
            return wholeRegex(HOUSE_INTRO_PATTERN).matcher(replacedScript).replaceAll("");
        }

        return script;
    }

    private static String replacingContact(Context context, String script, Contact contact) {
        String template = getTitle(context, contact.area) != null
                ? getTitle(context, contact.area) + " " + contact.name
                : contact.name;
        return CONTACT_NAME_PATTERN.matcher(script).replaceAll(template);
    }

    private static String replacingLocation(String script, String location) {
        return LOCATION_PATTERN.matcher(script).replaceAll(location);
    }

    private static Pattern wholeRegex(Pattern introPattern) {
        return Pattern.compile(
                introPattern +
                "[^\\n]+" + // One or more non-newline characters
                "\\R+" // One or more newline sequences
        );
    }

    private static @Nullable String getTitle(Context context, String area) {
        return switch (area) {
            case US_HOUSE, HOUSE -> context.getString(R.string.title_us_house);
            case US_SENATE, SENATE -> context.getString(R.string.title_us_senate);
            case STATE_LOWER, STATE_UPPER -> context.getString(R.string.title_state_rep);
            case GOVERNOR -> context.getString(R.string.title_governor);
            case ATTORNEY_GENERAL -> context.getString(R.string.title_attorney_general);
            case SECRETARY_OF_STATE -> context.getString(R.string.title_secretary_of_state);
            default -> null;
        };
    }
}