package fr.inria.spirals.repairnator.serializer;

import org.junit.Test;

import java.util.Calendar;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created by urli on 21/02/2017.
 */
public class TestSerializerUtils {

    @Test
    public void testFormatCompleteDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, 8, 27, 14, 15, 18);

        String obtainedStr = SerializerUtils.formatCompleteDate(calendar.getTime());
        String expectedStr = "27/09/17 14:15";

        assertThat(obtainedStr, is(expectedStr));
    }

    @Test
    public void testFormatOnlyDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, 8, 27, 14, 15, 18);

        String obtainedStr = SerializerUtils.formatOnlyDay(calendar.getTime());
        String expectedStr = "27/09/2017";

        assertThat(obtainedStr, is(expectedStr));
    }

}
