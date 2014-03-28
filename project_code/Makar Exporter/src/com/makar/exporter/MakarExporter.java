/**
 * you can put a one sentence description of your tool here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author		##author##
 * @modified	##date##
 * @version		##version##
 */

package com.makar.exporter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Library;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.app.tools.Tool;
import processing.mode.java.JavaBuild;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;

import com.android.dx.command.dexer.Main.Arguments;

public class MakarExporter implements Tool {

	JavaEditor editor;
	JFrame frame;

	public String getMenuTitle() {
		return "Export to Makar...";
	}

	public void init(Editor editor) {
		this.editor = (JavaEditor) editor;
	}

	public void run() {
		export();

	}

	public void export() {
		final Sketch sketch = editor.getSketch();
		if (handleExportCheckModified()) {
			final String code = editor.getText();
			editor.statusNotice("Exporting...");
			try {
				// CHECK FOR PROPER RENDERER
				Pattern pattern = Pattern.compile("size\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*,(.*)\\)");
				Matcher matcher = pattern.matcher(editor.getText());
				if (!matcher.find())
					throw new SketchException("Sketch must use either the P2D or P3D renderers");
				String rendererString = matcher.group(1).trim();
				if (!(rendererString.equals("P2D") || rendererString.equals("P3D")))
					throw new SketchException("Sketch must use either the P2D or P3D renderers");

				// CHECK FOR .tracking FILE
				File trackingFile = new File(sketch.getFolder().getAbsolutePath() + "/data/" + sketch.getName() + ".tracking");
				if (!trackingFile.exists())
					throw new SketchException("Sketch must have a .tracking file in the data folder");

				// REPLACE FILE INPUT FUNCTIONS WITH MAKAR VERSIONS AND @Synced
				// METHOD STUFF
				String tweakedCode = code.replace("loadImage", "Makar.loadImage");
				tweakedCode = tweakedCode.replace("loadShape", "Makar.loadShape");
				tweakedCode = tweakedCode.replace("loadBytes", "Makar.loadBytes");
				tweakedCode = tweakedCode.replace("loadFont", "Makar.loadFont");
				tweakedCode = tweakedCode.replace("loadStrings", "Makar.loadStrings");
				tweakedCode = tweakedCode.replace("loadTable", "Makar.loadTable");
				tweakedCode = tweakedCode.replace("loadShader", "Makar.loadShader");

				// synced methods
				matcher = Pattern.compile("@Synced([^\\(]*)\\(([^\\)]*)[^\\{]*\\{", Pattern.DOTALL).matcher(tweakedCode);
				if (matcher.find()) {
					String callString = "";
					String[] methodInfo = matcher.group(1).split(" ");
					String methodName = methodInfo[methodInfo.length - 1].trim();
					callString = "\"" + methodName + "\"";

					String[] params = matcher.group(2).split(",");
					for (String param : params) {
						if (param.equals(""))
							continue;

						callString += ", ";
						String[] parts = param.split(" ");
						String name = parts[parts.length - 1];
						callString += name;
					}
					String methodText = matcher.group(0);
					System.out.println(methodText + String.format("\nMakar.syncMethod(%s)", callString));

					tweakedCode = matcher.replaceAll(methodText + String.format("\nMakar.syncMethod(%s);", callString));
				}

				editor.setText(tweakedCode);
				editor.handleSave(true);

				File outDir = new File(sketch.getFolder().getAbsolutePath() + "/Makar");
				outDir.mkdirs();

				// EXPORT SKETCH

				((JavaMode) editor.getMode()).handleExportApplication(sketch);

				// find jar file
				ArrayList<File> jarFiles = new ArrayList<File>();
				search(sketch.getFolder().getAbsolutePath(), jarFiles, new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.equals(sketch.getName() + ".jar");
					}
				});

				if (jarFiles.size() == 0)
					throw new IOException("Can't locate '" + sketch.getName() + ".jar'");

				File jarFile = jarFiles.get(0);

				// DEXIFY MAIN JAR
				final File dexMainJar = new File(outDir + "/" + sketch.getName() + ".jar");
				Arguments arguments = new Arguments();
				arguments.parse(new String[] { "--output=" + dexMainJar.getAbsolutePath(), jarFile.getAbsolutePath() });
				int result = com.android.dx.command.dexer.Main.run(arguments);
				if (result != 0)
					throw new IOException("Error creating dexed jar");

				// DEX LIBRARIES
				JavaBuild javaBuild = new JavaBuild(sketch);
				javaBuild.build(false);

				for (Library lib : javaBuild.getImportedLibraries()) {
					if (lib.getName().equals("core") || lib.getName().equals("Makar"))
						continue;

					File libJarPath = new File(lib.getJarPath());

					File dexJar = new File(outDir + "/" + libJarPath.getName());
					arguments = new Arguments();
					arguments.parse(new String[] { "--output=" + dexJar.getAbsolutePath(), libJarPath.getAbsolutePath() });
					result = com.android.dx.command.dexer.Main.run(arguments);
					if (result != 0)
						throw new IOException("Error creating dexed jar");
				}

				// CREATE ZIP
				byte[] buf = new byte[1024];
				FileInputStream in;
				File sketchFile = new File(outDir + "/" + sketch.getName() + ".makar");
				ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(sketchFile));
				int len;

