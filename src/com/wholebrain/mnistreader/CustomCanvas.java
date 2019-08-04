package com.wholebrain.mnistreader;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.awt.Dimension;

public class CustomCanvas extends Pane {
    private Color[] pallet =new Color[256];
    private Color backGroundColor = Color.WHITE;
    private char currentChar;
    private byte[] image;
    private int numberOfRows, numberOfColumns,
            filterDownThreshold=0, filterUpThreshold=255;
    private boolean isLabelVisible = true, isFiltered = false;
    private Font labelFont = new Font(Font.getDefault().getName(),50);
    private Canvas canvas = new Canvas(280,280);
    private POSITION currentLabelPosition = POSITION._TOPLEFT_POSITION;

    /**
     * This enum has the purpose to make the position_radiobuttons easy to tell the canvas
     * which positioning the user wants.
     */
    private enum POSITION {
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

    /**
     * Sets the down threshold for filtering the image :
     * every value equal or below will show a "blank" pixel.
     * @param downValue Limit value to be a blank pixel.
     */
    public void setDownFilter(int downValue) {
        this.filterDownThreshold = downValue;
        updateFiltering();
    }

    /**
     * Sets the up threshold for filtering the image :
     * every value equal or above will show a "full" pixel.
     * @param upValue Limit value to be a fully opaque pixel.
     */
    public void setUpFilter(int upValue) {
        this.filterUpThreshold = upValue;
        updateFiltering();
    }

    /**
     * Checks if the image needs filtering and reinitialize the pallet in consequence.
     */
    private void updateFiltering(){
        isFiltered = !(filterDownThreshold ==0 && filterUpThreshold ==255);
        initializePallet(pallet[255]);
    }

    public CustomCanvas(){
        getChildren().add(canvas);
        setMinSize(280,280);
        canvas.getGraphicsContext2D().setFont(labelFont);
        canvas.getGraphicsContext2D().setTextBaseline(VPos.CENTER);
        canvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
    }

    /**
     * Loads the image inside the buffer and stores its metadatas.
     * @param imageBuffer Image buffer with each pixel opacity in byte.
     * @param numberOfRows Number of rows of the image.
     * @param numberOfColumns Number of columns of the image.
     * @param currentChar Character represented by the image.
     */
    public void loadImage(byte[] imageBuffer, int numberOfRows, int numberOfColumns,char currentChar){
        this.image = imageBuffer;
        this.numberOfRows=numberOfRows;
        this.numberOfColumns=numberOfColumns;
        this.currentChar=currentChar;
        repaint();
    }

    @Override protected void layoutChildren(){
        int w = (int)getWidth();
        int h = (int)getHeight();
        if(w!=canvas.getWidth() || h!=canvas.getHeight()){
            canvas.setWidth(w);
            canvas.setHeight(h);
            repaint();
        }
    }

    /**
     * Set the background color of the canvas.
     * @param backGroundColor {@link Color} of background.
     */
    public void setBackGroundColor(Color backGroundColor) {
        this.backGroundColor = backGroundColor;
        initializePallet(pallet[255]);
        repaint();
    }

    /**
     * Prepares the color pallet used to paint the images.
     */
    public void initializePallet(Color color){
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
     * Sets the boolean flag that informs the canvas if we cant to draw the label.
     * @param selected boolean true if the label is wanted to be drawn.
     */
    public void setLabelVisible(boolean selected) {
        isLabelVisible=selected;
        repaint();
    }

    /**
     * Makes the canvas repaint its content.
     */
    private void repaint(){
        double size = Double.min(canvas.getHeight(), canvas.getWidth());
        double resolution = Math.floor(Double.min(size/(1.0*numberOfRows), size/(1.0*numberOfColumns)));
        double xPos = Math.floor((canvas.getWidth()-resolution*numberOfColumns)/2.0);
        double yPos = Math.floor((canvas.getHeight()-resolution*numberOfRows)/2.0);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(backGroundColor);
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for(int y = 0; y<numberOfRows; y++)
            for (int x = 0; x<numberOfColumns; x++){
                gc.setFill(pallet[image[y*numberOfRows+x]&0xFF]);
                gc.fillRect(xPos+x*resolution, yPos+y*resolution,resolution,resolution);
            }
        if(isLabelVisible) printLabel(gc);
    }

    /**
     * Make the canvas draw the label.
     * @param gc The canvas' {@link GraphicsContext}.
     */
    private void printLabel(GraphicsContext gc) {
        gc.setFill(pallet[255]);
        String toPrint = String.valueOf(currentChar);
        double x = (1+4*currentLabelPosition.getHPosition())/6d*canvas.getWidth();
        double y = (1+4*currentLabelPosition.getVPosition())/6d*canvas.getHeight();
        gc.fillText(toPrint,x,y);
    }

    /**
     * Returns the text width in pixel.
     * @param font {@link Font} currently used by the canvas.
     * @param s {@link String} to be drawn on the canvas.
     * @return Pixel width.
     */
    private static double textWidth(Font font, String s){
        Text text = new Text(s);
        text.setFont(font);
        return text.getBoundsInLocal().getWidth();
    }

    /**
     * Returns an approximation of the text height in pixel.
     * @param font {@link Font} currently used by the canvas.
     * @param s {@link String} to be drawn on the canvas.
     * @return Approximation of the pixel height.
     */
    private static double textHeight(Font font, String s){
        Text text = new Text(s);
        text.setFont(font);
        return text.getBoundsInLocal().getHeight();
    }

    /**
     * Returns the approximations of the text size.
     * @param font {@link Font} currently used by the canvas.
     * @param s {@link String} to be drawn on the canvas.
     * @return {@link Dimension} sizes in pixel.
     */
    private static Dimension textSizes(Font font, String s){
        Text text = new Text(s);
        text.setFont(font);
        return new Dimension((int)text.getBoundsInLocal().getWidth(),(int)(font.getSize()*22d/36));
    }

    /**
     * Stores when the label has to be drawn.
     * @param positionString Is actually a {@link POSITION} called by a String thanks to the enum possibilities.
     */
    public void setLabelPosition(String positionString){
        currentLabelPosition=POSITION.valueOf(positionString);
        repaint();
    }

}
