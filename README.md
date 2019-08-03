# mnist-reader
A program that reads the MNIST and EMNIST datasets, draws their images on a canvas and shows the character currently drawn.

Please download the MNIST dataset at the following address : 
http://yann.lecun.com/exdb/mnist/

Also consider downloading the MUCH HEAVIER EMNIST datasets :
https://www.nist.gov/node/1298471/emnist-dataset (Binary format)

Once downloaded, unzip the file(s) and simply open it with the mnist-reader.
You must keep the "images" and "labels" files in the same folder.
For the MNIST dataset, no further requirement (default mapping is applied).
For the EMNIST dataset, you must also keep the "*-mapping.txt" file in the same folder as the others.

Upcoming functionalities :
- Filter to show only selected characters (or figures).
- Sorting 0->Z and Z->0.
- Another view that shows multiple images at once.
- Saving single or multiple images into PNG, JPG or both types files.

Added functionalities : 
- Mnist Compatibility.
- Choosing the colours (background and font) to draw the image. Transparency (alpha canal) is not taken into account by the canvas.
- Emnist compatibility (binary format).
- Automatic reading of the labels.
- Free resizability of the canvas.
- Positioning of the labels at  8 possible locations.

Feel free to comment on my work and to give suggestions.

Contact me at : g.wael@outlook.fr
