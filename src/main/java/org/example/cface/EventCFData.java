package org.example.cface;

import com.google.gson.Gson;
import org.example.cface.response.CFResponse;

import java.util.HashMap;

public class EventCFData {
    public HashMap<Integer, CFResponse> responses = new HashMap<>();
    public int maxFaces;

    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
