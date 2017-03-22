package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by urli on 02/02/2017.
 */
public class GoogleSpreadSheetInspectorSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetInspectorSerializer.class);
    private static final String RANGE = "All data!A1:K1";

    private Sheets sheets;

    public GoogleSpreadSheetInspectorSerializer(String googleSecretPath) throws IOException {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        if (this.sheets != null) {
            Build build = inspector.getBuild();

            String state = this.getPrettyPrintState(inspector.getState(), inspector.getTestInformations());

            String realState = (inspector.getState() != null) ? inspector.getState().name() : "null";
            String typeOfFailures = "";
            Set<String> failures = inspector.getTestInformations().getFailureNames();

            for (String failure : failures) {
                typeOfFailures += failure + ",";
            }

            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(build.getId() + "");
            dataCol.add(build.getRepository().getSlug());
            dataCol.add(state);
            dataCol.add(build.getPullRequestNumber() + "");
            dataCol.add(Utils.formatCompleteDate(build.getFinishedAt()));
            dataCol.add(Utils.formatOnlyDay(build.getFinishedAt()));
            dataCol.add(realState);
            dataCol.add(Utils.getHostname());
            dataCol.add(Utils.formatCompleteDate(new Date()));
            dataCol.add(this.getTravisUrl(build.getId(), build.getRepository().getSlug()));
            dataCol.add(typeOfFailures);

            List<List<Object>> dataRow = new ArrayList<List<Object>>();
            dataRow.add(dataCol);
            GoogleSpreadSheetUtils.insertData(dataRow, this.sheets, RANGE, this.logger);
        } else {
            GoogleSpreadSheetUtils.logWarningWhenSheetsIsNull(this.logger);
        }
    }
}
