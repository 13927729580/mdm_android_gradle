
package com.lightspeedsystems.mdm;


import android.util.Pair;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lightspeedsystems.mdm.util.LSLogger;

import org.json.JSONObject;

/**
 * Base HTTP/HTTPS communications support with HTTP servers. 
 * Handles GET and POST methods.
 * Uses HttpCommResponse for handling results data and errors.
 * 
 * Note. In some places, an explicit check is made to see if logging is enabled. This is so that
 *  any data formatting of protected data is only needed and done for logging of results, and as
 *  such, we need to know before doing the data formatting if logging is needed, and as a result,
 *  faster performance is acheived in most cases when logging is off.
 * 
 * @author Mike Zrubek
 */
public class HttpComm {
	
	private static String TAG = "HttpComm";

	private static int connectionTimeoutSecs = 15; // default server connection timeout, in seconds
	private static int socketTimeoutSecs = 30; // default server data transfer timeout, in seconds
	
	private boolean bIncludeHeaderAuth = false;
	private String authTokenKey = null;
	
//..https sample... http://www.java-samples.com/java/POST-toHTTPS-url-free-java-sample-program.htm	
	
	/**
	 * Creates a HttpComm instance.
	 */
	public HttpComm() {
		// nothing to do in the constructor.
	}
	
	
	public void setIncludeHeaderAuth(boolean bInclude) {
		bIncludeHeaderAuth = bInclude;
	}
	
	public void setAuthTokenKey(String authKey) {
		authTokenKey = authKey;
	}
	
	// ---- public signature interface methods for various calls to posting ---:
	
	/**
	 * Initializes static configurable communications parameters from settings. 
	 * Currently consists of: connection timeout value; data reading timeout value.
	 * @param settings instance of Settings to obtain values from.
	 */
	public static void initFromSettings(Settings settings) {
		if (settings != null) {
			connectionTimeoutSecs = settings.getSettingInt(Settings.SVRCONNECTTIMEOUT, connectionTimeoutSecs);
			socketTimeoutSecs = settings.getSettingInt(Settings.SVRCONNECTREADTIMEOUT, socketTimeoutSecs);
		}
	}
	
	
	// Http action convenience methods:
	
	/*
	 * Sends data to a server via HTTP post.
	 * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
	 * @param sparams optional string of name-value pair parameters, separated by "&".
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
    public int sendPost(String serverUrl, String sparams) {
    	return sendPost(serverUrl, sparams, null, null);
    }
	
	/*
	 * Sends data to a server via HTTP post.
	 * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
	 * @param sparams optional string of name-value pair parameters, separated by "&".
	 * @param responseData optional target for receiving and returning detailed results and data.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
    public int sendPost(String serverUrl, String sparams, HttpCommResponse responseData) {
    	return sendPost(serverUrl, sparams, null, responseData);
    }
	
	/*
	 * Sends data to a server via HTTP post.
	 * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
	 * @param jparams optional set of JSON parameters; can include nested JSON objects and arrays.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
	public int sendPost(String serverUrl, JSONObject jparams) {
		return sendPost(serverUrl, null, jparams, null);
	}
	
	/*
	 * Sends data to a server via HTTP post.
	 * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
	 * @param jparams optional set of JSON parameters; can include nested JSON objects and arrays.
	 * @param responseData optional target for receiving and returning detailed results and data.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
	public int sendPost(String serverUrl, JSONObject jparams, HttpCommResponse responseData) {
		return sendPost(serverUrl, null, jparams, responseData);
	}
	
	
	
	/*
	 * Gets data from a server or sends a command to a server using a HTTP get request.
	 * @param serverUrl server url to send to. Includes protocol://server_address:port/specific_target.
	 * @param sparams optional string of name-value pair parameters, separated by "&".
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
    public int sendGet(String serverUrl, String sparams) {
    	return sendGet(serverUrl, sparams, null, null);
    }
	
	/*
	 * Gets data from a server or sends a command to a server using a HTTP get request.
	 * @param serverUrl server url to send to. Includes protocol://server_address:port/specific_target.
	 * Command parameters can be included at the end of the sererURL.
	 * @param responseData optional target for receiving and returning detailed results and data.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
    public int sendGet(String serverUrl, HttpCommResponse responseData) {
    	return sendGet(serverUrl, null, null, responseData);
    }
    
	/*
	 * Gets data from a server or sends a command to a server using a HTTP get request.
	 * @param serverUrl server url to send to. Includes protocol://server_address:port/specific_target.
	 * @param sparams optional string of name-value pair parameters, separated by "&".
	 * @param responseData optional target for receiving and returning detailed results and data.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
    public int sendGet(String serverUrl, String sparams, HttpCommResponse responseData) {
    	return sendGet(serverUrl, sparams, null, responseData);
    }
	
	/*
	 * Gets data from a server or sends a command to a server using a HTTP get request.
	 * @param serverUrl server url to send to. Includes protocol://server_address:port/specific_target.
	 * @param jparams optional set of JSON parameters.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
	public int sendGet(String serverUrl, JSONObject jparams) {
		return sendGet(serverUrl, null, jparams, null);
	}
	
	/*
	 * Gets data from a server or sends a command to a server using a HTTP get request.
	 * @param serverUrl server url to send to. Includes protocol://server_address:port/specific_target.
	 * @param jparams optional set of JSON parameters.
	 * @param responseData optional target for receiving and returning detailed results and data.
	 * @return http response code, 0 if there was no response received or if an exception occurred.
	 */
	public int sendGet(String serverUrl, JSONObject jparams, HttpCommResponse responseData) {
		return sendGet(serverUrl, null, jparams, responseData);
	}


