/**
 * Container for Http Request results, including responses and errors.
 */
package com.lightspeedsystems.mdm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Pair;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author mikezrubek
 *
 */
public class HttpCommResponse {

	public final static int RESULTTYPE_TEXT = 1;
	public final static int RESULTTYPE_JSON = 2;
	public final static int RESULTTYPE_XML  = 3;
	public final static int RESULTTYPE_BINARY = 4;
	private int default_resulttype = RESULTTYPE_TEXT;
	
	public final static int EXCEPTIONTYPE_connectFailed  = 1;
	public final static int EXCEPTIONTYPE_connectTimeout = 2;
	public final static int EXCEPTIONTYPE_readTimeout    = 3;
	public final static int EXCEPTIONTYPE_ioerror 		 = 5;
	public final static int EXCEPTIONTYPE_unknownHost    = 6;
	public final static int EXCEPTIONTYPE_unknown = 0;
	
	private Context context;
	
	private int resultCode;
	private String resultReason;
	private StringBuilder resultStr;
	private Exception exception;
	private int exceptionType = EXCEPTIONTYPE_unknown;
	private int httpConnectTimeout;
	private int httpSocketTimeout;
	private boolean bIncludeResultStr;
    private URLConnection urlConnection;
	
	// values used for instrumentation and diagnosis:
	private boolean bEnableInstrumentation; // when true, want to keep track of these discrete steps.
	private HttpInstrumentation instrumentation;
	private ProgressCallbackInterface progressListener;
	
	public HttpCommResponse() {
		this(true);
	}
	
	public HttpCommResponse(Context context) {
		this(true);
		this.context = context;
	}
	
	/** Clears the result contents from the instance. */
	public void clear() {
		resultCode = 0;
		resultReason = null;
		resultStr = null;
		exception = null;
		exceptionType = EXCEPTIONTYPE_unknown;
		if (instrumentation != null)
			instrumentation = new HttpInstrumentation();
	}

	public HttpCommResponse(boolean bIncludeResultDetails) {
		bIncludeResultStr = bIncludeResultDetails;
	}	

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public String getResultReason() {
		return resultReason;
	}

	public void setResultReason(String resultReason) {
		this.resultReason = resultReason;
	}

	public String getResultStr() {
		if (resultStr == null)
			 return "";
		return resultStr.toString();
	}

	public void setResultStr(String str) {
		this.resultStr = new StringBuilder(str);
	}
	
	public void setAppendtoResultStr(String str) {
		if (resultStr == null)
			setResultStr(str);
		else
		    resultStr.append(str);
	}
	
	public boolean hasException() {
		return (exception != null);
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	public int getExceptionType() {
		return exceptionType;
	}
	
	public void setExceptionType(int i) {
		exceptionType = i;
	}
	
	public void setTimeouts(int connectionTimeoutSecs, int socketTimeoutSecs) {
		httpConnectTimeout = connectionTimeoutSecs;
		httpSocketTimeout  = connectionTimeoutSecs;
	}
	
	/**
	 * Conveniece method for indicating a successful request response.
	 * @return true if an ok response was received, false if no response or if an error occurred.
	 */
	public boolean isOK() {
		return (resultCode==200);
	}

    /**
     * Extracts result data from the given Http Response. Reads the data from the response as needed.
     * @param httpResp
     * @return http response code from the response data. 200=ok.
     */
    /*
    public int setResults(HttpResponse httpResp) {
        StatusLine sl = httpResp.getStatusLine();
        setResultCode(sl.getStatusCode());
        setResultReason(sl.getReasonPhrase());
        if (bIncludeResultStr)
            extractResponseData(httpResp, default_resulttype);
        return resultCode;
    }
    */

    /**
     * Extracts result data from the given Http Response. Reads the data from the response as needed.
     * @param inputStream
     * @return http response code from the response data. 200=ok.
     */
    public int setResults(InputStream inputStream) {
        if (urlConnection == null) {
            return -1;
        }
        try {
            if(urlConnection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) urlConnection;
                setResultCode(httpsConnection.getResponseCode());
                setResultReason(httpsConnection.getResponseMessage());
            } else {
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
                setResultCode(httpConnection.getResponseCode());
                setResultReason(httpConnection.getResponseMessage());
            }
        } catch (IOException ioe ) {

        }
        if (bIncludeResultStr)
            extractResponseData(inputStream, default_resulttype);
        return resultCode;
    }

