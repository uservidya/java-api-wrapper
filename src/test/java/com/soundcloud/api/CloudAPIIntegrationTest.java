package com.soundcloud.api;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CloudAPIIntegrationTest implements Params.Track, Endpoints {
    // http://sandbox-soundcloud.com/you/apps/java-api-wrapper-test-app
    // user: api-testing
    static final String CLIENT_ID     = "yH1Jv2C5fhIbZfGTpKtujQ";
    static final String CLIENT_SECRET = "C6o8jc517b6PIw0RKtcfQsbOK3BjGpxWFLg977UiguY";

    CloudAPI api;

    /*
    To get full HTTP logging, add the following system properties:
    -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
    -Dorg.apache.commons.logging.simplelog.showdatetime=true
    -Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG
    -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=ERROR
    */

    @Before
    public void setUp() throws Exception {
        api = new ApiWrapper(
                CLIENT_ID,
                CLIENT_SECRET,
                null,
                null,
                Env.SANDBOX);
    }

    private Token login() throws IOException {
        return login(null);
    }

    private Token login(String scope) throws IOException {
        return api.login("api-testing", "testing", scope);
    }

    @Test
    public void shouldUploadASimpleAudioFile() throws Exception {
        login();
        HttpResponse resp = api.post(Request.to(TRACKS).with(
                  TITLE, "Hello Android",
                  POST_TO_EMPTY, "")
                .withFile(ASSET_DATA, new File(getClass().getResource("hello.aiff").getFile())));

        int status = resp.getStatusLine().getStatusCode();
        assertThat(status, is(201));
    }

    @Test
    public void shouldUploadASimpleAudioFileBytes() throws Exception {
        login();

        File f = new File(getClass().getResource("hello.aiff").getFile());
        ByteBuffer bb = ByteBuffer.allocate((int) f.length());
        FileInputStream fis = new FileInputStream(f);
        for (;;) if (fis.getChannel().read(bb) <= 0) break;

        HttpResponse resp = api.post(Request.to(TRACKS).with(
                  TITLE, "Hello Android",
                  POST_TO_EMPTY, "")
                .withFile(ASSET_DATA, bb, "hello.aiff"));

        int status = resp.getStatusLine().getStatusCode();
        assertThat(status, is(201));
    }


    @Test(expected = IOException.class)
    public void shouldNotGetASignupTokenWhenInofficialApp() throws Exception {
        login();
        api.clientCredentials();
    }

    @Test
    public void shouldReturn401WithInvalidToken() throws Exception {
        login();
        api.setToken(new Token("invalid", "invalid"));
        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(401));
    }

    @Test
    public void shouldRefreshAutomaticallyWhenTokenExpired() throws Exception {
        login();

        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(200));

        final Token oldToken = api.getToken();

        assertThat(api.invalidateToken(), is(nullValue()));

        resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(200));
        // make sure we've got a new token
        assertThat(oldToken, not(equalTo(api.getToken())));
    }

    @Test
    public void shouldResolveUrls() throws Exception {
        login();

        long id = api.resolve("http://sandbox-soundcloud.com/api-testing");
        assertThat(id, is(1862213L));

        try {
            id = api.resolve("http://sandbox-soundcloud.com/i-do-no-exist");
            fail("expected resolver exception, got: "+id);
        } catch (CloudAPI.ResolverException e) {
            // expected
            assertThat(e.getStatusCode(), is(404));
        }
    }

    @Test
    public void shouldResolveStreamUrls() throws Exception {
        login();

        Stream resolved = api.resolveStreamUrl("https://api.sandbox-soundcloud.com/tracks/2100832/stream", false);

        assertThat(resolved.url, equalTo("https://api.sandbox-soundcloud.com/tracks/2100832/stream"));
        assertThat(resolved.streamUrl, containsString("http://ak-media.soundcloud.com/"));

        assertTrue("expire should be in the future", resolved.expires > System.currentTimeMillis());
        assertThat(resolved.eTag, equalTo("\"1298a3c38b12dc055ad0f7beb956bc56\""));
    }

    @Test @Ignore /* playcounts not deployed on sandbox */
    public void shouldResolveStreamUrlAndSkipPlaycountLogging() throws Exception {
        // need the playcount scope for this to work
        assertTrue(login(Token.SCOPE_PLAYCOUNT).scoped(Token.SCOPE_PLAYCOUNT));

        int count = Http.getJSON(api.get(Request.to("/tracks/2100832"))).getInt("playback_count");
        api.resolveStreamUrl("https://api.sandbox-soundcloud.com/tracks/2100832/stream", false);
        int count2 = Http.getJSON(api.get(Request.to("/tracks/2100832"))).getInt("playback_count");

        assertTrue(String.format("%d !> %d", count2, count), count2 > count);

        // resolve again, this time skipping count
        api.resolveStreamUrl("https://api.sandbox-soundcloud.com/tracks/2100832/stream", true);

        int count3 = Http.getJSON(api.get(Request.to("/tracks/2100832"))).getInt("playback_count");
        assertTrue(String.format("%d != %d", count3, count2), count3 == count2);
    }

    @Test
    public void shouldThrowResolverExceptionWhenStreamCannotBeResolved() throws Exception {
        login();
        try {
            Stream s = api.resolveStreamUrl("https://api.sandbox-soundcloud.com/tracks/999919191/stream", false);
            fail("expected resolver exception, got: "+s);
        } catch (CloudAPI.ResolverException e) {
            // expected
            assertThat(e.getStatusCode(), is(404));
        }
    }

    @Test
    public void shouldSupportRangeRequest() throws Exception {
        login();

        Stream resolved = api.resolveStreamUrl("https://api.sandbox-soundcloud.com/tracks/2100832/stream", false);
        assertThat(resolved.contentLength, is(19643L));

        HttpResponse resp = api
                .getHttpClient()
                .execute(resolved.streamUrl().range(50, 100).buildRequest(HttpGet.class));

        assertThat(resp.getStatusLine().toString(), resp.getStatusLine().getStatusCode(), is(206));
        Header range = resp.getFirstHeader("Content-Range");
        assertThat(range, notNullValue());
        assertThat(range.getValue(), equalTo("bytes 50-100/19643"));
        assertThat(resp.getEntity().getContentLength(), is(51L));
    }

    @Test
    public void readMyDetails() throws Exception {
        login();

        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(200));

        assertThat(
                resp.getFirstHeader("Content-Type").getValue(),
                containsString("application/json"));

        JSONObject me = Http.getJSON(resp);

        assertThat(me.getString("username"), equalTo("api-testing"));
        // writeResponse(resp, "me.json");
    }

    @Test
    public void shouldLoginWithNonExpiringScope() throws Exception {
        Token token = login(Token.SCOPE_NON_EXPIRING);
        assertThat(token.scoped(Token.SCOPE_NON_EXPIRING), is(true));
        assertThat(token.refresh, is(nullValue()));
        assertThat(token.getExpiresIn(), is(nullValue()));
        assertThat(token.valid(), is(true));

        // make sure we can issue a request with this token
        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void shouldNotRefreshWithNonExpiringScope() throws Exception {
        Token token = login(Token.SCOPE_NON_EXPIRING);
        assertThat(token.scoped(Token.SCOPE_NON_EXPIRING), is(true));
        assertThat(api.invalidateToken(), is(nullValue()));
        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(401));
    }

    @Test
    public void shouldChangeContentType() throws Exception {
        login();

        api.setDefaultContentType("application/xml");
        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));

        assertThat(
                resp.getFirstHeader("Content-Type").getValue(),
                containsString("application/xml"));
    }


    @Test
    public void shouldSupportConditionalGets() throws Exception {
        login();

        HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
        assertThat(resp.getStatusLine().getStatusCode(), is(200) /* ok */);
        String etag = Http.etag(resp);
        assertNotNull(etag);

        resp = api.get(Request.to(Endpoints.MY_DETAILS).ifNoneMatch(etag));
        assertThat(resp.getStatusLine().getStatusCode(), is(304) /* not-modified */);
    }


    @Test @Ignore
    public void shouldSupportConcurrentConnectionsToApiHost() throws Exception {
        login();

        int num = 20;
        final CyclicBarrier start = new CyclicBarrier(num, new Runnable() {
            @Override
            public void run() {
                System.err.println("starting...");
            }
        });
        final CyclicBarrier end = new CyclicBarrier(num);
        while (num-- > 0) {
            new Thread("t-"+num) {
                @Override public void run() {
                    try {
                        start.await();
                        System.err.println("running: "+toString());
                        try {
                            HttpResponse resp = api.get(Request.to(Endpoints.MY_DETAILS));
                            resp.getEntity().consumeContent();
                            assertThat(resp.getStatusLine().getStatusCode(), is(200));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            System.err.println("finished: "+toString());
                            end.await();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        start.await();
        end.await();
        System.err.println("all threads finished");
    }

    @Test @Ignore
    public void updateMyDetails() throws Exception {
        Request updateMe = Request.to(MY_DETAILS).with(
                Params.User.WEBSITE, "http://mywebsite.com")
                .withFile(Params.User.AVATAR, new File(getClass().getResource("cat.jpg").getFile()));

        HttpResponse resp = api.put(updateMe);
        assertThat(resp.getStatusLine().getStatusCode(), is(200));
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void writeResponse(HttpResponse resp, String file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        InputStream is = resp.getEntity().getContent();
        byte[] b = new byte[8192];
        int n;

        while ((n = is.read(b)) >= 0) fos.write(b, 0, n);
        is.close();
        fos.close();
    }
}
