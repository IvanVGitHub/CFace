package org.example.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AppConfig {
    private static String configFile = "settings.json";
    private static AppConfig instance;

    public static AppConfig getInstance() {
        if (instance == null)
            instance = loadConfig();
        return instance;
    }

    //поля
    public SettingsConnection connection = new SettingsConnection();
    public CFSettings compreface = new CFSettings();
    //конец полей

    //загружаем настройки из файла .json
    public static AppConfig loadConfig() {
        try {
            File file = new File(configFile);
            Gson gson = new Gson();
            if (file.exists()) {
                AppConfig conf = gson.fromJson(new FileReader(file), AppConfig.class);
                instance = conf;

                return conf;
            }
        } catch (Exception ex) {ex.printStackTrace();}
        return new AppConfig();
    }

    //сохраняем в файл .json
    public static void saveConfig() {
        if (instance == null)
            return;
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(instance);
            File file = new File(configFile);
            FileWriter wr = new FileWriter(file);
            wr.write(json);
            wr.close();
        } catch (Exception ex) {ex.printStackTrace();}
    }
}
