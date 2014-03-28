After making a couple of scene-mapped sketches, we’ve started working on an ‘alignment tool’ (not quite sure what to call it) that comes in the Makar library to make it much much easy (much less frustrating) to map the 3D graphics onto the real scenery. Basically, when running a Makar sketch in the the processing IDE you hit ‘c’ and it starts then goes like this:

1. Click on a point in the scene that can be seen in another photo.
2. Find that point in another photo and click to define that point in 3D space. This is the origin.
![Origin](https://raw.github.com/robclouth/devart-template/master/project_images/calib1.jpg)
3. Repeat the process for points that represent the x and y directions.
![X](https://raw.github.com/robclouth/devart-template/master/project_images/calib2.jpg)
![y](https://raw.github.com/robclouth/devart-template/master/project_images/calib3.jpg)
4. The sceneMatrix automatically updates and now your origin point should be (x=0, y=0, z=0), and the x-axis and y-axis should line up good and proper with the real scene.
5. This doesn’t work yet. Currently a transformation matrix hellscape.
6. I’m working on it…it’s almost there.

