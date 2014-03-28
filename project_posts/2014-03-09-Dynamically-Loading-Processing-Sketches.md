Since we chose Processing as the tool to make the sketches themselves, we needed a way to dynamically load sketches inside the Makar app as the user finds and scans it with their phone. This turned out to be a bit of nightmare. 

The first step is to export the sketch from Processing to get the compiled jar file. Before loading this into Android however, the jar needs to be dexed. Android uses a special format for class files, called dex. I can’t tell you much more about this (because I don’t know, and documentation on the web is limited) but basically you need to use the dex.jar tool that comes with the Android SDK. Running this on the sketch jar gives you a new, freshly dexed jar. The next step is to load this inside the Android app, using the DexClassLoader class. This is done like so:

```
final File dexDir = parent.getDir(id + "_dex", 0);
final DexClassLoader classloader = new DexClassLoader(jarPathString, dexDir.getAbsolutePath(), null, parent.getClassLoader());
Class<?> sketchClazz = classloader.loadClass(sketchName);
PApplet child = (PApplet) sketchClazz.newInstance();
```

Now you have loaded the sketch!
That was the easy part. 
Actually running the sketch isn’t so simple. There were three general problems to solve.

##Problems
1. How to initialise and start the sketch from the outside.
2. How to setup the data directory.
3. How to draw the sketch to a canvas of our choosing.
4. How to route input (touch and the like) to the hosted sketch

To work out what to do here took hours of digging through the Processing source, so hopefully this will save someone somewhere a bit of time.

##Solutions
1. Do this:
```
child.g = parent.g; //replaces the sketch drawing canvas with our own
child.noLoop(); //stops the sketch from drawing itself (we do that)
child.setup(); //runs the sketch setup()
child.start(); //does some other misc. sketch initialisation stuff
```

2. This was a total hack, because when you load something in a Processing sketch on Android (loadImage or whatever) it first tries to look in the assets folder, but to do this you need a [Context](http://developer.android.com/reference/android/content/Context.html), and to have this you need an [Activity](http://developer.android.com/reference/android/app/Activity.html)…and we don’t since we’re reanimating the sketch with black magic. Sooo, when the sketch is exported from the Processing IDE with our special tool (we’ll talk about that later) it edits the code to replace any loadWhatever() call with our own Makar.loadWhatever() version that redirects the loading to the parent app. Also the original data folder with the child sketch is copied to the Makar app directory for easy access.

3. This was fairly easy, once we decided to make the Makar app a Processing sketch too (see the first line of code in solution 1). But a word of warning: DO NOT TRY TO DYNAMICALLY HOST A SKETCH INSIDE A NON-PROCESSING-BASED APP!!! It’s a total nightmare. Drawing canvas issues and stuff. I can’t really remember the details, I’ve buried the memories deep. Just don’t do it.

4. Since the parent app is also a Processing sketch, this was easy - just route the mousePressed()’s and the like down to the child sketch.

So with all that (and some other stuff) you now have a sketch running inside another!
