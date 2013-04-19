package com.soundcloud.api;

/**
 * OAuth2 parameters
 */
public interface AuthParams {
    String GRANT_TYPE       = "grant_type";
    String CLIENT_ID        = "client_id";
    String CLIENT_SECRET    = "client_secret";
    String USERNAME         = "username";
    String PASSWORD         = "password";
    String REDIRECT_URI     = "redirect_uri";
    String CODE             = "code";
    String REFRESH_TOKEN    = "refresh_token";
    String RESPONSE_TYPE    = "response_type";
}
