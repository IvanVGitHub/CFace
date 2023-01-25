package org.example.cface.request;

import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.cface.response.CFResponse;
import org.example.config.AppConfig;
import org.example.config.CFSettings;

import java.io.IOException;

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

    public static String getConnectionApiKey(){
        CFSettings cfg = AppConfig.getInstance().compreface;
        return cfg.apiKey;
    }

    public static String getConnectionAddress(){
        CFSettings cfg = AppConfig.getInstance().compreface;
        String plugins = cfg.plugins;
        String address = cfg.host + (cfg.port > 0 ? ":" + cfg.port : "") + "/api/v1/recognition/recognize";
        if(plugins != null && !plugins.isEmpty())
            address += "?face_plugins="+plugins;
        return address;
    }

    public static CFResponse send(String base64image){
        SendFile sf = new SendFile(base64image);
        String json = sf.toJson();
        StringEntity requestEntity = new StringEntity(
                json,
                ContentType.APPLICATION_JSON);
        HttpPost request = new HttpPost(getConnectionAddress());
        request.setEntity(requestEntity);
        request.setHeader("x-api-key", getConnectionApiKey());

        CloseableHttpClient client = HttpClients.createDefault();

        try {
            try (CloseableHttpResponse httpResponse = client.execute(request)) {
                //получает код ответа от сервера (400 - лица не найдены, 200 - лица найдены)
//                System.out.println(httpResponse.getCode() + " " + httpResponse.getReasonPhrase());
                HttpEntity entity = httpResponse.getEntity();
                String resp = new String(entity.getContent().readAllBytes());
                // do something useful with the response body
                // and ensure it is fully consumed

                EntityUtils.consume(entity);
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
