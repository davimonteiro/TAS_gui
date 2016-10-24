package application.Node;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public class ArrowNode {

    public static class LeftArrowNode extends Parent {
    	
    	private AnchorPane pane;
    	private Label label;
    	private Line line;
    	private Polygon polygon;
    	
    	public LeftArrowNode(double layoutX, double layoutY,String name,double length){
    		pane=new AnchorPane();
        	pane.setLayoutX(layoutX);
        	pane.setLayoutY(layoutY);
	        
            double polygonSize=15.0;

        	line=new Line();
        	AnchorPane.setLeftAnchor(line, 0.0);
        	AnchorPane.setTopAnchor(line, 0.0);
        	line.setEndX(length-polygonSize);
        	line.setEndY(0);
            pane.getChildren().add(line);
        	
	        polygon = new Polygon(new double[]{
	                0, 0,
	                polygonSize,(polygonSize/2)*(-1),
	                polygonSize,0,
	                polygonSize,polygonSize/2,
	                0,0
	        }); 
	        polygon.setStroke(Color.BLACK);
        	AnchorPane.setLeftAnchor(polygon, length-polygonSize);
        	AnchorPane.setTopAnchor(polygon, polygonSize/2);
            pane.getChildren().add(polygon);
            
            label=new Label(name);
            double margin=15.0;
            label.setLayoutX(layoutX-length*2/3-margin);
            label.setLayoutY(layoutY-margin);
            pane.getChildren().add(label);
            	      
            getChildren().addAll(pane);	
    	}
    }
    
    public static class RightArrowNode extends Parent{
    	private AnchorPane pane;
    	private Label label;
    	private Line line;
    	private Polygon polygon;
    	
    	public RightArrowNode(double layoutX, double layoutY,String name,double length){
    		pane=new AnchorPane();
        	pane.setLayoutX(layoutX);
        	pane.setLayoutY(layoutY);
        	
            double polygonSize=-15.0;
	        
        	line=new Line();
        	AnchorPane.setLeftAnchor(line, 0.0);
        	AnchorPane.setTopAnchor(line, 0.0);
        	
        	line.setStartX(0);
        	line.setStartY(0);
        	line.setEndX(length*(-1)-polygonSize);
        	line.setEndY(0);
            pane.getChildren().add(line);
        	
	        polygon = new Polygon(new double[]{
	                0, 0,
	                polygonSize,polygonSize/2,
	                polygonSize,0,
	                polygonSize,(polygonSize/2)*(-1),
	                0,0
	        }); 
	        polygon.setStroke(Color.BLACK);
        	AnchorPane.setLeftAnchor(polygon, length+polygonSize);
        	AnchorPane.setTopAnchor(polygon, polygonSize/2);
            pane.getChildren().add(polygon);
            
            label=new Label(name);
            double margin=15.0;
        	AnchorPane.setLeftAnchor(label, margin);
        	AnchorPane.setTopAnchor(label, margin*(-1));
            pane.getChildren().add(label);
            	      
            getChildren().addAll(pane);	
    	}
    }
    
}
