package fr.inria.spirals.repairnator.serializer.gsheet.process;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.repairnator.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 02/02/2017.
 */
public class GoogleSpreadSheetScannerSerializer  implements ProcessSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetScannerSerializer.class);
    private static final String RANGE = "Scanner Data!A1:M1";

    private Sheets sheets;
    private ProjectScanner scanner;

    public GoogleSpreadSheetScannerSerializer(ProjectScanner scanner, String googleSecretPath) throws IOException {
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.scanner = scanner;
    }

    public void serialize() {
        if (this.sheets != null) {
            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(Utils.getHostname());
            dataCol.add(Utils.formatCompleteDate(this.scanner.getScannerRunningBeginDate()));
            dataCol.add(Utils.formatCompleteDate(this.scanner.getLookFromDate()));
            dataCol.add(this.scanner.getTotalRepoNumber());
            dataCol.add(this.scanner.getTotalRepoUsingTravis());
            dataCol.add(this.scanner.getTotalScannedBuilds());
            dataCol.add(this.scanner.getTotalBuildInJava());
            dataCol.add(this.scanner.getTotalJavaPassingBuilds());
            dataCol.add(this.scanner.getTotalBuildInJavaFailing());
            dataCol.add(this.scanner.getTotalBuildInJavaFailingWithFailingTests());
            dataCol.add(this.scanner.getTotalPRBuilds());
            dataCol.add(Utils.formatOnlyDay(this.scanner.getLookFromDate()));
            dataCol.add(this.scanner.getScannerDuration());
            dataCol.add(this.scanner.getRunId());

            List<List<Object>> dataRow = new ArrayList<List<Object>>();
            dataRow.add(dataCol);
            GoogleSpreadSheetUtils.insertData(dataRow, this.sheets, RANGE, this.logger);
        } else {
            GoogleSpreadSheetUtils.logWarningWhenSheetsIsNull(this.logger);
        }
    }

}
