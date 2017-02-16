package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.SerializerUtils;
import fr.inria.spirals.repairnator.serializer.gsheet.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 02/02/2017.
 */
public class GoogleSpreadSheetInspectorTimeSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetInspectorTimeSerializer.class);
    private static final String RANGE = "Duration Data!A1:J1";

    private Sheets sheets;

    public GoogleSpreadSheetInspectorTimeSerializer(String googleSecretPath) throws IOException {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getStepsDurationsInSeconds();
        int total = 0;
        int clonage = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int buildtime = durations.getOrDefault(BuildProject.class.getSimpleName(), 0);
        int test = durations.getOrDefault(TestProject.class.getSimpleName(), 0);
        int gatherTestInfo = durations.getOrDefault(GatherTestInformation.class.getSimpleName(), 0);
        int push = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int repair = durations.getOrDefault(NopolRepair.class.getSimpleName(), 0);

        total = clonage+buildtime+test+gatherTestInfo+push+repair;
        Build build = inspector.getBuild();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId()+"");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(SerializerUtils.formatCompleteDate(new Date()));
        dataCol.add(total);
        dataCol.add(clonage);
        dataCol.add(buildtime);
        dataCol.add(test);
        dataCol.add(gatherTestInfo);
        dataCol.add(push);
        dataCol.add(repair);
        dataCol.add(SerializerUtils.getHostname());

        List<List<Object>> dataRow = new ArrayList<List<Object>>();
        dataRow.add(dataCol);

        ValueRange valueRange = new ValueRange();
        valueRange.setValues(dataRow);

        try {
            AppendValuesResponse response = this.sheets.spreadsheets().values().append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange).setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
            if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                this.logger.debug("Time data have been inserted in Google Spreadsheet.");
            }
        } catch (IOException e) {
            this.logger.error("An error occured while inserting time data in Google Spreadsheet.",e);
        }
    }
}
