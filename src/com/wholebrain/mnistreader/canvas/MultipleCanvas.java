package com.wholebrain.mnistreader.canvas;

import javafx.event.EventHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

public final class MultipleCanvas extends CustomCanvas {
    private int firstLineStartY =0, imagesPerLine =1, imagesPerColumn =1, gap;

    @Override
    protected void paintLabels(GraphicsContext graphicsContext) {
        // todo
    }

    @Override
    protected EventHandler<MouseEvent> getHintEvent() {
        return mouseEvent -> {
            if (!showHint) Tooltip.uninstall(canvas, pxHint);
            else {
                int currentXMouse = (int)mouseEvent.getX();
                int currentYMouse = (int)mouseEvent.getY();
                int frameHDefinition = imageHDefinition*resolution+gap;

                if(currentXMouse%(frameHDefinition)< gap){
                    Tooltip.uninstall(canvas, pxHint);
                }
                else{
                    int frameVDefinition = imageVDefinition*resolution+gap;
                    int yMouse2 = currentYMouse+firstLineStartY;
                    if(yMouse2%(gap+ imageVDefinition *resolution)<gap)
                        Tooltip.uninstall(canvas, pxHint);
                    else{
                        int column = currentXMouse/frameHDefinition;
                        int line = yMouse2/(frameVDefinition);
                        int xStart = column*frameHDefinition+gap;
                        int yStart = line*frameVDefinition-firstLineStartY+gap;
                        int xCoord = (currentXMouse-xStart)/(resolution);
                        int yCoord = (currentYMouse-yStart)/(resolution);
                        if(xMouse!= xCoord || yMouse != yCoord){
                            pxHint.setX(mouseEvent.getScreenX()+10);
                            pxHint.setY(mouseEvent.getScreenY()+10);
                            xMouse=xCoord;
                            yMouse = yCoord;
                            indexBelowMouse = line*imagesPerLine+column;
                            String tipText = getHintText(line*imagesPerLine+column);
                            pxHint.setText(tipText);
                            if(!pxHint.isActivated()&& tipText!=null)
                                Tooltip.install(canvas, pxHint);
                        }


                    }
                }
            }
        };
    }

    @Override
    protected void notify(ImageBufferProvider listener) {
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
    public int getIndexFor(int position) {
        firstLineStartY = position %(gap+resolution* imageVDefinition);
//        calculateImageCount();
        return imagesPerLine *(position /(gap+resolution* imageVDefinition));
    }

    @Override
    public int getScrollValueForIndex(int index) {
        return (index/ imagesPerLine)*(gap+resolution* imageVDefinition);
    }

    @Override
    public int getScrollBarMaxValueFor(int elementCount) {
        int numberOfLines = (int)Math.ceil(elementCount*1.0/imagesPerLine);
        int totalSize = numberOfLines*(imageHDefinition *resolution)+(numberOfLines+1)*gap;
        return totalSize-(int)canvas.getHeight();
    }

    @Override
    public double getScrollBarUnitIncrement() {
        return resolution* imageVDefinition /4;
    }

    protected void paint(GraphicsContext graphicsContext){
        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        calculateImageCount();
        if(imageBuffers==null || imageBuffers.length ==0)  return;
        for (int i = 0; i< imagesPerLine * imagesPerColumn && i<imageBuffers.length; i++)
            drawImage(graphicsContext, i,
                    i% imagesPerLine * imageHDefinition *resolution+gap*(i% imagesPerLine +1),
                    gap*(1+i/ imagesPerLine)+resolution* imageVDefinition *(i/ imagesPerLine)- firstLineStartY);
    }

    private void drawImage(GraphicsContext gc, int index, int xPosOnContext, int yPosOnContext){
        for(int y = 0; y< imageVDefinition; y++)
            for (int x = 0; x< imageHDefinition; x++){
                gc.setFill(pallet[imageBuffers[index][y* imageVDefinition +x]&0xFF]);
                gc.fillRect(xPosOnContext+x*resolution, yPosOnContext+y*resolution,resolution,resolution);
            }
    }

    private void calculateImageCount(){
        imagesPerLine = Math.max((int)(canvas.getWidth()/(resolution * imageHDefinition)),1);
        gap = (int)((canvas.getWidth()-(imagesPerLine * imageHDefinition *resolution))/(imagesPerLine +1.0));
        if(gap<0) gap = 0;
        if(canvas.getHeight()< imageVDefinition *resolution+gap)
            gap=Math.min(gap,((int)canvas.getHeight()- imageVDefinition *resolution)/2);
        imagesPerColumn= (int)Math.ceil(canvas.getHeight()/(imageVDefinition *resolution+gap));
        if(firstLineStartY>0)
            imagesPerColumn++;
    }
}
