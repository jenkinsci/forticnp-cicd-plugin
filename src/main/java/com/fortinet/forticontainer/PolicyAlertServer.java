package com.fortinet.forticontainer;

import com.fortinet.forticontainer.common.ControllerUtil;
import com.fortinet.forticontainer.dto.AlertDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolicyAlertServer {

    private PolicyAlertServer() {
    }
    private static Gson gson = new Gson();

    public static List<AlertDto> getBuildAlertDto(SessionInfo session, CurrentBuildInfo currentBuildInfo) throws IOException {
        BufferedReader br = null;
        try {
            String policyAlertSearchUrl = session.getControllerHostUrl() + ControllerUtil.URI_JENKINS_FORWARD;
            URL instanceUrl = new URL(policyAlertSearchUrl);
            HttpURLConnection conn = (HttpURLConnection) instanceUrl.openConnection();
    
            JSONObject jsonObject = new JSONObject();
    
            jsonObject.put("resourceId", new ArrayList<String>(Arrays.asList(currentBuildInfo.getJenkinsJobId())));
            jsonObject.put("jobId", new ArrayList<String>(Arrays.asList(currentBuildInfo.getJenkinsJobId())));
            jsonObject.put("jobName", new ArrayList<String>(Arrays.asList(currentBuildInfo.getJobName())));
            jsonObject.put("resourceName", new ArrayList<String>(Arrays.asList(currentBuildInfo.getJobUrl())));
            jsonObject.put("skip", 0);
            jsonObject.put("limit", 50);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, session.getControllerToken());
            conn.setRequestProperty(ControllerUtil.HEADER_URL_PATH, ControllerUtil.URI_POLICY_SEARCH);
            conn.setRequestProperty(ControllerUtil.HEADER_HTTP_METHOD, "POST");
            conn.getOutputStream().write(jsonObject.toString().getBytes("UTF-8"));

            final InputStream inputStream = conn.getInputStream();
            //System.out.println("policy alert request body: " + jsonObject.toString());
            //System.out.println("policy alert response code: " + conn.getResponseCode());

            br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            output = sb.toString();

            JSONObject alertJson = JSONObject.fromObject(output);
            JSONArray policyAlertEntities = alertJson.getJSONArray("datas");
            Type projectListType = new TypeToken<ArrayList<AlertDto>>(){}.getType();

            List<AlertDto> response = gson.fromJson(String.valueOf(policyAlertEntities), projectListType);
            //System.out.println("the alert response is = " + gson.toJson(response));
            inputStream.close();

            return response;
        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}
