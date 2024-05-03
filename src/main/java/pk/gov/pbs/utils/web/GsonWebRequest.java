package pk.gov.pbs.utils.web;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import pk.gov.pbs.utils.Constants;
import pk.gov.pbs.utils.StaticUtils;

public class GsonWebRequest<T> extends Request<T> {
    private static final String base_url = Constants.WEB_API_ROOT;
    private final Class<T> clazz;
    private final Response.Listener<T> listener;
    private final byte[] requestBody;
    private Map<String, String> headers;

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param url URL of the request to make
     * @param clazz Relevant class object, for Gson's reflection
     * @param headers Map of request headers
     */
    public GsonWebRequest(int method, String url, byte[] requestBody, Class<T> clazz, Response.Listener<T> listener, Response.ErrorListener errorListener, Map<String, String> headers) {
        super(method, base_url + url, errorListener);
        this. requestBody = requestBody;
        this.clazz = clazz;
        this.headers = headers;
        this.listener = listener;

        this.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 60000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 3;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {
                VolleyLog.wtf(String.valueOf(error));
                throw error;
            }
        });
    }

    public GsonWebRequest(int method, String url, byte[] requestBody, Class<T> clazz, Response.Listener<T> listener, Response.ErrorListener errorListener){
        this(method, url, requestBody, clazz, listener, errorListener, null);
    }

    public GsonWebRequest(int method, String url, Class<T> clazz, Response.Listener<T> listener, Response.ErrorListener errorListener){
        this(method, url, null, clazz, listener, errorListener, null);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=utf-8";
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return requestBody;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            VolleyLog.d(json);
            return Response.success(
                    StaticUtils.getGson().fromJson(json, clazz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

}