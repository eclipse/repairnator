package fr.inria.spirals.repairnator.dockerpool.serializer;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.repairnator.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
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
public class GoogleSpreadSheetTreatedBuildTracking implements ProcessSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetTreatedBuildTracking.class);
    private static final String RANGE = "Treated Build Tracking!A1:G1";

    private Sheets sheets;
    private String runid;
    private Integer buildId;
    private String containerId;
    private String status;

    public GoogleSpreadSheetTreatedBuildTracking(String runid, Integer buildId, String googleSecretPath) throws IOException {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.runid = runid;
        this.buildId = buildId;
        this.containerId = "N/A";
        this.status = "DETECTED";
        this.serialize();
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void serialize() {
        if (this.sheets != null) {
            Date date = new Date();

            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(runid);
            dataCol.add(buildId);
            dataCol.add(containerId);
            dataCol.add(Utils.formatCompleteDate(date));
            dataCol.add(Utils.formatOnlyDay(date));
            dataCol.add(Utils.getHostname());
            dataCol.add(status);

            List<List<Object>> dataRows = new ArrayList<List<Object>>();
            dataRows.add(dataCol);
            GoogleSpreadSheetFactory.insertData(dataRows, this.sheets, RANGE, this.logger);
        } else {
            GoogleSpreadSheetFactory.logWarningWhenSheetsIsNull(this.logger);
        }
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
