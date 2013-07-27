(ns duel-example
  (:import [com.badlogic.gdx Input Input$Keys]
           [com.badlogic.gdx.math Intersector Rectangle])
  (:use (org.andreschnabel.macro core utils)))

(defn init-cb []
  (set-font "font" 30)
  (play-song "loop" true))

(defbatch move-speed 5.0
          bullet-move-speed 10.0
          reload-time 120
          death-time 2000)

(deflazy player-dim #(get-image-dim "player1hurt0"))
(deflazy bullet-dim #(get-image-dim "bullet"))

(defn gen-player [[num x y]]
  (atom {:num num
         :pos [x y]
         :health 0
         :kills 0
         :bullets '()
         :death-ticks 0}))

(def players (map #(gen-player %) [[1 50 40] [2 520 400]]))
(defbatch p1 (first players)
          p2 (second players))

(defn player-in-bounds [player-num pos]
  (and (< 0 (ycrd pos) (- (scr-h) (height (player-dim))))
    (or (and (= player-num 1) (< 0 (xcrd pos) (- (/ (scr-w) 2) (width (player-dim)))))
        (and (= player-num 2) (< (/ (scr-w) 2) (xcrd pos) (- (scr-w) (width (player-dim))))))))

(defn player-dead? [player] (<= 3 (:health @player)))

(defn move-player [player dx dy]
  (when (not (player-dead? player))
    (fassoc-in-place player :pos
      (fn [oldpos]
        (let [newpos (vec-add oldpos [dx dy])]
          (if (player-in-bounds (:num @player) newpos) newpos oldpos))))))

(defn shoot [player]
  (when (not (player-dead? player))
    (limit-rate :shooting reload-time
      (fassoc-in-place player :bullets #(cons (vec-add (:pos @player) (vec-scal-mul (player-dim) 0.25)) %))
      (play-sound "shot"))))

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

(deflazy bullet-move-vec (fn [num] [(* (if (= num 1) 1.0 (- 1.0)) bullet-move-speed) 0.0]))

(defn update-player [player]
  (when (player-dead? player)
    (when (= (:death-ticks @player) 0)
      (assoc-in-place player :death-ticks (ticks))
      (let [other-player (if (= 1 (:num @player)) p2 p1)]
        (fassoc-in-place other-player :kills ++)))
    (when (> (- (ticks) (:death-ticks @player)) death-time)
      (assoc-in-place player :death-ticks 0)
      (assoc-in-place player :health 0)))
  (fassoc-in-place player :bullets
    (fn [oldbullets]
      (->> oldbullets
           (map (fn [bpos] (vec-add bpos (bullet-move-vec (:num @player)))))
           (filter (fn [bpos] (< (- (width (bullet-dim))) (xcrd bpos) (scr-w))))))))

(defn-destr draw-player [num health pos bullets]
  (let [[x y] pos]
    (draw-image (str "player" num "hurt" health) x y)
    (foreach #(draw-image "bullet" (xcrd %) (ycrd %)) bullets)))

(defn draw-overlay []
  (draw-text (str "P1 score " (:kills @p1)) 100 100)
  (draw-text (str "P2 score " (:kills @p2)) 500 100))

(defn rect-from-pos-dim [pos dim] (Rectangle. (xcrd pos) (ycrd pos) (width (dim)) (height (dim))))

(defn hit-player [player] (when (not (player-dead? player)) (fassoc-in-place player :health ++)))

(defn bullet-player-coll [bpos player]
  (let [bullet-rect (rect-from-pos-dim bpos bullet-dim)
        player-rect (rect-from-pos-dim (:pos @player) player-dim)
        result (Intersector/overlapRectangles bullet-rect player-rect)]
    (when result (hit-player player))
    result))

(defn check-collisions []
  (foreach #(fassoc-in-place (nth players (first %)) :bullets
              (fn [oldbullets] (filter (fn [b] (not (bullet-player-coll b (nth players (second %))))) oldbullets)))
    '([0 1] [1 0])))

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
            (check-collisions)
            (draw-overlay))]
    (process-input)
    (render-scene)))

(defn -main [args] (init "DuelExample" [640 480] init-cb draw-cb))

(-main (into-array []))