package com.soundcloud.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHost;
import org.junit.Test;


public class EnvTest {
    @Test
    public void testIsApiHost() throws Exception {
        assertTrue(Env.LIVE.isApiHost(new HttpHost("api.soundcloud.com", 80, "http")));
        assertTrue(Env.LIVE.isApiHost(new HttpHost("api.soundcloud.com", 443, "https")));
        assertFalse(Env.LIVE.isApiHost(new HttpHost("foo.soundcloud.com", 443, "https")));
    }
}
