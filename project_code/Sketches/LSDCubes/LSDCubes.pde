import com.makar.*;

Window[] windows = new Window[12];
PVector gravity;
ArrayList<Thing> things;

public void setup() {
  size(400, 600, P3D);
  Makar.init(this);

  Makar.rotateScene(-1.52f, 0.3456f, -0.047f);
  Makar.translateScene(-137, -375, -10);

  gravity = new PVector(0, 0.3f, 0);

  things = new ArrayList<Thing>();

  // make windows
  windows[0] = new Window(10, 10, 50, 50);
  windows[1] = new Window(80, 10, 90, 50);
  windows[2] = new Window(190, 10, 50, 50);
  windows[3] = new Window(10, 120, 50, 50);
  windows[4] = new Window(100, 110, 50, 50);
  windows[5] = new Window(190, 110, 50, 50);
  windows[6] = new Window(10, 210, 50, 100);
  windows[7] = new Window(100, 210, 50, 100);
  windows[8] = new Window(190, 210, 50, 100);
  windows[9] = new Window(10, 340, 50, 110);
  windows[10] = new Window(100, 340, 50, 110);
  windows[11] = new Window(190, 340, 50, 110);
}

public void draw() {
  Makar.beginDraw();

  for (int i = 0; i < 12; i++) {
    windows[i].draw();
  }

  for (int i = things.size() - 1; i >= 0; i--) {
    Thing thing = things.get(i);
    if (!thing.isDestroyed) {
      thing.update();
      thing.draw();
    } 
    else
      things.remove(i);
  }

  Makar.endDraw();
}

public void mousePressed() {
  for (int i = 0; i < 12; i++) {
    if (windows[i].mouseArea.isMouseOver()) {
      windows[i].throwThing();
    }
  }
}

class Window {
  float x, y, w, h;
  int c;
  float timer = 0;
  MouseArea mouseArea;

  Window(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;

    c = color(random(255), random(255), random(255));

    mouseArea = new MouseArea(x, y, w, h);
  }

  public void throwThing() {
    // OBJModel model = models.get((int) random(models.size()));
    PVector pos = new PVector(x + w / 2, y + h / 2, 0);

    PVector vel = new PVector(0, -1f, -2f);
    Thing thing = new Thing(pos, vel);
    things.add(thing);

    timer = 60;
  }

  void draw() {
    timer--;

    if (timer > 0) {
      pushMatrix();

      translate(x, y, 0);
      translate(-0.5f, -0.5f, 0);
      scale(w, h, 1);
      translate(0.5f, 0.5f, 0);

      pushStyle();
      colorMode(HSB);
      fill(random(255), 255, 255);
      noStroke();

      box(1);
      popMatrix();
      popStyle();
    }
    // mouseArea.draw();
  }
}

class Thing {
  PVector pos, vel, acc, rot;
  boolean isDestroyed = false;

  Thing(PVector pos, PVector vel) {
    this.pos = new PVector();
    this.pos.set(pos);
    this.vel = new PVector();
    this.vel.set(vel);
    acc = new PVector();
    rot = new PVector(random(10), random(10), random(10));
  }

  void update() {
    vel.add(gravity);
    pos.add(vel);
    rot.x += 0.01f;
    rot.y += 0.03f;
    rot.z += 0.007f;

    if (pos.y > 600) {
      if (abs(vel.y) < 0.001f)
        isDestroyed = true;
      else {
        pos.y = 599;
        vel.y *= -0.8f;
      }
    }
  }

  void draw() {
    pushMatrix();
    translate(pos.x, pos.y, pos.z);
    rotateX(rot.x);
    rotateY(rot.y);
    rotateZ(rot.z);
    pushStyle();
    noStroke();

    colorMode(HSB);
    fill(random(255), 255, 255);
    box(20);
    popStyle();
    popMatrix();
  }
}

