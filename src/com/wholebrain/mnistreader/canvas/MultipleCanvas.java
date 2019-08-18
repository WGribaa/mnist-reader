package com.wholebrain.mnistreader.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public final class MultipleCanvas extends CustomCanvas {
    private int imageNumberOfRows = 1, imageNumberOfColumns = 1,
    image1StartY=0, xMouse, yMouse, xNumber, yNumber, gap;
    private double resolution = 1, currentYPos = 0;

    @Override
    protected void paintLabels(GraphicsContext graphicsContext) {

    }

    @Override
    protected void initializeHint(Canvas canvas) {

    }

    @Override
    public DIRECTION getScrollBarPosition() {
        return DIRECTION._RIGHT;
    }

    @Override
    public int getShownImageCount() {
        calculateImageCount();
        return xNumber*yNumber;
    }

    @Override
    public int getFirstShownIndex(int indexTry, int numberOfImages) {
        int lastToFillCanvas =numberOfImages-numberOfImages%getShownImageCount() -1;
        return (indexTry>lastToFillCanvas?
                lastToFillCanvas+1:
                indexTry-indexTry%xNumber);
    }

    protected void paint(GraphicsContext graphicsContext){
        if(imageBuffers==null || imageBuffers.length ==0)
            return; //todo
        calculateImageCount();
        // first row of images
        for (int i = 0; i<xNumber && i<imageBuffers.length; i++){
            drawImage(graphicsContext, i, image1StartY, i*(gap+1),0);
        }
        for (int line = 1; line<yNumber; line++){
            for (int column = 0; column<yNumber; column++)
                drawImage(graphicsContext,line*imageNumberOfColumns+column, 0,
                        column*(gap+1),line*(gap+1));
        }
    }

    private void calculateImageCount(){
        xNumber = (int)(canvas.getWidth()/(resolution * imageNumberOfColumns));
        gap = (int)((canvas.getWidth()-(xNumber*imageNumberOfColumns*resolution))/(xNumber+1.0));
        int remainY = (int)(canvas.getHeight()-(imageNumberOfRows-image1StartY)*resolution)-gap;
        yNumber = (int)(remainY/(imageNumberOfRows*resolution+gap));
    }

    private void drawImage(GraphicsContext gc, int index, int startY, int xPosOnContext, int yPosOnContext){
        for(int y = startY; y<imageNumberOfRows; y++)
            for (int x = 0; x<imageNumberOfColumns; x++){
                gc.setFill(pallet[imageBuffers[index][y*imageNumberOfRows+x]&0xFF]);
                gc.fillRect(xPosOnContext+x*resolution, yPosOnContext+y*resolution,resolution,resolution);
            }
    }
}
