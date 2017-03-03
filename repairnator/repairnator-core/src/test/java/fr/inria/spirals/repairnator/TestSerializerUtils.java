package fr.inria.spirals.repairnator;

import fr.inria.spirals.repairnator.SerializerUtils;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

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

    @Test
    public void testDuration() {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.set(2017, 2, 21, 15, 35);

        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(2017, 2, 21, 18, 56);

        Date date1 = calendar1.getTime();
        Date date2 = calendar2.getTime();

        String humanString = SerializerUtils.getDuration(date1, date2);

        assertThat(humanString, is("03:21"));
    }
}