    // --------------------------------------------------------------------------------------------
    // -----  Internal methods  -----
    // --------------------------------------------------------------------------------------------

    /*
     * Internal method for handling Post with various parameters and options.
     * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
     * @param sparams optional string of name-value pair parameters, separated by "&".
     * @param jparams optional set of JSON parameters; can include nested JSON objects and arrays.
     * @param responseData optional target for receiving and returning detailed results and data.
     * @return http response code, 0 if there was no response received or if an exception occurred.
     */
    /*
    private int sendPostOld(String serverUrl, String sparams, JSONObject jparams, HttpCommResponse responseData) {
        int responsecode = 0;
        HttpParams httpParams = createDefaultHttpParams();
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpResponse response = null;

        boolean doInstrument = (responseData != null && responseData.isInstrumented());

        try {
            if (doInstrument) responseData.instrumentStartTime();
            LSLogger.debug(TAG, "Initiating Post to server " + serverUrl
                    + " (connect timeout="+Integer.toString(connectionTimeoutSecs)
                    + " seconds; socket timeout="+Integer.toString(socketTimeoutSecs)+" seconds)");

            HttpPost httpPost = new HttpPost(serverUrl);
            if (jparams != null) {
                StringEntity reqEntity = new StringEntity(jparams.toString());
                reqEntity.setContentType("application/json");
                if (reqEntity.getContentLength() > 0) {
                    httpPost.setEntity(reqEntity);
                    if (LSLogger.isLoggingEnabled())
                        LSLogger.debug(TAG, "-Post param json-data: " + Utils.filterProtectedContent(jparams.toString()));
                }
            } else if (sparams != null) {
                StringEntity reqEntity = new StringEntity(sparams);
                reqEntity.setContentType("application/text");
                if (reqEntity.getContentLength() > 0) {
                    httpPost.setEntity(reqEntity);
                    if (LSLogger.isLoggingEnabled())
                        LSLogger.debug(TAG, "-Post param string-data: " + Utils.filterProtectedContent(sparams));
                }
            }
            httpPost.getParams();
            if (bIncludeHeaderAuth && authTokenKey != null) {
                LSLogger.debug(TAG, "Set the POST auth header.");
                httpPost.addHeader(getAuthHeader());
            }


            LSLogger.debug(TAG, "-Executing Post request: " + httpPost.getRequestLine());
            if (doInstrument) responseData.instrumentSendStartTime();
            response = httpclient.execute(httpPost);
            if (doInstrument) responseData.instrumentSendEndTime();

            if (responseData != null) {
                if (doInstrument) responseData.instrumentReadStartTime();
                responsecode = responseData.setResults(response);
                if (doInstrument) responseData.instrumentReadEndTime();
                if (LSLogger.isLoggingEnabled()) {
                    LSLogger.debug(TAG, "-Post Response =" + responseData.toString());
                    LSLogger.debug(TAG, "-Post data: " +
                            Utils.filterProtectedContent(responseData.getResultStr()));
                }
            } else {
                responsecode = response.getStatusLine().getStatusCode();
                LSLogger.debug(TAG, "-Post Response code =" + responsecode);
            }

        }
        catch (Exception e) {
            handleExceptions("POST", e, responseData);
        }
        finally {
            httpclient.getConnectionManager().shutdown();
            if (doInstrument) responseData.instrumentEndTime();
        }

        return responsecode;
    }
    */

