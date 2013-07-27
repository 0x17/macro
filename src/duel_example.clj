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
         :bullets '()
         :last-shot-ticks 0
         :death-ticks 0}))

(def players (map #(gen-player %) [[1 50 40] [2 520 400]]))
(defbatch p1 (first players)
          p2 (second players))

(defn move-player [player dx dy]
  (fassoc-in-place player :pos #(vec-add % [dx dy])))

(defn shoot [player]
  (when (> (- (ticks) (:last-shot-ticks @player)) reload-time)
    (fassoc-in-place player :bullets
      #(cons (vec-add (:pos @player) (vec-scal-mul (player-dim) 0.25)) %))
    (play-sound "shot")
    (assoc-in-place player :last-shot-ticks (ticks))))

(def key-actions
  {Input$Keys/ESCAPE        quit
   Input$Keys/LEFT          #(move-player p1 (- move-speed) 0)
   Input$Keys/RIGHT         #(move-player p1 move-speed 0)
   Input$Keys/UP            #(move-player p1 0 move-speed)
   Input$Keys/DOWN          #(move-player p1 0 (- move-speed))
   Input$Keys/CONTROL_LEFT  #(shoot p1)
   Input$Keys/A             #(move-player p2 (- move-speed) 0)
   Input$Keys/D             #(move-player p2 move-speed 0)
   Input$Keys/W             #(move-player p2 0 move-speed)
   Input$Keys/S             #(move-player p2 0 (- move-speed))
   Input$Keys/CONTROL_RIGHT #(shoot p2)
   Input$Keys/ALT_RIGHT     #(shoot p2)})

(deflazy bullet-move-vec
  (fn [num] [(* (if (= num 1) 1.0 (- 1.0)) bullet-move-speed) 0.0]))

(defn update-player [player]
  (fassoc-in-place player :bullets
    (fn [oldbullets] (map #(vec-add % (bullet-move-vec (:num @player))) oldbullets))))

(defn-destr draw-player [num health pos bullets]
  (let [[x y] pos]
    (draw-image (str "player" num "hurt" health) x y)
    (foreach #(draw-image "bullet" (first %) (second %)) bullets)))

(defn draw-overlay []
  (draw-text (str "P1 score " (:kills @p1)) 100 100)
  (draw-text (str "P2 score " (:kills @p2)) 500 100))

(defn draw-cb [delta]
  (letfn [(process-input []
            (foreach #((key-actions %)) (filter key-pressed (keys key-actions)))
            (let [mstate (mouse-state)]
              (if (mstate :lmb)
                (let [offset-pos (map - (mstate :pos) (map * (player-dim) (repeat-vec 2 0.5)))]
                  (assoc-in-place p1 :pos offset-pos)))))
          (render-scene []
            (draw-image "background" 0 0)
            (foreach #(do (update-player %) (draw-player @%)) players)
            (draw-overlay))]
    (process-input)
    (render-scene)))

(defn -main [args]
  (init "DuelExample" [640 480] init-cb draw-cb))

(-main (into-array []))