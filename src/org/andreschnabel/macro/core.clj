(ns org.andreschnabel.macro.core
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [com.badlogic.gdx ApplicationListener Gdx Input Input$Buttons]
           [com.badlogic.gdx.graphics GL10]
           [com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator]
           [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [java.io File])
  (:require [org.andreschnabel.macro.utils :as utils]))

(def ^:private scr-dims (atom (vector 0 0)))

(utils/deflazy sb #(SpriteBatch.))
(utils/deflazy atlas #(utils/atlas-for-dir (File. "src/data/")))

(def ^:private sounds (atom {}))
(def ^:private songs (atom {}))
(def ^:private song-playing (atom nil))

(def ^:private font (atom nil))

(defn scr-w [] (first @scr-dims))
(defn scr-h [] (second @scr-dims))

(defn init [caption scr-size init-cb draw-cb]
  (letfn [(dispose-all []
            (utils/foreach utils/safe-dispose (vals @sounds))
            (utils/foreach utils/safe-dispose (vals @songs))
            (utils/safe-dispose @font)
            (utils/safe-dispose (atlas))
            (utils/safe-dispose (sb)))
          (app-listener [init-cb draw-cb]
            (proxy [ApplicationListener] []
              (create []
                (.glClearColor Gdx/gl 0.0 0.0 0.0 1.0)
                (init-cb))
              (resize [w h]
                (reset! scr-dims (vector w h)))
              (render []
                (.glClear Gdx/gl GL10/GL_COLOR_BUFFER_BIT)
                (.begin (sb))
                (draw-cb (.getDeltaTime Gdx/graphics))
                (.end (sb)))
              (pause [])
              (resume [])
              (dispose []
                (dispose-all))))]
    (reset! scr-dims scr-size)
    (LwjglApplication. (app-listener init-cb draw-cb) caption (scr-w) (scr-h) false)))

(defn draw-image
  ([name x y] (draw-image name x y 0.0))
  ([name x y rz] (draw-image name x y rz 1.0 1.0))
  ([name x y rz sx sy]
  (let [region (.findRegion (atlas) name)]
    (if (not (nil? region))
      (let [w (.getRegionWidth region)
            h (.getRegionHeight region)]
        (.draw (sb) region x y (+ x (/ w 2.0)) (+ y (/ h 2.0)) w h sx sy rz))))))

(defn get-image-dim [name]
  (let [region (.findRegion (atlas) name)]
    (if (not (nil? region))
      (vector (.getRegionWidth region) (.getRegionHeight region))
      (vector 0 0))))

(defn set-font [name size]
  (if (not (nil? @font))
    (.dispose @font))
  (let [generator (FreeTypeFontGenerator. (utils/load-res name "ttf"))]
      (reset! font (.generateFont generator size))))

(defn draw-text [text x y]
  (.draw @font (sb) text x y))

(defn play-sound [name]
  (.play (utils/put-or-keep sounds name #(.newSound Gdx/audio (utils/load-res name "wav")))))

(defn play-song
  ([name] (play-song name false))
  ([name loop] (let [song (utils/put-or-keep songs name #(.newMusic Gdx/audio (utils/load-res name "mp3")))]
                (.setLooping song loop)
                (if (not (nil? @song-playing))
                  (.stop @song-playing))
                (.play song)
                (reset! song-playing song))))

(defn quit [] (.exit Gdx/app))

(defn key-pressed [key] (.isKeyPressed Gdx/input key))

(defn mouse-state [] {:pos (vector (.getX Gdx/input) (- (scr-h) (.getY Gdx/input)))
                      :lmb (.isButtonPressed Gdx/input Input$Buttons/LEFT)
                      :mmb (.isButtonPressed Gdx/input Input$Buttons/MIDDLE)
                      :rmb (.isButtonPressed Gdx/input Input$Buttons/RIGHT)})