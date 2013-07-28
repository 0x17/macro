(ns duel-example
  (:import [com.badlogic.gdx Input Input$Keys]
           [com.badlogic.gdx.math Intersector Rectangle])
  (:use (org.andreschnabel.macro core utils)))

(defbatch move-speed 5.0
          bullet-move-speed 10.0
          reload-time 120
          death-time 2000
          player-dim [50 50]
          bullet-dim [15 15]
          scr-dim [640 480]
          initial-positions [[1 50 40][2 520 400]])

(defn gen-player [[num x y]]
  {:num num
   :pos [x y]
   :health 0
   :kills 0
   :bullets '()
   :death-ticks 0})

(defn init-cb []
  (set-font "font" 30)
  (play-song "loop" true)
  {:players (mapv gen-player initial-positions)})

(defn player-in-bounds [player-num pos]
  (and (< 0 (ycrd pos) (- (height scr-dim) (height player-dim)))
    (or (and (= player-num 1) (< 0 (xcrd pos) (- (/ (width scr-dim) 2) (width player-dim))))
        (and (= player-num 2) (< (/ (width scr-dim) 2) (xcrd pos) (- (width scr-dim) (width player-dim)))))))

(defn player-dead? [player] (<= 3 (:health player)))

(defn move-player [player dx dy]
  (fassoc player :pos
    (fn [oldpos]
      (let [newpos (vec-add oldpos [dx dy])]
        (if (and (player-in-bounds (:num player) newpos) (not (player-dead? player)))
          newpos
          oldpos)))))

(defn shoot [player]
  (if (player-dead? player)
    player
    (limit-rate (keyword (str "shooting" (:num player))) reload-time
      (play-sound "shot")
      (fassoc player :bullets #(cons (vec-add (:pos player) (vec-scal-mul player-dim 0.25)) %)))))

(def key-actions
  {Input$Keys/LEFT          [#(move-player % (- move-speed) 0) 0]
   Input$Keys/RIGHT         [#(move-player % move-speed 0) 0]
   Input$Keys/UP            [#(move-player % 0 move-speed) 0]
   Input$Keys/DOWN          [#(move-player % 0 (- move-speed)) 0]
   Input$Keys/CONTROL_LEFT  [#(shoot %) 0]
   Input$Keys/SPACE         [#(shoot %) 0]
   Input$Keys/A             [#(move-player % (- move-speed) 0) 1]
   Input$Keys/D             [#(move-player % move-speed 0) 1]
   Input$Keys/W             [#(move-player % 0 move-speed) 1]
   Input$Keys/S             [#(move-player % 0 (- move-speed)) 1]
   Input$Keys/CONTROL_RIGHT [#(shoot %) 1]
   Input$Keys/ENTER         [#(shoot %) 1]
   Input$Keys/ALT_RIGHT     [#(shoot %) 1]})

(deflazy bullet-move-vec (fn [num] [(* (if (= num 1) 1.0 (- 1.0)) bullet-move-speed) 0.0]))

(defn update-player [player other-player]
  ;(when (player-dead? player)
  ;  (when (= (:death-ticks player) 0)
  ;    (assoc player :death-ticks (ticks))
  ;    (fassoc other-player :kills ++))
  ;  (when (> (- (ticks) (:death-ticks player)) death-time)
  ;    (merge player {:death-ticks 0 :health 0})))
  (fassoc player :bullets
    (fn [oldbullets]
      (->> oldbullets
           (map (fn [bpos] (vec-add bpos (bullet-move-vec (:num player)))))
           (filter (fn [bpos] (< (- (width bullet-dim)) (xcrd bpos) (width scr-dim))))))))

(defn-destr draw-player [num health pos bullets]
  (let [[x y] pos]
    (draw-image (str "player" num "hurt" health) x y)
    (foreach #(draw-image "bullet" (xcrd %) (ycrd %)) bullets)))

(defn draw-overlay [state]
  (draw-text (str "P1 score " (-> state :players first :kills)) 100 100)
  (draw-text (str "P2 score " (-> state :players second :kills)) 500 100))

(defn hit-player [player] (fassoc player :health (if (player-dead? player) identity ++)))

(defn rect-from-pos-dim [pos dim]
  (Rectangle. (xcrd pos) (ycrd pos) (width dim) (height dim)))

(defn player-bullet-coll [player bpos]
  (let [bullet-rect (rect-from-pos-dim bpos bullet-dim)
        player-rect (rect-from-pos-dim (:pos @player) player-dim)]
    (Intersector/overlapRectangles bullet-rect player-rect)))

(defn bullet-bullet-coll [b1pos b2pos bdim]
  (let [b1-rect (rect-from-pos-dim b1pos bdim)
        b2-rect (rect-from-pos-dim b2pos bdim)]
    (if (Intersector/overlapRectangles b1-rect b2-rect) [b1pos b2pos] nil)))

(defn check-player-bullet-collisions [players pdim bdim]
  (letfn [(remove-collided [x2 oldbullets] (remove (partial player-bullet-coll (nth players x2) pdim bdim) oldbullets))
          (check-collision [[x1 x2]] (fassoc (nth players x1) :bullets (partial remove-collided x2)))]
  (map check-collision '([0 1] [1 0]))))

(defn check-bullet-bullet-collisions [players]
  (let [p1 (first players)
        p2 (second players)
        colliding-pairs (remove nil? (map bullet-bullet-coll (:bullets p1) (:bullets p2)))
        colliding-p1 (map first colliding-pairs)
        colliding-p2 (map second colliding-pairs)]
    '((fassoc p1 :bullets #(set-diff % colliding-p1))
      (fassoc p2 :bullets #(set-diff % colliding-p2)))))

(defn update-state-with-action [passed-state action-pair]
  (assoc-in passed-state [:players (second action-pair)]
    (let [player (nth (:players passed-state) (second action-pair))]
      ((first action-pair) player))))

(defn process-input [state]
  (when (key-pressed Input$Keys/ESCAPE) (quit))
  (reduce #(update-state-with-action %1 (key-actions %2)) state (filter key-pressed (keys key-actions))))

(defn update-players [players]
  '((update-player (players 0) (players 1))
    (update-player (players 1) (players 0))))

(defn render-scene [state]
  (draw-image "background" 0 0)
  (foreach draw-player (:players state))
  (draw-overlay state)
  (->> state
    #(fassoc % :players update-players)
    #(fassoc % :players (comp check-bullet-bullet-collisions check-player-bullet-collisions))))


(defn draw-cb [delta state] (render-scene (process-input state)))

(defn -main [args] (init "DuelExample" [640 480] init-cb draw-cb))