    // --------------------------------------------------------------------------------------------
    // -----  Internal methods  -----
    // --------------------------------------------------------------------------------------------

    /*
     * Internal method for handling Post with various parameters and options.
     * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
     * @param sparams optional string of name-value pair parameters, separated by "&".
     * @param jparams optional set of JSON parameters; can include nested JSON objects and arrays.
     * @param responseData optional target for receiving and returning detailed results and data.
     * @return http response code, 0 if there was no response received or if an exception occurred.
     */
    private int sendPost(String serverUrl, String sparams, JSONObject jparams, HttpCommResponse responseData) {
        if (responseData == null) // although it makes no sense to pass in null for responsedata, it can occur.
            responseData = new HttpCommResponse();

        boolean doInstrument = (responseData != null && responseData.isInstrumented());

        try {
            if (doInstrument) responseData.instrumentStartTime();
            LSLogger.debug(TAG, "Initiating Post to server " + serverUrl
                    + " (connect timeout="+Integer.toString(connectionTimeoutSecs)
                    + " seconds; socket timeout="+Integer.toString(socketTimeoutSecs)+" seconds)");

            List<Pair<String,String>> headers = new ArrayList<Pair<String,String>>();
            if (bIncludeHeaderAuth && authTokenKey != null) {
                LSLogger.debug(TAG, "Set the POST auth header.");
                getAuthHeader(headers);
            }
            String postData = null;

            if (jparams != null) {
                postData = jparams.toString();
                headers.add(new Pair<String, String>("Content-Type", "application/json"));
                headers.add(new Pair<String, String>("Content-Length", Integer.toString(postData.length())));
                headers.add(new Pair<String, String>("Content-Language", "en-US"));

                if (LSLogger.isLoggingEnabled())
                    LSLogger.debug(TAG, "-Post param json-data: " + Utils.filterProtectedContent(jparams.toString()));
            } else if (sparams != null) {
                postData = sparams;
                headers.add(new Pair<String, String>("Content-Type", "application/text"));
                headers.add(new Pair<String, String>("Content-Length", Integer.toString(postData.length())));
                headers.add(new Pair<String, String>("Content-Language", "en-US"));

                if (LSLogger.isLoggingEnabled())
                    LSLogger.debug(TAG, "-Post param string-data: " + Utils.filterProtectedContent(sparams));
            }

            LSLogger.debug(TAG, "-Executing Post request: " + serverUrl);

            if (doInstrument) responseData.instrumentSendStartTime();
            InputStream inputStream = responseData.postUrlConnection(serverUrl, headers, postData);
            if (doInstrument) responseData.instrumentSendEndTime();
            if (doInstrument) responseData.instrumentReadStartTime();
            responseData.setResults(inputStream);
            if (doInstrument) responseData.instrumentReadEndTime();

            if (LSLogger.isLoggingEnabled()) {
                LSLogger.debug(TAG, "-Post Response =" + responseData.toString());
                LSLogger.debug(TAG, "-Post data: " +
                        Utils.filterProtectedContent(responseData.getResultStr()));
                LSLogger.debug(TAG, "-Post Response code =" + responseData.getResultCode());
            }

        }
        catch (Exception e) {
            handleExceptions("POST", e, responseData);
        }
        finally {
            responseData.disconnectUrlConnection();
            if (doInstrument) responseData.instrumentEndTime();
        }

        return responseData.getResultCode();
    }


