package org.a5calls.android.a5calls.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for DatabaseHelper.
 */
public class DatabaseHelperTest {
    @Test
    public void sanitizesStrings() throws Exception {
        assertEquals("TX-BetoO''Rourke", DatabaseHelper.sanitizeContactId("TX-BetoO'Rourke"));
        assertEquals("cats", DatabaseHelper.sanitizeContactId("cats"));
        assertEquals("cat''s", DatabaseHelper.sanitizeContactId("cat's"));
        assertEquals("cat''s cats cats''", DatabaseHelper.sanitizeContactId("cat's cats cats'"));

        // Don't sanitize double quotes
        assertEquals("cat''s", DatabaseHelper.sanitizeContactId("cat''s"));
    }
}