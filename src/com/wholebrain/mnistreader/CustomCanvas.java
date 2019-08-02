package com.wholebrain.mnistreader;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CustomCanvas extends Pane {
    //    private Label charLabel = new Label();
    private Color[] pallet =new Color[256];
    private Color backGroundColor = Color.WHITE;
    private char currentChar;
    private byte[] image;
    private int numberOfRows, numberOfColumns;
    private boolean isLabelVisible = true;
    private Font labelFont = new Font(Font.getDefault().getName(),40);
    private Canvas canvas = new Canvas(280,280);

    public CustomCanvas(){
        getChildren().add(canvas);
        setMinSize(280,280);
        canvas.getGraphicsContext2D().setFont(labelFont);
    }
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
    private void repaint(){
        double size = Double.min(canvas.getHeight(), canvas.getWidth());
        double xPos = (canvas.getWidth()-size)/2;
        double yPos = (canvas.getHeight()-size)/2;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double resolution = Double.min(size/numberOfRows, size/numberOfColumns);
        gc.setFill(backGroundColor);
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for(int y = 0; y<numberOfRows; y++)
            for (int x = 0; x<numberOfColumns; x++){
                gc.setFill(pallet[image[y*numberOfRows+x]&0xFF]);
                gc.fillRect(xPos+x*resolution, yPos+y*resolution,resolution, resolution);
            }
        if(isLabelVisible) updateLabel(gc);
    }


    private void updateLabel(GraphicsContext gc) {
        gc.setFill(pallet[255]);
        gc.fillText(String.valueOf(currentChar), 10, labelFont.getSize()*27/36+10);

    }

    public void setBackGroundColor(Color backGroundColor) {
        this.backGroundColor = backGroundColor;
        repaint();
    }

    /**
     * Prepares the color pallet used to paint the images.
     */
    void initializePallet(Color color){
        pallet = new Color[256];
        for (int i = 0; i<256; i++)
            pallet[i]= new Color(color.getRed(), color.getGreen(), color.getBlue(), i/256d);
        repaint();
    }

    public void setLabelVisible(boolean selected) {
        isLabelVisible=selected;
        repaint();
    }
}
