package org.example;

import org.apache.commons.lang3.StringUtils;
import org.example.cface.EventCFData;
import org.example.cface.request.CFRequest;
import org.example.cface.response.CFResponse;
import org.example.config.AppConfig;
import org.example.config.SettingsConnection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

public class Main {
//    static String host = "172.20.3.231", db = "test",user = "ivanUser", pwd = "Qwerty!@#456";
    static Connection connection;
    static Connection getConnection() throws SQLException {
        //получаем настройки из файла .json
        SettingsConnection data = AppConfig.getInstance().connection;
        if (connection == null) {
            String url = "jdbc:mysql://" + data.host + "/" + data.database;
            connection = DriverManager.getConnection(url, data.username, data.password);
        }
        return connection;
    }

    public static void main(String[] args) {
        System.out.println("Starting...");
        try {
            ResultSet s = getEvents();
            ArrayList<Integer> ids = new ArrayList<>();
            while(s.next())
                ids.add(s.getInt("id"));
            s.close();

            String idsText = StringUtils.join(ids, ',');
            getConnection().createStatement().execute(
                    "UPDATE event SET processing=1 WHERE id in (" + idsText + ");");
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

        //задержка консоли после выполнение программы
/*        System.out.println("App closing, press Enter to close...");
        Scanner sc = new Scanner(System.in);
        sc.nextLine();

        System.exit(0);*/
    }

    //проверяем кадры события CompreFace'ом
    public static void processEvent(int eventId) throws SQLException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println( "[" + df.format(new Date()) +  "] Event #" + eventId + ". Starting...");
        Statement stmt = getConnection().createStatement();
        boolean end = false;
        int offset = 0;
        int limit = 10;
        CFResponse resp = new CFResponse();
        EventCFData eventCFData = new EventCFData();
        int counter = 0;
        while(true){
            ResultSet images = stmt.executeQuery(
                    "SELECT * FROM eventImages WHERE event_id=" + eventId
                         + " LIMIT " + limit + " OFFSET " + offset
            );
            offset += limit;
            if(!images.next())
                break;
            do {
                counter++;
                int imageId = images.getInt("id");

                resp = CFRequest.send(images.getString("image"));
                if (resp.result.size() == 0) {
//                System.out.println("Face not found in image #" + imageId + " from event #" + eventId + "!");
                    continue;
                }
                eventCFData.responses.put(imageId, resp);
                eventCFData.maxFaces = Math.max(eventCFData.maxFaces, resp.result.size());
            } while(images.next());
        }

        if (eventCFData.maxFaces > 0) {
            System.out.println("[" + df.format(new Date()) +  "] Event #" + eventId + ". Faces found: up to " + eventCFData.maxFaces
                    + " on " + eventCFData.responses.size() + " images out of " + counter + ".");
        } else
            System.out.println("[" + df.format(new Date()) +  "] Event #" + eventId + ". No faces found in " + counter +" images.");

        String json = eventCFData.toJson();
        //TODO: тестово возвращаем значение processing в "0", должно быть "2"
        PreparedStatement updateEventStmt = getConnection().prepareStatement(
                "UPDATE event SET processing=2, data=? WHERE id=?;");
        updateEventStmt.setString(1, json);
        updateEventStmt.setInt(2, eventId);
        updateEventStmt.execute();

/*        //получаем список неповторяющихся имён
        names.clear();
        for (CFResult result : resp.result) {
            for (CFSubject subject : result.subjects) {
                names.add(subject.subject);
            }
        }
        System.out.println(names.toArray().length > 0 ? Arrays.toString(names.toArray()) : "0 faces found");*/
    }

    public static ResultSet getEvents() throws SQLException {
        int limit = AppConfig.getInstance().compreface.recordedEventCount;
        if( limit <= 0)
            limit = 5;
        return getConnection().createStatement().executeQuery(
                "SELECT id FROM event WHERE (processing is null or processing=0) " +
                        "and exists(SELECT id from eventImages where event_id=event.id LIMIT 1) order by id desc LIMIT "+limit+";");
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
