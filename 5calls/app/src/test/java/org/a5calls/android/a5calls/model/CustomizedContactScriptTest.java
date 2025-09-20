package org.a5calls.android.a5calls.model;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class CustomizedContactScriptTest {

    @Test
    public void testDefaultConstructor() {
        CustomizedContactScript script = new CustomizedContactScript();
        assertNull(script.id);
        assertNull(script.script);
    }

    @Test
    public void testParameterizedConstructor() {
        String testId = "contact123";
        String testScript = "Hello, this is a test script";

        CustomizedContactScript script = new CustomizedContactScript(testId, testScript);
        assertEquals(testId, script.id);
        assertEquals(testScript, script.script);
    }

    @Test
    public void testParameterizedConstructorWithNulls() {
        CustomizedContactScript script = new CustomizedContactScript(null, null);
        assertNull(script.id);
        assertNull(script.script);
    }

    @Test
    public void testParameterizedConstructorWithEmptyStrings() {
        CustomizedContactScript script = new CustomizedContactScript("", "");
        assertEquals("", script.id);
        assertEquals("", script.script);
    }

    @Test
    public void testParcelableImplementation() {
        String testId = "contact456";
        String testScript = "This is a test script for parcelable";

        CustomizedContactScript original = new CustomizedContactScript(testId, testScript);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        CustomizedContactScript fromParcel = CustomizedContactScript.CREATOR.createFromParcel(parcel);

        assertEquals(original.id, fromParcel.id);
        assertEquals(original.script, fromParcel.script);

        parcel.recycle();
    }

    @Test
    public void testParcelableWithNulls() {
        CustomizedContactScript original = new CustomizedContactScript(null, null);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        CustomizedContactScript fromParcel = CustomizedContactScript.CREATOR.createFromParcel(parcel);

        assertNull(fromParcel.id);
        assertNull(fromParcel.script);

        parcel.recycle();
    }

    @Test
    public void testParcelableWithEmptyStrings() {
        CustomizedContactScript original = new CustomizedContactScript("", "");

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        CustomizedContactScript fromParcel = CustomizedContactScript.CREATOR.createFromParcel(parcel);

        assertEquals("", fromParcel.id);
        assertEquals("", fromParcel.script);

        parcel.recycle();
    }

    @Test
    public void testDescribeContents() {
        CustomizedContactScript script = new CustomizedContactScript();
        assertEquals(0, script.describeContents());
    }

    @Test
    public void testCreatorNewArray() {
        CustomizedContactScript[] array = CustomizedContactScript.CREATOR.newArray(5);
        assertNotNull(array);
        assertEquals(5, array.length);

        for (CustomizedContactScript script : array) {
            assertNull(script);
        }
    }

    @Test
    public void testParcelableWithLongScript() {
        String testId = "contact789";
        StringBuilder longScript = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longScript.append("This is a very long script line ").append(i).append(". ");
        }

        CustomizedContactScript original = new CustomizedContactScript(testId, longScript.toString());

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        CustomizedContactScript fromParcel = CustomizedContactScript.CREATOR.createFromParcel(parcel);

        assertEquals(original.id, fromParcel.id);
        assertEquals(original.script, fromParcel.script);

        parcel.recycle();
    }

    @Test
    public void testParcelableWithSpecialCharacters() {
        String testId = "contact_special!@#$%";
        String testScript = "Script with special chars: ñáéíóú 中文 🎉 \n\t\r";

        CustomizedContactScript original = new CustomizedContactScript(testId, testScript);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        CustomizedContactScript fromParcel = CustomizedContactScript.CREATOR.createFromParcel(parcel);

        assertEquals(original.id, fromParcel.id);
        assertEquals(original.script, fromParcel.script);

        parcel.recycle();
    }
}