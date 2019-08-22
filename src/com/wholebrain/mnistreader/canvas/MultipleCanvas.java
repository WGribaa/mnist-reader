package com.wholebrain.mnistreader.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public final class MultipleCanvas extends CustomCanvas {
    private int firstLineStartY =0, xMouse, yMouse, imagesPerLine =1, imagesPerColumn =1, gap;

    @Override
    protected void paintLabels(GraphicsContext graphicsContext) {
        // todo
    }

    @Override
    protected void initializeHint(Canvas canvas) {
        //todo
    }

    @Override
    protected void notify(SizeChangeListener listener) {
        listener.notifySizeChange();
    }

    @Override
    public DIRECTION getScrollBarPosition() {
        return DIRECTION._RIGHT;
    }

    @Override
    public int getShownImageCount() {
        return Math.max(imagesPerLine * (imagesPerColumn),1);
    }

    @Override
    public int getIndexFor(int scrollValue) {
        firstLineStartY = scrollValue %(gap+resolution* imageVResolution);
        calculateImageCount();
        return imagesPerLine *(scrollValue/(gap+resolution* imageVResolution));
    }

    @Override
    public int getScrollValueForIndex(int index) {
        calculateImageCount();
        return (index/ imagesPerLine)*(gap+resolution* imageVResolution);
    }

    @Override
    public int getScrollBarMaxValueFor(int elementCount) {
        calculateImageCount();
        int numberOfLines = (int)Math.ceil(elementCount*1.0/imagesPerLine);
        int totalSize = numberOfLines*(imageHResolution*resolution)+(numberOfLines+1)*gap;
        return totalSize-(int)canvas.getHeight();
    }

    @Override
    public double getScrollBarUnitIncrement() {
        return resolution*imageVResolution/4;
    }

    protected void paint(GraphicsContext graphicsContext){
        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        calculateImageCount();
        if(imageBuffers==null || imageBuffers.length ==0)  return;
        for (int i = 0; i< imagesPerLine * imagesPerColumn && i<imageBuffers.length; i++)
            drawImage(graphicsContext, i,
                    i% imagesPerLine *imageHResolution*resolution+gap*(i% imagesPerLine +1),
                    gap*(1+i/ imagesPerLine)+resolution*imageVResolution*(i/ imagesPerLine)- firstLineStartY);
    }

    private void drawImage(GraphicsContext gc, int index, int xPosOnContext, int yPosOnContext){
        for(int y = 0; y< imageVResolution; y++)
            for (int x = 0; x< imageHResolution; x++){
                gc.setFill(pallet[imageBuffers[index][y* imageVResolution +x]&0xFF]);
                gc.fillRect(xPosOnContext+x*resolution, yPosOnContext+y*resolution,resolution,resolution);
            }
    }

    private void calculateImageCount(){
        imagesPerLine = Math.max((int)(canvas.getWidth()/(resolution * imageHResolution)),1);
        gap = (int)((canvas.getWidth()-(imagesPerLine * imageHResolution *resolution))/(imagesPerLine +1.0));
        if(gap<0) gap = 0;
        if(canvas.getHeight()<imageVResolution*resolution+gap)
            gap=Math.min(gap,((int)canvas.getHeight()-imageVResolution*resolution)/2);
        imagesPerColumn= (int)Math.ceil(canvas.getHeight()/(imageVResolution*resolution+gap));
        if(firstLineStartY>0)
            imagesPerColumn++;
    }
}
