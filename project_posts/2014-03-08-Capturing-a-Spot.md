When making a location-dependent AR scene, you need a way of scanning the target environment so that other people that come to the scene later, see the AR graphics in the place that you want them. Basically, a universal reference for everyone. This is a 3D map of the scene. The Metaio library handles the scanning maths, we just needed to package it up into a simple process.

1. Track the scene by moving the phone horizontally
![sketch](https://raw.github.com/robclouth/devart-template/master/project_images/tracking1.jpg)
2. Take photos at various angles
![sketch](https://raw.github.com/robclouth/devart-template/master/project_images/tracking2.jpg)
3. The 3D map file and photo are packaged up into a .tracking file
![sketch](https://raw.github.com/robclouth/devart-template/master/project_images/tracking3.jpg)

Step 2 is important for demoing the sketch as you develop it in processing. The Makar processing library will load the tracking file and display the images you took as the background while hijacking the Processing camera so that the angle and perspective matches that of the photo.