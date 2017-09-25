package fr.inria.spirals.jtravis.helpers;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by urli on 21/12/2016.
 */
public class RepositoryHelperTest {

    MockWebServer server;

    @After
    public void tearDown() {
        if (server != null) {
            try {
                server.shutdown();
            } catch (IOException e) {
            }
        }
        RepositoryHelper.getInstance().setEndpoint(AbstractHelper.TRAVIS_API_ENDPOINT);
    }

    @Test
    public void testGetSpoonRepoFromSlugWorks() throws IOException {
        if(Version.getVersionV3()) {
            Repository spoonRepo = RepositoryHelper.getRepositoryFromSlug("INRIA/spoon");

            assertEquals(2800492, spoonRepo.getId());
        }
        else {
            server = new MockWebServer();
            String mockAnswer = "{\"repo\":{\"id\":2800492,\"slug\":\"INRIA/spoon\",\"active\":true,\"description\":\"Spoon is a library to analyze, rewrite, transform, transpile Java source code. It parses source files to build a well-designed AST with powerful analysis and transformation API. It fully supports Java 8. Made at Inria with :heart:, :beers: and :sparkles:\",\"last_build_id\":190205141,\"last_build_number\":\"2433\",\"last_build_state\":\"started\",\"last_build_duration\":null,\"last_build_language\":null,\"last_build_started_at\":\"2017-01-09T10:28:08Z\",\"last_build_finished_at\":null,\"github_language\":null}}";
            server.enqueue(new MockResponse().setBody(mockAnswer));

            server.start();
            HttpUrl baseUrl = server.url("/repos/INRIA/spoon");

            RepositoryHelper.getInstance().setEndpoint(baseUrl.toString());
            Repository spoonRepo = RepositoryHelper.getRepositoryFromSlug("INRIA/spoon");

            assertEquals("INRIA/spoon", spoonRepo.getSlug());
            assertEquals(2800492, spoonRepo.getId());
            assertTrue(spoonRepo.getLastBuildId() > 0);
        }
    }

    @Test
    public void testGetSpoonRepoFromIdWorks() {
        Repository spoonRepo = RepositoryHelper.getRepositoryFromId(2800492);

        assertEquals("INRIA/spoon",spoonRepo.getSlug());
        assertEquals(2800492, spoonRepo.getId());
        //assertTrue(spoonRepo.getLastBuildId() > 0);
    }

    @Test
    public void testGetUnknownRepoThrowsException() {
        Repository unknownRepo = RepositoryHelper.getRepositoryFromSlug("surli/unknown");
        assertTrue(unknownRepo == null);
    }
}
