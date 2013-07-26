(ns duel-example
  (:import [com.badlogic.gdx Input Input$Keys])
  (:use (org.andreschnabel.macro core utils)))

(defn init-cb []
  (set-font "font" 30)
  (play-song "loop" true))

(def move-speed (float 5.0))

(defn gen-player [& num x y]
  (atom {:num num
         :pos [x y]
         :health 0
         :kills 0
         :bullets []
         :last-shot-ticks (long 0)
         :death-ticks (long 0)}))

(def players (map #(gen-player %) [[1 50 40] [2 520 400]]))

(def move-player [player dx dy]
  (let [oldpos (@player :pos)]
    (reset! @player (assoc @player :pos [(+ (first oldpos) dx) (+ (second oldpos))]))))

(def key-actions
  {Input$Keys/ESCAPE quit
   Input$Keys/LEFT #(move-player (first players) (- move-speed) 0)
   Input$Keys/RIGHT #(move-player (first players) move-speed 0)})

(defn draw-player [player]
  (let [num (@player :num)
        health (@player :health)
        x (first (@player :pos))
        y (second (@player :pos))]
    (draw-image (str "player" num "hurt" health) x y)))

(defn draw-cb [delta]
  (letfn [(process-input []
            (doall (map #((key-actions %)) (filter key-pressed (keys key-actions))))
            (let [mstate (mouse-state)]
              (if (mstate :lmb)
                (quit))))
          (render-scene []
            (draw-image "background" 0 0)
            (doseq [player players] (draw-player player))
            (draw-text "Test" (float 100) (float 100)))]
    (process-input)
    (render-scene)))

(defn -main [args]
  (init "DuelExample" [640 480] init-cb draw-cb))

(-main (into-array []))