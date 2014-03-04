Makar is a pretty huge undertaking to be honest - especially to do in just a month - but it’s something me, Felipe and Jakab have been wanting to do for a while and so we thought this would be the perfect opportunity to try it. If we don’t win it’s no biggie since we’ll have made and documented a prototype of the system in the process.

As an introduction: we’re fascinated by the creative power of augmented reality. The fact that you could create a second ‘world’ that mirrors the real except without confinements of reality, a planet-sized playground of undiscovered real-digital-hybrid wonders. Epic. But for this to happen there needs to be creators to fill the space - and at the moment AR is just too impenetrable for the less tech-savvy of us, and the technologies too fragmented meaning you need a separate app for every augmentation. Makar should possibly maybe probably help with this.

Today we sat down and decided how we would like the system to work, for both the viewer and the creator:

As a Viewer:
1. The user opens up the Makar app and looks on the map to find a nearby artwork.
2. They go to the site and see one of the special Makar QRCodes on a wall and scan it.
3. Using the id contained in the QRCode, the app talks to the Makar server and retrieves the artwork information (author, comments, rating), artwork assets (code, sounds, images) and the tracking data (used for the AR tracking of the augmented scene).
4. The app uses the downloaded tracking data to track the scene, and renders the artwork.
5. Any interactions with the artwork are bounced via the server so that the state is shared between multiple users viewing the same artwork.
6. When the user leaves the artwork, the state is saved onto the server so that any changes remain.

As a Creator:
1. Firstly, the creator logs into the Makar system and creates a new Artwork, and they are given a unique QR code to print out.
2. The creator finds a space they want to augment.
3. The creator points the device at the space and if the app says its suitable, starts tracking it.
4. The user then takes a series of photos of the scene that are saved alongside the recording position of the device when each one was taken. These are used to simulate the augmentation offsite, making mapping and testing the scene much easier.
5. The QR code is placed at the site.
6. Offsite, the creator develops the artwork using the Processing framework and the Makar library. The reference photos taken at the site are used to get a good mapping. 
7. Once finished, the Artwork is declared ‘online’ and the public can now find and view the created artwork.

A nice diagram will follow shortly…
