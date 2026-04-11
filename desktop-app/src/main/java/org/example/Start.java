package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class Start extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LocationGui.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Location Chaser");
        primaryStage.setResizable(false);
        primaryStage.show();

        // keyboard control
        startAutoCtrlF5();
    }

    private void startAutoCtrlF5() {
        Thread keyPressThread = new Thread(() -> {
            try {
                Robot robot = new Robot();
                while (true) {
                    // Ctrl + F5
                    robot.keyPress(KeyEvent.VK_CONTROL);
                    robot.keyPress(KeyEvent.VK_F5);
                    robot.keyRelease(KeyEvent.VK_F5);
                    robot.keyRelease(KeyEvent.VK_CONTROL);

                    System.out.println("Ctrl + F5");

                    // 15 saniye bekle
                    Thread.sleep(15000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        keyPressThread.setDaemon(true); // Uygulama kapanınca thread de kapansın
        keyPressThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
