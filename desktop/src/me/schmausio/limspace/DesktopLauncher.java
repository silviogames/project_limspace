package me.schmausio.limspace;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import me.schmausio.limspace.Main;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("project_limspace");
		config.setWindowedMode(Main.SCREEN_WIDTH * 2,Main.SCREEN_HEIGHT * 2);
		config.setResizable(false);
		new Lwjgl3Application(new Main(), config);
	}
}
