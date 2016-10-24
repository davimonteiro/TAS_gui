package application.view.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import application.log.Log;
import application.log.LogEntry;
import application.log.Report;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;


public class LogController implements Initializable{	
	
	@FXML
	private TableView<LogEntry> logTableView;
	
	@FXML
	private TextField filterTextField;
	
	@FXML
	private DatePicker fromDatePicker;
	
	@FXML
	private DatePicker toDatePicker;
	
	@FXML
	private Button clearButton;
	
	@FXML
	private Button reportButton;
	
	private Stage stage;
	
	private FilteredList<LogEntry> filteredData = new FilteredList<>(Log.logData,p->true);
	
    private final static String PATTERN = "yyyy-MM-dd";


	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {			
		this.generateTableView();
		this.setButtons();
		this.initializeDatePicker();
		this.initializeFilter();
	}
	
	public void setStage(Stage stage){
		this.stage=stage;
	}
	
	private void setButtons(){
		clearButton.setOnAction(event->{
			Log.clear();
		});
		
		
		reportButton.setOnAction(event->{
			
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File("results" + File.separator));
			fileChooser.setTitle("Save Report");
			File file = fileChooser.showSaveDialog(stage);
			if (file != null) {
				String filePath=file.getPath()+".pdf";
		    	Report report=new Report(filePath);
		    	report.open();
		    	report.addTitle("Tele Assistance System");
		    	report.addSubTitle("Diagnosis Report");
		    	
		    	report.addEmptyLine(1);

		    	StringBuilder date=new StringBuilder();
		    	
		    	if(fromDatePicker.getValue()!=null)
		    		date.append("From: "+fromDatePicker.getValue().toString());
		    	
		    	if(toDatePicker.getValue()!=null)
		    		date.append("             To: "+toDatePicker.getValue().toString());
		    		
		    	report.addSentence(date.toString());
		    	
		    	if(!filterTextField.getText().isEmpty())
		    		report.addSentence("Filter: "+filterTextField.getText());
		    	
		    	report.addEmptyLine(1);
		    	String[] columns={"Time","Title","Message"};

		    	List<String> values=new ArrayList<>();
		    	for(LogEntry entry:filteredData){
		    		values.add(entry.getTime());
		    		values.add(entry.getTitle());
		    		values.add(entry.getMessage());
		    	}
		    	
			    float[] columnWidths = {1f, 1f, 2f};
			    
		    	report.addTable(columns,columnWidths,values);
		    	report.close();	
		    	
		    	if (Desktop.isDesktopSupported()) {
		    	    try {
		    	        File pdf = new File(filePath);
		    	        Desktop.getDesktop().open(pdf);
		    	    } catch (IOException ex) {
		    	    	return;
		    	    }
		    	}
		    	
		    }
		});
	}
	
	private void initializeDatePicker(){
        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = 
                DateTimeFormatter.ofPattern(PATTERN);
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        };             
        fromDatePicker.setConverter(converter);
        fromDatePicker.setPromptText(PATTERN.toLowerCase());
        //fromDatePicker.requestFocus();
        
        toDatePicker.setConverter(converter);
        toDatePicker.setPromptText(PATTERN.toLowerCase());
        //toDatePicker.requestFocus();
        
        
        final Callback<DatePicker, DateCell> dayCellFactory = 
                new Callback<DatePicker, DateCell>() {
                    @Override
                    public DateCell call(final DatePicker datePicker) {
                        return new DateCell() {
                            @Override
                            public void updateItem(LocalDate item, boolean empty) {
                                super.updateItem(item, empty);
                               
                                if (item.isBefore(
                                		fromDatePicker.getValue())
                                    ) {
                                        setDisable(true);
                                        setStyle("-fx-background-color: #ffc0cb;");
                                }   
                        }
                    };
                }
            };
            toDatePicker.setDayCellFactory(dayCellFactory);
	}
	
	private void initializeFilter(){
		
		fromDatePicker.valueProperty().addListener((observable, oldValue, newValue)->{
            filteredData.setPredicate(log -> {
                if (newValue == null) {
                    return true;
                }
                String date=log.getTime().split(" ")[0];
                String fromDate=newValue.toString();
                
                return date.compareTo(fromDate)>=0 ;
             });
		});
		
		toDatePicker.valueProperty().addListener((observable, oldValue, newValue)->{
            filteredData.setPredicate(log -> {
                if (newValue == null) {
                    return true;
                }

                String date=log.getTime().split(" ")[0];
                String fromDate=newValue.toString();
                
                return date.compareTo(fromDate)<=0 ;
             });
		});
		
		
		filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			
            filteredData.setPredicate(log -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (log.getTitle().toLowerCase().indexOf(lowerCaseFilter) != -1) {
                    return true; 
                } else if (log.getMessage().toLowerCase().indexOf(lowerCaseFilter) != -1) {
                    return true; 
                }
                return false; 
            });
        });

        SortedList<LogEntry> sortedData = new SortedList<>(filteredData);
                
        sortedData.comparatorProperty().bind(logTableView.comparatorProperty());

        logTableView.setItems(sortedData);
	}
	
	private void generateTableView(){
		TableColumn<LogEntry,String> timeColumn = new TableColumn<LogEntry,String>("Time");
		timeColumn.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("time"));
		timeColumn.prefWidthProperty().bind(logTableView.widthProperty().divide(6).multiply(1));

		TableColumn<LogEntry,String> titleColumn = new TableColumn<LogEntry,String>("Title");
		titleColumn.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("title"));
		titleColumn.prefWidthProperty().bind(logTableView.widthProperty().divide(6));

		TableColumn<LogEntry,String> messageColumn = new TableColumn<LogEntry,String>("Message");
		messageColumn.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("message"));
		messageColumn.prefWidthProperty().bind(logTableView.widthProperty().divide(6).multiply(4));

		logTableView.getColumns().addAll(timeColumn,titleColumn,messageColumn);	
		timeColumn.setSortType(TableColumn.SortType.DESCENDING);
		logTableView.getSortOrder().add(timeColumn);
	}

}
