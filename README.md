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
- Another view that shows multiple images at once.
- Saving single or multiple images into PNG, JPG or both types files.
- Panel and/or dialog that shows details about the currently displayed image.
- Panel and/or dialog that shows details about the currently read Dataset file.

Added functionalities : 
- Snapshots in all formats supported natively by JAVA (JPG, PNG, BMP, TIF, TIFF, WBMP). Fast snapshots to the last folder used to save snapshots, or to the dataset directory if no saved snapshots were previously taken.
- Image mean for the whole dataset, for filtered characters and for a specific character.
- Hint appearing over the next to the cursor to give details (coordinate and value from 0 to 255) about individual pixels.
- Multiple sorting.
- Filters to show only selected characters (or figures).
- Positioning of the labels at  8 possible locations.
- Free resizability of the canvas.
- Automatic reading of the labels.
- Emnist compatibility (binary format).
- Choosing the colours (background and font) to draw the image. Transparency (alpha canal) is not taken into account by the canvas.
- Mnist Compatibility.

Feel free to comment on my work and to give suggestions.


MNIST : Thanks to  Yann LeCun, Professor
The Courant Institute of Mathematical Sciences
New York University
and
Corinna Cortes, Research Scientist
Google Labs, New York
corinna at google dot com 


EMNIST : Thanks to Cohen, G., Afshar, S., Tapson, J., & van Schaik, A. (2017). EMNIST: an extension of MNIST to handwritten letters. Retrieved from http://arxiv.org/abs/1702.05373

Contact me at : g.wael@outlook.fr
