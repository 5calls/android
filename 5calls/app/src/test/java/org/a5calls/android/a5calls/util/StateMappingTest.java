package org.a5calls.android.a5calls.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for StateMapping utility class.
 */
public class StateMappingTest {

    @Test
    public void testGetStateName_validStates() {
        assertEquals("California", StateMapping.getStateName("CA"));
        assertEquals("New York", StateMapping.getStateName("NY"));
        assertEquals("Texas", StateMapping.getStateName("TX"));
        assertEquals("Washington D.C.", StateMapping.getStateName("DC"));
    }

    @Test
    public void testGetStateName_caseInsensitive() {
        assertEquals("California", StateMapping.getStateName("ca"));
        assertEquals("New York", StateMapping.getStateName("ny"));
        assertEquals("California", StateMapping.getStateName("Ca"));
        assertEquals("New York", StateMapping.getStateName("Ny"));
    }

    @Test
    public void testGetStateName_invalidStates() {
        assertNull(StateMapping.getStateName("XX"));
        assertNull(StateMapping.getStateName("ZZ"));
        assertNull(StateMapping.getStateName("ABC"));
        assertNull(StateMapping.getStateName("123"));
    }

    @Test
    public void testGetStateName_nullAndEmpty() {
        assertNull(StateMapping.getStateName(null));
        assertNull(StateMapping.getStateName(""));
        assertNull(StateMapping.getStateName("   "));
    }

    @Test
    public void testGetStateName_territories() {
        assertEquals("Puerto Rico", StateMapping.getStateName("PR"));
        assertEquals("U.S. Virgin Islands", StateMapping.getStateName("VI"));
        assertEquals("American Samoa", StateMapping.getStateName("AS"));
        assertEquals("Guam", StateMapping.getStateName("GU"));
    }
}