package com.wholebrain.mnistreader.canvas;

public interface ImageBufferProvider {
    void notifySizeChange();
    void forceDeltaPosition(int delta);
}