    public void disconnectUrlConnection() {
        if (urlConnection == null) {
            return;
        }
        if(urlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) urlConnection;
            httpsConnection.disconnect();
        } else {
            HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
            httpConnection.disconnect();
        }
    }

    public InputStream openUrlConnection(String urlStr, List<Pair<String,String>> headers) {
//		Log.d(Globals.appName,"openUrlConnection url is "+urlStr);
        InputStream inStream = null;

        try {
            URL url = new URL(urlStr);
            urlConnection = url.openConnection();

            if(urlConnection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) urlConnection;
				httpsConnection.setSSLSocketFactory(new TLSSocketFactory());
//                httpsConnection.setInstanceFollowRedirects(true);
                httpsConnection.setRequestMethod("GET");
                httpsConnection.setReadTimeout(5 * 1000);
                if(headers != null) {
                    for (int i=0; i<headers.size();i++) {
                        httpsConnection.setRequestProperty(headers.get(i).first,headers.get(i).second);
                    }
                }

                inStream = httpsConnection.getInputStream();
                resultCode = httpsConnection.getResponseCode();

                if (resultCode != HttpsURLConnection.HTTP_OK) {
                    inStream.close();
                    inStream = null;
                }
            } else {
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
//                httpConnection.setInstanceFollowRedirects(true);
                httpConnection.setRequestMethod("GET");
                httpConnection.setReadTimeout(5 * 1000);
                if(headers != null) {
                    for (int i=0; i<headers.size();i++) {
                        httpConnection.setRequestProperty(headers.get(i).first,headers.get(i).second);
                    }
                }

                inStream = httpConnection.getInputStream();

                resultCode = httpConnection.getResponseCode();

                if (resultCode != HttpURLConnection.HTTP_OK) {
                    inStream.close();
                    inStream = null;
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

		return inStream;
    }

    public InputStream postUrlConnection(String urlStr, List<Pair<String,String>> headers, String postData) {
//		Log.d(Globals.appName,"postUrlConnection url is "+urlStr);
        InputStream inStream = null;

        try {
            URL url = new URL(urlStr);
            urlConnection = url.openConnection();

            if(urlConnection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) urlConnection;
//                httpsConnection.setInstanceFollowRedirects(true);
                httpsConnection.setRequestMethod("POST");
                httpsConnection.setReadTimeout(5 * 1000);
                if(headers != null) {
                    for (int i=0; i<headers.size();i++) {
                        httpsConnection.setRequestProperty(headers.get(i).first,headers.get(i).second);
                    }
                }

                httpsConnection.setUseCaches(false);
                httpsConnection.setDoInput(true);
                httpsConnection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(httpsConnection.getOutputStream());
                wr.writeBytes(postData);
                wr.flush();
                wr.close();

                inStream = httpsConnection.getInputStream();
                resultCode = httpsConnection.getResponseCode();

                if (resultCode != HttpsURLConnection.HTTP_OK) {
                    inStream.close();
                    inStream = null;
                }
            } else {
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
//                httpConnection.setInstanceFollowRedirects(true);
                httpConnection.setRequestMethod("GET");
                httpConnection.setReadTimeout(5 * 1000);
                if(headers != null) {
                    for (int i=0; i<headers.size();i++) {
                        httpConnection.setRequestProperty(headers.get(i).first,headers.get(i).second);
                    }
                }

                httpConnection.setUseCaches(false);
                httpConnection.setDoInput(true);
                httpConnection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(httpConnection.getOutputStream());
                wr.writeBytes(postData);
                wr.flush();
                wr.close();

                inStream = httpConnection.getInputStream();

                resultCode = httpConnection.getResponseCode();

                if (resultCode != HttpURLConnection.HTTP_OK) {
                    inStream.close();
                    inStream = null;
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inStream;
    }

    /**
     * Reads response data, writing it to the file path given.
     * @param httpResp response instance
     * @param targetPath path to file to write data to
     * @return number of bytes written, 0 if error
     * @throws IOException if an IO error occurs
     */
    /*
	public long saveResponseToFile(HttpResponse httpResp, String targetPath) throws IOException {
		// create file and write to it:
		long bytes = 0;
		if (httpResp != null) {
			HttpEntity resEntity = httpResp.getEntity();
			if (resEntity != null) {
				InputStream ins = resEntity.getContent();
				File file = new File(targetPath);
				FileOutputStream outstr = new FileOutputStream(file);
				byte[] buffer = new byte[2049];
				int len=0;
				while ((len=ins.read(buffer,0,2048)) != -1) {
					outstr.write(buffer,0,len);
					bytes += len;
				}
				outstr.close();
			}
		}
		return bytes;
	}
*/
    /**
     * Reads the server-returned data from the response. Stores the data in resultStr field in the format indicated.
     * (The default formatting takes the response content as text and creates a string containing all of it.)
     * @param httpResp instance of HttpResponse from an http operation.
     * @param resultTypeFormat one of the RESULTTYPE_ values.
     */
    /*
    public void extractResponseData(HttpResponse httpResp, int resultTypeFormat) {
        if (httpResp != null) {
            HttpEntity resEntity = httpResp.getEntity();
            if (resEntity != null) {
                try {
                    BufferedReader rdr = new BufferedReader(new InputStreamReader(resEntity.getContent()));
                    String inputLine;
                    int len = (int)resEntity.getContentLength();
                    if (len <= 0)
                        len = 64;
                    resultStr = new StringBuilder(len);
                    while ((inputLine = rdr.readLine()) != null)
                        resultStr.append(inputLine);
                    rdr.close();
                } catch (Exception ex) {
                    setResultStr(ex.toString());
                    exception = ex;
                }
            }
        }
    }
    */
    /**
     * Reads the server-returned data from the response. Stores the data in resultStr field in the format indicated.
     * (The default formatting takes the response content as text and creates a string containing all of it.)
     * @param inputStream instance of InputStream from an http operation.
     * @param resultTypeFormat one of the RESULTTYPE_ values.
     */
    public void extractResponseData(InputStream inputStream, int resultTypeFormat) {
        if (inputStream != null) {
                try {
                    BufferedReader rdr = new BufferedReader(new InputStreamReader(inputStream));
                    String inputLine;
                    int len = (int)urlConnection.getContentLength();
                    if (len <= 0)
                        len = 64;
                    resultStr = new StringBuilder(len);
                    while ((inputLine = rdr.readLine()) != null)
                        resultStr.append(inputLine);
                    rdr.close();
                } catch (Exception ex) {
                    setResultStr(ex.toString());
                    exception = ex;
                }
        }
    }


    /**
	 * Reads data from a server response input stream, as a result of a Http method action (Get, Post, Put, etc.).
	 * Stores contents in this instance's resultStr.
	 * @param stream java.io.InputStream to read from.
	 * @throws IOException thrown from reading the stream upon error.
	 */
	public void readInputStream(InputStream stream)  throws IOException {
		BufferedReader rdr = new BufferedReader(new InputStreamReader(stream));
		String inputLine;
		resultStr = new StringBuilder();
		while ((inputLine = rdr.readLine()) != null) 
			resultStr.append(inputLine);
		rdr.close();
	}
	
	/**
	 * Gets the string representation of this instance, which included the result code and reason.
	 */
	public String toString() {
		String s = Integer.toString(resultCode) + " : " + (resultReason==null?"(null)":resultReason);
		return s;
	}
	
	// ------------------------------------------------------------------------
	// -- internal instrumentation support -----
	// ------------------------------------------------------------------------

	public void setInstrumentation(boolean doInstrumentation) {
		bEnableInstrumentation = doInstrumentation;
		if (bEnableInstrumentation)
			instrumentation = new HttpInstrumentation();
	}
	public boolean isInstrumented() {
		return bEnableInstrumentation;
	}
	public String getInstrumentationCurrentStatusMsg() {
		String res = null;
		if (bEnableInstrumentation)
			res = instrumentation.currentStatusMsg;
		else if (context != null)
			res = context.getResources().getString(R.string.instrumentation_notenabled);
		return res;
	}
	public String getInstrumentationFinalSummaryMsg() {
		String res = null;
		if (bEnableInstrumentation)
			res = instrumentation.summaryMsg;
		else if (context != null)
			res = context.getResources().getString(R.string.instrumentation_notenabled);
		return res;
	}
	
	public void setProgressListener(ProgressCallbackInterface listener) {
		progressListener = listener;
	}
	
	// note: all these ASSUME setInstrumentation(true) was called. Otherwise a null-pointer could occur.
	
	protected void instrumentStartTime() {
		instrumentation.startTime = new Date().getTime();
		if (context != null)
			instrumentation.currentStatusMsg = context.getResources().getString(R.string.instrumentation_svrstarted);
	}
	protected void instrumentSendStartTime() {
		instrumentation.timeSendStarted = new Date().getTime();
		if (progressListener != null && context != null) {
		    instrumentation.currentStatusMsg = 
		    		String.format(context.getResources().getString(R.string.instrumentation_svrsending),
					httpConnectTimeout);
			progressListener.statusMsg(instrumentation.currentStatusMsg );
		}
	}
	protected void instrumentSendEndTime() {
		instrumentation.timeSendEnded = new Date().getTime();
		if (context != null)
			instrumentation.currentStatusMsg = context.getResources().getString(R.string.instrumentation_svrread);
	}
	protected void instrumentReadStartTime() {
		instrumentation.timeReadStarted = new Date().getTime();
		if (progressListener != null && context != null) {
		    instrumentation.currentStatusMsg = 
		    		String.format(context.getResources().getString(R.string.instrumentation_svrreading),
					httpSocketTimeout);
			progressListener.statusMsg(instrumentation.currentStatusMsg);
		}
	}
	protected void instrumentReadEndTime() {
		instrumentation.timeReadEnded = new Date().getTime();
		if (context != null)
			instrumentation.currentStatusMsg = context.getResources().getString(R.string.instrumentation_svrsent);
	}
	protected void instrumentEndTime() {
		instrumentation.completionTime = new Date().getTime();
		instrumentation.calcStats();
		if (context != null)
			instrumentation.currentStatusMsg = context.getResources().getString(R.string.instrumentation_svrfinished);
		if (progressListener != null)
			progressListener.statusMsg(instrumentation.currentStatusMsg);
	}
	protected long instrumentGetDurationSending() {
		return instrumentation.durationSending;
	}
	protected long instrumentGetDurationReading() {
		return instrumentation.durationReading;
	}
	protected long instrumentGetDurationTotal() {
		return instrumentation.durationTotal;
	}
	
	private class HttpInstrumentation {
		 long startTime;			// time when process started
		 long timeSendStarted;   // time when request was sent
		 long timeSendEnded;     // time when request was finished being sent
		 long timeReadStarted;   // time response read began
		 long timeReadEnded;     // time when response read completed
		 long completionTime;    // time when the process completed.
		 String currentStatusMsg;// internal message string for where things are now
		 
		 // summary values
		 long durationSending;
		 long durationReading;
		 long durationTotal;
		 String summaryMsg;
		 		 
		 protected void calcStats() {
			 if (timeSendStarted != 0 && timeSendEnded != 0)
				 durationSending = timeSendEnded-timeSendStarted;
			 if (timeReadStarted != 0 && timeReadEnded != 0)
				 durationReading = timeReadEnded - timeReadStarted;
			 if (completionTime != 0)
				 durationTotal = completionTime - startTime;
			 //summaryMsg = String.format("Overall time: %d ms\n  Time connecting/sending: %d ms\n  Time reading: %d", 
			 //			instrumentation.durationTotal, instrumentation.durationSending, instrumentation.durationReading);			 

			 if (context!=null)
				 // get overall time (use the time spent sending and receiving/reading)
				 summaryMsg = String.format("(" + context.getResources().getString(R.string.instrumentation_summarytime) +")",
					 timeReadEnded-timeSendStarted);			 
		 }
	}

}
