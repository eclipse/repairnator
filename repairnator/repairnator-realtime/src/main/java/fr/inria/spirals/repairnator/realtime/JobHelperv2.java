package fr.inria.spirals.repairnator.realtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fr.inria.jtravis.API_VERSION;
import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.TravisConstants;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.jtravis.helpers.JobHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

class JobHelperv2 extends JobHelper {
    JobHelperv2(JTravis jTravis) {
        super(jTravis);
    }

    public Optional<List<JobV2>> allFromV2() {
        String url = "/" + TravisConstants.JOBS_ENDPOINT;
        try {
            String response = this.get(url, API_VERSION.v2, TravisConstants.DEFAULT_NUMBER_OF_RETRY);
            JsonObject jsonObj = getJsonFromStringContent(response);
            JsonArray jsonArray = jsonObj.getAsJsonArray("jobs");
            List<JobV2> result = new ArrayList<>();

            for (JsonElement jsonElement : jsonArray) {
                result.add(createGson().fromJson(jsonElement, JobV2.class));
            }

            return Optional.of(result);
        } catch (IOException |JsonSyntaxException e) {
            this.getLogger().error("Error while getting jobs from V2 API", e);
        }

        return Optional.empty();
    }

    public List<JobV2> allSubSequentJobsFrom(int start) throws Exception {

        List<String> l = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            l.add(Integer.toString(start + i));
        }
        //?ids[]=' + job_ids.join('&ids[]='
        String sep = "ids[]=";
        String url = "/" + "jobs"
                + "?" + sep
                + StringUtils.join(l, "&" + sep);
        String response = this.get(url, API_VERSION.v2, 2);
        JsonObject jsonObj = getJsonFromStringContent(response);
        JsonArray jsonArray = jsonObj.getAsJsonArray("jobs");
        List<JobV2> result = new ArrayList();
        Iterator var6 = jsonArray.iterator();

        while (var6.hasNext()) {
            JsonElement jsonElement = (JsonElement) var6.next();
            result.add(createGson().fromJson(jsonElement, JobV2.class));
        }
        //System.out.println(result.size());
        return result;

    }


    public List<Build> allSubSequentBuildsFrom(int start) throws Exception {

        List<String> l = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            l.add(Integer.toString(start + i));
        }
        //?ids[]=' + job_ids.join('&ids[]='
        String sep = "ids[]=";
        String url = "/" + "builds"
                + "?" + sep
                + StringUtils.join(l, "&" + sep);
        String response = this.get(url, API_VERSION.v2, 2);
        JsonObject jsonObj = getJsonFromStringContent(response);
        JsonArray jsonArray = jsonObj.getAsJsonArray("builds");
        List<Build> result = new ArrayList();
        Iterator var6 = jsonArray.iterator();

        while (var6.hasNext()) {
            JsonElement jsonElement = (JsonElement) var6.next();
            result.add(createGson().fromJson(jsonElement, Build.class));
        }
        //System.out.println(result.size());
        return result;

    }

}
