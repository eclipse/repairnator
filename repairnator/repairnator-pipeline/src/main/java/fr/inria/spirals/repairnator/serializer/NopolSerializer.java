package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
import fr.inria.spirals.repairnator.process.nopol.PatchAndDiff;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 16/02/2017.
 */
public class NopolSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(NopolSerializer.class);

    public NopolSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.NOPOL);
    }


    private List<Object> serializeNopolInfoAsList(BuildToBeInspected buildToBeInspected, NopolInformation nopolInformation, PatchAndDiff patchAndDiff, int patchNumber) {

        Build build = buildToBeInspected.getBuggyBuild();
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

        if (patchAndDiff == null) {
            dataCol.add("N/A");
            dataCol.add("N/A");
            dataCol.add("N/A");
            dataCol.add("N/A");
        } else {
            Patch patch = patchAndDiff.getPatch();
            dataCol.add(patchNumber + "/" + nopolInformation.getPatches().size());
            dataCol.add(patch.getType().name());
            dataCol.add(patchAndDiff.getDiff());
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

    private JsonElement serializeNopolInfoAsJson(BuildToBeInspected buildToBeInspected, NopolInformation nopolInformation, PatchAndDiff patchAndDiff, int patchNumber) {
        Build build = buildToBeInspected.getBuggyBuild();

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("nopolDateEndStr", Utils.formatCompleteDate(nopolInformation.getDateEnd()));
        this.addDate(result, "nopolDateEnd", nopolInformation.getDateEnd());

        result.addProperty("nopolDayEnd", Utils.formatOnlyDay(nopolInformation.getDateEnd()));
        result.addProperty("testClassLocation", nopolInformation.getLocation().getClassName());
        result.addProperty("failures", StringUtils.join(nopolInformation.getLocation().getFailures(), ","));
        result.addProperty("allocatedTime", nopolInformation.getAllocatedTime());
        result.addProperty("passingTime", nopolInformation.getPassingTime());
        result.addProperty("status", nopolInformation.getStatus().name());

        if (nopolInformation.getStatus() == NopolStatus.EXCEPTION) {
            result.addProperty("exceptionDetail", nopolInformation.getExceptionDetail());
        }

        if (patchAndDiff != null) {
            Patch patch = patchAndDiff.getPatch();
            result.addProperty("totalPatches", nopolInformation.getPatches().size());
            result.addProperty("patchNumber", patchNumber);
            result.addProperty("patchType", patch.getType().name());
            result.addProperty("patch", patchAndDiff.getDiff());
            result.addProperty("patchLocation", patch.getRootClassName() + ":" + patch.getLineNumber());
        }

        NopolContext nopolContext = nopolInformation.getNopolContext();
        result.addProperty("nopolContext", "localizer: " + nopolContext.getLocalizer().name() + ";solver: " + nopolContext.getSolver().name()
                + ";synthetizer: " + nopolContext.getSynthesis().name() + ";type: " + nopolContext.getType().name());
        result.addProperty("nbAngelicValues", nopolInformation.getNbAngelicValues());
        result.addProperty("nbStatements", nopolInformation.getNbStatements());
        result.addProperty("ignoreStatus", nopolInformation.getIgnoreStatus().name());
        result.addProperty("runId", buildToBeInspected.getRunId());

        return result;
    }

    @Override
    public void serializeData(ProjectInspector inspector){
        if (inspector.getJobStatus().getNopolInformations() != null) {
            BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();

            List<SerializedData> allDatas = new ArrayList<>();

            for (NopolInformation nopolInformation : inspector.getJobStatus().getNopolInformations()) {
                if (nopolInformation.getPatches().isEmpty()) {
                    SerializedData data = new SerializedData(this.serializeNopolInfoAsList(buildToBeInspected, nopolInformation, null, 0),
                            this.serializeNopolInfoAsJson(buildToBeInspected, nopolInformation, null, 0));
                    allDatas.add(data);
                } else {
                    int patchNumber = 1;

                    for (PatchAndDiff patchAndDiff : nopolInformation.getPatches()) {
                        SerializedData data = new SerializedData(this.serializeNopolInfoAsList(buildToBeInspected, nopolInformation, patchAndDiff, patchNumber),
                                this.serializeNopolInfoAsJson(buildToBeInspected, nopolInformation, patchAndDiff, patchNumber));
                        allDatas.add(data);
                        patchNumber++;
                    }
                }

                for (SerializerEngine engine : this.getEngines()) {
                    engine.serialize(allDatas, this.getType());
                }
            }
        }
    }
}
