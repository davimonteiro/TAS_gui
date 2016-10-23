package application.view.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import profile.ProfileExecutor;
import service.composite.CompositeService;
import service.registry.ServiceRegistry;
import tas.services.assistance.AssistanceServiceCostProbe;
import tas.configuration.TASConfiguration;
import tas.configuration.TASStart;
import application.MainGui;
import application.Node.ArrowNode;
import application.Node.ArrowNode.RightArrowNode;
import application.Node.InstanceNode;
import application.model.CostEntry;
import application.model.PerformanceEntry;
import application.model.ReliabilityEntry;
import application.utility.FileManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ApplicationController implements Initializable {

	Stage primaryStage;
	ChartController chartController;
	TableViewController tableViewController;

	String workflowPath = "resources" + File.separator + "TeleAssistanceWorkflow.txt";
	String resultFilePath = "results" + File.separator + "result.csv";
	String logFilePath = "results" + File.separator + "log.csv";

	ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(5);

	CompositeService compositeService;
	AssistanceServiceCostProbe probe;
	ServiceRegistry serviceRegistry;
	TASStart tasStart;

	Set<Button> profileRuns = new HashSet<>();

	int maxSteps;

	Set<String> registeredServices = new HashSet<>();

	@FXML
	ListView<AnchorPane> serviceListView;

	@FXML
	ListView<AnchorPane> profileListView;

	@FXML
	TextArea workflowTextArea;

	@FXML
	TableView<ReliabilityEntry> reliabilityTableView;

	@FXML
	TableView<CostEntry> costTableView;

	@FXML
	TableView<PerformanceEntry> performanceTableView;

	@FXML
	MenuItem openWorkflowMenuItem;

	@FXML
	MenuItem openServicesMenuItem;

	@FXML
	MenuItem configureMenuItem;

	@FXML
	MenuItem openLogMenuItem;

	@FXML
	MenuItem openProfileMenuItem;

	@FXML
	MenuItem saveRunMenuItem;

	@FXML
	MenuItem saveLogMenuItem;

	@FXML
	AnchorPane reliabilityChartPane;

	@FXML
	AnchorPane costChartPane;

	@FXML
	AnchorPane performanceChartPane;

	@FXML
	ScrollPane serviceScrollPane;

	@FXML
	ScrollPane profileScrollPane;

	@FXML
	MenuItem openRunMenuItem;

	@FXML
	Button aboutButton;

	@FXML
	MenuItem saveReliabilityGraphMenuItem;

	@FXML
	MenuItem saveCostGraphMenuItem;

	@FXML
	MenuItem savePerformanceGraphMenuItem;

	@FXML
	MenuItem helpMenuItem;

	@FXML
	MenuItem exampleScenariosMenuItem;

	@FXML
	ToolBar toolBar;

	@FXML
	Button configureButton;

	@FXML
	AnchorPane canvasPane;

	ProgressBar progressBar;
	Label invocationLabel;

	Map<String, TASConfiguration> configurations;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		chartController = new ChartController(reliabilityChartPane, costChartPane, performanceChartPane);
		tableViewController = new TableViewController(reliabilityTableView, costTableView, performanceTableView);

		try {
			String content = new String(Files.readAllBytes(Paths.get(workflowPath)));
			workflowTextArea.setText(content);

			this.generateSequenceDiagram(workflowPath);

		} catch (IOException e) {
			e.printStackTrace();
		}

		this.fillProfiles();
		this.setButton();

		scheduExec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						serviceListView.getItems().clear();
						final Set<String> services = compositeService.getCache().getServices();

						for (final String service : registeredServices) {

							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									if (services != null && services.contains(service))
										addService(service, true);
									else
										addService(service, false);
								}
							});
						}
					}
				});
			}
		}, 0, 1000, TimeUnit.MILLISECONDS);

	}

	private void generateSequenceDiagram(String workflowPath) {

		canvasPane.getChildren().clear();

		FileManager fileManager = new FileManager(workflowPath);
		fileManager.setMode(FileManager.READING);
		fileManager.open();
		String line;

		double initialX = 50.0;
		double initialY = 50.0;
		double instanceNodeLen = 300;
		InstanceNode assistanceServiceNode = new InstanceNode(initialX, initialY, "TeleAssistanceService",
				instanceNodeLen);
		canvasPane.getChildren().add(assistanceServiceNode);

		double layoutX = 50.0;
		double layoutY = assistanceServiceNode.getCenterY();
		double intervalX = 200;
		double intervalY = 30;

		Map<String, InstanceNode> instanceNodes = new HashMap<>();

		while ((line = fileManager.readLine()) != null) {
			if (line.contains(".")) {
				// System.out.println(line);
				String[] strs = line.split(" ");
				for (int i = 0; i < strs.length; i++) {
					if (strs[i].contains(".") && !strs[i].contains("this")) {
						// System.out.println(strs[i]);
						String[] values = strs[i].split("\\.");
						// System.out.println(values.length);
						String service = values[0].replaceAll("\\s*", "");
						String operation = values[1].replaceAll("\\s*", "");

						if (!instanceNodes.containsKey(service)) {
							layoutX += intervalX;
							InstanceNode serviceNode = new InstanceNode(layoutX, initialY, service, instanceNodeLen);
							canvasPane.getChildren().add(serviceNode);
							instanceNodes.put(service, serviceNode);
							// System.out.println(serviceNode.getX());
						}

						InstanceNode serviceNode = instanceNodes.get(service);
						layoutY += intervalY;
						double length = serviceNode.getCenterX() - assistanceServiceNode.getCenterX();

						RightArrowNode arrowNode = new RightArrowNode(assistanceServiceNode.getCenterX(), layoutY,
								operation, length);
						canvasPane.getChildren().add(arrowNode);

					}
				}

			}
		}
		fileManager.close();
	}

	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	public void setConfigurations(Map<String, TASConfiguration> configurations) {
		this.configurations = configurations;
		this.addItems();
	}

	public void setCompositeService(CompositeService service) {
		this.compositeService = service;
	}

	public void setProbe(AssistanceServiceCostProbe probe) {
		this.probe = probe;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		openServicesMenuItem.fire();
	}

	public void setTasStart(TASStart tasStart) {
		this.tasStart = tasStart;
	}

	private void addItems() {
		final ToggleGroup group = new ToggleGroup();
		boolean selected = true;
		for (String key : configurations.keySet()) {
			ToggleButton button = new ToggleButton(key);
			button.setToggleGroup(group);
			button.setUserData(key);
			if (selected) {
				button.setSelected(true);
				selected = false;
			}

			Separator separator = new Separator();
			separator.setPrefWidth(27);
			toolBar.getItems().addAll(button, separator);
		}
		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observable, final Toggle oldValue,
					final Toggle newValue) {
				if ((newValue == null)) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							group.selectToggle(oldValue);
						}
					});
				}

				configurations.get(oldValue.getUserData()).removeConfiguration();
				configurations.get(newValue.getUserData()).setConfiguration();

			}
		});
		progressBar = new ProgressBar(0);
		invocationLabel = new Label();
		toolBar.getItems().addAll(new Label("Progress "), progressBar, invocationLabel);
	}

	private void setButton() {

		openWorkflowMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources" + File.separator));
				fileChooser.setTitle("Select workflow");
				FileChooser.ExtensionFilter extension = new FileChooser.ExtensionFilter("Add Files(*.txt)", "*.txt");
				fileChooser.getExtensionFilters().add(extension);
				File file = fileChooser.showOpenDialog(primaryStage);
				if (file != null) {
					System.out.println(file.getPath());
					try {
						String content = new String(Files.readAllBytes(file.toPath()));
						workflowPath = file.getPath();
						workflowTextArea.setText(content);

						generateSequenceDiagram(workflowPath);

						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								for (Button runButton : profileRuns)
									runButton.setDisable(false);
							}
						});

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		openServicesMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				List<String> services = serviceRegistry.getAllServices();
				for (String service : services) {
					if (!service.equals("TeleAssistanceService"))
						registeredServices.add(service);
				}
			}
		});

		configureMenuItem.setOnAction(event -> {
			try {

				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainGui.class.getResource("view/configureDialog.fxml"));
				GridPane configurePane = (GridPane) loader.load();

				Stage dialogStage = new Stage();
				dialogStage.setTitle("ReSeP Configuration");

				ConfigureController controller = (ConfigureController) loader.getController();
				controller.setStage(dialogStage);
				// controller.setService(tasStart.getService(serviceName));

				Scene dialogScene = new Scene(configurePane);
				dialogScene.getStylesheets().add(MainGui.class.getResource("view/application.css").toExternalForm());

				dialogStage.initOwner(primaryStage);
				dialogStage.setScene(dialogScene);
				dialogStage.setResizable(false);
				dialogStage.show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		configureButton.setOnAction(event -> {
			try {

				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainGui.class.getResource("view/configureDialog.fxml"));
				GridPane configurePane = (GridPane) loader.load();

				Stage dialogStage = new Stage();
				dialogStage.setTitle("ReSep Configuration");

				ConfigureController controller = (ConfigureController) loader.getController();
				controller.setStage(dialogStage);
				// controller.setService(tasStart.getService(serviceName));

				Scene dialogScene = new Scene(configurePane);
				dialogScene.getStylesheets().add(MainGui.class.getResource("view/application.css").toExternalForm());

				dialogStage.initOwner(primaryStage);
				dialogStage.setScene(dialogScene);
				dialogStage.setResizable(false);
				dialogStage.show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		openProfileMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources" + File.separator));
				fileChooser.setTitle("Select profile");
				FileChooser.ExtensionFilter extension = new FileChooser.ExtensionFilter("Add Files(*.xml)", "*.xml");
				fileChooser.getExtensionFilters().add(extension);
				File file = fileChooser.showOpenDialog(primaryStage);
				if (file != null) {
					System.out.println(file.getPath());
					addProfile(file.getPath());
				}
			}
		});

		saveRunMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources" + File.separator));
				fileChooser.setTitle("Save Run");
				File file = fileChooser.showSaveDialog(primaryStage);
				if (file != null) {
					try {
						Files.copy(Paths.get(resultFilePath), Paths.get(file.getPath() + ".csv"),
								StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		openRunMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Select profile");
				FileChooser.ExtensionFilter extension = new FileChooser.ExtensionFilter("Add Files(*.csv)", "*.csv");
				fileChooser.getExtensionFilters().add(extension);
				File file = fileChooser.showOpenDialog(primaryStage);
				if (file != null) {
					try {
						BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
						String line;
						int invocationNum = 0;
						while ((line = br.readLine()) != null) {
							String[] str = line.split(",");
							if (str.length >= 3) {
								invocationNum = Integer.parseInt(str[0]);
							}
						}
						br.close();

						chartController.generateReliabilityChart(file.getPath(), invocationNum);
						chartController.generateCostChart(file.getPath(), invocationNum);
						chartController.generatePerformanceChart(file.getPath(), invocationNum);
						tableViewController.fillReliabilityDate(file.getPath());
						tableViewController.fillCostData(file.getPath());
						tableViewController.fillPerformanceData(file.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		saveLogMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources" + File.separator));
				fileChooser.setTitle("Save Log");
				File file = fileChooser.showSaveDialog(primaryStage);
				if (file != null) {
					try {
						Files.copy(Paths.get(logFilePath), Paths.get(file.getPath() + ".csv"),
								StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		saveReliabilityGraphMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					FileChooser fileChooser = new FileChooser();
					fileChooser.setInitialDirectory(new File("results" + File.separator));
					fileChooser.setTitle("Save Reliability Graph");
					File file = fileChooser.showSaveDialog(primaryStage);
					if (file != null) {
						try {
							SnapshotParameters param = new SnapshotParameters();
							param.setDepthBuffer(true);
							WritableImage snapshot = chartController.reliabilityChart.snapshot(param, null);
							BufferedImage tempImg = SwingFXUtils.fromFXImage(snapshot, null);

							File outputfile = new File(file.getPath() + ".png");
							ImageIO.write(tempImg, "png", outputfile);

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		saveCostGraphMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					FileChooser fileChooser = new FileChooser();
					fileChooser.setInitialDirectory(new File("results" + File.separator));
					fileChooser.setTitle("Save Cost Graph");
					File file = fileChooser.showSaveDialog(primaryStage);
					if (file != null) {
						try {
							SnapshotParameters param = new SnapshotParameters();
							param.setDepthBuffer(true);
							WritableImage snapshot = chartController.costChart.snapshot(param, null);
							BufferedImage tempImg = SwingFXUtils.fromFXImage(snapshot, null);

							File outputfile = new File(file.getPath() + ".png");
							ImageIO.write(tempImg, "png", outputfile);

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		savePerformanceGraphMenuItem.setOnAction(event -> {
			try {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("results" + File.separator));
				fileChooser.setTitle("Save Performance Graph");
				File file = fileChooser.showSaveDialog(primaryStage);
				if (file != null) {
					try {
						SnapshotParameters param = new SnapshotParameters();
						param.setDepthBuffer(true);
						WritableImage snapshot = chartController.performanceChart.snapshot(param, null);
						BufferedImage tempImg = SwingFXUtils.fromFXImage(snapshot, null);

						File outputfile = new File(file.getPath() + ".png");
						ImageIO.write(tempImg, "png", outputfile);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		aboutButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					// System.out.println("about");

					FXMLLoader loader = new FXMLLoader();
					loader.setLocation(MainGui.class.getResource("view/aboutDialog.fxml"));
					AnchorPane aboutPane = (AnchorPane) loader.load();

					Stage dialogStage = new Stage();
					dialogStage.setTitle("About");
					dialogStage.setResizable(false);

					Scene dialogScene = new Scene(aboutPane);
					dialogStage.initOwner(primaryStage);
					dialogStage.setScene(dialogScene);
					dialogStage.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		helpMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					FXMLLoader loader = new FXMLLoader();
					loader.setLocation(MainGui.class.getResource("view/helpDialog.fxml"));
					AnchorPane helpPane = (AnchorPane) loader.load();

					Stage dialogStage = new Stage();
					dialogStage.setTitle("Help");
					dialogStage.setResizable(false);

					Scene dialogScene = new Scene(helpPane);
					dialogStage.initOwner(primaryStage);
					dialogStage.setScene(dialogScene);
					dialogStage.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		openLogMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					FXMLLoader loader = new FXMLLoader();
					loader.setLocation(MainGui.class.getResource("view/logDialog.fxml"));
					AnchorPane helpPane = (AnchorPane) loader.load();

					Stage dialogStage = new Stage();
					dialogStage.setTitle("Log");

					LogController controller = (LogController) loader.getController();
					controller.setStage(dialogStage);

					Scene dialogScene = new Scene(helpPane);
					dialogScene.getStylesheets()
							.add(MainGui.class.getResource("view/application.css").toExternalForm());

					dialogStage.initOwner(primaryStage);
					dialogStage.setScene(dialogScene);
					dialogStage.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		/*
		 * exampleScenariosMenuItem.setOnAction(new EventHandler<ActionEvent>()
		 * {
		 * 
		 * @Override public void handle(ActionEvent event) { try { FXMLLoader
		 * loader = new FXMLLoader(); AnchorPane helpPane = null; if
		 * (tasStart.getTasConfiguration() instanceof
		 * TASConfigurationBasicScenarios){
		 * loader.setLocation(MainGui.class.getResource(
		 * "view/ExampleScenarios.fxml")); helpPane = (AnchorPane)
		 * loader.load();
		 * 
		 * ExampleScenarios exampleScenarios =
		 * (ExampleScenarios)loader.getController();
		 * exampleScenarios.setScenario((TASConfigurationBasicScenarios)tasStart
		 * .getTasConfiguration()); } else if(tasStart.getTasConfiguration()
		 * instanceof TASConfigurationActivFORMS){
		 * loader.setLocation(MainGui.class.getResource(
		 * "view/ExampleScenariosActivFORMS.fxml")); helpPane = (AnchorPane)
		 * loader.load();
		 * 
		 * ExampleScenariosActivFORMS exampleScenarios =
		 * (ExampleScenariosActivFORMS)loader.getController();
		 * exampleScenarios.setScenario((TASConfigurationActivFORMS)tasStart.
		 * getTasConfiguration());
		 * 
		 * }
		 * 
		 * Stage dialogStage = new Stage();
		 * dialogStage.setTitle("Example Scenarios");
		 * 
		 * Scene dialogScene = new Scene(helpPane);
		 * dialogStage.initOwner(primaryStage);
		 * dialogScene.getStylesheets().add(MainGui.class.getResource(
		 * "view/application.css").toExternalForm());
		 * 
		 * 
		 * dialogStage.setScene(dialogScene); dialogStage.show(); } catch
		 * (Exception e) { e.printStackTrace(); } } });
		 */
	}

	private void fillProfiles() {
		File folder = new File("resources" + File.separator + "files" + File.separator);
		File[] files = folder.listFiles();

		try {
			for (File file : files) {
				if (file.isFile()) {
					// System.out.println(file.getName());
					if (file.getName().lastIndexOf('.') > 0)
						this.addProfile(file.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// this.addProfile("resources/files/inputProfile1.xml");
		// this.addProfile("/inputProfile2.xml");
	}

	private void addProfile(String profilePath) {
		final String path = profilePath;

		AnchorPane itemPane = new AnchorPane();
		// itemPane.setPrefHeight(40);
		// itemPane.setMinHeight(40);

		Button inspectButton = new Button();
		inspectButton.setPrefWidth(32);
		inspectButton.setPrefHeight(32);
		inspectButton.setLayoutY(5);
		inspectButton.setId("inspectButton");
		// inspectButton.setStyle("-fx-background-image:
		// url('resources/images/inspect.png');");

		inspectButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {

					FXMLLoader loader = new FXMLLoader();
					loader.setLocation(MainGui.class.getResource("view/inputProfileDialog.fxml"));
					AnchorPane pane = (AnchorPane) loader.load();

					Stage dialogStage = new Stage();
					dialogStage.setTitle("Input Profile");

					InputProfileController controller = (InputProfileController) loader.getController();
					controller.setStage(dialogStage);
					controller.viewProfile(path);

					Scene dialogScene = new Scene(pane);
					dialogScene.getStylesheets()
							.add(MainGui.class.getResource("view/application.css").toExternalForm());

					dialogStage.initOwner(primaryStage);
					dialogStage.setScene(dialogScene);
					dialogStage.show();

					/*
					 * final Stage dialog = new Stage();
					 * dialog.initModality(Modality.APPLICATION_MODAL);
					 * dialog.initOwner(primaryStage); final TextArea textArea =
					 * new TextArea(); String content = new
					 * String(Files.readAllBytes(Paths.get(path)));
					 * textArea.setText(content); dialog.setOnCloseRequest(new
					 * EventHandler<WindowEvent>() {
					 * 
					 * @Override public void handle(WindowEvent event) { String
					 * newContent = textArea.getText(); try { PrintWriter out =
					 * new PrintWriter(new BufferedWriter(new FileWriter(path,
					 * false))); out.write(newContent); out.flush();
					 * out.close(); } catch (IOException e) {
					 * e.printStackTrace(); } } }); //
					 * textArea.setEditable(false); Scene dialogScene = new
					 * Scene(textArea, 800, 600); dialog.setScene(dialogScene);
					 * dialog.show();
					 */

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		// profileInspects.put(inspectButton, profilePath);

		Button runButton = new Button();
		runButton.setPrefWidth(32);
		runButton.setPrefHeight(32);
		runButton.setLayoutY(5);
		runButton.setId("runButton");
		profileRuns.add(runButton);
		if (this.workflowPath == null)
			runButton.setDisable(true);
		// profileInspects.put(runButton, profilePath);

		Label label = new Label();
		label.setLayoutY(15);

		// Paths.get(profilePath).getFileName();
		// String[] strs=profilePath.split("/");
		label.setText(Paths.get(profilePath).getFileName().toString().split("\\.")[0]);

		final Circle circle = new Circle();
		circle.setLayoutY(20);
		circle.setFill(Color.GREEN);
		circle.setRadius(10);

		runButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				if (runButton.getId().equals("runButton")) {
					// System.out.println("start workflow");
					// System.out.println("Run workflow with profile!!");

					/*
					 * if(tasStart.isPaused()){ tasStart.go();
					 * 
					 * Platform.runLater(new Runnable() {
					 * 
					 * @Override public void run() {
					 * circle.setFill(Color.DARKRED);
					 * runButton.setId("stopButton"); } }); }
					 * 
					 * else{
					 */

					probe.reset();

					Task<Void> task = new Task<Void>() {
						@Override
						protected Void call() throws Exception {

							System.out.println("Task has been called!!");
							System.out.println(workflowPath);

							if (workflowPath != null) {

								System.out.println(workflowPath);

								System.out.println("Set circle color!!");

								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										circle.setFill(Color.DARKRED);
										runButton.setId("stopButton");
									}
								});

								System.out.println("Before executing workflow!!");

								tasStart.executeWorkflow(workflowPath, path);

								System.out.println("Finish executing workflow!!");

								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										circle.setFill(Color.GREEN);
										runButton.setId("runButton");
										chartController.clear();
										tableViewController.clear();
										chartController.generateReliabilityChart(resultFilePath,
												tasStart.getCurrentSteps());
										chartController.generateCostChart(resultFilePath, tasStart.getCurrentSteps());
										chartController.generatePerformanceChart(resultFilePath,
												tasStart.getCurrentSteps());
										tableViewController.fillReliabilityDate(resultFilePath);
										tableViewController.fillCostData(resultFilePath);
										tableViewController.fillPerformanceData(resultFilePath);
									}
								});
							}
							return null;
						}
					};

					// System.out.println("Bind progress bar with task!!");

					Thread thread = new Thread(task);
					thread.setDaemon(true);
					thread.start();

					System.out.println("Start task!!");
					ProfileExecutor.readFromXml(path);
					maxSteps = ProfileExecutor.profile.getMaxSteps();
					Task<Void> progressTask = new Task<Void>() {
						@Override
						protected Void call() throws Exception {
							while (probe.workflowInvocationCount < maxSteps) {
								// System.out.println(probe.workflowInvocationCount);
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										invocationLabel.setText(" " + probe.workflowInvocationCount + " / " + maxSteps);
									}
								});
								// invocationLabel.setText(""+probe.workflowInvocationCount);
								updateProgress(probe.workflowInvocationCount, maxSteps);
								Thread.sleep(1000);
							}
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									invocationLabel.setText("" + maxSteps + " / " + maxSteps);
								}
							});
							updateProgress(probe.workflowInvocationCount, maxSteps);
							return null;
						}
					};
					progressBar.progressProperty().bind(progressTask.progressProperty());
					new Thread(progressTask).start();
				} else {

					System.out.println("stop workflow");
					tasStart.stop();
					// tasStart.pause();

					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							circle.setFill(Color.GREEN);
							runButton.setId("runButton");
							chartController.clear();
							tableViewController.clear();
							chartController.generateReliabilityChart(resultFilePath, tasStart.getCurrentSteps());
							chartController.generateCostChart(resultFilePath, tasStart.getCurrentSteps());
							chartController.generatePerformanceChart(resultFilePath, tasStart.getCurrentSteps());
							tableViewController.fillReliabilityDate(resultFilePath);
							tableViewController.fillCostData(resultFilePath);
							tableViewController.fillPerformanceData(resultFilePath);
						}
					});
				}
			}
		});

		AnchorPane.setLeftAnchor(circle, 10.0);
		AnchorPane.setLeftAnchor(label, 40.0);
		AnchorPane.setRightAnchor(inspectButton, 60.0);
		AnchorPane.setRightAnchor(runButton, 10.0);

		itemPane.getChildren().setAll(circle, label, runButton, inspectButton);

		profileListView.getItems().add(itemPane);
	}

	private void addService(String serviceName, boolean state) {

		AnchorPane itemPane = new AnchorPane();
		// itemPane.setPrefHeight(40);
		// itemPane.setMinHeight(40);

		Button inspectButton = new Button();
		inspectButton.setPrefWidth(32);
		inspectButton.setPrefHeight(32);
		inspectButton.setLayoutY(5);
		inspectButton.setId("inspectButton");
		inspectButton.setOnAction(event -> {

			try {

				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainGui.class.getResource("view/serviceProfileDialog.fxml"));
				AnchorPane helpPane = (AnchorPane) loader.load();

				Stage dialogStage = new Stage();
				dialogStage.setTitle(serviceName);

				ServiceProfileController controller = (ServiceProfileController) loader.getController();
				controller.setStage(dialogStage);
				controller.setServiceProfileClasses(tasStart.getServiceProfileClasses());
				controller.setService(tasStart.getService(serviceName));

				Scene dialogScene = new Scene(helpPane);
				dialogScene.getStylesheets().add(MainGui.class.getResource("view/application.css").toExternalForm());

				dialogStage.initOwner(primaryStage);
				dialogStage.setScene(dialogScene);
				dialogStage.setResizable(false);
				dialogStage.show();

			} catch (Exception e) {
				e.printStackTrace();
			}

		});

		Label label = new Label();
		label.setLayoutY(15);
		label.setText(serviceName);

		Circle circle = new Circle();
		circle.setLayoutY(20);
		if (state)
			circle.setFill(Color.GREEN);
		else
			circle.setFill(Color.DARKRED);
		circle.setRadius(10);

		AnchorPane.setLeftAnchor(circle, 10.0);
		AnchorPane.setLeftAnchor(label, 40.0);
		AnchorPane.setRightAnchor(inspectButton, 10.0);
		itemPane.getChildren().setAll(circle, label, inspectButton);

		serviceListView.getItems().add(itemPane);
	}

	/*
	 * private void fillMockProfiles(String name, int length){ for(int
	 * i=0;i<length;i++){ this.addProfile(name+"i"); } }
	 * 
	 * private void fillMockServices(String name, int length){
	 * 
	 * for(int i=0;i<length;i++){
	 * 
	 * AnchorPane itemPane=new AnchorPane(); itemPane.setPrefHeight(40);
	 * itemPane.setMinHeight(40);
	 * 
	 * Button inspectButton=new Button(); inspectButton.setPrefWidth(32);
	 * inspectButton.setPrefHeight(32); inspectButton.setLayoutY(5);
	 * inspectButton.setId("inspectButton");
	 * 
	 * Label label=new Label(); label.setLayoutY(15); label.setText(name+i);
	 * 
	 * Circle circle = new Circle(); circle.setLayoutY(20);
	 * circle.setFill(Color.GREEN); circle.setRadius(10);
	 * 
	 * AnchorPane.setLeftAnchor(circle, 10.0);
	 * AnchorPane.setLeftAnchor(label,40.0);
	 * AnchorPane.setRightAnchor(inspectButton, 10.0);
	 * itemPane.getChildren().setAll(circle,label,inspectButton);
	 * 
	 * serviceVBox.getChildren().add(itemPane);
	 * serviceVBox.getChildren().add(new Separator()); } }
	 */

}