				// add main dex
				in = new FileInputStream(dexMainJar);
				zip.putNextEntry(new ZipEntry(dexMainJar.getName()));
				while ((len = in.read(buf)) > 0)
					zip.write(buf, 0, len);
				zip.closeEntry();
				in.close();

				// add library dexes
				File[] libJars = outDir.listFiles(new FilenameFilter() {
					public boolean accept(File file, String name) {
						return name.endsWith(".jar") && !name.equals(dexMainJar.getName());
					}
				});
				for (File jar : libJars) {
					in = new FileInputStream(jar);
					zip.putNextEntry(new ZipEntry("libs/" + jar.getName()));
					while ((len = in.read(buf)) > 0)
						zip.write(buf, 0, len);
					zip.closeEntry();
					in.close();
					jar.delete();
				}

				// add data folder
				final ArrayList<File> dataFiles = new ArrayList<File>();
				search(sketch.getDataFolder().getAbsolutePath(), dataFiles, new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return true;
					}
				});

				for (File dataFile : dataFiles) {
					if (dataFile.getName().endsWith(".tracking"))
						continue;

					in = new FileInputStream(dataFile);
					zip.putNextEntry(new ZipEntry("data/" + dataFile.getAbsolutePath().replace(sketch.getDataFolder().getAbsolutePath(), "")));
					while ((len = in.read(buf)) > 0)
						zip.write(buf, 0, len);
					zip.closeEntry();
					in.close();
				}

				// add tracking map from tracking file
				zip.putNextEntry(new ZipEntry("map"));
				ZipInputStream zis = new ZipInputStream(new FileInputStream(trackingFile));
				ZipEntry ze = zis.getNextEntry();
				ByteArrayOutputStream bos;
				while (ze != null) {
					String filename = ze.getName();
					if (!filename.equals("map")) {
						ze = zis.getNextEntry();
						continue;
					}

					bos = new ByteArrayOutputStream();
					while ((len = zis.read(buf)) > 0)
						zip.write(buf, 0, len);

					bos.close();
					break;
				}
				zis.closeEntry();
				zis.close();
				zip.closeEntry();

				zip.close();

				dexMainJar.delete();

				editor.statusNotice("Done exporting.");

				Base.openFolder(outDir);
			} catch (IOException e) {
				editor.statusNotice("Error during export. See console for more information.");
				System.out.println(e.getMessage());
				// e.printStackTrace();
			} catch (SketchException e) {
				editor.statusNotice("Error during export. See console for more information.");
				System.out.println(e.getMessage());
				// e.printStackTrace();
			} catch (IllegalArgumentException e) {
				editor.statusNotice("Error during export. See console for more information.");
				e.printStackTrace();
			} catch (SecurityException e) {
				editor.statusNotice("Error during export. See console for more information.");
				// e.printStackTrace();
			} finally {
				editor.handleSave(true);
				editor.setText(code);
			}
		}

	}

	protected boolean handleExportCheckModified() {
		if (editor.getSketch().isModified()) {
			Object[] options = { "OK", "Cancel" };
			int result = JOptionPane.showOptionDialog(editor, "Save changes before export?", "Save", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			if (result == JOptionPane.OK_OPTION) {
				editor.handleSave(true);

			} else {
				// why it's not CANCEL_OPTION is beyond me (at least on the mac)
				// but f-- it.. let's get this shite done..
				// } else if (result == JOptionPane.CANCEL_OPTION) {
				editor.statusNotice("Export canceled, changes must first be saved.");
				// toolbar.clear();
				return false;
			}
		}
		return true;
	}

	private void search(String path, ArrayList<File> foundFiles, FilenameFilter filter) {
		File root = new File(path);
		File[] list = root.listFiles();

		for (File f : list) {
			if (f.isDirectory()) {
				search(f.getAbsolutePath(), foundFiles, filter);
			} else {
				if (filter.accept(f.getAbsoluteFile(), f.getName()))
					foundFiles.add(f);
			}
		}
	}
}
