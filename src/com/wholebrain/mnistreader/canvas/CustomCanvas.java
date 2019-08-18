package com.wholebrain.mnistreader.canvas;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.awt.image.BufferedImage;

public abstract class CustomCanvas extends Pane {
    private int filterDownThreshold=0, filterUpThreshold = 255;
    private boolean isFiltered = false;
    private Font labelFont = new Font(Font.getDefault().getName(),50);

    protected Canvas canvas = new Canvas(280,280);
    protected byte[][] imageBuffers;
    protected POSITION currentLabelPosition = POSITION._TOPLEFT_POSITION;
    protected char[] currentChars = {};
    protected boolean isLabelVisible = true;
    protected Color backGroundColor = Color.WHITE;
    protected Color[] pallet = new Color[256];
    protected int imageVResolution, imageHResolution, xMouse, yMouse, resolution=1;
    protected double xPos, yPos;
    protected boolean showHint = true, showHintCoord = true, showHintValue = true;


    /**
     * An enum to tell the scene where to put the {@link ScrollBar}.
     */
    public enum DIRECTION{
        _BOTTOM,
        _RIGHT,
        _LEFT;

    }
    /**
     * This enum has the purpose to make the position_radiobuttons easy to tell the canvas
     * which positioning the user wants.
     */
    protected enum POSITION {
        _TOPLEFT_POSITION(0,0),
        _TOPRIGHT_POSITION(1,0),
        _BOTTOMLEFT_POSITION(0,1),
        _BOTTOMRIGHT_POSITION(1,1),
        _TOP_POSITION(0.5,0),
        _BOTTOM_POSITION(0.5,1),
        _LEFT_POSITION(0,0.5),
        _RIGHT_POSITION(1,0.5);
        private double vPosition,hPosition;

        POSITION(double hPosition, double vPosition){
            this.vPosition=vPosition;
            this.hPosition=hPosition;
        }

        double getVPosition(){
            return vPosition;
        }
        double getHPosition(){
            return hPosition;
        }

    }

    protected abstract void paint(GraphicsContext graphicsContext);
    protected abstract void paintLabels(GraphicsContext graphicsContext);
    protected abstract void initializeHint(Canvas canvas);
    public final void layoutChildren(){
        int w = (int)getWidth();
        int h = (int)getHeight();
        if(w!=canvas.getWidth() || h!=canvas.getHeight()){
            canvas.setWidth(w);
            canvas.setHeight(h);
            repaint();
        }
    }
    public abstract DIRECTION getScrollBarPosition();
    public abstract int getShownImageCount();
    public abstract int getFirstShownIndex(int indexTry, int numberOfImages);
    public abstract int getIndexFor(int value);
    public abstract int getScrollValueForIndex(int index);
    public abstract int getScrollBarMaxValueFor(int elementCount);
    public abstract double getScrollBarUnitIncrement();

    public CustomCanvas(){
        getChildren().add(canvas);
        setMinSize(280,280);
        canvas.getGraphicsContext2D().setFont(labelFont);
        canvas.getGraphicsContext2D().setTextBaseline(VPos.CENTER);
        canvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        initializeHint(canvas);
    }

    /**
     * Returns a snapshot of the currently draw content of the canvas.
     * @param imageType File type of the wanted image scnapshot.
     * @return {@link BufferedImage}.
     */
    public final BufferedImage getSnapshot(String imageType){
        WritableImage image = new WritableImage((int)(canvas.getWidth()),(int)(canvas.getHeight()));
        canvas.snapshot(null, image);
        BufferedImage bufferedImage= SwingFXUtils.fromFXImage(image,null);
        switch(imageType){
            case "jpg":
            case "bmp":
            case "wbmp":
                BufferedImage correctedBufferedImage = new BufferedImage((int)canvas.getWidth(), (int)canvas.getHeight(),
                        imageType.equals("wbmp")?BufferedImage.TYPE_BYTE_BINARY:BufferedImage.TYPE_INT_RGB);
                correctedBufferedImage.createGraphics().drawImage(bufferedImage,0,0,java.awt.Color.WHITE,null);
                System.out.println("Image corrected to "+imageType);
                return correctedBufferedImage;
        }
        return bufferedImage;
    }

