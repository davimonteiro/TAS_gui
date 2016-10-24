package application;

import java.io.File;

import application.utility.Utility;
import application.view.controller.ApplicationController;
import br.uece.travelapp.TravelPlannerApp;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainGui extends Application {

	private static final String logFile = "results" + File.separator + "log.csv";
	private static final String resultFile = "results" + File.separator + "result.csv";

	@Override
	public void start(Stage primaryStage) {
		try {

			Utility.createFile(logFile);
			Utility.createFile(resultFile);

			TravelPlannerApp travelPlannerApp = new TravelPlannerApp();

			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("view/application.fxml"));
			SplitPane pane = (SplitPane) loader.load();

			ApplicationController controller = (ApplicationController) loader.getController();
			controller.setPrimaryStage(primaryStage);
			controller.setTasStart(travelPlannerApp);
			controller.setCompositeService(travelPlannerApp.getAssistanceService());
			controller.setProbe(travelPlannerApp.getMonitor());
			controller.setConfigurations(travelPlannerApp.getConfigurations());
			controller.setServiceRegistry(travelPlannerApp.getServiceRegistry());

			Scene scene = new Scene(pane);
			scene.getStylesheets().add(getClass().getResource("view/application.css").toExternalForm());

			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent arg0) {
					System.exit(0);
				}
			});

			primaryStage.setScene(scene);
			primaryStage.setTitle("Travel Planner Application");
			primaryStage.show();

			Screen screen = Screen.getPrimary();
			Rectangle2D bounds = screen.getVisualBounds();

			primaryStage.setX(bounds.getMinX());
			primaryStage.setY(bounds.getMinY());
			primaryStage.setWidth(bounds.getWidth());
			primaryStage.setHeight(bounds.getHeight());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
