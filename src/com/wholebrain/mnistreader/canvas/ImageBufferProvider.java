package com.wholebrain.mnistreader.canvas;

public interface ImageBufferProvider {
    void notifySizeChange();
    int getIndexOfImageBuffer(int position);
    void forceDeltaPosition(int delta);
}
