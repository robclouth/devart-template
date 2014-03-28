package io.diffuse.makar;

import com.makar.Makar;

import processing.core.PApplet;

public class TestSketch extends PApplet {
	public void setup() {
		size(400, 400, P3D);

		Makar.init(this);
	}

	public void draw() {
		Makar.beginDraw();
		lights();

		translate(width / 2 + 30, height / 2, 0);

		box(45);
		translate(0, 0, -50);
		box(30);
		Makar.endDraw();
	}
	
	@Override
	 public void handleDraw() {
	    if (DEBUG) {
	      println("inside handleDraw() " + millis() +
	              " changed=" + surfaceChanged +
	              " ready=" + surfaceReady +
	              " paused=" + paused +
	              " looping=" + looping +
	              " redraw=" + redraw);
	    }
	    if (surfaceChanged) {
	      int newWidth = surfaceView.getWidth();
	      int newHeight = surfaceView.getHeight();
	      if (newWidth != width || newHeight != height) {
	        width = newWidth;
	        height = newHeight;
	        g.setSize(width, height);
	      }
	      surfaceChanged = false;
	      surfaceReady = true;
	      if (DEBUG) {
	        println("surfaceChanged true, resized to " + width + "x" + height);
	      }
	    }

//	    if (surfaceView.isShown()) {
//	      println("surface view not visible, getting out");
//	      return;
//	    } else {
//	      println("surface set to go.");
//	    }

	    // don't start drawing (e.g. don't call setup) until there's a legitimate
	    // width and height that have been set by surfaceChanged().
//	    boolean validSize = width != 0 && height != 0;
//	    println("valid size = " + validSize + " (" + width + "x" + height + ")");
	    if (g != null && surfaceReady && !paused && (looping || redraw)) {
//	      if (!g.canDraw()) {
//	        // Don't draw if the renderer is not yet ready.
//	        // (e.g. OpenGL has to wait for a peer to be on screen)
//	        return;
//	      }

	      g.beginDraw();

	      long now = System.nanoTime();

	      if (frameCount == 0) {
	        try {
	          //println("Calling setup()");
	          setup();
	          //println("Done with setup()");

	        } catch (RendererChangeException e) {
	          // Give up, instead set the new renderer and re-attempt setup()
	          return;
	        }
//	        this.defaultSize = false;

	      } else {  // frameCount > 0, meaning an actual draw()
	        // update the current frameRate
	        double rate = 1000000.0 / ((now - frameRateLastNanos) / 1000000.0);
	        float instantaneousRate = (float) rate / 1000.0f;
	        frameRate = (frameRate * 0.9f) + (instantaneousRate * 0.1f);

	        // use dmouseX/Y as previous mouse pos, since this is the
	        // last position the mouse was in during the previous draw.
	        pmouseX = dmouseX;
	        pmouseY = dmouseY;
	        //pmotionX = dmotionX;
	        //pmotionY = dmotionY;

	        //println("Calling draw()");
	        draw();
	        //println("Done calling draw()");

	        // dmouseX/Y is updated only once per frame (unlike emouseX/Y)
	        dmouseX = mouseX;
	        dmouseY = mouseY;
	        //dmotionX = motionX;
	       // dmotionY = motionY;

	        // these are called *after* loop so that valid
	        // drawing commands can be run inside them. it can't
	        // be before, since a call to background() would wipe
	        // out anything that had been drawn so far.
	        //dequeueMotionEvents();
	        //dequeueKeyEvents();

	        redraw = false;  // unset 'redraw' flag in case it was set
	        // (only do this once draw() has run, not just setup())

	      }

	      g.endDraw();

	      frameRateLastNanos = now;
	      frameCount++;
	    }
	  }
}
