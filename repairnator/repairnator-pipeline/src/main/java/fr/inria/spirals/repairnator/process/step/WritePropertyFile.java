package fr.inria.spirals.repairnator.process.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.MetricsSerializerAdapter;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.Metrics4Bears;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.MetricsSerializerAdapter4Bears;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.PipelineState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class WritePropertyFile extends AbstractStep {

    private static final String PROPERTY_FILENAME = "repairnator.json";
    private static final String PROPERTY_FILENAME_BEARS = "bears.json";

    public WritePropertyFile(ProjectInspector inspector) {
        super(inspector, false);
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Writing file with properties...");

        String filePath;
        Gson gson;
        String jsonString;
        boolean repairnatorFileSuccessfullyWritten;
        boolean bearsFileSuccessfullyWritten;

        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            filePath = this.getInspector().getRepoLocalPath() + File.separator + PROPERTY_FILENAME;
            gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Metrics.class, new MetricsSerializerAdapter()).create();
            jsonString = gson.toJson(this.getInspector().getJobStatus().getProperties());
            repairnatorFileSuccessfullyWritten = this.writeJsonFile(filePath, jsonString);
            if (!repairnatorFileSuccessfullyWritten) {
                this.addStepError("Fail to write the property file " + PROPERTY_FILENAME);
            }
        }

        filePath = this.getInspector().getRepoLocalPath() + File.separator + PROPERTY_FILENAME_BEARS;
        gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Metrics4Bears.class, new MetricsSerializerAdapter4Bears()).create();
        jsonString = gson.toJson(this.getInspector().getJobStatus().getMetrics4Bears());
        bearsFileSuccessfullyWritten = this.writeJsonFile(filePath, jsonString);
        if (!bearsFileSuccessfullyWritten) {
            this.addStepError("Fail to write the property file " + PROPERTY_FILENAME_BEARS);
            return StepStatus.buildError(this, PipelineState.PROPERTY_FILE_NOT_WRITTEN);
        }

        return StepStatus.buildSuccess(this);
    }

    private boolean writeJsonFile(String filePath, String jsonString) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(file));
            outputStream.write(jsonString);
            outputStream.flush();
            outputStream.close();
            this.getInspector().getJobStatus().addFileToPush(file.getName());
            return true;
        } catch (IOException e) {
            this.getLogger().error("An exception occurred when writing the following property file: " + file.getPath(), e);
        }
        return false;
    }
}
