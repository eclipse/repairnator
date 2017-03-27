package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import com.google.api.services.sheets.v4.Sheets;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.SquashRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 02/02/2017.
 */
public class GoogleSpreadSheetInspectorTimeSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetInspectorTimeSerializer.class);
    private static final String RANGE = "Duration Data!A1:O1";

    private Sheets sheets;

    public GoogleSpreadSheetInspectorTimeSerializer() {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets();
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        if (this.sheets != null) {
            Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();
            int total = 0;
            int clonage = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
            int checkoutBuild = durations.getOrDefault(CheckoutBuild.class.getSimpleName(), 0);
            int buildtime = durations.getOrDefault(BuildProject.class.getSimpleName(), 0);
            int test = durations.getOrDefault(TestProject.class.getSimpleName(), 0);
            int gatherTestInfo = durations.getOrDefault(GatherTestInformation.class.getSimpleName(), 0);
            int squashRepository = durations.getOrDefault(SquashRepository.class.getSimpleName(), 0);
            int push = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
            int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
            int computeSourceDir = durations.getOrDefault(ComputeSourceDir.class.getSimpleName(), 0);
            int repair = durations.getOrDefault(NopolRepair.class.getSimpleName(), 0);

            int totalDuration = clonage + checkoutBuild + buildtime + test + gatherTestInfo + squashRepository + push +
                    computeClasspath + computeSourceDir + repair;

            Build build = inspector.getBuild();

            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(build.getId() + "");
            dataCol.add(build.getRepository().getSlug());
            dataCol.add(Utils.formatCompleteDate(new Date()));
            dataCol.add(Utils.getHostname());
            dataCol.add(totalDuration);
            dataCol.add(clonage);
            dataCol.add(checkoutBuild);
            dataCol.add(buildtime);
            dataCol.add(test);
            dataCol.add(gatherTestInfo);
            dataCol.add(squashRepository);
            dataCol.add(push);
            dataCol.add(computeClasspath);
            dataCol.add(computeSourceDir);
            dataCol.add(repair);

            List<List<Object>> dataRow = new ArrayList<List<Object>>();
            dataRow.add(dataCol);
            GoogleSpreadSheetFactory.insertData(dataRow, this.sheets, RANGE, this.logger);
        } else {
            GoogleSpreadSheetFactory.logWarningWhenSheetsIsNull(this.logger);
        }
    }
}
