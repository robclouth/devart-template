import com.makar.*;

Key[] keys = new Key[29];
float keyWidth = 50;
float keyHeight = 50;
float keySpace = 0;

@Shared @Persistent int currPos = 0;
@Shared @Persistent char[] text = new char[20];

void setup() {
  size(400, 400, P3D);

  textFont(loadFont("font.vlw"), 50);
  Makar.init(this);

  Makar.rotateSceneX(4.52);
  Makar.rotateSceneZ(6.24);
  Makar.translateScene(-150, 400, 500);

  for (int i=0; i<26; i++) {
    keys[i] = new Key((i%8)*keyWidth, (i/8)*keyHeight, keyWidth, keyHeight, 
    String.valueOf((char)(i+'a')), String.valueOf((char)(i+'a')));
  }
  keys[26] = new Key(keyWidth*2, keyHeight*3, keyWidth, keyHeight, ".", ".");
  keys[27] = new Key(keyWidth*3, keyHeight*3, keyWidth, keyHeight, ";-p", ";-p");
  keys[28] = new Key(keyWidth*4, keyHeight*3, keyWidth*4, keyHeight, "space", " ");
}

void draw() {
  Makar.beginDraw();

  //lights();
  
    fill(255);
    noStroke();
    rect(0, 0, keyWidth*8, keyWidth*4);

  for (int i=0; i<keys.length; i++) {
    keys[i].draw();
  }

  fill(0);
  textSize(150);
  colorMode(HSB);
  for (int j=0; j<4; j++) {
    fill((frameCount/100. * (j+1))%255, 255, 255);
    pushMatrix();
    translate(200, j*130 - 400, 200);
    rotateY(frameCount/100. * (j+1));
    for (int i=0; i<text.length; i++) {
      pushMatrix();
      rotateY(i*(TWO_PI/(float)text.length));
      translate(300, 0, 0);
      rotateY(-PI/2);

      text(text[i], 0, 0);

      popMatrix();
    }
    popMatrix();
  }

  Makar.endDraw();
}

void mousePressed() {
  for (int i=0; i<keys.length; i++) {
    if (keys[i].mouseArea.isMouseOver())
      keys[i].pressed();
  }
}

class Key {
  MouseArea mouseArea;
  float x, y, w, h;
  String name, string;

  Key(float x, float y, float w, float h, String name, String string) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.name = name;
    this.string = string;
    mouseArea = new MouseArea(x, y, w, h);
  }

  void draw() {
    pushMatrix();

    boolean isMouseOver = mouseArea.isMouseOver() && mousePressed;

    pushStyle();
    if(isMouseOver){
      noStroke();
      fill(40);
      pushMatrix();
      translate(0, 0, -1);
    rect(x,y,w,h);
    popMatrix();
    }
    textAlign(CENTER, CENTER);
    fill(isMouseOver? 255 : 40);
    textSize(30);
    text(name, x+w/2, y+h/2, -5);
    popStyle();

    popMatrix();
  }

  void pressed() {
    for(int i=0; i<string.length(); i++){
      text[currPos] = string.charAt(i);
      currPos--;
      if(currPos<0) currPos = text.length-1;
    }
  }
}
