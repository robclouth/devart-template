
Rather than coding all the AR stuff ourselves (a mathematical nightmare) we’ll be using a library to do all the hard stuff. After checking out [Vuforia](https://www.vuforia.com/) first, we ended up settling on [Metaio](http://www.metaio.com/) . This was because of a few reasons: 

1. It has SLAM (simultaneous localization and mapping) technology which is totally awesome.

http://www.youtube.com/watch?v=Y9HMn6bd-v8

...and it seems to run pretty fast on my god-awful phone, which is even more awesome. This is much better than the 2D tracking that Vuforia does which only works on flat surfaces. Metaio can track full 3D scenes.

2. You can access the scans generated when the phone starts tracking the scene, unlike Vuforia. This is essential for this project because we need the scan to be saved on the server so that when someone views the sketch, it is aligned nicely with the real surroundings.

However, Metaio has one negative: with the free version you must use have a Metaio splash screen and watermark in your app. This is fine for the prototype, but for the final version we would need a Pro license ($5000!!). I suppose if we win this then some of the cash would go towards that. 

Even though this a closed-source library, it shouldn’t be much of a problem because the library isn’t actually needed outside of the Android app. When developing sketches for Makar in Processing, the ‘AR’ness is simulated.
 

