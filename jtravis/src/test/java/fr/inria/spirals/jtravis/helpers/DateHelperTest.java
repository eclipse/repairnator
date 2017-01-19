package fr.inria.spirals.jtravis.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import okhttp3.ResponseBody;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/12/2016.
 */
public class DateHelperTest {

    private class Helper extends AbstractHelper {
        public Helper() {
            super();
        }

        public Date parseDate(JsonElement element) {
            Gson gson = createGson();
            return gson.fromJson(element, Date.class);
        }
    }

    @Test
    public void shouldReturnTheRightDateWhenCorrectlyFormatted() {
        String dateStr = "2016-12-21T09:48:50Z";
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(dateStr);

        Helper helper = new Helper();
        Date expectedDate = TestUtils.getDate(2016, 12, 21, 9, 48, 50);
        Date obtainedDate = helper.parseDate(jsonElement);
        assertEquals(expectedDate, obtainedDate);
    }

    @Test(expected = JsonSyntaxException.class)
    public void shouldReturnNullWhenWrongFormat() {
        String dateStr = "21/12/20016-09:48:50";
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(dateStr);

        Helper helper = new Helper();
        Date obtainedDate = helper.parseDate(jsonElement);
    }
}
