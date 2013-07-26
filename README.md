# Macro
Very minimalistic Clojure game framework built on top of libgdx. There's also a Scala version of this called "Micro".

All assets must be placed in "data/" sub folder by convention.
* Images should be ".png"-files.
* Sounds should be ".wav"-files.
* Music should be ".mp3"-files.
* Fonts should be ".ttf"-files.

## Dependencies
* gdx
* gdx-freetype (extension)

## Globals
scrW - screen width

scrH - screen height

## Methods

### Control flow
(init caption scrSize initCallback drawCallback) - start application.
* caption - window title
* scrSize - screen size to use given as pair (w,h).
* initCallback - called once after everything was setup.
* drawCallback - callback is called each frame.

quit - close application

### Input
(key-pressed keyCode) => bool - true, iff. key w/ given code is pressed.

(mouse-state) => MouseState(pos,lmb,mmb,rmb) - state of mouse (lmb=left mouse button, ...)

### Visuals
(draw-image name x y rz sx sy) - draw image transformed at coordinates. Origin is bottom left.
* name - name of .png file (w/out extension) in "data/" path.
* rz - rotation around z-axis in degrees CCW.
* sx - scaling factor along x-axis.
* sy - scaling factor along y-axis.

(get-image-dim name) - dimensions of image (w,h) in pixels

#### Font
(set-font name size) - set font for subsequent drawText calls.
* name - name of .ttf file (w/out extension) in "data/" path.
* size - size of font for subsequent drawText calls.

(draw-text text x y) - draw text w/ currently selected font. Origin bottom left.
* text - string of text to be drawn.

### Audio
(play-sound name)

(play-song name looping)

(stop-song name)