    /*
     * Internal method for handling a Get with various parameters and options.
     * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
     * @param sparams optional string of name-value pair parameters, separated by "&".
     * @param jparams optional set of JSON parameters; can include nested JSON objects and arrays.
     * Ignores sparams if jparams is not null.
     * @param responseData optional target for receiving and returning detailed results and data.
     *  (Although this is optional, it seldom makes sense to call this w/o this instance, since usually,
     *  some result data is required. However, if only the result code is needed, null would be useful here.)
     * @return http response code, 0 if there was no response received or if an exception occurred.
     */
    private int sendGet(String serverUrl, String sparams, JSONObject jparams, HttpCommResponse responseData) {
        if (responseData == null) // although it makes no sense to pass in null for responsedata, it can occur.
            responseData = new HttpCommResponse();
        boolean doInstrument = (responseData.isInstrumented());

        if (doInstrument)
            responseData.setTimeouts(connectionTimeoutSecs, socketTimeoutSecs);

        try {
            if (doInstrument) responseData.instrumentStartTime();
            LSLogger.debug(TAG, "Initiating new Get to server " + serverUrl
                    + " (connect timeout="+Integer.toString(connectionTimeoutSecs)
                    + " seconds; socket timeout="+Integer.toString(socketTimeoutSecs)+" seconds)");

            if (jparams != null)
                sparams = jsonParamsToStringParams(jparams);

            if (sparams != null && sparams.length()>0) {
                // append string params to the server url: insert param separator character as needed
                if (!serverUrl.endsWith("?") && sparams.indexOf(0)!='?')
                    serverUrl = serverUrl + "?" + sparams;
                else
                    serverUrl += sparams;
            }

            List<Pair<String,String>> headers = null;
            if (bIncludeHeaderAuth && authTokenKey != null) {
                LSLogger.debug(TAG, "Set the GET auth header.");
                headers = new ArrayList<Pair<String,String>>();
                getAuthHeader(headers);
            }

            if (LSLogger.isLoggingEnabled())
                LSLogger.debug(TAG, "-Executing New Get request: " +
                        Utils.filterProtectedContent(serverUrl));


            if (doInstrument) responseData.instrumentSendStartTime();
            InputStream inputStream = responseData.openUrlConnection(serverUrl,headers);
            if (doInstrument) responseData.instrumentSendEndTime();
            if (doInstrument) responseData.instrumentReadStartTime();
            responseData.setResults(inputStream);
            if (doInstrument) responseData.instrumentReadEndTime();
            if (LSLogger.isLoggingEnabled()) {
                LSLogger.debug(TAG, "-Get Response =" + responseData.toString());
                //LSLogger.info(TAG, "-Get data: " + Utils.filterProtectedContent(responseData.getResultStr()));
            }
        }
        catch (Exception e) {
            handleExceptions("GET", e, responseData);
        }
        finally {
            responseData.disconnectUrlConnection();
            if (doInstrument) responseData.instrumentEndTime();
        }
        return responseData.getResultCode();
    }

