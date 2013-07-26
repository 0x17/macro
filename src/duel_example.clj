(ns duel-example
  (:import [com.badlogic.gdx Input Input$Keys])
  (:use (org.andreschnabel.macro core utils)))

(defn init-cb []
  (set-font "font" 30)
  (comment (play-song "loop" true)))

(def move-speed (float 5.0))

(defn gen-player [[num x y]]
  (atom {:num num
         :pos [x y]
         :health 0
         :kills 0
         :bullets []
         :last-shot-ticks (long 0)
         :death-ticks (long 0)}))

(def players (map #(gen-player %) [[1 50 40] [2 520 400]]))

(defn move-player [player dx dy]
  (let [oldpos (@player :pos)]
    (reset! player (assoc @player :pos [(+ (first oldpos) dx) (+ (second oldpos) dy)]))))

(def key-actions
  {Input$Keys/ESCAPE  quit
   Input$Keys/LEFT    #(move-player (first players) (- move-speed) 0)
   Input$Keys/RIGHT   #(move-player (first players) move-speed 0)
   Input$Keys/UP      #(move-player (first players) 0 move-speed)
   Input$Keys/DOWN    #(move-player (first players) 0 (- move-speed))
   Input$Keys/A       #(move-player (second players) (- move-speed) 0)
   Input$Keys/D       #(move-player (second players) move-speed 0)
   Input$Keys/W       #(move-player (second players) 0 move-speed)
   Input$Keys/S       #(move-player (second players) 0 (- move-speed))})

(defmacro defn-destr
  "Automatic destructuring into parameters.
  For functions with >1 args."
  [name args body]
  `(def ~name
     (fn (~args ~body)
       ([param-to-val#] (let [{:keys ~args} param-to-val#] ~body)))))

(defn-destr draw-player [num health pos]
  (let [[x y] pos]
    (draw-image (str "player" num "hurt" health) x y)))

(defn draw-overlay []
  (draw-text "Some text" (float 15) (float 470)))

(defn draw-cb [delta]
  (letfn [(process-input []
            (doall (map #((key-actions %)) (filter key-pressed (keys key-actions))))
            (let [mstate (mouse-state)]
              (if (mstate :lmb)
                (quit))))
          (render-scene []
            (draw-image "background" 0 0)
            (doseq [player players] (draw-player @player))
            (draw-overlay))]
    (process-input)
    (render-scene)))

(defn -main [args]
  (init "DuelExample" [640 480] init-cb draw-cb))

(-main (into-array []))