package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by urli on 22/12/2016.
 */
public class DateHelper {

    public static Date getDateFromJSONStringElement(JsonElement json) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
        try {
            return format.parse(json.getAsString());
        } catch (ParseException e) {
            return null;
        }
    }
}
