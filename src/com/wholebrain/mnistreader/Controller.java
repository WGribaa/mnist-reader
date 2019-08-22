package com.wholebrain.mnistreader;

import com.wholebrain.mnistreader.canvas.CustomCanvas;
import com.wholebrain.mnistreader.canvas.MultipleCanvas;
import com.wholebrain.mnistreader.canvas.SingleCanvas;

import com.wholebrain.mnistreader.canvas.SizeChangeListener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class Controller implements Initializable, SizeChangeListener {
    // FXML GUI elements
    @FXML public BorderPane main_layout;
    @FXML public Menu labelposition_menu, filters_menu, showonly_menu, sorters_menu, means_menu;
    @FXML public MenuItem open_menu, close_menu, showall_chars_menuitem, switch_view,
            mean_set_menuitem,mean_char_menuitem,save_snapshot_menuitem,fast_snapshot_menuitem;
    @FXML public CheckMenuItem show_labels_checkbox, hint_show_menuitem, hint_coordinates_menuitem,
            hint_value_menuitem,hint_index_menuitem;;
    @FXML public RadioMenuItem _TOPLEFT_POSITION_radiomenu, _TOPRIGHT_POSITION_radiomenu,
            _BOTTOMLEFT_POSITION_radiomenu, _BOTTOMRIGHT_POSITION_radiomenu, _TOP_POSITION_radiomenu,
            _BOTTOM_POSITION_radiomenu, _LEFT_POSITION_radiomenu, _RIGHT_POSITION_radiomenu;
    @FXML public TextField jumpto_textfield;
    @FXML public ColorPicker empty_color_picker, full_color_picker;
    @FXML public Slider empty_threshold_slider, full_threshold_slider;

    @FXML public Label index_label;
    @FXML public ScrollBar index_scrollbar;
    @FXML public VBox bottom_vbox;

    // Programmatically added GUI elements.
    private CustomCanvas canvas = new SingleCanvas();
    private List<CheckMenuItem> charFilters= new ArrayList<>();
    private List<MenuItem> showOnlyFilters = new ArrayList<>();
    private Stage primaryStage;

    // Dataset related
    private DatasetReader reader = new DatasetReader();
    private int currentImageIndex = 0;

    // Filters
    private Set<Character> filteredChars = new HashSet<>();
    private List<Integer> filteredImageIndices = new ArrayList<>();

    // Sorter
    private Comparator<Integer> currentSorter;
    private SorterList sorters = new SorterList();

    // Image
    private File lastImageFolder;
    private int lastExtension;

    //ScrollBar
    private ScrollValueListener scrollValueListener = new ScrollValueListener();

    // FXML event
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
        loadFile(fileChooser.showOpenDialog(primaryStage));
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
        filteredChars.addAll(reader.getCharSet());
        updateCharFiltering();
    }

    @FXML
    public void on_mean_dataset() {
        byte[][][] loadingImageBuffers = new byte[1][][];
        new ProgressDialog("Loading image buffers...",0,
                new Task(){
                    @Override
                    protected Void call(){
                        loadingImageBuffers[0] =reader.getAllImageBuffers();
                        return null;
                    }
                });
        launchMeanImage(loadingImageBuffers[0],"of whole dataset");
    }

    @FXML
    public void on_mean_set() {
        StringBuilder charsInSet = new StringBuilder(filteredChars.size()<=1?"":"s").append(" [");
        for (char c : filteredChars)
            charsInSet.append(c).append(",");
        List<Character> characterList = new ArrayList<>(filteredChars);
        charsInSet.replace(charsInSet.lastIndexOf(","),charsInSet.lastIndexOf(",")+1,"]");
        launchMeanImage(loadImageBuffersForChars(characterList), "for character"+charsInSet.toString());
    }

    @FXML
    public void on_mean_char() {
        ArrayList<Character> currentCharList = new ArrayList<>();
        char currentChar = reader.getCharForIndex(
                filteredImageIndices.get(canvas.getIndexFor((int)index_scrollbar.getValue())));
        currentCharList.add(currentChar);

        launchMeanImage(loadImageBuffersForChars(currentCharList),"for character ["+currentChar+"]", currentChar);
    }

    @FXML
    public void on_save_snapshot(){
        FileChooser fileChooser = new FileChooser();
        String[] formats = ImageIO.getWriterFileSuffixes();
        String[] formatNames = new String[formats.length];
        ObservableList<FileChooser.ExtensionFilter> chooserFilter = fileChooser.getExtensionFilters();
        for (int i = 0; i< formats.length; i++){
            formatNames[i] = formats[i].toUpperCase()+" image file";
            formats[i] = "*."+formats[i];
            chooserFilter.add(new FileChooser.ExtensionFilter(formatNames[i],formats[i]));
        }
        fileChooser.setInitialDirectory(lastImageFolder == null ? reader.getCurrentFile().getParentFile():lastImageFolder);
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(lastExtension));
        int currentImageIndex = canvas.getIndexFor((int)index_scrollbar.getValue());
        String initialFileName =reader.getCurrentFile().getName().substring(0,reader.getCurrentFile().getName().lastIndexOf("idx")-1)
                .concat("#").concat(Integer.toString(currentImageIndex));
        if(reader.hasLabelsProperty().get())
            initialFileName = initialFileName.concat("[").concat(Character.toString(reader.getLabel(currentImageIndex))).concat("]");
        fileChooser.setInitialFileName(initialFileName);
        File file = fileChooser.showSaveDialog(primaryStage);
        if(file!=null){
            lastExtension=fileChooser.getExtensionFilters().indexOf(fileChooser.getSelectedExtensionFilter());
            lastImageFolder=file.getParentFile();
            String imageType = file.getName().substring(file.getName().lastIndexOf(".")+1);
            try {
                ImageIO.write(canvas.getSnapshot(imageType),imageType,file);
                toast(canvas, "Snapshot saved as "+file.getName(),3000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void on_fast_snapshot() {
        int currentImageIndex = canvas.getIndexFor((int)index_scrollbar.getValue());
        String[] formats = ImageIO.getWriterFileSuffixes();
        String fileName =reader.getCurrentFile().getName().substring(0,reader.getCurrentFile().getName().lastIndexOf("idx")-1)
                .concat("#").concat(Integer.toString(currentImageIndex));
        String imageType = formats[lastExtension];
        if(reader.hasLabelsProperty().get())
            fileName = fileName.concat("[").concat(Character.toString(reader.getLabel(currentImageIndex))).concat("]");
        File file = new File(lastImageFolder==null?reader.getCurrentFile().getParent():lastImageFolder.getPath(),
                fileName.concat(".").concat(imageType));
        int firstFreeIndex=1;
        while(file.exists()){
            file = new File(file.getParent(),fileName.concat("(")
                    .concat(Integer.toString(firstFreeIndex)).concat(").").concat(imageType));
            firstFreeIndex++;
        }
        try {
            ImageIO.write(canvas.getSnapshot(imageType),imageType,file);
            toast(canvas, "Snapshot saved as "+file.getName(),3000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void on_switch_view() {
        if(canvas instanceof  SingleCanvas){
            canvas = new MultipleCanvas();
            switch_view.setText("Switch to single view");
        }else{
            canvas = new SingleCanvas();
            switch_view.setText("Switch to multiple view");
        }

        canvas.initializePallet(full_color_picker.getValue());
        canvas.setBackGroundColor(empty_color_picker.getValue());
        canvas.setDownFilter((int)empty_threshold_slider.getValue());
        canvas.setUpFilter((int)full_threshold_slider.getValue());
        setCanvas(canvas);
        if(reader.hasOpenFile().get()) {
            canvas.setImageResolution(reader.getColumnCount(), reader.getRowCount());
        }
        updateCharFiltering();
        initializeHints();

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        index_scrollbar.setMax(0);
        index_scrollbar.valueProperty().addListener(scrollValueListener);

        empty_color_picker.setValue(Color.WHITE);
        empty_color_picker.setOnAction(e->canvas.setBackGroundColor(empty_color_picker.getValue()));
        empty_color_picker.getCustomColors().add(Color.WHITE);

        full_color_picker.setValue(Color.BLACK);
        full_color_picker.setOnAction(e-> canvas.initializePallet(full_color_picker.getValue()));
        full_color_picker.getCustomColors().add(Color.BLACK);


        jumpto_textfield.setTextFormatter(new TextFormatter<>(change -> {
            if(!change.getText().matches("\\d*"))
                return null;
            return change;
        }));
        jumpto_textfield.textProperty().addListener((observable, oldValue, newValue) -> {

            if (filteredImageIndices.isEmpty())
                return;
            if (newValue == null || newValue.isEmpty())
                newValue = "0";

            int newInt, maxValue=Collections.max(filteredImageIndices);
            try {
                newInt = Math.min(Integer.parseInt(newValue), maxValue);

            } catch (NumberFormatException e) {
                jumpto_textfield.setText(oldValue);
                newInt = maxValue;
                //throw new NumberFormatException("The input number \""+newValue+"\" is out of Integer range.");
            }
            if (!filteredImageIndices.contains(newInt))
                newInt = findClosestIntInSortedList(newInt, filteredImageIndices);

//            canvas.startAtBeginning();
            index_scrollbar.setValue(getScrollValueForImageIndex(newInt));
        });

        ChangeListener<Boolean> sendVisibleProperty =((observable, oldValue, newValue) ->
                canvas.setLabelVisible(show_labels_checkbox.isSelected()&&!show_labels_checkbox.isDisable()));
        show_labels_checkbox.selectedProperty().addListener(sendVisibleProperty);
        show_labels_checkbox.disableProperty().addListener(sendVisibleProperty);

        initializeBindings();
        initializeSorters();

        initializeHints();
        setCanvas(canvas);
        canvas.initializePallet(full_color_picker.getValue());
    }

    @Override
    public void notifySizeChange() {
        setupScrollBar();
        int newScrollValue = getScrollValueForImageIndex(currentImageIndex);
        if((int)index_scrollbar.getValue()==newScrollValue)
            update(newScrollValue);
        else
            index_scrollbar.setValue(newScrollValue);
    }

    /**
     * Sets a usable {@link CustomCanvas} to start communication with the current {@link Controller Controller class}.
     * @param canvas {@link CustomCanvas} to use.
     */
    private void setCanvas(CustomCanvas canvas){
        this.canvas = canvas;
        main_layout.setCenter(canvas);
        main_layout.setRight(null);
        main_layout.setLeft(null);
        bottom_vbox.getChildren().clear();
        bottom_vbox.getChildren().add(index_label);
        switch (canvas.getScrollBarPosition()){
            case _BOTTOM:
                index_scrollbar.setOrientation(Orientation.HORIZONTAL);
                bottom_vbox.getChildren().add(index_scrollbar);
                break;
            case _RIGHT:
                index_scrollbar.setOrientation(Orientation.VERTICAL);
                main_layout.setRight(index_scrollbar);
                break;
            case _LEFT:
                index_scrollbar.setOrientation(Orientation.VERTICAL);
                main_layout.setLeft(index_scrollbar);
                break;
        }
        canvas.setSizeChangeListener(this);
    }

    /**
     * Initialize all needed bindings between node properties.
     */
    private void initializeBindings(){
        labelposition_menu.disableProperty().bind(show_labels_checkbox.disableProperty()
                .or(show_labels_checkbox.selectedProperty().not()));

        filters_menu.disableProperty().bind(show_labels_checkbox.disableProperty());
        showonly_menu.disableProperty().bind(show_labels_checkbox.disableProperty());

        full_threshold_slider.minProperty().bind(empty_threshold_slider.valueProperty());
        empty_threshold_slider.maxProperty().bind(full_threshold_slider.valueProperty());
        empty_threshold_slider.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setDownFilter(newValue.intValue()));
        full_threshold_slider.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setUpFilter(newValue.intValue()));

        means_menu.disableProperty().bind(reader.hasOpenFile().not());
        fast_snapshot_menuitem.disableProperty().bind(reader.hasOpenFile().not());
        save_snapshot_menuitem.disableProperty().bind(reader.hasOpenFile().not());

        show_labels_checkbox.disableProperty().bind(reader.hasLabelsProperty().not());
        showall_chars_menuitem.disableProperty().bind(reader.hasLabelsProperty().not());
        filters_menu.disableProperty().bind(reader.hasLabelsProperty().not());
        showonly_menu.disableProperty().bind(reader.hasLabelsProperty().not());
        mean_char_menuitem.disableProperty().bind(reader.hasLabelsProperty().not());
        mean_set_menuitem.disableProperty().bind(reader.hasLabelsProperty().not());
    }

    /**
     * Initialize the available {@link Comparator<Integer>} which will sort the displayed characters.
     */
    private void initializeSorters() {
        Comparator<Integer> sorterDefault = Comparator.comparingInt(i->i);
        Comparator<Integer> sorterInvertedDefault = (i1,i2)->i2-i1;
        Comparator<Integer> sorter0z = Comparator.comparingInt(i -> reader.getCharForIndex(i));
        Comparator<Integer> sorterz0 = (i1, i2) -> reader.getCharForIndex(i2)-reader.getCharForIndex(i1);
        Comparator<Integer> sorterAZaz09 = Comparator.comparingInt(i -> (reader.getCharForIndex(i) - (byte)'A' + Byte.MAX_VALUE) % Byte.MAX_VALUE);
        Comparator<Integer> sorter90zaZA = (i1, i2) -> (reader.getCharForIndex(i2)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE - (reader.getCharForIndex(i1)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE;
        Comparator<Integer> sorteraz09AZ = (i1, i2) ->
                - ((reader.getCharForIndex(i2)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE-31-Byte.MAX_VALUE)% Byte.MAX_VALUE
                        +((reader.getCharForIndex(i1)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE-31-Byte.MAX_VALUE)%Byte.MAX_VALUE;
        Comparator<Integer> sorterZA90za = Comparator.comparingInt(i ->
                -((reader.getCharForIndex(i) - (byte) 'A' + Byte.MAX_VALUE) % Byte.MAX_VALUE - 31 - Byte.MAX_VALUE) % Byte.MAX_VALUE);

        sorters.put("Default order", sorterDefault);
        sorters.put("Inverted order", sorterInvertedDefault);
        sorters.put("0 -> 9 -> A -> Z -> a -> z",sorter0z);
        sorters.put("0 <- 9 <- A <- Z <- a <- z", sorterz0);
        sorters.put("A -> Z -> a -> z -> 0 -> 9",sorterAZaz09);
        sorters.put("A <- Z <- a <- z <- 0 <- 9",sorter90zaZA);
        sorters.put("a -> z -> 0 -> 9 -> A -> Z",sorteraz09AZ);
        sorters.put("a <- z <- 0 <- 9 <- A <- Z",sorterZA90za);

        EventHandler<ActionEvent> sortEvent = event -> {
            currentSorter = sorters.getComparator(((RadioMenuItem) event.getSource()).getText());
            sort();
            index_scrollbar.setValue(getScrollValueForImageIndex(currentImageIndex));
        };

        ToggleGroup sorterGroup = new ToggleGroup();
        for(int i = 0; i<sorters.size(); i++){
            RadioMenuItem newMenuItem = new RadioMenuItem();
            newMenuItem.setText(sorters.getString(i));
            sorterGroup.getToggles().add(newMenuItem);
            if (i==0)newMenuItem.setSelected(true);
            else if (i>1)
                newMenuItem.setDisable(true);
            newMenuItem.setOnAction(sortEvent);
            sorters_menu.getItems().add(newMenuItem);
            if(i>1)
                newMenuItem.disableProperty().bind(reader.hasLabelsProperty().not());
        }
        sorters_menu.getItems().add(2,new SeparatorMenuItem());
        currentSorter=sorters.getComparator(0);

    }

    /**
     * Initializes the behaviour of the {@link javafx.scene.control.Tooltip mouse hint}.
     */
    private void initializeHints() {
        Platform.runLater(()->canvas.mouseTransparentProperty().bind(primaryStage.focusedProperty().not()));
        EventHandler<ActionEvent> handler = event -> sendHintSetupToCanvas();

        hint_show_menuitem.setOnAction(handler);
        hint_index_menuitem.setOnAction(handler);
        hint_coordinates_menuitem.setOnAction(handler);
        hint_value_menuitem.setOnAction(handler);
        sendHintSetupToCanvas();
    }

    /**
     * Sends the user configuration of the {@link javafx.scene.control.Tooltip mouse hint}.
     */
    private void sendHintSetupToCanvas(){
        boolean isCoordShown = hint_coordinates_menuitem.isSelected(),
                isValueShown = hint_value_menuitem.isSelected(),
                isIndexShown = hint_index_menuitem.isSelected(),
                isHintShown = hint_show_menuitem.isSelected()&&(isCoordShown || isValueShown || isIndexShown);

        canvas.updateHintSetup(isHintShown,isIndexShown, isCoordShown,isValueShown);
    }

    /**
     * Stores a link to the {@link Stage primary stage} to be able to close it.
     * @param primaryStage Main {@link Stage window}.
     */
    void setStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Send the file to the DatasetReader, then change every related variables and properties.
     * @param file to read.
     */
    private void loadFile(File file) {
        if(file == null || file.equals(reader.getCurrentFile()))return;
        reInitializeMenus();
        reader.setCurrentFile(file);
        canvas.setImageResolution(reader.getColumnCount(), reader.getRowCount());
        primaryStage.setTitle("Datasets Images Reader : " + file.getName());


        if (!reader.hasLabelsProperty().get()) {
            for (int i = 0; i<reader.getNumberOfImages();i++)
                filteredImageIndices.add(i);
            setupScrollBar();
            index_scrollbar.setValue(0);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Datasets Images Reader");
            alert.setHeaderText("The labels file \""+DatasetReader.getLabelsFileName(file)+"\" could not be found.");
            alert.setContentText("The labels won't be shown. To show the labels, please put the labels file next to its associated images file.");
            alert.showAndWait();
        }else {
            filteredChars.addAll(reader.getCharSet());
            loadFilters();
            updateCharFiltering();
        }
        update(0);

    }

    /**
     * Sets the menus to the initial settings.
     */
    private void reInitializeMenus(){
        index_scrollbar.setValue(0);
        filteredChars.clear();
        filteredImageIndices.clear();
        canvas.setLabelVisible(false);
    }

    /**
     * Change basic size properties of the {@link java.awt.Scrollbar scroll bar}
     * to map the count of displayed images.
     */
    private void setupScrollBar(){
        index_scrollbar.setMin(0);
        index_scrollbar.setMax(canvas.getScrollBarMaxValueFor(filteredImageIndices.size()));
        index_scrollbar.setUnitIncrement(canvas.getScrollBarUnitIncrement());
        index_scrollbar.setBlockIncrement(filteredImageIndices.size()/20);
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
            if(((CheckMenuItem)event.getSource()).isSelected()) {
                addCharToFilter(c);
            }
            else
                removeCharToFilter(c);
        };
        EventHandler<ActionEvent> showOnlyEvent = event -> {
            char c = ((MenuItem)event.getSource()).getText().charAt(0);
            filteredChars.clear();
            for(CheckMenuItem filter : charFilters)
                filter.setSelected(false);
            //noinspection RedundantCast
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
            filteredChars.remove(c);
            updateCharFiltering();
        }
    }

    /**
     * Updates the variable filteredImageIndexes to correspond
     * to the indexes of the filtered characters.
     */
    private void updateCharFiltering(){
        filteredImageIndices.clear();
        for(char c : filteredChars)
            filteredImageIndices.addAll(reader.getIndicesForChar(c));
        sort();
        setupScrollBar();
        int newScrollIndex = filteredImageIndices.contains(currentImageIndex) ?
                getScrollValueForImageIndex(currentImageIndex):
                getScrollValueForImageIndex(findClosestInt(currentImageIndex, filteredImageIndices));

        index_scrollbar.valueProperty().removeListener(scrollValueListener);
        index_scrollbar.setValue(newScrollIndex);
        index_scrollbar.valueProperty().addListener(scrollValueListener);
        update(newScrollIndex);
    }

    /**
     * Asks the canvas to get the value of the {@link ScrollBar} for a specific image index.
     * @param imageIndex to ask for its scroll value.
     * @return scroll value.
     */
    private int getScrollValueForImageIndex(int imageIndex){
        return Math.min(canvas.getScrollValueForIndex(filteredImageIndices.indexOf(imageIndex)),
                (int)index_scrollbar.getMax());
    }

    /**
     * Sort displayed characters in the chosen order.
     */
    private void sort(){
        if(filteredImageIndices.size()==0) return;
        filteredImageIndices.sort(currentSorter);
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
    private static void correctOrientation(DatasetReader reader, byte[] imageBuffer){
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
     * Simply a byte by byte generated image for the fun !
     * @return imageBuffer readable by the {@link SingleCanvas custom canvas}.
     */
    private byte[] getNullImage(){
        int x  = reader.getColumnCount(),
                y = reader.getRowCount();
        byte[] imgData = new byte[x*y];

        pxRect(imgData, 2,15,5,24,x);
        pxRect(imgData, 6,15,9,24,x);
        pxSimpleTriangle(imgData, 2,15,8,x,true);
        pxRect(imgData, 10,7,13,15,x);
        pxRect(imgData, 14,7,17,15,x);
        pxSimpleTriangle(imgData,10,15,16,x,false);
        pxRect(imgData, 18,2,21,24,x);
        pxRect(imgData, 22,2,25,24,x);

        return imgData;
    }

    /**
     * Asks the dataset to give all the image buffers corresponding to a {@link List<Character> list of chars}
     * and launch {@link ProgressDialog a window with a static progress bar} because it is a long process.
     * @param listOfChars list of characters.
     */
    private byte[][] loadImageBuffersForChars(List<Character> listOfChars){
        byte[][][] loadingImageBuffers = new byte[1][][];
        new ProgressDialog("Loading image buffers...",0,
                new Task(){
                    @Override
                    protected Void call(){
                        loadingImageBuffers[0] =reader.getAllImageBuffersForChars(listOfChars);
                        return null;
                    }
                });
        return loadingImageBuffers[0];
    }

    /**
     * Creates and shows a window that show the pixel-wise mean image of a set of images.
     * @param image Images to mean, as an array of image buffers (which are arrays of byte).
     * @param title Title to show on the window. The title will automatically add " in {imageFileName}".
     */
    private void launchMeanImage(byte[][] image, String title) {
        launchMeanImage(image, title, '?');
    }

    /**
     * Creates and shows a window that shows the pixel-wise mean image of a set of images.
     * @param image Images to mean, as an array of image buffers (which are arrays of byte).
     * @param title Title to show on the window. The title will automatically add " in {imageFileName}".
     * @param currentChar Character of the label to be shown.
     */
    private void launchMeanImage(byte[][] image, String title, char currentChar){
        SingleCanvas meanCanvas = new SingleCanvas();
        meanCanvas.setImageResolution(reader.getColumnCount(),reader.getRowCount());
        meanCanvas.setLabelVisible(false);
        meanCanvas.initializePallet(full_color_picker.getValue());
        meanCanvas.setBackGroundColor(empty_color_picker.getValue());
        meanCanvas.updateHintSetup(true, false,true, true);

        if(reader.isNeedsTransformation()) {
            //loading bar dialog for transformations
            new ProgressDialog("Transforming image ", image.length,
                    new Task() {
                        @Override
                        protected Void call() {
                            for (int i = 0; i< image.length; i++) {
                                correctOrientation(reader,image[i]);
                                updateProgress(i,image.length);
                            }
                            return null;
                        }
                    });
        }

        byte[] meanImageBuffer = new byte[reader.getPixelCount()];
        new ProgressDialog("Calculating mean for image ", image.length,
                new Task() {
                    @Override
                    protected Void call(){
                        long[] meanImage = new long[reader.getPixelCount()];
                        for (int i =0;i<image.length;i++) {
                            for (int j = 0; j < image[i].length; j++)
                                meanImage[j] += ((long) image[i][j] & 0xff);
                            updateProgress(i,image.length-1);
                        }
                        for(int i = 0; i<meanImage.length; i++) {
                            meanImageBuffer[i] = (byte)(Math.round(meanImage[i]/(image.length*1.0)));
                        }
                        return null;
                    }
                });

        meanCanvas.loadImage(meanImageBuffer, currentChar);

        BorderPane borderPane = new BorderPane(meanCanvas);
        Scene meanScene = new Scene(borderPane);
        Stage meanStage = new Stage();
        meanCanvas.mouseTransparentProperty().bind(meanStage.focusedProperty().not());
        meanStage.setScene(meanScene);
        meanStage.setTitle("Mean image "+title+" in "+reader.getCurrentFile().getName());
        meanCanvas.setPrefSize(280,280);
        meanStage.setX(Math.min(primaryStage.getX()+primaryStage.getWidth(), Toolkit.getDefaultToolkit().getScreenSize().width-280));
        meanStage.setY(primaryStage.getY());
        meanStage.show();
    }

    /**
     * Update the current image index, retrieve the needed imageBuffers from the {@link DatasetReader}
     * and asks the {@link CustomCanvas} to draw them accordingly.
     * @param scrollValue current value of the {@link ScrollBar}.
     */
    private void update(int scrollValue){
        if (!reader.hasOpenFile().get()) return;
        if (filteredImageIndices.size() == 0) {
            canvas.loadImages(new byte[][] {getNullImage()}, new char[] {'?'});
            index_label.setText("No image.");
            setupScrollBar();
            jumpto_textfield.setText(null);
            return;
        }
        int updatedFilteredImageIndex = canvas.getIndexFor(scrollValue);
        currentImageIndex = filteredImageIndices.get(updatedFilteredImageIndex);
        List<Integer> shownIndices = filteredImageIndices.subList(
                updatedFilteredImageIndex,
                Math.min(updatedFilteredImageIndex+canvas.getShownImageCount()-1,
                        filteredImageIndices.size()-1)+1);
        byte[][] imageBuffers = reader.getImageBuffers(shownIndices);
        char[] chars = reader.getLabels(shownIndices);

        if (reader.isNeedsTransformation()) {
            for(byte[] imageBuffer : imageBuffers)
                correctOrientation(reader, imageBuffer);
        }
        index_label.setText(currentImageIndex+" ("+index_scrollbar.getValue()+")");
        canvas.loadImages(imageBuffers,chars);
    }

    private class ScrollValueListener implements ChangeListener<Number>{
        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
            update(newValue.intValue());
        }

    }

    // Utils methods

    /**
     * Finds the closest int in a {@link List<Integer> list of integer}
     * from a int.
     * Example : List = {0 ; 10 ; 20 ; 30 ; 40}. findClosestInt(21,List) will return 30.
     * @param value int to consider
     * @param sortedList {@link List<Integer>} sorted with int values from smallest to biggest.
     * @return the closest int in the list.
     */
    private static int findClosestIntInSortedList(final int value, final List<Integer> sortedList) {
        int lo = 0, hi = sortedList.size() - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (sortedList.get(mid) < value) lo = mid + 1;
            else if (sortedList.get(mid) > value) hi = mid;
            else return mid;
        }
        return lo > hi || sortedList.get(lo) < value ? sortedList.get(lo-1) : sortedList.get(lo);
    }

    /**
     * Finds the closest int in a {@link List<Integer> List of integer} from an int.
     * @param value value to consider.
     * @param list {@link List<Integer>} sorted or not.
     * @return the closest int.
     */
    private static int findClosestInt(final int value, final List<Integer> list) {
        return list.stream()
                .min(Comparator.comparingInt(i -> Math.abs(i - value))).orElse(0);
    }

    /**
     * Colors a rectangle in the imageBuffer.
     * @param imageBuffer as an array of byte.
     * @param startX X coordinate of the top left corner of the rectangle.
     * @param startY Y coordinate of the top left corner of the rectangle.
     * @param endX X coordinate of the bottom right corner of the rectangle.
     * @param endY Y coordinate of the bottom right corner of the rectangle.
     */
    private static void pxRect(byte[] imageBuffer, int startX, int startY, int endX, int endY, int columnCount){
        for (int i = startY; i<endY; i++){
            for (int j = startX; j<endX; j++){
                imageBuffer[i*columnCount+j]=(byte)255;
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
    private static void pxSimpleTriangle(byte[] imageBuffer, int startX, @SuppressWarnings("SameParameterValue") int startY, int endX,
                                         int columnCount, boolean up){
        int height = (endX-startX)/2;
        int i = 0;
        while(i<=height){
            for (int j =0; j<=i; j++) {
                int i1 = (up ? -j : j) * columnCount;
                imageBuffer[columnCount * startY + i1 + startX + i] = (byte) 255;
                imageBuffer[columnCount * startY + i1 + endX-i]=(byte)255;
            }
            i++;
        }

    }

    /**
     * Shows a {@link Popup} at the bottom center of the specified {@link Region region}, inside a simple {@link Label}.
     * @param region Region to put the Popup over.
     * @param message Text to show.
     * @param msPeriod Time in milliseconds to show the {@link Popup}.
     */
    private static void toast(Region region, String message, @SuppressWarnings("SameParameterValue") int msPeriod){
        final Popup popup = new Popup();
        Label popupText = new Label(message);
        popupText.setStyle("-fx-font-weight: bold");
        popup.getContent().add(popupText);
        Window window = region.getScene().getWindow();
        popup.setOnShown(e -> {
            popup.setX(window.getX() + window.getWidth() / 2 - popup.getWidth() / 2);
            popup.setY(window.getY() + region.getScene().getY()+ region.getLayoutY() + region.getHeight() - popup.getHeight()-10);
            new Thread(()->{
                try {
                    Thread.sleep(msPeriod);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                Platform.runLater(popup::hide);
            }).start();
        });
        popup.show(window);
    }
}