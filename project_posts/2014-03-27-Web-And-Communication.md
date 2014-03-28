##Website and communication

#Intro

One of the main points of our development goals was to make the sketch creation process as seamless as possible. We wanted the creators to be able to be inspired by a spot, track it with the Makar app and have it at their hands in the easiest and most convenient way possible.

Taking this as a starting point we decided to create our custom website where people can register, create and manage their sketches. We used Node.js (http://nodejs.org/) to build the server, which seemed to be the right choice, mainly because of the brilliant and growing community around it. For those who don’t know about Node.js, it’s an amazing platform to build network applications for example websites and real-time communication apps, which is exactly what we needed. To build a clean, MVC type of architecture we used Express.js (http://expressjs.com/) which is a minimal and flexible framework built on top of Node.

One of the biggest advantages of Node is that it’s super simple to add “modules” which take care of different functionalities inside the server. We implemented the main functions of the server using these modules, for example passport.js (http://passportjs.org/) for the user login and authentication,  socket.io (http://socket.io/) for the real0time communication or mongoose.js (http://mongoosejs.com/) to interact with our MongoDB database.

#Code

The idea was that after tracking the given spot, the creator can go online and create a sketch (after signup/login). We wanted to make it possible to give some additional information to the sketch like title, description, etc, so we saved these in the Sketch collection. When saving the new collection, it was important to bear in mind that a QR code should be generated that’s uniquely relates to the sketch. As it turned out, it was quite a simple task, just by adding the “qrcode” Node module to our server
```
fs.rename(tempPath, targetPath, function(err){
            if (err) console.log(err);
        new Sketch({
            authorId: req.user.id,
            title : b.title,
            description: b.description,
            createdAt: Date.now(),
            sketchName: sketchName,
            sketchId: sketchId
        }).save(function(err, sketch) {
                if (err)
                    res.json(err);
                QRCode.save(path.resolve('./public/QRCodes/' + sketch._id + '.png'), '/sketches/' + sketch._id + '/info',
                    function(err, written){
                    if (err) console.log(err);
                    res.redirect('/sketches/' + sketch._id);
                });
            });
    });
```

The communication between the different instances of the sketch was done with socket.io, and the state was saved with MongoDB. Below is some code showing the socket stuff:

```
socket.on('set variable', function(data){
            console.log(data);
            var isPersistent = data.isPersistent;
            var name = data.name;
            var value = data.value;
            socket.broadcast.to(room).emit('set variable', {name:name, value:value});
            if (isPersistent) {
                Sketch.findOne({
                   _id: room
                }, 'variables', function(err, sketch){
                    var variables = sketch.variables;
                    if(variables === undefined) {variables = {}}
                    variables[name] = value;
                    Sketch.update({
                        _id: room
                    }, {$set: {variables: variables}}, {upsert: true}, function(err){
                        if (err) console.log(err);
                    });
                });
            }
        });
```
Basically, when socket.io recieves a “set variable” message from the Makar app, it first broadcasts it out to the other people viewing that sketch, and then saves it to the ‘variables’ field of the Sketch item in the database. Socket.io has this concept of ‘rooms’ and we used this so that communication is kept within those view that sketch…otherwise sketches could cross communicate (which could also be awesome but that’s for later). 

The website is simple. You can signup, create and edit sketches, upload the .makar files and download the QRCodes for printing. This prototype version was made with Bootstrap for quick development. It’s looks ok for now.

The whole thing is run locally at the moment. So for demoing we took a laptop running the server around the city (!!!) but we’re looking into to free/cheap node.js hosting so that this thing can actually be used by other people (that’s the whole point of course). Any suggestions are welcome.