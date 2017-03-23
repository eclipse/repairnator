package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by urli on 03/03/2017.
 */
public class GoogleSpreadSheetInspectorTrackTreatedBuilds extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetInspectorTrackTreatedBuilds.class);
    private static final String RANGE = "Treated Build Tracking!A1:E1";

    private Sheets sheets;
    private String runid;

    public GoogleSpreadSheetInspectorTrackTreatedBuilds(BuildToBeInspected buildToBeInspected, String googleSecretPath) throws IOException {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.runid = UUID.randomUUID().toString();
        this.serializeBuildToBeInspected(buildToBeInspected);
    }

    private List<Object> getDataColumn(Build build, String status) {
        Date date = new Date();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(runid);
        dataCol.add(build.getId());
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.formatCompleteDate(date));
        dataCol.add(Utils.formatOnlyDay(date));
        dataCol.add(Utils.getHostname());
        dataCol.add(status);

        return dataCol;
    }

    private void serializeBuildToBeInspected(BuildToBeInspected buildToBeInspected) {
        if (this.sheets != null) {
            Build build = buildToBeInspected.getBuild();

            List<Object> dataCol = getDataColumn(build, "DETECTED");

            List<List<Object>> dataRows = new ArrayList<List<Object>>();
            dataRows.add(dataCol);
            GoogleSpreadSheetFactory.insertData(dataRows, this.sheets, RANGE, this.logger);
        } else {
            GoogleSpreadSheetFactory.logWarningWhenSheetsIsNull(this.logger);
        }
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        if (this.sheets != null) {
            Build build = inspector.getBuild();

            List<Object> dataCol = getDataColumn(build, "TREATED");

            List<List<Object>> dataRows = new ArrayList<List<Object>>();
            dataRows.add(dataCol);
            GoogleSpreadSheetFactory.insertData(dataRows, this.sheets, RANGE, this.logger);
        } else {
            GoogleSpreadSheetFactory.logWarningWhenSheetsIsNull(this.logger);
        }
    }

}
