package org.example.cface.request;

import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.cface.response.CFResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class CFRequest {

    public static class SendFile{
        public String file;
        public SendFile(String fileBase64Data){
            file = fileBase64Data;
        }
        public String toJson(){
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public static String address = "http://172.20.3.231:8000/api/v1/recognition/recognize?face_plugins=landmarks,gender,age";
//    public String address = "http://172.20.3.231:8000/api/v1/recognition/recognize";
    public static String apikey = "5e8198f5-d1e1-4ba0-be24-d8b8db15b001";



    public static CFResponse send(String base64image){

        SendFile sf = new SendFile(base64image);
        String json = sf.toJson();
        StringEntity requestEntity = new StringEntity(
                json,
                ContentType.APPLICATION_JSON);

        HttpPost request = new HttpPost(address);
        request.setEntity(requestEntity);
        request.setHeader("x-api-key", apikey);

        CloseableHttpClient client = HttpClients.createDefault();

        try {
            try (CloseableHttpResponse response2 = client.execute(request)) {
                System.out.println(response2.getCode() + " " + response2.getReasonPhrase());
                HttpEntity entity2 = response2.getEntity();
                String resp = new String(entity2.getContent().readAllBytes());
                // do something useful with the response body
                // and ensure it is fully consumed

                EntityUtils.consume(entity2);
                Gson gson = new Gson();
                CFResponse response = gson.fromJson(resp, CFResponse.class);
                return response;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
