package se306.scheduler.visualisation;

import javafx.collections.FXCollections;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import se306.scheduler.graph.Node;

import java.util.*;

import static javafx.geometry.Pos.CENTER;

/* Class to display output schedule in a graph
 * Adapted from code at https://stackoverflow.com/questions/27975898/gantt-chart-from-scratch
 */
public class OutputSchedule<X,Y> extends XYChart<X,Y>{

    /** a node which displays a value on hover, but is otherwise empty */
    class HoveredThresholdNode extends StackPane {
        HoveredThresholdNode(Node node) {
            setPrefSize(15, 15);

            final Label label = new Label("Node: " + node.getName() +"\n" +
                    "Start Time: " + node.getStartTime() +"\n" +
                    "Finish Time: " + node.getFinishTime());
            label.setMinSize(120,80);
            label.setStyle("-fx-background-color:lightgrey;" +
                    "-fx-background-radius: 10;");
            label.setAlignment(CENTER);

            setOnMouseEntered(mouseEvent -> {
                label.setTranslateX(mouseEvent.getX());
                label.setTranslateY(mouseEvent.getY());
                getChildren().setAll(label);
                toFront();
            });
            setOnMouseExited(mouseEvent -> {
                getChildren().clear();
            });
        }
    }

    public static class ExtraData {

        public int length;
        public String styleClass;


        public ExtraData(int lengthMs, String styleClass) {
            super();
            this.length = lengthMs;
            this.styleClass = styleClass;
        }
        public double getLength() {
            return length;
        }


    }

    private double blockHeight = 10;
    private String[] labels;

    public void update(List<Node> newSchedule){

        //TODO Why does this not update sometimes
        this.getData().clear();

        //Debugging
//        System.out.println(this.getData().toString());
//        System.out.println();

        //Run through each processor
        for (int processor=0; processor<labels.length; processor++){
            Series series = new Series();

            //Check if any node is scheduled in that processor
            for (Node node: newSchedule){
                if (node.getProcessor()-1==processor){
//                    System.out.println(node.toString());
                    //Add node data to graph
                    Data data = new Data<>(node.getStartTime(), labels[processor], new ExtraData(node.getWeight(), "status-"+node.getName()));
                    data.setNode(new HoveredThresholdNode(node));
                    series.getData().add(data);
                }
            }
            this.getData().addAll(series);
        }
//        System.out.println(this.getData().toString());
//        layoutPlotChildren();
    }

    public OutputSchedule(Axis<X> xAxis, Axis<Y> yAxis, int numProcessors, double windowHeight) {
        super(xAxis,yAxis);

        //Create correct labels for Y-axis
        labels = new String[numProcessors];
        for (int i=0; i<numProcessors; i++){
            labels[i] = "Processor " + Integer.toString(i+1);
        }

        if (!(xAxis instanceof NumberAxis && yAxis instanceof CategoryAxis)){
            throw new IllegalArgumentException("Axis type incorrect, X should be NumberAxis and Y should be CategoryAxis");
        }
        //Apply Y-axis labels
        ((CategoryAxis) yAxis).setCategories(FXCollections.observableArrayList(Arrays.asList(labels)));
        setData(FXCollections.observableArrayList());

        ((NumberAxis) xAxis).setMinorTickCount(0);

        this.setAnimated(false);

        //Set block height based on parent window size
        blockHeight = 0.8 * windowHeight/numProcessors;

    }

    private static double getLength( Object obj) {
        return ((ExtraData) obj).getLength();
    }

    @Override protected void layoutPlotChildren() {

        for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {

            Series<X,Y> series = getData().get(seriesIndex);

            Iterator<Data<X,Y>> iterator = getDisplayedDataIterator(series);
            while(iterator.hasNext()) {
                Data<X,Y> item = iterator.next();
                double x = getXAxis().getDisplayPosition(item.getXValue());
                double y = getYAxis().getDisplayPosition(item.getYValue());
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
                }
                javafx.scene.Node block = item.getNode();
                Rectangle rectangle;
                if (block != null) {
                    if (block instanceof StackPane) {
                        StackPane region = (StackPane)item.getNode();
                        if (region.getShape() == null) {
                            rectangle = new Rectangle( getLength( item.getExtraValue()), getBlockHeight());
                        } else if (region.getShape() instanceof Rectangle) {
                            rectangle = (Rectangle)region.getShape();
                        } else {
                            return;
                        }
                        rectangle.setWidth( getLength( item.getExtraValue()) * ((getXAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis)getXAxis()).getScale()) : 1));
                        rectangle.setHeight(getBlockHeight() * ((getYAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis)getYAxis()).getScale()) : 1));
                        y -= getBlockHeight() / 2.0;

                        region.setShape(null);
                        region.setShape(rectangle);
                        region.setScaleShape(false);
                        region.setCenterShape(false);
                        region.setCacheShape(false);

                        block.setLayoutX(x);
                        block.setLayoutY(y);
                    }
                }
            }
        }
    }

    public double getBlockHeight() {
        return blockHeight;
    }

    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        javafx.scene.Node block = createContainer(item);
        getPlotChildren().add(block);
    }

    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final javafx.scene.Node block = item.getNode();
        getPlotChildren().remove(block);
        removeDataItemFromDisplay(series, item);
    }

    @Override protected void dataItemChanged(Data<X, Y> item) {
    }

    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        for (int j=0; j<series.getData().size(); j++) {
            Data<X,Y> item = series.getData().get(j);
            javafx.scene.Node container = createContainer(item);
            getPlotChildren().add(container);
        }
    }

    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        for (XYChart.Data<X,Y> d : series.getData()) {
            final javafx.scene.Node container = d.getNode();
            getPlotChildren().remove(container);
        }
        removeSeriesFromDisplay(series);

    }


    private javafx.scene.Node createContainer(final Data<X,Y> item) {

        javafx.scene.Node container = item.getNode();

        if (container == null) {
            container = new StackPane();
            item.setNode(container);
        }

        //Randomly pick a shade of blue for block
        Random random = new Random();
        int red = random.nextInt(100);
        int green = random.nextInt(150);
        int blue = 255;
        String colour = "rgb(" + Integer.toString(red) + "," + Integer.toString(green) + "," + Integer.toString(blue) + ");";
        container.setStyle("-fx-background-color:" + colour + ";");

        return container;
    }

    @Override protected void updateAxisRange() {
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<X> xData = null;
        List<Y> yData = null;
        if(xa.isAutoRanging()) xData = new ArrayList<X>();
        if(ya.isAutoRanging()) yData = new ArrayList<Y>();
        if(xData != null || yData != null) {
            for(Series<X,Y> series : getData()) {
                for(Data<X,Y> data: series.getData()) {
                    if(xData != null) {
                        xData.add(data.getXValue());
                        xData.add(xa.toRealValue(xa.toNumericValue(data.getXValue()) + getLength(data.getExtraValue())));
                    }
                    if(yData != null){
                        yData.add(data.getYValue());
                    }
                }
            }
            if(xData != null) xa.invalidateRange(xData);
            if(yData != null) ya.invalidateRange(yData);
        }
    }

}