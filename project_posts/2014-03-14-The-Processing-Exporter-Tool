To make all the dexing and the jiggery-pokery detailed in the last post easy, we decided to make a [Processing tool](http://processing.org/reference/tools/) for the IDE, that adds a one-click ‘Export to Makar’ option in the tools menu.

This single click does the following:
- Tweaks the code to replace load data methods with our own
- Builds the sketch and dexes it
```
((JavaMode) editor.getMode()).handleExportApplication(sketch);
…
Arguments arguments = new Arguments();
arguments.parse(new String[] { "--output=" + dexMainJar.getAbsolutePath(), jarFile.getAbsolutePath() });
int result = com.android.dx.command.dexer.Main.run(arguments);
```
- Finds all the included libraries with JavaBuild.getImportedLibraries() and dexes them
- Zips everything (jars, data folder, etc.) up into a nice SketchName.makar file, ready to be uploaded to the Makar website