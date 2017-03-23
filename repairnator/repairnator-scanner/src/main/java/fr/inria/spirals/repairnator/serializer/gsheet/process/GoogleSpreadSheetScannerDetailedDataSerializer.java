package fr.inria.spirals.repairnator.serializer.gsheet.process;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fernanda on 13/03/17.
 */
public class GoogleSpreadSheetScannerDetailedDataSerializer implements ProcessSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetScannerDetailedDataSerializer.class);
    private static final String RANGE = "Scanner Detailed Data!A1:K1";

    private Sheets sheets;
    private List<BuildToBeInspected> buildsToBeInspected;

    public GoogleSpreadSheetScannerDetailedDataSerializer(List<BuildToBeInspected> buildsToBeInspected, String googleSecretPath) throws IOException {
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.buildsToBeInspected = buildsToBeInspected;
    }

    public void serialize() {
        if (this.sheets != null) {
            if (this.buildsToBeInspected.size() > 0) {
                Date date = new Date();

                List<List<Object>> dataRows = new ArrayList<List<Object>>();

                for (BuildToBeInspected buildToBeInspected : this.buildsToBeInspected) {
                    Build build = buildToBeInspected.getBuild();

                    Build previousBuild = buildToBeInspected.getPreviousBuild();
                    int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

                    List<Object> dataCol = new ArrayList<Object>();

                    dataCol.add(build.getId() + "");
                    dataCol.add(previousBuildId + "");
                    dataCol.add(buildToBeInspected.getStatus().toString());
                    dataCol.add(build.getRepository().getSlug());
                    dataCol.add(Utils.formatCompleteDate(date));
                    dataCol.add(Utils.formatOnlyDay(date));
                    dataCol.add(Utils.getHostname());
                    dataCol.add("http://travis-ci.org/" + build.getRepository().getSlug() + "/builds/" + build.getId());
                    dataCol.add("http://travis-ci.org/" + build.getRepository().getSlug() + "/builds/" + previousBuildId);
                    String committerEmail = (build.getCommit().getCommitterEmail() != null) ? build.getCommit().getCommitterEmail() : "-";
                    dataCol.add(committerEmail);
                    dataCol.add(buildToBeInspected.getRunId());
                    dataRows.add(dataCol);
                }

                GoogleSpreadSheetUtils.insertData(dataRows, this.sheets, RANGE, this.logger);
            }
        } else {
            GoogleSpreadSheetUtils.logWarningWhenSheetsIsNull(this.logger);
        }
    }

}
