package org.a5calls.android.a5calls.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.Contact;

import java.util.regex.Pattern;

public class ScriptReplacements {
    private static final Pattern HOUSE_INTRO_PATTERN =
            Pattern.compile("\\*{2}WHEN CALLING HOUSE:\\*{2}\\n", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENATE_INTRO_PATTERN =
            Pattern.compile("\\*{2}WHEN CALLING SENATE:\\*{2}\\n", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_NAME_PATTERN =
            Pattern.compile("\\[\\s?REP/SEN NAME\\s?]|\\[\\s?SENATOR/REP NAME\\s?]|\\[\\s?SENATOR NAME]|\\[\\s?REPRESENTATIVE NAME\\s?]", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATION_PATTERN =
            Pattern.compile("\\[\\s?CITY[,/]\\s?ZIP\\s?]|\\[\\s?CITY[,/]\\s?STATE\\s?]", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN = Pattern.compile("\\[\\s*NAME\\s*]", Pattern.CASE_INSENSITIVE);

    // Other ways writers might mention the contact area in string replacements.
    private static final String HOUSE = "House";
    private static final String SENATE = "Senate";

    public static String replacing(Context context, String script, Contact contact,
                                   @Nullable String location, @Nullable String userName) {
        String replacedScript = chooseSubscript(script, contact);
        replacedScript = replacingContact(context, replacedScript, contact);

        if (!TextUtils.isEmpty(location)) {
            replacedScript = replacingLocation(replacedScript, location);
        }

        if (!TextUtils.isEmpty(userName)) {
            replacedScript = replacingUserName(replacedScript, userName);
        }

        return replacedScript;
    }

    private static String chooseSubscript(String script, Contact contact) {
        String area = contact.area;

        if (area.equals(Contact.AREA_HOUSE) || area.equals(HOUSE)) {
            String replacedScript = HOUSE_INTRO_PATTERN.matcher(script).replaceAll("");
            return wholeRegex(SENATE_INTRO_PATTERN).matcher(replacedScript).replaceAll("");
        } else if (area.equals(Contact.AREA_SENATE) || area.equals(SENATE)) {
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

    private static String replacingUserName(String script, String userName) {
        return NAME_PATTERN.matcher(script).replaceAll(userName);
    }

    private static Pattern wholeRegex(Pattern introPattern) {
        return Pattern.compile(
                introPattern +
                "[^\\n]+" + // One or more non-newline characters
                "([\\n\\r]|(\\r\\n))+" // One or more newline sequences
        );
    }

    private static @Nullable String getTitle(Context context, String area) {
        return switch (area) {
            case Contact.AREA_HOUSE, HOUSE -> context.getString(R.string.title_us_house);
            case Contact.AREA_SENATE, SENATE -> context.getString(R.string.title_us_senate);
            case Contact.AREA_STATE_LOWER, Contact.AREA_STATE_UPPER -> context.getString(R.string.title_state_rep);
            case Contact.AREA_GOVERNOR -> context.getString(R.string.title_governor);
            case Contact.AREA_ATTORNEY_GENERAL -> context.getString(R.string.title_attorney_general);
            case Contact.AREA_SECRETARY_OF_STATE -> context.getString(R.string.title_secretary_of_state);
            default -> null;
        };
    }
}