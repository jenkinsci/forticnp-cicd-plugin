package com.fortinet.forticontainer.common;

import com.fortinet.UserConfiguration;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

public class ControllerUtil {

    public static final String URI_JENKINS_JOB = "/api/v1/jenkins/job";
    public static final String URI_JENKINS_IMAGE = "/api/v1/jenkins/image";
    public static final String URI_JENKINS_FORWARD = "/api/v1/jenkins/forward";
    public static final String URI_POLICY_SEARCH = "/api/v1/policy/search";
    public static final String URI_VULNERS_SEARCH = "/api/v1/vulners/search";
    public static final String URI_IMAGES_SEARCH = "/api/v1//images/search";
    public static final String URI_AUTH_CREDENTIALS_TOKEN = "/api/v1/auth/credentials/token";
    public static final String URI_CONTROLLER_HOST = "/api/v1/controller/host";
    public static final String URI_MONITOR_HEALTH = "/monitor/health";
    
    public static final String HEADER_CONTROLLER_TOKEN = "x-controller-token";
    public static final String HEADER_URL_PATH = "x-url-path";
    public static final String HEADER_HTTP_METHOD = "x-http-method";
    public static final String HEADER_E_ACCESS_SERVICE = "x-e-access-service";
    public static final String HEADER_CT_INFO = "x-ct-info";

    private static String getAccessToken(String webHostUrl, String credentialToken) throws Exception {
        final String tokenApiUrl = webHostUrl + URI_AUTH_CREDENTIALS_TOKEN;
        //System.out.println("getAccessToken " + tokenApiUrl);

        URL tokenApi = new URL(tokenApiUrl);
        HttpURLConnection conn = (HttpURLConnection)tokenApi.openConnection();
        conn.setDoOutput(true);

        // follow 5.1.2 token API spec
        conn.setRequestMethod("POST");
        String fullCredentialToken = "Basic " + credentialToken;
        conn.setRequestProperty("Authorization", fullCredentialToken);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String urlencodedBody = "grant_type=client_credentials";
        conn.getOutputStream().write(urlencodedBody.getBytes(StandardCharsets.UTF_8));

        // handle response
        int responseCode = conn.getResponseCode();
        //System.out.println("getAccessToken response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            final InputStream inputStream = conn.getInputStream();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                output = sb.toString();
                //System.out.println(output); // debug

                JSONObject tokenApiResponse = JSONObject.fromObject(output);
                String token = tokenApiResponse.getString("access_token");
                
                String accessToken = "Bearer " + token;
                return accessToken;
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        } else {
            throw new RuntimeException("HTTP code: " + responseCode + " from " + tokenApiUrl);
        }
    }

    private static String getOnlineControllerHostByHostList(JSONArray hostList) throws Exception {
        if (hostList != null) {
            UserConfiguration userConfig = UserConfiguration.get();
            for (Object hostAddrObj : hostList) {
                String hostAddr = hostAddrObj.toString();
                checkControllerConnection(hostAddr, userConfig.getCredentialTokenString());
                return hostAddr;
            }
        }
        return "";
    }

    private static String getOnlineControllerHostByApiResponse(JSONObject jsonObj) throws Exception {
        // {"host":[],"serviceIp":[],"nodeHostIp":[]}
        final String serviceIpKey = "serviceIp";
        final String hostKey = "host";
        final String nodeHostIpKey = "nodeHostIp";

        // uses nodeHostIp
        List<JSONArray> allHosts = new ArrayList<JSONArray>();
        if (!jsonObj.get(nodeHostIpKey).equals(JSONNull.getInstance())) {
            allHosts.add(jsonObj.getJSONArray(nodeHostIpKey));
        }

        Exception ex = null;
        for (JSONArray hostList : allHosts) {
            try {
                String controllerHost = getOnlineControllerHostByHostList(hostList);
                if (!controllerHost.isEmpty()) {
                    return controllerHost;
                }
            } catch (Exception e) {
                // catch exception and try next one
                ex = e;
                continue;
            }
        }

        String exMsg = "cannot get an online protector host";
        if (ex != null) {
            exMsg += ": " + ex.getMessage();
        }

        throw new RuntimeException(exMsg);
    }

    public static String getControllerHostByUserConfig(UserConfiguration userConfiguration, PrintStream ps) {
        final int tries = 3;
        for (int i = 0; i < tries; ++ i) {
            try {
                String controllerHost = userConfiguration.getManualHostAddressByCheck() == null
                                        || userConfiguration.getManualHostAddressByCheck().isEmpty() ?
                                        ControllerUtil.requestControllerHostUrl(userConfiguration.getWebHostAddress(), userConfiguration.getCredentialTokenString()) :
                                        userConfiguration.getManualHostAddressByCheck();
    
                ps.println("Using Protector host: " + controllerHost);
                return controllerHost;
            } catch (Exception e) {
                if (ps != null) {
                    ps.println("Try: " + i + ", failed to get protector host, exception: " + e.getMessage());
                }
            }
        }
        return "";
    }

    public static void checkControllerConnection(String host, String token) throws Exception {
        if (host.isEmpty()) {
            throw new RuntimeException("get empty protector host address");
        } else {
            final String heartBeatUrl = host + URI_MONITOR_HEALTH;

            URL url = new URL(heartBeatUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(HEADER_CONTROLLER_TOKEN, token);
    
            int responseCode = conn.getResponseCode();
            //System.out.println("checkConnection Response Code : " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("cannot verify health status on: " + host + ", HTTP response: " + responseCode);
            }
        }
    }

    public static String requestControllerHostUrl(String webHostUrl, String credentialToken) throws Exception {
        final String accessToken = getAccessToken(webHostUrl, credentialToken);
        //System.out.println("get access token: " + accessToken);

        if (!accessToken.isEmpty()) {    
            // request controller host
            final String controllerHostApiUrl = webHostUrl + URI_CONTROLLER_HOST;
    
            URL controllerHostApi = new URL(controllerHostApiUrl);
            HttpURLConnection conn = (HttpURLConnection)controllerHostApi.openConnection();
            conn.setDoOutput(true);
    
            // follow controller host API spec
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(HEADER_E_ACCESS_SERVICE, "fortics-web");
            conn.setRequestProperty(HEADER_CT_INFO, credentialToken);
            conn.setRequestProperty("Authorization", accessToken);
    
            //System.out.println("x-ct-info: " + credentialToken);
            //System.out.println("Authorization: " + accessToken);

            // handle response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                final InputStream inputStream = conn.getInputStream();
    
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
                    StringBuilder sb = new StringBuilder();
                    String output;
                    while ((output = br.readLine()) != null) {
                        sb.append(output);
                    }
                    
                    output = sb.toString();
                    //System.out.println(controllerHostApi + " response: " + output);
    
                    JSONObject response = JSONObject.fromObject(output);
                    return getOnlineControllerHostByApiResponse(response);
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } else {
                throw new RuntimeException("request protector host failed, code: " + responseCode);
            }
        }
        else {
            throw new RuntimeException("access token is empty from " + webHostUrl);
        }
    }
}