    /**
     * Loads the image inside the buffer and stores its metadatas.
     * @param imageBuffers Image buffers with each pixel opacity in byte.
     * @param numberOfRows Number of rows of the image.
     * @param numberOfColumns Number of columns of the image.
     * @param currentChars Characters represented by the images.
     */
    public final void loadImages(byte[][] imageBuffers, int numberOfRows, int numberOfColumns,char[] currentChars){
        this.imageBuffers = imageBuffers;
        this.imageVResolution =numberOfRows;
        this.imageHResolution =numberOfColumns;
        this.currentChars=currentChars;
        repaint();
    }

    /**
     * Prepares the color pallet used to paint the images.
     */
    public final void initializePallet(Color color){
        pallet = new Color[256];
        if(!isFiltered)
            for (int i = 0; i<256; i++)
                pallet[i]= new Color(color.getRed(), color.getGreen(), color.getBlue(), i/256d);
        else {
            for (int i = 0; i<= filterDownThreshold; i++)
                pallet[i]=backGroundColor;
            for (int i = filterDownThreshold +1; i< filterUpThreshold; i++)
                pallet[i]= new Color(color.getRed(), color.getGreen(), color.getBlue(), i/256d);
            for (int i = filterUpThreshold; i<256; i++)
                pallet[i] = color;
        }
        repaint();
    }

    /**
     * Set the background color of the canvas.
     * @param backGroundColor {@link Color} of background.
     */
    public final void setBackGroundColor(Color backGroundColor) {
        this.backGroundColor = backGroundColor;
        initializePallet(pallet[255]);
        repaint();
    }

    /**
     * Sets the boolean flag that informs the canvas if we want to draw the labels.
     * @param selected boolean true if the labels are wanted to be drawn.
     */
    public final void setLabelVisible(boolean selected) {
        isLabelVisible=selected;
        repaint();
    }

    /**
     * Stores when the label has to be drawn.
     * @param positionString Is actually a {@link POSITION} called by a String thanks to the enum possibilities.
     */
    public final void setLabelPosition(String positionString){
        currentLabelPosition= POSITION.valueOf(positionString);
        repaint();
    }

    /**
     * Sets the down threshold for filtering the image :
     * every value equal or below will show a "blank" pixel.
     * @param downValue Limit value to be a blank pixel.
     */
    public final void setDownFilter(int downValue) {
        this.filterDownThreshold = downValue;
        updateFiltering();
    }

    /**
     * Sets the up threshold for filtering the image :
     * every value equal or above will show a "full" pixel.
     * @param upValue Limit value to be a fully opaque pixel.
     */
    public final void setUpFilter(int upValue) {
        this.filterUpThreshold = upValue;
        updateFiltering();
    }

    /**
     * Setups the behaviour of the {@link Tooltip}.
     * @param showHint Tells if the {@link Tooltip} is enabled.
     * @param showHintCoord Tells if the {@link Tooltip} shows the cursor coordinate on the {@link Canvas canvas}.
     * @param showHintValue Tells if the {@link Tooltip} shows the value of the pixel pointed by the cursor.
     */
    public final void sendHintSetup(boolean showHint, boolean showHintCoord, boolean showHintValue) {
        this.showHint=showHint;
        this.showHintCoord=showHintCoord;
        this.showHintValue=showHintValue;
    }

    /**
     * Makes the {@link Canvas canvas} repaint itself.
     */
    protected final void repaint(){
        paint(canvas.getGraphicsContext2D());
        if(isLabelVisible) paintLabels(canvas.getGraphicsContext2D());
    }

    /**
     * Checks if the image needs filtering and reinitialize the pallet in consequence.
     */
    private void updateFiltering(){
        isFiltered = !(filterDownThreshold ==0 && filterUpThreshold ==255);
        initializePallet(pallet[255]);
    }
}