    /*
     * Internal method for handling a Get with various parameters and options.
     * @param serverUrl server url to post to. Includes protocol://server_address:port/specific_target.
     * @param sparams optional string of name-value pair parameters, separated by "&".
     * @param jparams optional set of JSON parameters; can include nested JSON objects and arrays.
     * Ignores sparams if jparams is not null.
     * @param responseData optional target for receiving and returning detailed results and data.
     *  (Although this is optional, it seldom makes sense to call this w/o this instance, since usually,
     *  some result data is required. However, if only the result code is needed, null would be useful here.)
     * @return http response code, 0 if there was no response received or if an exception occurred.
     */
    /*
    private int sendGetOld(String serverUrl, String sparams, JSONObject jparams, HttpCommResponse responseData) {
        if (responseData == null) // although it makes no sense to pass in null for responsedata, it can occur.
            responseData = new HttpCommResponse();
        boolean doInstrument = (responseData.isInstrumented());
        HttpParams httpParams = createDefaultHttpParams();
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpResponse response = null;

        if (doInstrument)
            responseData.setTimeouts(connectionTimeoutSecs, socketTimeoutSecs);

        try {
            if (doInstrument) responseData.instrumentStartTime();
            LSLogger.debug(TAG, "Initiating Get to server " + serverUrl
                    + " (connect timeout="+Integer.toString(connectionTimeoutSecs)
                    + " seconds; socket timeout="+Integer.toString(socketTimeoutSecs)+" seconds)");

            if (jparams != null)
                sparams = jsonParamsToStringParams(jparams);

            if (sparams != null && sparams.length()>0) {
                // append string params to the server url: insert param separator character as needed
                if (!serverUrl.endsWith("?") && sparams.indexOf(0)!='?')
                    serverUrl = serverUrl + "?" + sparams;
                else
                    serverUrl += sparams;
            }

            HttpGet httpGet = new HttpGet(serverUrl);
            if (bIncludeHeaderAuth && authTokenKey != null) {
                LSLogger.debug(TAG, "Set the GET auth header.");
                httpGet.addHeader(getAuthHeader());
            }

            if (LSLogger.isLoggingEnabled())
                LSLogger.debug(TAG, "-Executing Get request: " +
                        Utils.filterProtectedContent(httpGet.getRequestLine().toString()));
            if (doInstrument) responseData.instrumentSendStartTime();
            response = httpclient.execute(httpGet);
            if (doInstrument) responseData.instrumentSendEndTime();
            if (doInstrument) responseData.instrumentReadStartTime();
            responseData.setResults(response);
            if (doInstrument) responseData.instrumentReadEndTime();
            if (LSLogger.isLoggingEnabled()) {
                LSLogger.debug(TAG, "-Get Response =" + responseData.toString());
                //LSLogger.info(TAG, "-Get data: " + Utils.filterProtectedContent(responseData.getResultStr()));
            }
        }
        catch (Exception e) {
            handleExceptions("GET", e, responseData);
        }
        finally {
            httpclient.getConnectionManager().shutdown();
            if (doInstrument) responseData.instrumentEndTime();
        }
        return responseData.getResultCode();
    }
    */

    /**
	 * Downloads a file from a server.
	 * @param serverUrl URL to remote file
	 * @param targetPath local file to save to
	 * @return HttpCommResponse with details of success or failure. Upon success, downloaded contents are
	 * written to the targetPath file.
	 */
	public HttpCommResponse downloadFile(String serverUrl, String targetPath) {
		HttpCommResponse responseData = new HttpCommResponse(false);
        boolean doInstrument = (responseData.isInstrumented());

        if (doInstrument)
            responseData.setTimeouts(connectionTimeoutSecs, socketTimeoutSecs);

        if (doInstrument) responseData.instrumentStartTime();
        if (LSLogger.isLoggingEnabled())
            LSLogger.debug(TAG, "Initiating new FileDownload-Get to server " + serverUrl
                    + " (connect timeout="+Integer.toString(connectionTimeoutSecs)
                    + " seconds; socket timeout="+Integer.toString(socketTimeoutSecs)+" seconds)");

		InputStream inStream = responseData.openUrlConnection(serverUrl, null);
		if (inStream != null) {
			try {
				FileOutputStream outStream = new FileOutputStream(targetPath);
				try {
					int bytesRead = -1;
					byte[] buffer = new byte[4096];
					while ((bytesRead = inStream.read(buffer)) != -1) {
						outStream.write(buffer, 0, bytesRead);
					}
				} catch (IOException ioe) {
				}

				try {
					outStream.close();
				} catch (IOException ioe) {
				}
			} catch (FileNotFoundException fe) {

			} finally {
				try {
					inStream.close();
				} catch (IOException ioe) {
				}
				responseData.disconnectUrlConnection();
			}

            if (doInstrument) responseData.instrumentEndTime();
        }
		return responseData;
	}

