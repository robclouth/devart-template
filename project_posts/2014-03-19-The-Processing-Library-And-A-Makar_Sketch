The Makar library serves a few purposes:
1. Demoing the sketch offsite by using the .tracking file
2. Providing you with easy ways of aligning virtual objects to real objects
3. Gives some tools and methods for interacting with the objects in the sketch, such as MouseArea

Below is a simple Makar sketch in full that shows a seizure-inducing bouncing box, mapped to the scene. The comments explain all.
```
import com.makar.*; //import the Makar library

MouseArea area; // a clickable zone

void setup() {
  size(400, 400, P3D); // the sketch must be P3D
 
  Makar.init(this); // this initiates the library, loading in the .tracking file from the data directory

  // these adjust the scene. The values can be found manually or the alignment tool can be used (discussed (and made) later)
  Makar.rotateScene(0.1,0.52,6.01); 
  Makar.translateScene(123, -32, 20); 

  area = new MouseArea(100, 100, 100, 100); // creates the MouseArea (x, y, width, height)
}

void draw() {
  Makar.beginDraw(); // this adjusts the camera to match the photos taken at the spot
  
  lights();
  noStroke();



  pushMatrix();
  translate(0, 0, abs(sin(frameCount/10.0)*100));
  colorMode(HSB);
  fill(random(255), 255, 255);
  boolean isMouseOver = area.isMouseOver(); // detects if the mouse is over the MouseArea
  box(isMouseOver? 100 : 50);
  popMatrix();  
 
  Makar.endDraw(); // resets the camera to normal
}
```

Anything drawn inside Makar.beginDraw() and Makar.endDraw() are ARified. 
The background photo can be changed with the number keys, and the camera angle automatically updates.



