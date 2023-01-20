package org.example;

import com.mysql.cj.jdbc.ConnectionImpl;
import org.apache.commons.lang3.StringUtils;
import org.example.cface.EventData;
import org.example.cface.request.CFRequest;
import org.example.cface.response.CFResponse;
import org.example.cface.response.CFResult;
import org.example.cface.response.CFSubject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;

public class Main {
    static String host = "172.20.3.231", db="test",user="ivanUser", pwd="Qwerty!@#456";
    static Connection connection;
    static Connection getConnection() throws SQLException {
        if(connection == null){
            String url = "jdbc:mysql://" + host + "/" + db;
            connection = DriverManager.getConnection(url, user, pwd);
        }
        return connection;
    }

    public static void main(String[] args) {
        try {
            ResultSet s = getEvents();
            ArrayList<Integer> ids = new ArrayList<>();
            while(s.next())
                ids.add(s.getInt("id"));
            s.close();

            String idsText = StringUtils.join(ids, ',');
            getConnection().createStatement()
                    .execute("UPDATE event SET processing=1 WHERE id in (" + idsText + ");");
            for (int id : ids)
                processEvent(id);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void processEvent(int eventId) throws SQLException {
        Statement stmt = getConnection().createStatement();

        ResultSet images = stmt.executeQuery("SELECT * FROM eventImages WHERE event_id=" + eventId + " LIMIT 10");
        ArrayList<EventData> eDataList = new ArrayList<>();
        //TODO: поле data отсутствует!!!!!!!!!! создавать его с параметром mediumtext нельзя!!!!!!!!!!
//        PreparedStatement updater = getConnection().prepareStatement("UPDATE eventImages SET data=? where id=?");
        CFResponse resp = new CFResponse();
        while (images.next()) {
            int imageId = images.getInt("id");
//            if(imageId < 18000)
//                continue;
            resp = CFRequest.send(images.getString("image"));
            if (resp.result.size() == 0) {
                System.out.println("Face not found in image #" + imageId + " from event #" + eventId + "!");
                continue;
            }
            EventData ed = new EventData(resp);
            ed.data.faces_count = resp.result.size();
            String json = ed.toJson();
//            updater.setString(1,json);
//            updater.setInt(1,imageId);
            //количество изменённых строк
//            int rows = updater.executeUpdate();
            System.out.println("Image #" + imageId + " from event #" + eventId
                    + " updated! Faces found: " + resp.result.size());
        }
        //TODO: тестово возвращаем значение в "0", должно быть "2"
        getConnection().createStatement().execute(
                "UPDATE event SET processing=0 WHERE id=" + eventId);

        HashSet<String> names = new HashSet<String>();
        for (CFResult result : resp.result) {
            for (CFSubject subject : result.subjects) {
                names.add(subject.subject);
            }
        }
        System.out.println(Arrays.toString(names.toArray()));
    }

    public static ResultSet getEvents() throws SQLException {
        return getConnection().createStatement().executeQuery(
                "SELECT id FROM event WHERE (processing is null or processing=0) " +
                        "and exists(SELECT id from eventImages where event_id=event.id LIMIT 1) LIMIT 5;");
    }

    private static String encodeFileToBase64(String filename) {
        File file = new File(filename);
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            throw new IllegalStateException("could not read file " + file, e);
        }
    }
}