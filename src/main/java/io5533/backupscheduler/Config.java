package io5533.backupscheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Config {
	public static final File CONFIG_FILE = new File("config/backup.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile Config instance = null;
	public static Config getInstance() {
		if (instance == null) {
			try {
				instance = Config.load();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

	public int tick = 36000;
	public boolean admin_commands = true;
	public String backup_command = "";
	public String clean_command = "";
	public Config() {}

	private static Config load() throws IOException {

		if (CONFIG_FILE.exists() && Files.size(CONFIG_FILE.toPath()) > 0) {
			Config config = GSON.fromJson(Files.readString(CONFIG_FILE.toPath()), Config.class);
			if (config != null) return config;
		}
		Config config = new Config();
		config.save();
		return config;
	}

	public void save() throws IOException {
		if (CONFIG_FILE.toPath().getParent() != null) Files.createDirectories(CONFIG_FILE.toPath().getParent());
		Files.writeString(CONFIG_FILE.toPath(), GSON.toJson(this));
	}
}