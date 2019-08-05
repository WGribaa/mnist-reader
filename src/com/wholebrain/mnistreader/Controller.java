package com.wholebrain.mnistreader;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeSet;


public class Controller implements Initializable {
    @FXML public BorderPane main_layout;
    @FXML public Menu labelposition_menu, filters_menu, showonly_menu;
    @FXML public MenuItem open_menu, close_menu, showall_labels_menuitem;;
    @FXML public CheckMenuItem labels_checkbox;
    @FXML public RadioMenuItem _TOPLEFT_POSITION_radiomenu, _TOPRIGHT_POSITION_radiomenu,
            _BOTTOMLEFT_POSITION_radiomenu, _BOTTOMRIGHT_POSITION_radiomenu, _TOP_POSITION_radiomenu,
            _BOTTOM_POSITION_radiomenu, _LEFT_POSITION_radiomenu, _RIGHT_POSITION_radiomenu;
    @FXML public Label index_label;
    @FXML public TextField jumpto_textfield;
    @FXML public ScrollBar index_scrollbar;
    @FXML public ColorPicker empty_color_picker, full_color_picker;
    @FXML public Slider empty_threshold_slider, full_threshold_slider;

    private CustomCanvas canvas = new CustomCanvas();
    private List<CheckMenuItem> charFilters= new ArrayList<>();
    private List<MenuItem> showOnlyFilters = new ArrayList<>();
    private Stage primaryStage;

    private DatasetReader reader = new DatasetReader();
    private int currentImageIndex = 0;
    private List<Character> filteredChars = new ArrayList<>();
    private List<Integer> filteredImageIndexes = new ArrayList<>();

    /**
     * Closes the application.
     */
    @FXML
    public void on_close(){
        primaryStage.close();
    }

