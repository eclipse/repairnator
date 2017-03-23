package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 16/02/2017.
 */
public class GoogleSpreadSheetNopolSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetNopolSerializer.class);
    private static final String RANGE = "Nopol Stats!A1:Q1";

    private Sheets sheets;

    public GoogleSpreadSheetNopolSerializer(String googleSecretPath) throws IOException {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
    }

    private List<Object> serializeNopolInfo(BuildToBeInspected buildToBeInspected, NopolInformation nopolInformation, Patch patch, int patchNumber) {

        Build build = buildToBeInspected.getBuild();
        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(nopolInformation.getDateEnd()));
        dataCol.add(Utils.formatOnlyDay(nopolInformation.getDateEnd()));
        dataCol.add(build.getId());
        dataCol.add(build.getRepository().getSlug());

        dataCol.add(nopolInformation.getLocation().getClassName());
        dataCol.add(StringUtils.join(nopolInformation.getLocation().getFailures(), ","));
        dataCol.add(nopolInformation.getAllocatedTime());
        dataCol.add(nopolInformation.getPassingTime());
        dataCol.add(nopolInformation.getStatus().name());

        if (nopolInformation.getStatus() == NopolStatus.EXCEPTION) {
            dataCol.add(nopolInformation.getExceptionDetail());
        } else {
            dataCol.add("N/A");
        }

        if (patch == null) {
            dataCol.add("N/A");
            dataCol.add("N/A");
            dataCol.add("N/A");
            dataCol.add("N/A");
        } else {
            dataCol.add(patchNumber + "/" + nopolInformation.getPatches().size());
            dataCol.add(patch.getType().name());
            dataCol.add(patch.asString());
            dataCol.add(patch.getRootClassName() + ":" + patch.getLineNumber());
        }

        NopolContext nopolContext = nopolInformation.getNopolContext();
        dataCol.add("localizer: " + nopolContext.getLocalizer().name() + ";solver: " + nopolContext.getSolver().name()
                + ";synthetizer: " + nopolContext.getSynthesis().name() + ";type: " + nopolContext.getType().name());
        dataCol.add(nopolInformation.getNbAngelicValues());
        dataCol.add(nopolInformation.getNbStatements());
        dataCol.add(nopolInformation.getIgnoreStatus().name());
        dataCol.add(buildToBeInspected.getRunId());
        return dataCol;
    }

    @Override
    public void serializeData(ProjectInspector inspector){
        if (this.sheets != null) {
            if (inspector.getJobStatus().getNopolInformations() != null) {
                BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();

                List<List<Object>> dataRow = new ArrayList<List<Object>>();

                for (NopolInformation nopolInformation : inspector.getJobStatus().getNopolInformations()) {
                    List<Object> dataCol;

                    if (nopolInformation.getPatches().isEmpty()) {
                        dataCol = this.serializeNopolInfo(buildToBeInspected, nopolInformation, null, 0);
                        dataRow.add(dataCol);
                    } else {
                        int patchNumber = 1;

                        for (Patch patch : nopolInformation.getPatches()) {
                            dataCol = this.serializeNopolInfo(buildToBeInspected, nopolInformation, patch, patchNumber++);
                            dataRow.add(dataCol);
                        }
                    }
                }

                if (!dataRow.isEmpty()) {
                    ValueRange valueRange = new ValueRange();
                    valueRange.setValues(dataRow);

                    try {
                        AppendValuesResponse response = this.sheets.spreadsheets().values()
                                .append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange)
                                .setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
                        if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                            this.logger.debug("Data have been inserted in Google Spreadsheet.");
                        }
                    } catch (IOException e) {
                        this.logger.error("An error occured while inserting data in Google Spreadsheet.", e);
                    }
                }
            } else {
                this.logger
                        .warn("NopolRepair step seems not defined in inspector. Maybe you should not use this serializer.");
            }
        } else {
            this.logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
        }
    }
}