		/**
         * Downloads a file from a server.
    //     * @param serverUrl URL to remote file
    //     * @param targetPath local file to save to
         * @return HttpCommResponse with details of success or failure. Upon success, downloaded contents are
         * written to the targetPath file.
         */
    /*
	public HttpCommResponse downloadFile(String serverUrl, String targetPath) {
		HttpCommResponse responseData = new HttpCommResponse(false);
		boolean doInstrument = (responseData.isInstrumented());
		HttpParams httpParams = createDefaultHttpParams();
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpResponse response = null;
        
        if (doInstrument)
        	responseData.setTimeouts(connectionTimeoutSecs, socketTimeoutSecs);
 
        try {
        	if (doInstrument) responseData.instrumentStartTime();
        	if (LSLogger.isLoggingEnabled())
        		LSLogger.debug(TAG, "Initiating FileDownload-Get to server " + serverUrl 
            		+ " (connect timeout="+Integer.toString(connectionTimeoutSecs)
            		+ " seconds; socket timeout="+Integer.toString(socketTimeoutSecs)+" seconds)");
                        
            HttpGet httpGet = new HttpGet(serverUrl);
            if (LSLogger.isLoggingEnabled())
            	LSLogger.debug(TAG, "-Executing Get request: " + 
            			Utils.filterProtectedContent(httpGet.getRequestLine().toString()));
            if (doInstrument) responseData.instrumentSendStartTime();
            response = httpclient.execute(httpGet);	            
            if (doInstrument) responseData.instrumentSendEndTime();
            if (doInstrument) responseData.instrumentReadStartTime();
            // write file:
            long bytesWritten = responseData.saveResponseToFile(response, targetPath);
        	responseData.setResults(response);
        	if (doInstrument) responseData.instrumentReadEndTime();
            LSLogger.debug(TAG, "-FileDownload Response =" + responseData.toString() + " Bytes written="+bytesWritten);
        } 
        catch (Exception e) {
        	handleExceptions("GET", e, responseData);
        }
        finally {
            httpclient.getConnectionManager().shutdown();
            if (doInstrument) responseData.instrumentEndTime();	 
        }	 

		return responseData;
	}
*/
	// ---------------------------------------------------------------------------
	// ------ Internal helper methods ----------
	// ---------------------------------------------------------------------------

	/* Create the default HTTP connection parameters. This is used to set communications timeout values. */
        /*
	private HttpParams createDefaultHttpParams() {
	    HttpParams httpParameters = new BasicHttpParams();
	    // Set the timeout in milliseconds until a connection is established.
	    HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeoutSecs*1000);
	    // Set the default socket timeout (SO_TIMEOUT)  in milliseconds which is the timeout for waiting for data.
	    HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutSecs*1000);
	    return httpParameters;
	}
	*/
	
	/* Converts the given json to a set of string parameters, in the form of name=value&name2=value2 ...*/
	private String jsonParamsToStringParams(JSONObject json) {
		StringBuilder sb = new StringBuilder();
		if (json != null) {
			boolean first = true;
			Iterator<?> iter = json.keys();
			if (iter != null) {
				while (iter.hasNext()) {
					try {
						String key = (String)iter.next();
						String value = (String)json.getString(key);
						if (value != null)  {
							if (first) 
								first = false;								
							else
								sb.append("&");
							sb.append(key);
							sb.append("=");
							sb.append(value);
						}
					} catch (Exception jex) {
						LSLogger.exception(TAG, jex);
					}
				}
			}
		}
		return sb.toString();
	}
	
