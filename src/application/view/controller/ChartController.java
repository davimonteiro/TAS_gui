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
	
	private AnchorPane reliabilityChartPane;
	private AnchorPane costChartPane;
	private AnchorPane performanceChartPane;

	private ScatterChart<Number, String> reliabilityChart;
	private StackedBarChart<String, Number> performanceChart;
	private LineChart<Number, Number> costChart;

	public ChartController(AnchorPane reliabilityChartPane, AnchorPane costChartPane, AnchorPane performanceChartPane) {
		this.reliabilityChartPane = reliabilityChartPane;
		this.costChartPane = costChartPane;
		this.performanceChartPane = performanceChartPane;
	}

	public void generateReliabilityChart(String resultFilePath, int maxSteps) {
		try {
			XYChart.Series<Number, String> reliabilitySeries = new XYChart.Series<>();

			NumberAxis xAxis = new NumberAxis("Invocations", 0, maxSteps, 1);

			if (maxSteps >= 100)
				xAxis.setTickUnit(maxSteps / 20);

			CategoryAxis yAxis = new CategoryAxis();

			setReliabilityChart(new ScatterChart<Number, String>(xAxis, yAxis));
			reliabilityChartPane.getChildren().add(getReliabilityChart());
			getReliabilityChart().prefWidthProperty().bind(reliabilityChartPane.widthProperty());
			getReliabilityChart().prefHeightProperty().bind(reliabilityChartPane.heightProperty());

			getReliabilityChart().setLegendVisible(false);

			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;
			int invocationNum = 0;
			int minVocationNum = Integer.MAX_VALUE;
			String service;
			boolean result;

			List<String> categories = new ArrayList<>();
			categories.add("AssistanceService");
			while ((line = br.readLine()) != null) {
				String[] str = line.split(",");
				if (str.length >= 3) {
					invocationNum = Integer.parseInt(str[0]);
					if (minVocationNum > invocationNum)
						minVocationNum = invocationNum;
					service = str[1];
					result = Boolean.parseBoolean(str[2]);
					reliabilitySeries.getData().add(this.createReliabilityData(invocationNum, service, result, maxSteps));
					if (!categories.contains(service) && !service.equals("AssistanceService"))
						categories.add(service);
				}
			}
			br.close();

			yAxis.setAutoRanging(false);
			yAxis.setCategories(FXCollections.<String>observableArrayList(categories));
			yAxis.invalidateRange(categories);

			getReliabilityChart().getData().add(reliabilitySeries);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateCostChart(String resultFilePath, int maxSteps) {
		try {
			XYChart.Series<Number, Number> costSeries = new XYChart.Series<>();
			;

			NumberAxis xAxis = new NumberAxis("Invocations", 0, maxSteps, 1);
			if (maxSteps >= 100)
				xAxis.setTickUnit(maxSteps / 20);

			NumberAxis yAxis = new NumberAxis();

			setCostChart(new LineChart<Number, Number>(xAxis, yAxis));
			costChartPane.getChildren().add(getCostChart());
			getCostChart().prefWidthProperty().bind(costChartPane.widthProperty());
			getCostChart().prefHeightProperty().bind(costChartPane.heightProperty());

			getCostChart().setLegendVisible(false);
			getCostChart().getData().clear();

			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;

			double totalCost = 0;
			int invocationNum = 0;
			int minVocationNum = Integer.MAX_VALUE;
			String service;

			costSeries.getData().clear();

			costSeries.getData().add(new Data<Number, Number>(0, totalCost));

			while ((line = br.readLine()) != null) {
				String[] str = line.split(",");
				if (str.length >= 3) {
					invocationNum = Integer.parseInt(str[0]);
					if (minVocationNum > invocationNum)
						minVocationNum = invocationNum;
					service = str[1];

					if (service.equals("AssistanceService")) {
						totalCost = totalCost + Double.parseDouble(str[3]);
						costSeries.getData().add(new Data<Number, Number>(invocationNum, totalCost));
					}
				}
			}
			br.close();

			yAxis.setLabel("Cost");
			yAxis.setLowerBound(0);
			yAxis.setUpperBound(totalCost);
			yAxis.setTickUnit(100);

			getCostChart().getData().add(costSeries);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generatePerformanceChart(String resultFilePath, int maxSteps) {
		try {

			CategoryAxis xAxis = new CategoryAxis();
			xAxis.setLabel("Invocations");

			NumberAxis yAxis = new NumberAxis();
			yAxis.setLabel("Response Time / ms ");

			setPerformanceChart(new StackedBarChart<String, Number>(xAxis, yAxis));
			performanceChartPane.getChildren().add(getPerformanceChart());
			getPerformanceChart().prefWidthProperty().bind(performanceChartPane.widthProperty());
			getPerformanceChart().prefHeightProperty().bind(performanceChartPane.heightProperty());

			BufferedReader br = new BufferedReader(new FileReader(resultFilePath));
			String line;
			String invocationNum;
			String service;
			String invisible = new String();
			int tickUnit = maxSteps / 20;

			Map<String, XYChart.Series<String, Number>> delays = new LinkedHashMap<>();

			while ((line = br.readLine()) != null) {
				String[] str = line.split(",");
				if (str.length == 6) {

					if (maxSteps >= 100 && Integer.parseInt(str[0]) % tickUnit != 0) {
						invisible += (char) 29;
						invocationNum = invisible;
					} else
						invocationNum = str[0];

					service = str[1];

					if (!service.equals("AssistanceService")) {
						Double delay = Double.parseDouble(str[5]);

						XYChart.Series<String, Number> delaySeries;
						if (delays.containsKey(service))
							delaySeries = delays.get(service);
						else {
							delaySeries = new XYChart.Series<>();
							delaySeries.setName(service);
							delays.put(service, delaySeries);
						}
						// categories.add(invocationNum);
						delaySeries.getData().add(new XYChart.Data<String, Number>(invocationNum, delay));
					}
				}
			}
			br.close();

			getPerformanceChart().setCategoryGap(getPerformanceChart().widthProperty().divide(maxSteps * 5).get());

			List<String> categories = new ArrayList<>();
			invisible = new String();

			for (int i = 0; i <= maxSteps; i++) {
				if (maxSteps >= 100 && i % tickUnit != 0) {
					invisible += (char) 29;
					categories.add(invisible);
				} else {
					categories.add(String.valueOf(i));
				}
			}

			xAxis.setAutoRanging(false);
			xAxis.setTickLabelsVisible(true);
			xAxis.setCategories(FXCollections.<String>observableArrayList(categories));
			xAxis.invalidateRange(categories);

			getPerformanceChart().getData().addAll(delays.values());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clear() {
		reliabilityChartPane.getChildren().clear();
		costChartPane.getChildren().clear();
		performanceChartPane.getChildren().clear();
	}

	public Data<Number, String> createReliabilityData(int num, String service, boolean result, int maxSteps) {
		Data<Number, String> data = new Data<Number, String>(num, service);
		if (result) {

			Rectangle rect = new Rectangle();
			rect.setHeight(20);
			rect.widthProperty().bind(getReliabilityChart().widthProperty().divide(maxSteps).divide(3));
			rect.setFill(Color.LIMEGREEN);
			data.setNode(rect);
			data.setNode(rect);
		} else {
			Rectangle rect = new Rectangle();
			rect.setHeight(40);
			rect.widthProperty().bind(getReliabilityChart().widthProperty().divide(maxSteps).divide(2));
			rect.setFill(Color.RED);
			data.setNode(rect);
			data.setNode(rect);
		}
		return data;
	}

	public ScatterChart<Number, String> getReliabilityChart() {
		return reliabilityChart;
	}

	public void setReliabilityChart(ScatterChart<Number, String> reliabilityChart) {
		this.reliabilityChart = reliabilityChart;
	}

	public StackedBarChart<String, Number> getPerformanceChart() {
		return performanceChart;
	}

	public void setPerformanceChart(StackedBarChart<String, Number> performanceChart) {
		this.performanceChart = performanceChart;
	}

	public LineChart<Number, Number> getCostChart() {
		return costChart;
	}

	public void setCostChart(LineChart<Number, Number> costChart) {
		this.costChart = costChart;
	}
}