    /**
     * Open a dialog to load a dataset.
     */
    @FXML
    public void on_open() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All compatible files","*.idx3-ubyte","*-idx3-ubyte"),
                new FileChooser.ExtensionFilter("Mnist DataSet files (*.idx3-ubyte)","*.idx3-ubyte"),
                new FileChooser.ExtensionFilter("Emnist DataSet files ","*-idx3-ubyte"));
        fileChooser.setInitialDirectory(reader.getCurrentFile() != null ?reader.getCurrentFile().getParentFile(): null);
        loadFile(fileChooser.showOpenDialog(null));
    }

    @FXML
    public void mnist_goto() {
        gotoHtmlLink("http://yann.lecun.com/exdb/mnist/");
    }

    @FXML
    public void emnist_goto() {
        gotoHtmlLink("https://www.nist.gov/node/1298471/emnist-dataset");
    }

    @FXML
    public void send_position_tocanvas(ActionEvent actionEvent) {
        String positionString = actionEvent.getSource().toString();
        positionString= positionString.substring(positionString.indexOf('=')+1,positionString.lastIndexOf("_radiomenu"));
        canvas.setLabelPosition(positionString);
    }

    @FXML
    public void on_showall_labels() {
        for(CheckMenuItem m : charFilters)
            m.setSelected(true);
        resetFilteredImageIndexes();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        index_scrollbar.setMax(0);
        index_scrollbar.valueProperty().addListener((observable, oldValue, newValue) ->
                updateIndex(newValue.intValue()));

        empty_color_picker.setValue(Color.WHITE);
        empty_color_picker.setOnAction(e->canvas.setBackGroundColor(empty_color_picker.getValue()));
        empty_color_picker.getCustomColors().add(Color.WHITE);

        full_color_picker.setValue(Color.BLACK);
        full_color_picker.setOnAction(e-> canvas.initializePallet(full_color_picker.getValue()));
        full_color_picker.getCustomColors().add(Color.BLACK);

        canvas.initializePallet(full_color_picker.getValue());

        jumpto_textfield.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if(!text.matches("\\d*"))
                return null;
            return change;
        }));
        jumpto_textfield.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue==null || newValue.isEmpty())
                newValue="0";

            int newInt;
            try {
                newInt = Math.min(Integer.parseInt(newValue),reader.getNumberOfImages()-1);

            } catch (NumberFormatException e) {
                jumpto_textfield.setText(oldValue);
                newInt = reader.getNumberOfImages()-1;
                //throw new NumberFormatException("The input number \""+newValue+"\" is out of Integer range.");
            }
            if (!filteredImageIndexes.contains(newInt))
                newInt= findClosestInt(newInt, currentImageIndex, filteredImageIndexes);
            updateIndex(newInt);
        });

        labels_checkbox.setOnAction(e->canvas.setLabelVisible(labels_checkbox.isSelected()));

        initializeBindings();
    }

    /**
     * Initialize all needed bindings between node properties.
     */
    private void initializeBindings(){
        labelposition_menu.disableProperty().bind(labels_checkbox.disableProperty()
                .and(labels_checkbox.selectedProperty().not()));

        filters_menu.disableProperty().bind(labels_checkbox.disableProperty());
        showonly_menu.disableProperty().bind(labels_checkbox.disableProperty());

        full_threshold_slider.minProperty().bind(empty_threshold_slider.valueProperty());
        empty_threshold_slider.maxProperty().bind(full_threshold_slider.valueProperty());
        empty_threshold_slider.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setDownFilter(newValue.intValue()));
        full_threshold_slider.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setUpFilter(newValue.intValue()));

    }

    /**
     * Stores a link to the {@link Stage primary stage} to be able to close it.
     * @param primaryStage Main {@link Stage window}.
     */
    void setStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        main_layout.setCenter(canvas);
    }

    /**
     * Send the file to the DatasetReader, then change every related variables and properties.
     * @param file to read.
     */
    private void loadFile(File file) {
        if(file == null) return;
        reader.setCurrentFile(file);
        primaryStage.setTitle("Datasets Images Reader" + " : " + file.getName());

        resetFilteredImageIndexes();

        labels_checkbox.setDisable(false);
        setupScrollBar();

        loadFilters();
        updateIndex(filteredImageIndexes.get(0));
    }

    private void setupScrollBar(){
        index_scrollbar.setMin(0);
        index_scrollbar.setMax(filteredImageIndexes.size());
        index_scrollbar.setBlockIncrement(filteredImageIndexes.size()/20);
    }

    /**
     * Update the displayed infos about the current image index.
     */
    private void updateIndex(int newIndex){
        if(reader.getCurrentFile()==null) return;
        if(filteredImageIndexes.size()==0) {
            canvas.loadImage(getNullImage(), 112, 112, '?');
            index_label.setText("No image.");
            jumpto_textfield.setText(null);
        }else {
            currentImageIndex = filteredImageIndexes.get(Math.min(newIndex,filteredImageIndexes.size()-1));
            index_label.setText(String.valueOf(currentImageIndex));
            index_scrollbar.setValue(newIndex);
            paint();
        }
    }

    /**
     * Displays a colored representation of the current image on the {@link CustomCanvas canvas}.
     */
    private void paint(){
        byte[] imageBuffer = reader.getImageBuffer(currentImageIndex);


        if (reader.isNeedsTransformation()) correctOrientation(imageBuffer);

        canvas.loadImage(imageBuffer, reader.getRowCount(),
                reader.getColumnCount(),
                reader.getLabel(currentImageIndex));
    }

    /**
     * Adds a {@link CheckMenuItem} for every char present in dataset.
     */
    private void loadFilters(){
        filters_menu.getItems().clear();
        showonly_menu.getItems().clear();

        showOnlyFilters.clear();
        charFilters.clear();
        if(reader.getNumberOfImages()== 0)
            return;

        EventHandler<ActionEvent> filterEvent = event -> {
            char c = ((CheckMenuItem)event.getSource()).getText().charAt(0);
            if(((CheckMenuItem)event.getSource()).isSelected())
                addCharToFilter(c);
            else
                removeCharToFilter(c);
        };
        EventHandler<ActionEvent> showOnlyEvent = event -> {
            char c = ((MenuItem)event.getSource()).getText().charAt(0);
            filteredChars.clear();
            for(CheckMenuItem filter : charFilters)
                filter.setSelected(false);
            charFilters.get(showOnlyFilters.indexOf((MenuItem)event.getSource())).setSelected(true);
            addCharToFilter(c);
        };

        for (char c : reader.getCharSet()) {
            filteredChars.add(c);

            CheckMenuItem newCharFilter = new CheckMenuItem(String.valueOf(c));
            newCharFilter.setSelected(true);
            newCharFilter.setOnAction(filterEvent);
            charFilters.add(newCharFilter);

            MenuItem newShowOnlyItem = new MenuItem(String.valueOf(c));
            newShowOnlyItem.setOnAction(showOnlyEvent);
            showOnlyFilters.add(newShowOnlyItem);
        }
        charFilters.sort(Comparator.comparingInt(o -> o.getText().charAt(0)));
        showOnlyFilters.sort(Comparator.comparingInt(o -> o.getText().charAt(0)));

        filters_menu.getItems().addAll(charFilters);
        showonly_menu.getItems().addAll(showOnlyFilters);
    }

    /**
     * Resets the variable {@link List<Integer> filteredImageIndexes} to a ascendant list of integer
     * from 0 to the number of images inside the dataset.
     */
    private void resetFilteredImageIndexes(){
        filteredImageIndexes.clear();
        for (int i=0; i<reader.getNumberOfImages(); i++)
            filteredImageIndexes.add(i);
        updateIndex(currentImageIndex);
    }

    /**
     * Adds a character to the filtering process.
     * @param c char to add.
     */
    private void addCharToFilter(char c){
        if(!filteredChars.contains(c)){
            filteredChars.add(c);
            updateCharFiltering();
        }
    }

    /**
     * Removes a character to the filtering process.
     * @param c char to add.
     */
    private void removeCharToFilter(char c) {
        if(filteredChars.contains(c)){
            filteredChars.remove(filteredChars.indexOf(c));
            updateCharFiltering();
        }
    }

    /**
     * Updates the variable filteredImageIndexes to correspond
     * to the indexes of the filtered characters.
     */
    private void updateCharFiltering(){
        filteredImageIndexes.clear();
        for(char c : filteredChars)
            filteredImageIndexes.addAll(reader.getIndexForChar(c));
        filteredImageIndexes.sort(Comparator.comparingInt(o -> o));
        setupScrollBar();
        updateIndex(currentImageIndex);
    }

    /**
     * The EMNIST dataset needs 2 transformations in order to be drawn as identifiable character :
     * 1 - A horizontal mirroring (or a vertical axial symmetry)
     * 2 - A 90° counter-clockwise rotation.
     * The transformation resulting is an axial symmetry along a descendant diagonal.
     * Modifies directly the content of the array passed as parameter.
     * @param imageBuffer Array of bytes of the image.
     * @
     */
    private void correctOrientation(byte[] imageBuffer){
        byte buffer;
        int rowCount = reader.getRowCount(),
                columnCount = reader.getColumnCount();
        for (int y=0;y<columnCount;y++)
            for(int x = y+1; x<rowCount; x++){
                buffer = imageBuffer[y*columnCount+x];
                imageBuffer[y*columnCount+x] = imageBuffer[x*columnCount+y];
                imageBuffer[x*columnCount+y] = buffer;
            }
    }

    /**
     * Open the user's main web browsing application (if available) and browses to the link.
     * @param link Link to open in the Web Browser.
     */
    private void gotoHtmlLink(String link){
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Shows some infos about us aka I, me and myself at the moment.
     * @throws IOException Common Exception occurring during the FXML loading step.
     */
    @FXML
    public void about_us() throws IOException {
        Stage infoStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("infopanel/infopanel.fxml"));
        Parent root = loader.load();
        infoStage.opacityProperty().set(0.9);
        infoStage.setTitle("About us...");
        infoStage.setScene(new Scene(root));
        infoStage.initModality(Modality.APPLICATION_MODAL);
        infoStage.setResizable(false);
        infoStage.centerOnScreen();
        infoStage.show();
    }

    /**
     * Finds the closest int in a {@link List<Integer> list of integer}
     * from a int.
     * Example : List = {0 ; 10 ; 20 ; 30 ; 40}. findClosestInt(21,List) will return 30.
     * @param value int to consider
     * @param oldValue previous int, to know the direction of the search.
     * @param sortedList {@link }List<Integer>} sorted with int values from smallest to biggest.
     * @return the closest int in the list in the correct direction.
     */
    private static int findClosestInt(int value, int oldValue, List<Integer> sortedList) {
        /*if(value>=oldValue && sortedList.get(sortedList.size()-1)<value)
            return 0;
        else if (value < oldValue && sortedList.get(0)>value)
            return sortedList.get(sortedList.size()-1);


        int exp = (int)Math.floor(Math.log(sortedList.size())/Math.log(2)),
                index = (int)Math.pow(2,exp);
        boolean dir; // true = ascend; false = descend.
        while(exp>=0){
            dir = sortedList.get(Math.min(index, sortedList.size() - 1)) < value;
            exp--;
            index = dir?index+(int)Math.pow(2,exp):index-(int)Math.pow(2,exp);
        }

        return value>=oldValue ?
                (sortedList.get(index)>value?
                        sortedList.get(index):
                        sortedList.get(index+1)):
                (sortedList.get(index)<value?
                        sortedList.get(index):
                        sortedList.get(index-1))
         ;*/ // Old fashion. Using a TreeSet is 5 to 18 times faster !

        Integer ret = (value>=oldValue)?
                new TreeSet<>(sortedList).higher(value):
                new TreeSet<>(sortedList).lower(value);
        return ret!=null ?
                ret.intValue() :
                value<oldValue ? sortedList.get(0) : sortedList.get(sortedList.size()-1);
    }

    /**
     * Simply a byte by byte generated image for the fun !
     * @return imageBuffer readable by the {@link CustomCanvas custom canvas}.
     */
    private byte[] getNullImage(){
        byte[] imgData = new byte[12544];
        pxRect(imgData, 5,62,20,96);
        pxRect(imgData, 22,62,37,96);
        pxSimpleTriangle(imgData, 5,62,36,true);
        pxRect(imgData, 39,28,54,62);
        pxRect(imgData, 56,28,71,62);
        pxSimpleTriangle(imgData,39,62,70,false);
        pxRect(imgData, 73,10,88,96);
        pxRect(imgData, 90,10,105,96);
        return imgData;
    }

    /**
     * Colors a rectangle in the imageBuffer.
     * @param imageBuffer as an array of byte.
     * @param startX X coordinate of the top left corner of the rectangle.
     * @param startY Y coordinate of the top left corner of the rectangle.
     * @param endX X coordinate of the bottom right corner of the rectangle.
     * @param endY Y coordinate of the bottom right corner of the rectangle.
     */
    private void pxRect(byte[] imageBuffer, int startX, int startY, int endX, int endY){
        for (int i = startY; i<endY; i++){
            for (int j = startX; j<endX; j++){
                imageBuffer[i*112+j]=(byte)255;
            }
        }
    }

    /**
     * Colors a horizontal based equilateral triangle int the imageBuffer.
     * @param imageBuffer as an array of byte.
     * @param startX X coordinate of the left corner of the triangle.
     * @param startY Y coordinate of the left corner of the triangle.
     * @param endX X coordinate of the right corner of the triangle.
     * @param up Direction of the third corner of the triangle : true = up ; false = down.
     */
    private void pxSimpleTriangle(byte[] imageBuffer, int startX, int startY, int endX, boolean up){
        int height = (endX-startX)/2;
        int i = 0;
        while(i<=height){
            for (int j =0; j<=i; j++) {
                imageBuffer[112 * startY + (up?-j:j) * 112 + startX + i] = (byte) 255;
                imageBuffer[112 * startY + (up?-j:j) * 112 + endX-i]=(byte)255;
            }
            i++;
        }

    }

}
