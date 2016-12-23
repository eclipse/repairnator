package fr.inria.spirals.jtravis.helpers;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * This abstract helper is the base helper for all the others.
 * It defines constants for Travis CI API and methods to do requests and to parse them.
 *
 * @author Simon Urli
 */
public abstract class AbstractHelper {
    public final static String TRAVIS_API_ENDPOINT="https://api.travis-ci.org/";

    private final static String USER_AGENT = "MyClient/1.0.0";
    private final static String ACCEPT_APP = "application/vnd.travis-ci.2+json";
    private OkHttpClient client;

    public AbstractHelper() {
        client = new OkHttpClient();
    }

    private Request.Builder requestBuilder(String url) {
        return new Request.Builder().header("User-Agent",USER_AGENT).header("Accept", ACCEPT_APP).url(url);
    }

    private void checkResponse(Response response) throws IOException {
        if (response.code() != 200) {
            throw new IOException("The response answer is not 200: "+response.code()+" "+response.message());
        }
    }

    protected ResponseBody rawGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Call call = this.client.newCall(request);
        Response response = call.execute();
        checkResponse(response);
        return response.body();
    }

    protected ResponseBody get(String url) throws IOException {
        Request request = this.requestBuilder(url).get().build();
        Call call = this.client.newCall(request);

        Response response = call.execute();
        checkResponse(response);
        return response.body();
    }

    protected static Gson createGson() {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

}
