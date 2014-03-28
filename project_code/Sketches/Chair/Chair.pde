import com.makar.*;

BoxedShape leg1, leg2, leg3, leg4;
BoxedShape seat, back;
color cLegs = color(121,107,102);
color cSurfaces = color(208,187,130);
color black = color(0,0,0);

int boxSize = 14;
int frontLegHeight = 8;
int backLegHeight = 16;
int legDistance = 140;
int backHeight = 4;

boolean stable = false;
float currScale;
float easing = 0.05;

void setup() {
  size(600,600,P3D);
  Makar.init(this);
  noStroke();
  leg1 = new BoxedShape(1, 1, frontLegHeight, boxSize, cLegs);
  leg2 = new BoxedShape(1, 1, frontLegHeight, boxSize, cLegs);
  leg3 = new BoxedShape(1, 1, backLegHeight, boxSize, cLegs);
  leg4 = new BoxedShape(1, 1, backLegHeight, boxSize, cLegs);
  seat = new BoxedShape(legDistance/boxSize, legDistance/boxSize+1, 1, boxSize, cSurfaces);
  back = new BoxedShape(1, legDistance/boxSize-1, backHeight, boxSize, cSurfaces);
}

void draw() {
  
  Makar.beginDraw();
//  background(255);
  lights();
  if (!stable) {
    float targetScale = 100;
    float dScale = targetScale - currScale;
    if (abs(dScale) > 1) {
      currScale += dScale * easing;
    }
  } else {
    float targetScale = 1;
    float dScale = targetScale - currScale;
    if (abs(dScale) > 1) {
      currScale += dScale * easing;
    }
  }
  
  translate(50,0,-120);
  rotateX(0.4);
  rotateZ(-0.65);
  
  fill(100);
  
  leg1.draw();
  
  pushMatrix();
  translate(0,legDistance,0);
  leg2.draw();
  popMatrix();
  
  pushMatrix();
  translate(-legDistance,0,0);
  leg3.draw();
  popMatrix();
  
  pushMatrix();
  translate(-legDistance,legDistance,0);
  leg4.draw();
  popMatrix();
  
 
  
  pushMatrix();
  translate(-legDistance+boxSize,0,frontLegHeight*boxSize);
  seat.draw();
  popMatrix();
  
  pushMatrix();
  translate(-legDistance,boxSize,(backLegHeight-backHeight)*boxSize);
  back.draw();
  popMatrix();
  
  Makar.endDraw();
}

class BoxedShape {
  int xnum, ynum, znum, size;
  color fillColor;
  BoxedShape(int _xnum, int _ynum, int _znum, int _size, color _fillColor) {
    xnum = _xnum;
    ynum = _ynum;
    znum = _znum;
    size = _size;
    fillColor = _fillColor;
  }
  void draw() {
    float scale = 0.1*currScale/10.;
    float offset = frameCount/100.;
    for(int i = 0; i<xnum; i++) {
      for(int j = 0; j<ynum; j++) {
        for(int k = 0; k<znum; k++) {
          float n = noise(i*scale+offset, j*scale+offset, k*scale+offset)*2*size;
          float nInv = 2*(size - n)*currScale/10.;
          fill(fillColor);
          pushMatrix();
          translate(i*size+nInv,j*size+nInv,k*size+nInv);
          box(n);
          popMatrix();
        }
      }
    }
  }
}

void mousePressed() {
   stable = !stable;
}


