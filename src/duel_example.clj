(ns duel-example
  (:import [com.badlogic.gdx Input Input$Keys])
  (:use (org.andreschnabel.macro core utils)))

(defn init-cb []
  (set-font "font" 30)
  (comment (play-song "loop" true)))

(defbatch move-speed 5.0
          bullet-move-speed 10.0
          reload-time 120
          death-time 2000)

(deflazy player-dim #(get-image-dim "player1hurt0"))

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
  (fassoc-in-place player :pos
    (fn [oldpos] [(+ (first oldpos) dx) (+ (second oldpos) dy)])))

(defn shoot [player])

(def key-actions
  {Input$Keys/ESCAPE        quit
   Input$Keys/LEFT          #(move-player (first players) (- move-speed) 0)
   Input$Keys/RIGHT         #(move-player (first players) move-speed 0)
   Input$Keys/UP            #(move-player (first players) 0 move-speed)
   Input$Keys/DOWN          #(move-player (first players) 0 (- move-speed))
   Input$Keys/CONTROL_LEFT  #(shoot (first players))
   Input$Keys/A             #(move-player (second players) (- move-speed) 0)
   Input$Keys/D             #(move-player (second players) move-speed 0)
   Input$Keys/W             #(move-player (second players) 0 move-speed)
   Input$Keys/S             #(move-player (second players) 0 (- move-speed))
   Input$Keys/CONTROL_RIGHT #(shoot (second players))
   Input$Keys/ALT_RIGHT     #(shoot (second players))})

(defn-destr draw-player [num health pos]
  (let [[x y] pos]
    (draw-image (str "player" num "hurt" health) x y)))

(defn draw-overlay []
  (draw-text (str "P1 score " (:kills @(first players))) 100 100)
  (draw-text (str "P2 score " (:kills @(second players))) 500 100))

(defn draw-cb [delta]
  (letfn [(process-input []
            (foreach #((key-actions %)) (filter key-pressed (keys key-actions)))
            (let [mstate (mouse-state)]
              (if (mstate :lmb)
                (let [offset-pos (map - (mstate :pos) (map * (player-dim) (repeat-vec 2 0.5)))]
                  (assoc-in-place (first players) :pos offset-pos)))))
          (render-scene []
            (draw-image "background" 0 0)
            (foreach #(draw-player @%) players)
            (draw-overlay))]
    (process-input)
    (render-scene)))

(defn -main [args]
  (init "DuelExample" [640 480] init-cb draw-cb))

(-main (into-array []))