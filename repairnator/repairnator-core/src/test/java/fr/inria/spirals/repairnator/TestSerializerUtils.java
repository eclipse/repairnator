package fr.inria.spirals.repairnator;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

        String obtainedStr = Utils.formatCompleteDate(calendar.getTime());
        String expectedStr = "27/09/17 14:15";

        assertThat(obtainedStr, is(expectedStr));
    }

    @Test
    public void testFormatOnlyDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, 8, 27, 14, 15, 18);

        String obtainedStr = Utils.formatOnlyDay(calendar.getTime());
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

        String humanString = Utils.getDuration(date1, date2);

        assertThat(humanString, is("03:21"));
    }

    @Test
    public void testGetLastTimeFromDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat simpleDateFormatAux = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        Date date = null;
        try {
            date = simpleDateFormat.parse("01/01/2017");

            Date obtainedDate = Utils.getLastTimeFromDate(date);

            String expectedStr = "01/01/2017 23:59:59";

            assertThat(simpleDateFormatAux.format(obtainedDate), is(expectedStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
