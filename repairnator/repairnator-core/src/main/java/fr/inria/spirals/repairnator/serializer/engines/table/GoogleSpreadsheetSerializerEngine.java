package fr.inria.spirals.repairnator.serializer.engines.table;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 27/03/2017.
 */
public class GoogleSpreadsheetSerializerEngine implements SerializerEngine {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadsheetSerializerEngine.class);

    private Sheets sheets;

    public GoogleSpreadsheetSerializerEngine() {
        this.sheets = GoogleSpreadSheetFactory.getSheets();
    }

    @Override
    public void serialize(List<SerializedData> data, Serializers serializer) {
        List<List<Object>> allRows = new ArrayList<>();

        for (SerializedData oneRow : data) {
            allRows.add(oneRow.getAsList());
        }

        GoogleSpreadSheetFactory.insertData(allRows, this.sheets, serializer.getRange(), this.logger);
    }
}
