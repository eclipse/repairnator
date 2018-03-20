package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.ParseException;
import com.martiansoftware.jsap.StringParser;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public class PeriodStringParser extends StringParser {
    private static PeriodStringParser instance;

    private PeriodStringParser() {}

    public static PeriodStringParser getParser() {
        if (instance == null) {
            instance = new PeriodStringParser();
        }
        return instance;
    }

    @Override
    public Object parse(String s) throws ParseException {
        try {
            return Duration.parse(s);
        } catch (DateTimeParseException e) {
            throw new ParseException(e);
        } catch (NullPointerException e) {
            throw new ParseException("No period given to parse");
        }
    }
}
