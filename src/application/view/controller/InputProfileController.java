package application.view.controller;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import application.utility.Convert;
import profile.InputProfile;
import profile.InputProfileValue;
import profile.InputProfileVariable;
import profile.ProfileExecutor;
import service.atomic.ServiceProfileAttribute;


public class InputProfileController implements Initializable {

	private Stage stage;
	
	@FXML
	TextArea profileTextArea;
	
	@FXML
	TextField maxStepsTextField;
	
	@FXML
	Label qosRequirementLabel;
	
	@FXML
	ListView<String> variableListView;
	
	@FXML
	TableView<ValueEntry> valueTableView;
	
	@FXML
	TextField dataTextField;
	
	@FXML
	TextField ratioTextField;
	
	@FXML
	Button addValueButton;
	
	@FXML
	Button saveButton1;
	
	@FXML
	Button saveButton2;
	
	ObservableList<ValueEntry> valueData = FXCollections.observableArrayList();
	InputProfileVariable currentVariable=null;

	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initializeValueTable();
	}

	public void setStage(Stage stage){
		this.stage=stage;
	}
	
	private void initializeValueTable(){
		
		valueTableView.setEditable(true);
           
		TableColumn<ValueEntry,String> nameColumn = new TableColumn<ValueEntry,String>("Value");
		nameColumn.setCellValueFactory(new PropertyValueFactory<ValueEntry, String>("name"));
		nameColumn.prefWidthProperty().bind(valueTableView.widthProperty().divide(4));
			
		TableColumn<ValueEntry,String> typeColumn = new TableColumn<ValueEntry,String>("Type");
		typeColumn.setCellValueFactory(new PropertyValueFactory<ValueEntry, String>("type"));
		typeColumn.prefWidthProperty().bind(valueTableView.widthProperty().divide(4));

	    Callback<TableColumn<ValueEntry, String>, TableCell<ValueEntry, String>> cellFactory =
	    		new Callback<TableColumn<ValueEntry, String>, TableCell<ValueEntry, String>>() {
	    			public TableCell<ValueEntry, String> call(TableColumn<ValueEntry, String> p) {
	    					return new EditingCell();
	                }
	        };
	        
		TableColumn<ValueEntry,String> valueColumn = new TableColumn<ValueEntry,String>("Data");
		valueColumn.setCellValueFactory(new PropertyValueFactory<ValueEntry, String>("data"));
		valueColumn.prefWidthProperty().bind(valueTableView.widthProperty().divide(4));
		valueColumn.setCellFactory(cellFactory);		

		
		valueColumn.setOnEditCommit(
				new EventHandler<CellEditEvent<ValueEntry, String>>() {
	                    @Override
	                    public void handle(CellEditEvent<ValueEntry, String> t) {
	                    	ValueEntry value=(ValueEntry) t.getTableView().getItems().get(
	                                t.getTablePosition().getRow());
	                    	//attribute.setValue(t.getNewValue());
	                    	
	                    	Object data= value.getRealData(t.getNewValue());
	                    	if(data!=null){
		                    	value.getProfileValue().setData(data);
	                    	}
	                    }
	                }
	        );
		
		TableColumn<ValueEntry,String> ratioColumn = new TableColumn<ValueEntry,String>("Ratio");
		ratioColumn.setCellValueFactory(new PropertyValueFactory<ValueEntry, String>("ratio"));
		ratioColumn.prefWidthProperty().bind(valueTableView.widthProperty().divide(4));
		ratioColumn.setCellFactory(cellFactory);
		
		ratioColumn.setOnEditCommit(
				new EventHandler<CellEditEvent<ValueEntry, String>>() {
	                    @Override
	                    public void handle(CellEditEvent<ValueEntry, String> t) {
	                    	ValueEntry value=(ValueEntry) t.getTableView().getItems().get(
	                                t.getTablePosition().getRow());
	                    	double ratio=Double.parseDouble(t.getNewValue());
	                    	value.getProfileValue().setRatio(ratio);
	                    }
	                }
	        );
			
		valueTableView.setItems(valueData);
		valueTableView.getColumns().addAll(nameColumn,typeColumn,valueColumn,ratioColumn);
	}

	public void viewProfile(String filePath){
		try{
			String content = new String(Files.readAllBytes(Paths.get(filePath)));
			
	        Source xmlInput = new StreamSource(new StringReader(content));
	        StringWriter stringWriter = new StringWriter();
	        StreamResult xmlOutput = new StreamResult(stringWriter);
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", 5);
	        Transformer transformer = transformerFactory.newTransformer(); 
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.transform(xmlInput, xmlOutput);	        
			profileTextArea.setText(xmlOutput.getWriter().toString());
			
			stage.setOnCloseRequest(event -> {
			});
			
			
			saveButton1.setOnAction(event->{
				String newContent = profileTextArea.getText();
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, false)));
					out.write(newContent);
					out.flush();
					out.close();
					stage.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			
			ProfileExecutor.readFromXml(filePath);
			InputProfile profile=ProfileExecutor.profile;
			maxStepsTextField.setText(profile.getMaxSteps()+"");
			maxStepsTextField.textProperty().addListener(new ChangeListener<String>() {
			    @Override
			    public void changed(ObservableValue<? extends String> observable,
			            String oldValue, String newValue) {
			    	profile.setMaxSteps(Integer.parseInt(newValue));
			    }
			});
			
			qosRequirementLabel.setText(profile.getQosRequirement());

			
			variableListView.getItems().addAll(profile.getVariableNames());
			variableListView.getSelectionModel().selectedItemProperty().addListener(
	                new ChangeListener<String>() {
	                    public void changed(ObservableValue<? extends String> val, 
	                        String oldVal, String newVal) {
	                    	
	                    	valueData.clear();
	                    	
	                    	currentVariable=profile.getVariable(newVal);
	                    	List<InputProfileValue> values=currentVariable.getValues();
	                    	for(int i=0;i<values.size();i++){
	                    		InputProfileValue value=values.get(i);
	                    		valueData.add(new ValueEntry("value "+i,value.getData().getClass().getSimpleName(),
	                    				value.getData().toString(),String.valueOf(value.getRatio()),value));
	                    	}
	                    	String type=values.get(0).getData().getClass().getSimpleName();
	                    	addValueButton.setDisable(false);
	                    	addValueButton.setOnAction(event->{
	                    		InputProfileValue newValue=new InputProfileValue(Convert.toObject(type, dataTextField.getText()),
	                    				Double.parseDouble(ratioTextField.getText()));
	                    		currentVariable.getValues().add(newValue);
	                    		valueData.add(new ValueEntry("value "+valueData.size(),newValue.getData().getClass().getSimpleName(),
	                    				newValue.getData().toString(),String.valueOf(newValue.getRatio()),newValue));
	                    		
	                    		dataTextField.clear();
	                    		ratioTextField.clear();
	                    		
	                    	});
	                    	
	                }
	            });
			
			
			saveButton2.setOnAction(event->{
				ProfileExecutor.writeToXml(filePath);
				stage.close();
			});
			
			
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
	
	public class ValueEntry{
	    private  SimpleStringProperty name;  
	    private  SimpleStringProperty type;
	    private  SimpleStringProperty data;
	    private  SimpleStringProperty ratio;
	    private  InputProfileValue profileValue;
	    
	    public ValueEntry(String name,String type,String data, String ratio,InputProfileValue profileValue){
	    	this.name=new SimpleStringProperty(name);
	    	this.type=new SimpleStringProperty(type);
	    	this.data=new SimpleStringProperty(data);
	    	this.ratio=new SimpleStringProperty(ratio);
	    	this.profileValue=profileValue;
	    }
	    	    
	    public InputProfileValue getProfileValue(){
	    	return this.profileValue;
	    }
	    
	    public void setRatio(String ratio){
	    	this.ratio=new SimpleStringProperty(ratio);
	    }
	    
	    public Object getRealData(String data){
	    	this.data=new SimpleStringProperty(data);
	    	Object realData=null;
			try {
				switch (type.get()) {
				case "boolean": 
				case "Boolean":
				{
					if (data.equals("true"))
						realData= true;
					else
						realData= false;
					break;
				}
				case "short":
				case "Short":
				{
					realData= Short.parseShort(data);
					break;
				}
				case "int": 
				case "Integer": 
				{
					realData= Integer.parseInt(data);
					break;
				}
				case "long": 
				case "Long": 
				{
					realData= Long.parseLong(data);
					break;
				}
				case "float": 
				case "Float": 
				{
					realData= Float.parseFloat(data);
					break;
				}
				case "double":
				case "Double": 
				{
					realData= Double.parseDouble(data);
					break;
				}
				default: {
					System.out.println("Wrong attribute!!!!");
					realData=data;
					break;
				}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}			
			return realData;
	    }
	    
	    public String getName(){
	    	return this.name.get();
	    }
	    
	    public String getType(){
	    	return this.type.get();
	    }
	    
	    public String getData(){
	    	return this.data.get();
	    }
	    
	    public String getRatio(){
	    	return this.ratio.get();
	    }
	}
	
	
	  class EditingCell extends TableCell<ValueEntry, String> {
		  
	        private TextField textField;
	 
	        public EditingCell() {
	        }
	 
	        @Override
	        public void startEdit() {
	            if (!isEmpty()) {
	                super.startEdit();
	                createTextField();
	                setText(null);
	                setGraphic(textField);
	                textField.selectAll();
	            }
	        }
	 
	        @Override
	        public void cancelEdit() {
	            super.cancelEdit();
	 
	            setText((String) getItem());
	            setGraphic(null);
	        }
	 
	        @Override
	        public void updateItem(String item, boolean empty) {
	            super.updateItem(item, empty);
	 
	            if (empty) {
	                setText(null);
	                setGraphic(null);
	            } else {
	                if (isEditing()) {
	                    if (textField != null) {
	                        textField.setText(getString());
	                    }
	                    setText(null);
	                    setGraphic(textField);
	                } else {
	                    setText(getString());
	                    setGraphic(null);
	                }
	            }
	        }
	 
	        private void createTextField() {
	            textField = new TextField(getString());
	            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);

	            textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
	                @Override
	                public void changed(ObservableValue<? extends Boolean> arg0, 
	                    Boolean arg1, Boolean arg2) {
	                        if (!arg2) {
	                            commitEdit(textField.getText());
	                        }
	                }
	            });
	            
	        }
	 
	        private String getString() {
	            return getItem() == null ? "" : getItem().toString();
	        }
	    }
	
}
