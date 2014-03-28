We wanted Makar sketches to be networked sketches with real-time shared interaction, and that stay as you left them. We wanted it to be a collaborative experience, and one where you feel like you can make a lasting change to your environment. But this stuff is tricky to code unfortunately. So we decided to add a series of @Tags that sketch developers can use to say if a variable is to be shared by all people viewing the sketch, and if that variable shouldn’t reset when everyone leaves. These are @Shared and @Persistent.

They are used before a variable like this:
```
@Shared @Persistent int[] colours = new int[20];
```

- @Shared means that Makar will look for changes in this variable, and if there is one, it sends the new value to the server and it shares it with everyone.

- @Persistent means that the value of the variable is saved to the cloud when it changes so that the changes are permanent. 

These tags make it super easy to create a shared and lasting experience.

There is a third one @Synced that you put on methods. This one means that when you call this method, the same method with the same parameters is called for everyone viewing the sketch. This tag isn’t quite finished yet though, we’re working on it.

Behind the scenes, these tags are Java annotations. The Makar app scans for these in the sketch when it loads it, and puts listeners on the target variables looking for changes, which it sends via socket.io to the Makar server which relays it and stores it. To simplify the whole change-checking thing the value is serialized to json first, then a String comparison is made. Thus: these tags work for any variable that can be serialised by [Gson](https://code.google.com/p/google-gson/).