	/* Internal common exception/error handler. Helps identify specific possible exceptions so
	 * they can be individually handled as desired. */
	private void handleExceptions(String methodAction, Exception ex, HttpCommResponse resp) {
        LSLogger.exception(TAG, "HTTP " + methodAction + " Exception", ex);
        if (resp != null)
        	resp.setException(ex);

        // now look for specific exceptions...
		if (ex.getClass().getSimpleName().equalsIgnoreCase("ConnectTimeoutException")) {
			resp.setExceptionType(HttpCommResponse.EXCEPTIONTYPE_connectTimeout);
			LSLogger.error(TAG, "--Connection Timed out.--"); // timed out trying to connect to a running server
		} else if (ex.getClass().getSimpleName().equalsIgnoreCase("HttpHostConnectException")) {
			resp.setExceptionType(HttpCommResponse.EXCEPTIONTYPE_connectFailed);
			LSLogger.error(TAG, "--Host Connection exception.--"); // host not found (wrong url or server is down)
		} else if (ex.getClass().getSimpleName().equalsIgnoreCase("SocketTimeoutException")) {
			resp.setExceptionType(HttpCommResponse.EXCEPTIONTYPE_readTimeout);
			LSLogger.error(TAG, "--Socket Timeout exception.--"); // server took too long to respond
		} else if (ex.getClass().getSimpleName().equalsIgnoreCase("IOException")) {
			resp.setExceptionType(HttpCommResponse.EXCEPTIONTYPE_ioerror);
			LSLogger.error(TAG, "--IO exception.--");
		} else if (ex.getClass().getSimpleName().equalsIgnoreCase("UnknownHostException")) {
			resp.setExceptionType(HttpCommResponse.EXCEPTIONTYPE_unknownHost);
			LSLogger.error(TAG, "--UnknownHost exception.--");
		} else {
			LSLogger.error(TAG, "--General exception.-- " + ex.getClass().getName());
		}
	}
   
	// -----------------------------------------------------------------
	// HTTP authority methods; mostly copiedfrom mbc.HttpComm.java
	// -----------------------------------------------------------------
	

    private void getAuthHeader(List<Pair<String,String>> headers) {
        headers.add(new Pair<String, String>("Authorization", "Token token=" + authTokenKey));

        return;
    }

    /*
    private Header getAuthHeader() {
		List<String> authHeaders = new ArrayList<String>();
		authHeaders.add("Token token=" + authTokenKey);

		//Header h = new BasicHeader("Authorization", authHeaders.toString());
		Header h = new BasicHeader("Authorization", "Token token=" + authTokenKey);
		return h;
	}
	private HttpHeaders getPostAuthHeader() {
		HttpHeaders headers = new HttpHeaders();
		List<String> authHeaders = new ArrayList<String>();
		authHeaders.add("Token token=" + authTokenKey);
		headers.put("Authorization", authHeaders);
		//Header h = new BasicHeader("Authorization", authHeaders.toString());
		//Header h = new BasicHeader("Authorization", "Token token=" + Globals.API_KEY);
		return headers;
	}
	*/
	
	
    // older code sample..
	/*
    public int xsendGet(String serverUrl, String params, HttpCommResponse responseData) {
    	if (responseData == null)
    		responseData = new HttpCommResponse();
    	try {
    	        URL url = new URL(serverUrl);
    	        URLConnection yc = url.openConnection();
    	        LSLogger.info(TAG, "Connected to/reading from server " + serverUrl);
    	        //BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
    	        responseData.readInputStream(yc.getInputStream());
        } catch (ConnectException connex) {    
        	LSLogger.exception(TAG, "HTTPGet Connection Exception.", connex);
        } catch (Exception ex) {
        	LSLogger.exception(TAG, "HTTPPost Exception.", ex);
        }
    	return responseData.getResultCode();
    }
    */
    
 
}