## 1.1.3-SNAPSHOT

  * Handle more broken HTTP client behaviour
  * Remove sandbox environment (it is no longer supported)

## 1.1.2 2012-10-04

  * Compatibility fixes with broken httpclient versions used in Android pre-gingerbread
  * Add support for default parameters

## 1.1.1 2012-04-30

  * Respect system proxy settings
  * Added skip_logging parameter to resolveStreamUrl
  * Added workarounds for some HTTPClient bugs
  * Fixed some Android compatibility problems (IOException constructor)

## 1.1.0 2011-11-09

  * Support httpmime-4.1.x (GH-2)
  * Support for conditional GETs
  * Support for Range requests
  * added CloudApi#resolveStreamUrl(String)
  * added CloudApi#getHttpClient()
  * Changed the handling of max connections per route
  * Added some endpoints
  * Added PostResource example
  * Added support for HEAD requests
  * Added stream resolving
  * Added Facebook login example

## 1.0.1 2011-07-04

  * Support for non-expiring scope
  * Cancellation of uploads
  * Added Request#withFile(String, byte[]), Request#withFile(String, ByteBuffer)
  * Added Request#withEntity(HttpEntity) and Request#withContent(String, String)
  * Added PutResource example
  * Added setting to change the default content type for requests
    (CloudAPI#setDefaultContentType(String))

## 1.0.0 2011-05-19

  * Initial release
