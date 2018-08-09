package fr.inria.spirals.repairnator.process.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.inspectors.properties.PropertiesSerializerAdapter;
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
        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            filePath = this.getInspector().getRepoLocalPath() + File.separator + PROPERTY_FILENAME;
        } else {
            filePath = this.getInspector().getRepoLocalPath() + File.separator + PROPERTY_FILENAME_BEARS;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Properties.class, new PropertiesSerializerAdapter()).create();
        String jsonString = gson.toJson(this.getInspector().getJobStatus().getProperties());

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

            return StepStatus.buildSuccess(this);
        } catch (IOException e) {
            this.addStepError("An exception occurred when writing the following property file: " + file.getPath(), e);
        }
        return StepStatus.buildError(this, PipelineState.PROPERTY_FILE_NOT_WRITTEN);
    }

}
