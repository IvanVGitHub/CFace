package org.example.cface;

import com.google.gson.Gson;
import org.example.cface.response.CFResponse;

public class EventData {
    public CFResponse response;
    public EventCalculatedData data = new EventCalculatedData();

    public EventData(CFResponse response){
        this.response = response;
    }
    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
