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
//        calculateImageCount();
        System.out.println("Shown image count = "+
                (Math.max(imagesPerLine * (imagesPerColumn),1)));
        return Math.max(imagesPerLine * (imagesPerColumn),1);
    }

    @Override
    public int getIndexFor(int scrollValue) {
        firstLineStartY = scrollValue %(gap+resolution* imageVResolution);
        System.out.println("Calls calculate for getIndexFor");
        calculateImageCount();
        /*System.out.println("GET INDEX FOR : "+scrollValue+"\n\tLine starts at "+line1StartY+"\n\t"
                +(int)(imagesPerLine *(scrollValue/(gap+resolution* imageVResolution)))+
                "\n\tImages shown = "+ imagesPerLine +"*"+ imagesPerColumn);*/
        System.out.println("\nIndex for scrollValue "+scrollValue+" is "+
                (imagesPerLine *(scrollValue/(gap+resolution* imageVResolution)))+ " starting at line "+ firstLineStartY);
        return imagesPerLine *(scrollValue/(gap+resolution* imageVResolution));
    }

    @Override
    public int getScrollValueForIndex(int index) {
        System.out.println("Calls calculate for getScrollValueForIndex");
        calculateImageCount();

//        int totalVRes = gap*(Math.ceil())


        System.out.println("Scroll value for "+index+" is "+
                ((index/ imagesPerLine)*(gap+resolution* imageVResolution)));
        /*System.out.println("\n\n\tScroll Value for index "+ index +" = "+
                (line1StartY+ (index/ imagesPerLine)*(gap+resolution* imageVResolution))+
                "\n\t with line 1 starting at "+line1StartY);*/
        return (index/ imagesPerLine)*(gap+resolution* imageVResolution);
    }

    @Override
    public int getScrollBarMaxValueFor(int elementCount) {
        System.out.println("Calls calculate for getScrollBarMaxValue");
        calculateImageCount();
        int numberOfLines = (int)Math.ceil(elementCount*1.0/imagesPerLine);
        int totalSize = numberOfLines*(imageHResolution*resolution)+(numberOfLines+1)*gap;
        System.out.println("\n\nMax scroll value is "+(totalSize-(int)canvas.getHeight())+" with size "+
                canvas.getWidth()+"*"+canvas.getHeight());
        return totalSize-(int)canvas.getHeight();
    }

    @Override
    public double getScrollBarUnitIncrement() {
//        return resolution*imageVResolution/4;
        return 1;
    }

    protected void paint(GraphicsContext graphicsContext){
        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        System.out.println("Calls calculate for paint");
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
        imagesPerColumn= (int)Math.ceil(canvas.getHeight()/(imageVResolution*resolution+gap));/*
        int remainY = (int)(canvas.getHeight()-(imageVResolution - firstLineStartY)*resolution)-gap;
        imagesPerColumn = Math.max((remainY/(imageVResolution *resolution+gap)+1),1);
        if(getHeight()> (gap+imageVResolution*resolution)*imagesPerColumn)
            imagesPerColumn++;*/
        if(firstLineStartY>0)
            imagesPerColumn++;
        System.out.println("*x = "+imagesPerLine+", y = "+imagesPerColumn+", start line = "+firstLineStartY+", gap = "+gap+", w*h="+canvas.getWidth()+"*"+canvas.getHeight());
        /*System.out.println("*calculating image count with lineY = "+line1StartY);
        /*System.out.println("*********** MULTIPLECANVAS.PAINT()*********\n\tResolution = "+resolution
                +"\tImage definition = "+numberOfColumns+"*"+numberOfRows
                +"("+(int)canvas.getWidth()+"*"+(int)canvas.getHeight()+")\n\txNumber = "+xNumber
                +"\n\tyNumber = "+yNumber+"\n\tGAP = "+gap);*/
    }
}
