package application.view.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ChartController {
	AnchorPane reliabilityChartPane;
	AnchorPane costChartPane;
	AnchorPane performanceChartPane;

	ScatterChart<Number,String> reliabilityChart;
	StackedBarChart<String,Number> performanceChart;
	LineChart<Number,Number> costChart;
		
	//XYChart.Series<Number,String> reliabilitySeries = new XYChart.Series<>();;
	//XYChart.Series<Number,Number> costSeries = new XYChart.Series<>();; 
	
	
	public ChartController(AnchorPane reliabilityChartPane,AnchorPane costChartPane,AnchorPane performanceChartPane){
		this.reliabilityChartPane=reliabilityChartPane;
		this.costChartPane=costChartPane;
		this.performanceChartPane=performanceChartPane;
	}
	
	/*
	public void generateMockReliabilityChart(int invocationNum, String[] services){
	
		NumberAxis xAxis = new NumberAxis("Invocation",0,invocationNum,1);
        xAxis.setMinorTickCount(0);
        
        CategoryAxis yAxis = new CategoryAxis();
        String [] categories = {"FASService0","FASService1","FASService2"};
        yAxis.setAutoRanging(false); 
        yAxis.setCategories(FXCollections.<String>observableArrayList(Arrays.asList(categories))); 
        yAxis.invalidateRange(Arrays.asList(categories));
        
        reliabilityChart=new ScatterChart<Number,String>(xAxis,yAxis);
        
        reliabilityChartPane.getChildren().add(reliabilityChart);
        reliabilityChart.prefWidthProperty().bind(reliabilityChartPane.widthProperty());
        reliabilityChart.prefHeightProperty().bind(reliabilityChartPane.heightProperty());
        
        XYChart.Series<Number,String> series = new XYChart.Series<>();
        
        int serviceNum=services.length;
        int count=0;
        
        Random rand = new Random();
        for(int i=1;i<=invocationNum;i++){
        	if(rand.nextInt(100)>10){
        		series.getData().add(this.createReliabilityData(i, services[count%serviceNum],true));
        	}
        	else{
        		series.getData().add(this.createReliabilityData(i, services[count%serviceNum],false));
        		count++;
        		series.getData().add(this.createReliabilityData(i, services[count%serviceNum],true));
        	}	
        }
        reliabilityChart.setLegendVisible(false);
        reliabilityChart.getData().add(series);
	}*/
	
	public void generateReliabilityChart(String resultFilePath,int maxSteps){
		try{
			
			//reliabilityChartPane.getChildren().clear();
			
			XYChart.Series<Number,String> reliabilitySeries = new XYChart.Series<>();;
			
			NumberAxis xAxis = new NumberAxis("Invocations",0,maxSteps,1);
			
			if(maxSteps>=100)
				xAxis.setTickUnit(maxSteps/20);
			
	        CategoryAxis yAxis = new CategoryAxis();
	        
	        reliabilityChart=new ScatterChart<Number,String>(xAxis,yAxis);
	        reliabilityChartPane.getChildren().add(reliabilityChart);
	        reliabilityChart.prefWidthProperty().bind(reliabilityChartPane.widthProperty());
	        reliabilityChart.prefHeightProperty().bind(reliabilityChartPane.heightProperty());
	       
	        reliabilityChart.setLegendVisible(false);
	       			
			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;
			int invocationNum=0;
			int minVocationNum=Integer.MAX_VALUE;
			String service;
			boolean result;
			
	        List<String> categories=new ArrayList<>();
			categories.add("AssistanceService");
			while ((line = br.readLine()) != null) {
				String[] str=line.split(",");
				if(str.length>=3){
					invocationNum=Integer.parseInt(str[0]);
					if(minVocationNum>invocationNum)
						minVocationNum=invocationNum;
					service=str[1];
					result=Boolean.parseBoolean(str[2]);
					//if(!service.equals("AssistanceService")){
						reliabilitySeries.getData().add(this.createReliabilityData(invocationNum, service, result,maxSteps));
						if(!categories.contains(service) && !service.equals("AssistanceService"))
							categories.add(service);
					//}
				}
			}
			br.close();
	                
			
			//NumberAxis xAxis = new NumberAxis("Invocation",minVocationNum,invocationNum,1);
			//xAxis.setLabel("Invocation");
			//xAxis.setLowerBound(1);
			//xAxis.setUpperBound(invocationNum);
			//xAxis.setTickUnit(1);
			//xAxis.setAutoRanging(true);
	        //xAxis.setMinorTickCount(1);
	        
	        //CategoryAxis yAxis = new CategoryAxis();
	        yAxis.setAutoRanging(false); 
	        yAxis.setCategories(FXCollections.<String>observableArrayList(categories)); 
	        yAxis.invalidateRange(categories);
	        
	        reliabilityChart.getData().add(reliabilitySeries);

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void generateCostChart(String resultFilePath,int maxSteps){
		
		try{
			//costChartPane.getChildren().clear();
			
			XYChart.Series<Number,Number> costSeries = new XYChart.Series<>();; 

			
			NumberAxis xAxis = new NumberAxis("Invocations",0,maxSteps,1);
			if(maxSteps>=100)
				xAxis.setTickUnit(maxSteps/20);
			
			NumberAxis yAxis = new NumberAxis();
			
	        costChart=new LineChart<Number,Number>(xAxis,yAxis);	              
	        costChartPane.getChildren().add(costChart);
	        costChart.prefWidthProperty().bind(costChartPane.widthProperty());
	        costChart.prefHeightProperty().bind(costChartPane.heightProperty());
	        
	        costChart.setLegendVisible(false);
	        costChart.getData().clear();	        
			
			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;
			
			double totalCost=0;	
			int invocationNum=0;
			int minVocationNum=Integer.MAX_VALUE;
			String service;

			costSeries.getData().clear();
			
			costSeries.getData().add(new Data<Number, Number>(0,totalCost));						

	        while ((line = br.readLine()) != null) {
				String[] str=line.split(",");
				if(str.length>=3){
					invocationNum=Integer.parseInt(str[0]);
					if(minVocationNum>invocationNum)
						minVocationNum=invocationNum;
					service=str[1];
					
					if(service.equals("AssistanceService")){
						totalCost=totalCost+Double.parseDouble(str[3]);
						costSeries.getData().add(new Data<Number, Number>(invocationNum,totalCost));						
					}
				}
			}
			br.close();
			
			//NumberAxis xAxis = new NumberAxis("Invocation",minVocationNum,invocationNum,1);
			//xAxis.setLabel("Invocation");
			//xAxis.setLowerBound(minVocationNum);
			//xAxis.setUpperBound(invocationNum);
			//xAxis.setTickUnit(1);
	        //xAxis.setMinorTickCount(0);
	        
	        //NumberAxis yAxis = new NumberAxis("Cost",0,totalCost,100);
			yAxis.setLabel("Cost");
			yAxis.setLowerBound(0);
			yAxis.setUpperBound(totalCost);
			yAxis.setTickUnit(100);
	        
	        //yAxis.setMinorTickCount(0);
	        costChart.getData().add(costSeries);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void generatePerformanceChart(String resultFilePath, int maxSteps){
		try{
				
			CategoryAxis xAxis = new CategoryAxis();
			xAxis.setLabel("Invocations");

			NumberAxis yAxis = new NumberAxis();
			yAxis.setLabel("Response Time / ms ");
			
	        performanceChart=new StackedBarChart<String,Number>(xAxis,yAxis);
	        performanceChartPane.getChildren().add(performanceChart);
	        performanceChart.prefWidthProperty().bind(performanceChartPane.widthProperty());
	        performanceChart.prefHeightProperty().bind(performanceChartPane.heightProperty());
			
	        //performanceChart.setLegendVisible(false);
	       			
			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;
			String invocationNum;
			String service;
			String invisible=new String();
			int tickUnit=maxSteps/20;
			//boolean result;
			
	        //List<String> categories=new ArrayList<>();
			//categories.add("AssistanceService");
			Map<String,XYChart.Series<String,Number>> delays=new LinkedHashMap<>();
	        //List<String> categories=new ArrayList<>();

			while ((line = br.readLine()) != null) {
				String[] str=line.split(",");
				if(str.length==6){
					
					if(maxSteps>=100 && Integer.parseInt(str[0])%tickUnit!=0){
						invisible+=(char)29;
						invocationNum=invisible;
					}
					else
						invocationNum=str[0];
						
					//System.out.println(invocationNum);
					
					service=str[1];
					//result=Boolean.parseBoolean(str[2]);
					
					if(!service.equals("AssistanceService")){
						Double delay=Double.parseDouble(str[5]);
						
						XYChart.Series<String,Number> delaySeries;
						if(delays.containsKey(service))
							delaySeries=delays.get(service);
						else{
							delaySeries=new XYChart.Series<>();
							delaySeries.setName(service);
							delays.put(service, delaySeries);
						}
						//categories.add(invocationNum);
						delaySeries.getData().add(new XYChart.Data<String,Number>(invocationNum,delay));
					}
				}
			}
			br.close();
	             
			performanceChart.setCategoryGap(performanceChart.widthProperty().divide(maxSteps*5).get());
			
			//NumberAxis xAxis = new NumberAxis("Invocation",minVocationNum,invocationNum,1);
			//xAxis.setLabel("Invocation");
			//xAxis.setLowerBound(1);
			//xAxis.setUpperBound(invocationNum);
			//xAxis.setTickUnit(1);
			//xAxis.setAutoRanging(true);
	        //xAxis.setMinorTickCount(1);
	        
	        //CategoryAxis yAxis = new CategoryAxis();
	        //xAxis.setAutoRanging(false); 
	        //xAxis.setCategories(FXCollections.<String>observableArrayList(categories)); 
	        //xAxis.invalidateRange(categories);
	        
			/*
			for (XYChart.Series<String, Number> series : delays.values()) {
			     for (Data<String, Number> data : series.getData()) {
			          Tooltip tooltip = new Tooltip();
			          tooltip.setText(data.getYValue().toString());
			          Tooltip.install(data.getNode(), tooltip);                    
			     }
			}*/
			
	        List<String> categories=new ArrayList<>();
	        invisible=new String();
	        
	        	for(int i=0;i<=maxSteps;i++){
	        		if(maxSteps>=100 && i%tickUnit!=0){
						invisible+=(char)29;
		        		categories.add(invisible);
	        		}
	        		else{
		        		categories.add(String.valueOf(i));
		        		//System.out.println(i);
	        		}
	        	}

	        
	        /*
        	for(int i=0;i<=maxSteps;i++)
        		categories.add(String.valueOf(i));*/
	        
	        //xAxis.setAutoRanging(true); 
		    xAxis.setAutoRanging(false); 
	        xAxis.setTickLabelsVisible(true);
	        xAxis.setCategories(FXCollections.<String>observableArrayList(categories)); 
	        xAxis.invalidateRange(categories);
	        
	        performanceChart.getData().addAll(delays.values());
	        
	        
			/*
			for (XYChart.Series<String, Number> series : performanceChart.getData()) {
			     for (Data<String, Number> data : series.getData()) {
			          Tooltip tooltip = new Tooltip();
			          tooltip.setText(data.getYValue().toString());
			          Tooltip.install(data.getNode(), tooltip);   
			     }
			}*/

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	public void generatePerformanceChart(String resultFilePath){
		try{
			
			//reliabilityChartPane.getChildren().clear();
			
			//XYChart.Series<Long,Long> reliabilitySeries = new XYChart.Series<>();;
			
			//NumberAxis xAxis = new NumberAxis("Invocations",0,maxSteps,1);
			
			
			NumberAxis xAxis = new NumberAxis();
			xAxis.setLabel("time / ms");
			NumberAxis yAxis = new NumberAxis();
			yAxis.setLabel("delay / ms");
	        
			performanceChart=new BarChart<Number,Number>(xAxis,yAxis);
			performanceChartPane.getChildren().add(performanceChart);
	        performanceChart.prefWidthProperty().bind(performanceChartPane.widthProperty());
	        performanceChart.prefHeightProperty().bind(performanceChartPane.heightProperty());
	        //reliabilityChart.setLegendVisible(false);
	       			
			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;
			String service;
			boolean result;
			Long minTime=new Long(0);
			
			Map<String,XYChart.Series<Number,Number>> delays=new HashMap<>();
				
			while ((line = br.readLine()) != null) {
				String[] str=line.split(",");
				if(str.length==6){
					service=str[1];
					result=Boolean.parseBoolean(str[2]);
					if(!service.equals("AssistanceService")){
						if(minTime==0){
							minTime=Long.parseLong(str[4]);
						}
						Long begin=Long.parseLong(str[4])-minTime;
						Long delay=Long.parseLong(str[5]);
						System.out.println(begin+" , "+delay);
						
						XYChart.Series<Number,Number> delaySeries;
						if(delays.containsKey(service))
							delaySeries=delays.get(service);
						else{
							delaySeries=new XYChart.Series<>();
							delaySeries.setName(service);
							delays.put(service, delaySeries);
						}
						
						delaySeries.getData().add(new XYChart.Data<Number,Number>(begin,delay));
					}
				}
			}
			br.close();
			
			performanceChart.getData().addAll(delays.values());
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
	
	public void clear(){
	    reliabilityChartPane.getChildren().clear();
	    costChartPane.getChildren().clear();
	    performanceChartPane.getChildren().clear();
	}
	
	public Data<Number, String> createReliabilityData(int num,String service,boolean result,int maxSteps){
        Data<Number, String> data=  new Data<Number, String>(num, service);
        if(result){
        	
            Rectangle rect = new Rectangle();
            rect.setHeight(20);
            //rect.setWidth(5);
            rect.widthProperty().bind(reliabilityChart.widthProperty().divide(maxSteps).divide(3));
            rect.setFill(Color.LIMEGREEN);
            data.setNode(rect);
            data.setNode(rect);
        }
        else{
            Rectangle rect = new Rectangle();
            rect.setHeight(40);
            //rect.setWidth(5);
            rect.widthProperty().bind(reliabilityChart.widthProperty().divide(maxSteps).divide(2));
            rect.setFill(Color.RED);
            data.setNode(rect);
            data.setNode(rect);
        }
        return data;
	}
}
