package com.fortinet.forticontainer;

import com.google.gson.JsonObject;
import hudson.util.CopyOnWriteMap;
import net.sf.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.fortinet.forticontainer.common.ControllerUtil;

public class JenkinsServer {

    private static enum ProcessStatusEnum {
        WAIT(0),
        INIT(1),
        UPLOADING(5),
        SCANNING(10),
        ANALYZING(15),
        POLICY_CHECK(17),
        COMPLETED(20);

        private int status;

        ProcessStatusEnum(int status) {
            this.status = status;
        }
    
        public static ProcessStatusEnum fromInteger(int status) {
            switch (status) {
                case 0:
                    return WAIT;
                case 1:
                    return INIT;
                case 5:
                    return UPLOADING;
                case 10:
                    return SCANNING;
                case 15:
                    return ANALYZING;
                case 17:
                    return POLICY_CHECK;
                case 20:
                    return COMPLETED;
            }
            return null;
        }
    }

    private JenkinsServer() {

    }

    public static Boolean reserveJob(SessionInfo sessionInfo, String jobId) throws IOException {
        final String serverUrl = sessionInfo.getControllerHostUrl() + ControllerUtil.URI_RESERVE_JOB + "/" + jobId;
        System.out.println("server url: " + serverUrl);
        URL instanceUrl = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) instanceUrl.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, sessionInfo.getControllerToken());
            conn.getOutputStream().write(new byte[0]);

            if (conn.getResponseCode() == 200) {
                return true;
            } else {
                System.out.println("reserveJob response code:" + conn.getResponseCode());
                return false;
            }
        } catch (IOException e) {
            System.out.println("reserveJob failed, exception: " + e.getMessage());
            throw e;
        }
    }

    public static String addImage(SessionInfo sessionInfo, CurrentBuildInfo currentBuildInfo, String jenkinsId) throws IOException {
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("jenkinsId", jenkinsId);

        final String serverUrl = sessionInfo.getControllerHostUrl() + ControllerUtil.URI_JENKINS_FORWARD;
        URL instanceUrl = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) instanceUrl.openConnection();

        InputStream inputStream = null;
        BufferedReader br = null;
        try {
            JSONObject jsonObject = JSONObject.fromObject(jsonMap);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, sessionInfo.getControllerToken());
            conn.setRequestProperty(ControllerUtil.HEADER_URL_PATH, ControllerUtil.URI_JENKINS_ADD_IMAGE + "/" + jenkinsId);
            conn.setRequestProperty(ControllerUtil.HEADER_HTTP_METHOD, "POST");
            conn.getOutputStream().write(jsonObject.toString().getBytes("UTF-8"));

            inputStream = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
            StringBuilder sb = new StringBuilder();
            String output;
            while((output = br.readLine()) != null) {
                sb.append(output);
            }

            inputStream.close();
            output = sb.toString();
            JSONObject jsonOutput = JSONObject.fromObject(output);
            long imageId = jsonOutput.getLong("imageId");
            return Long.toString(imageId);
        } catch(IOException e) {
            System.out.println("add image failed, exception: " + e.getMessage());
            throw e;
        } finally {
            if(inputStream != null) {
                inputStream.close();
            }
            if(br != null)  {
                br.close();
            }
        }
    }

    public static String addJob(SessionInfo sessionInfo, CurrentBuildInfo currentBuildInfo) throws IOException{
        //set up
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("jobName", currentBuildInfo.getJobName());
        jsonMap.put("jobHost", currentBuildInfo.getJobUrl());
        jsonMap.put("buildNumber", currentBuildInfo.getBuildNumber());


        final String serverUrl = sessionInfo.getControllerHostUrl() + ControllerUtil.URI_JENKINS_FORWARD;
        URL instanceUrl = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) instanceUrl.openConnection();

        InputStream inputStream = null;
        BufferedReader br = null;

        try {
            //ps.println("post to: " + serverUrl);
            //ps.println("body: " + jsonMap.toString());
            JSONObject jsonObject = JSONObject.fromObject(jsonMap);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, sessionInfo.getControllerToken());
            conn.setRequestProperty(ControllerUtil.HEADER_URL_PATH, ControllerUtil.URI_JENKINS_JOB);
            conn.setRequestProperty(ControllerUtil.HEADER_HTTP_METHOD, "POST");
            conn.getOutputStream().write(jsonObject.toString().getBytes("UTF-8"));
            //ps.println("request posted, response code: " + conn.getResponseCode());

            inputStream = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }

            inputStream.close();
            output = sb.toString();
            //ps.println("response body: " + output);
            JSONObject jsonOutput = JSONObject.fromObject(output);
            long jobId = jsonOutput.getLong("jenkinsId");
            return Long.toString(jobId);
        } catch (IOException e) {
            System.out.println("addJob failed, exception: " + e.getMessage());
            throw e;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (br != null) {
                br.close();
            }
        }
    }

    public static Boolean uploadImage(String jobId, String imageName, String imageId, SessionInfo sessionInfo, PrintStream ps) throws IOException, InterruptedException {
        String fileName = URLEncoder.encode(imageName,"UTF-8");
        System.out.println("the encode name is " + fileName);
        Runtime runtime = Runtime.getRuntime();
        Boolean result = false;
        String saveDockerCmd = String.format("docker save %s -o /tmp/%s.tar", imageName, fileName);
        System.out.println("the saveDockerCmd is " + saveDockerCmd);
        ps.println("saving docker file to local: " + saveDockerCmd);
        Process process = runtime.exec(saveDockerCmd);

        int exitVal = -1;
        try {
            exitVal = process.waitFor();
        } catch (InterruptedException e) {
            ps.println("Failed to save image " + e.getMessage());
        }

        if(exitVal != 0) {
            ps.println("docker process exit value: " + exitVal);
            return false;
        }

        //save docker to fileName;
        String imageFilePath = String.format("/tmp/%s.tar",fileName);
        File imageFile = new File(imageFilePath);
        if(!imageFile.exists()) {
            ps.println("The image file does not exist: " + imageFile.getAbsolutePath());
            return false;
        }
        try {
            result = sendImageFileToServer(imageFilePath, imageName, imageId, sessionInfo, jobId);
            ps.println("The image has been uploaded for scanning");
        } catch (Exception ex) {
            ps.println("Failed to send image file: " + imageFilePath + " to server, error: " + ex.getMessage());
            return false;
        }

        // try to remove the tmp file, ignore if cannot remove
        final int sleepTime = 5000;
        for (int i = 0; i < 3; ++i) {
            Boolean deleteFileResult = false;
            try {
                deleteFileResult = imageFile.delete();
                if(!deleteFileResult) {
                    ps.println("failed to delete image file: " + imageFile.getPath());
                }
            } catch (Exception e) {
                ps.println("failed to delete image file: " + imageFile.getPath() + ", error: " + e.getMessage());
                deleteFileResult = false;
            } finally {
                if (!deleteFileResult) {
                    Thread.sleep(sleepTime);
                    continue;
                } else {
                    break;
                }
            }
        }

        return result;
    }


    public static Boolean updateJobStatus(SessionInfo sessionInfo, String jobId, Integer statusCode) throws IOException{

        Map<String, Integer> jsonMap = new HashMap<>();
        jsonMap.put("status", statusCode);

        final String updateJobStatusUrl = sessionInfo.getControllerHostUrl() + ControllerUtil.URI_JENKINS_FORWARD;
        System.out.println("the update status url is " + updateJobStatusUrl);
        URL instanceUrl = new URL(updateJobStatusUrl);
        HttpURLConnection conn = (HttpURLConnection) instanceUrl.openConnection();
        try {
            JSONObject jsonObject = JSONObject.fromObject(jsonMap);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, sessionInfo.getControllerToken());
            conn.setRequestProperty(ControllerUtil.HEADER_URL_PATH, ControllerUtil.URI_JENKINS_JOB + "/" + jobId);
            conn.setRequestProperty(ControllerUtil.HEADER_HTTP_METHOD, "PUT");

            conn.getOutputStream().write(jsonObject.toString().getBytes("UTF-8"));

            int responseCode =  conn.getResponseCode();

            if(responseCode == 200) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public static Integer checkJobStatus(SessionInfo sessionInfo, String jobId, PrintStream ps) throws IOException {

        final String checkJobStatusUrl = sessionInfo.getControllerHostUrl() + ControllerUtil.URI_JENKINS_FORWARD;
        System.out.println("job status API url is " + checkJobStatusUrl);
        URL instanceUrl = new URL(checkJobStatusUrl);
        HttpURLConnection conn = (HttpURLConnection) instanceUrl.openConnection();

        BufferedReader br = null;
        try {
            JSONObject jsonObject = new JSONObject();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, sessionInfo.getControllerToken());
            conn.setRequestProperty(ControllerUtil.HEADER_URL_PATH, ControllerUtil.URI_JENKINS_JOB + "/" + jobId);
            conn.setRequestProperty(ControllerUtil.HEADER_HTTP_METHOD, "GET");
            conn.getOutputStream().write(jsonObject.toString().getBytes("UTF-8"));
            int responseCode =  conn.getResponseCode();
            System.out.println("job status API response code: " + responseCode);
            final InputStream inputStream = conn.getInputStream();

            if(responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                output = sb.toString();

                if (ps != null) {
                    ps.print("in checkJobStatus()");
                    ps.println("request api: " + checkJobStatusUrl);
                    ps.println("request header object: " + conn.getHeaderFields().toString());
                    ps.println("server response code: " + responseCode);
                    ps.println("server response body: " + output);
                }

                JSONObject jsonOutput = JSONObject.fromObject(output);
                System.out.println("checkJobStatus response: " + jsonOutput);

                Integer responseResult = jsonOutput.getInt("result");
                Integer responseStatus = jsonOutput.getInt("status");
                System.out.println("result: " + responseResult + ", status: " + responseStatus);

                final ProcessStatusEnum processStatus = ProcessStatusEnum.fromInteger(responseStatus);
                if (ps != null) {
                    ps.println("Image scan status: " + processStatus.name());
                }

                inputStream.close();
                if(responseStatus < 20) {
                    return 0;
                } else {
                    return responseResult;
                }
            } else {
                throw new IOException("The protector service returns HTTP code: " + responseCode);
            }

        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
    public static Boolean addPolicyThreshold() {
        return true;
    }

    private static Boolean sendImageFileToServer(String imageFilePath, String imageName, String imageId, SessionInfo sessionInfo, String jobId) throws Exception {
        String url = sessionInfo.getControllerHostUrl() + ControllerUtil.URI_JENKINS_IMAGE + "/" + jobId;
        System.out.println("sendImageFileToServer : the url send is : " + url);
        String charset = "UTF-8";
        File binaryFile = new File(imageFilePath);
        if(binaryFile.exists()) {
            System.out.println("the binary file is exist");
        }
        String boundary = "===" + Long.toHexString(System.currentTimeMillis()) + "==="; // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty(ControllerUtil.HEADER_CONTROLLER_TOKEN, sessionInfo.getControllerToken());
        connection.setRequestProperty(ControllerUtil.HEADER_IMAGE_ID, imageId);
        connection.setRequestProperty("imageName",imageName);
        OutputStream output = connection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);

        FileInputStream inputStream = null;
        try {
            String fileName = binaryFile.getName();
            System.out.println("the file name is " + fileName);
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF);
            writer.flush();

            inputStream = new FileInputStream(binaryFile);
            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);
            while (bytesRead != -1) {
                output.write(buffer, 0, bytesRead);
                System.out.println("bytes sent: " + bytesRead);
                bytesRead = inputStream.read(buffer);
            }
            output.flush();

            writer.append(CRLF);
            writer.flush();

            writer.append("--" + boundary + "--").append(CRLF).flush();
            writer.close();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        int responseCode =connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            return true;
        } else {
            return false;
        }
    }
}
