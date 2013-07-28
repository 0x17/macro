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

;;; PLAYER -------------------------------------------------------------------------------------------------------------
(defn gen-player [[num x y]]
  {:num num
   :pos [x y]
   :health 0
   :kills 0
   :bullets '()
   :death-ticks 0})

(defn player-in-bounds [player-num pos]
  (and (< 0 (ycrd pos) (- (height scr-dim) (height player-dim)))
    (or (and (= player-num 1) (< 0 (xcrd pos) (- (/ (width scr-dim) 2) (width player-dim))))
        (and (= player-num 2) (< (/ (width scr-dim) 2) (xcrd pos) (- (width scr-dim) (width player-dim)))))))

(defn player-dead? [player] (<= 3 (:health player)))

(defn move-player [dx dy player]
  (fassoc player :pos
    (fn [oldpos]
      (let [newpos (vec-add oldpos [dx dy])]
        (if (and (player-in-bounds (:num player) newpos) (not (player-dead? player)))
          newpos
          oldpos)))))

(defn shoot [player]
  (play-sound "shot")
  (fassoc player :bullets #(cons (vec-add (:pos player) (vec-scal-mul player-dim 0.25)) %)))

(defn try-shoot [player]
  (if (player-dead? player)
    player
    (limit-rate (keyword (str "shooting" (:num player))) reload-time (shoot player))))

(deflazy bullet-move-vec (fn [num] [(* (if (= num 1) 1.0 (- 1.0)) bullet-move-speed) 0.0]))

;;; INPUT --------------------------------------------------------------------------------------------------------------
(defmacro mv-pl [dx dy] `(partial move-player ~dx ~dy))

(def key-actions
  {Input$Keys/LEFT          [(mv-pl (- move-speed) 0) 0]
   Input$Keys/RIGHT         [(mv-pl move-speed 0) 0]
   Input$Keys/UP            [(mv-pl 0 move-speed) 0]
   Input$Keys/DOWN          [(mv-pl 0 (- move-speed)) 0]
   Input$Keys/CONTROL_LEFT  [shoot 0]
   Input$Keys/SPACE         [shoot 0]
   Input$Keys/A             [(mv-pl (- move-speed) 0) 1]
   Input$Keys/D             [(mv-pl move-speed 0) 1]
   Input$Keys/W             [(mv-pl 0 move-speed) 1]
   Input$Keys/S             [(mv-pl 0 (- move-speed)) 1]
   Input$Keys/CONTROL_RIGHT [shoot 1]
   Input$Keys/ENTER         [shoot 1]
   Input$Keys/ALT_RIGHT     [shoot 1]})

(defn update-state-with-action [passed-state action-pair]
  (assoc-in passed-state [:players (second action-pair)]
    (let [player (nth (:players passed-state) (second action-pair))]
      ((first action-pair) player))))

(defn process-input [state]
  (when (key-pressed Input$Keys/ESCAPE) (quit))
  (reduce #(update-state-with-action %1 (key-actions %2)) state (filter key-pressed (keys key-actions))))

;;; GAME LOGIC ---------------------------------------------------------------------------------------------------------
(defn update-bullets [player]
  (fassoc player :bullets
    (fn [oldbullets]
      (->> oldbullets
        (map (partial vec-addf (bullet-move-vec (:num player))))
        (filter #(< (- (width bullet-dim)) (xcrd %) (width scr-dim)))))))

(defn just-died? [player] (and (player-dead? player) (= (:death-ticks player) 0)))

(defn player-scores [player] (fassoc player :kills ++))

(defn try-die [player]
  (assoc-if (just-died? player) player :death-ticks (ticks)))

(defn try-respawn [player]
  (merge-if-elsel (and (player-dead? player) (> (- (ticks) (:death-ticks player)) death-time))
    player {:death-ticks 0 :health 0}))

(defn update-player [player]
  [(just-died? player) (-> player try-die try-respawn update-bullets)])

(defn update-score [player scored?]
  (if scored? (player-scores player) player))

(defn update-players [players]
  (let [p1-pair (update-player (players 0))
        p2-pair (update-player (players 1))]
    [(update-score (p1-pair 1) (p2-pair 0))
     (update-score (p2-pair 1) (p1-pair 0))]))

;;; COLLISIONS ---------------------------------------------------------------------------------------------------------
(defn rect-from-pos-dim [pos dim]
  (Rectangle. (xcrd pos) (ycrd pos) (width dim) (height dim)))

(defn player-bullet-coll [player bpos]
  (let [bullet-rect (rect-from-pos-dim bpos bullet-dim)
        player-rect (rect-from-pos-dim (:pos player) player-dim)]
    (Intersector/overlapRectangles bullet-rect player-rect)))

(defn bullet-bullet-coll [b1pos b2pos bdim]
  (let [b1-rect (rect-from-pos-dim b1pos bdim)
        b2-rect (rect-from-pos-dim b2pos bdim)]
    (if (Intersector/overlapRectangles b1-rect b2-rect) [b1pos b2pos] nil)))

(defn check-player-bullet-collisions [players pdim bdim]
  (letfn [(remove-collided [x2 oldbullets] (remove (partial player-bullet-coll (nth players x2) pdim bdim) oldbullets))
          (check-collision [[x1 x2]] (fassoc (nth players x1) :bullets (partial remove-collided x2)))]
    (mapv check-collision  [[0 1] [1 0]])))

(defn check-bullet-bullet-collisions [players]
  (let [colliding-pairs (remove nil? (map bullet-bullet-coll (:bullets (players 0)) (:bullets (players 1))))
        colliding-p1 (map first colliding-pairs)
        colliding-p2 (map second colliding-pairs)]
    [(fassoc (players 0) :bullets #(set-diff % colliding-p1))
     (fassoc (players 1) :bullets #(set-diff % colliding-p2))]))

;;; DRAWING ------------------------------------------------------------------------------------------------------------
(defn-destr draw-player [num health pos bullets]
  (let [[x y] pos]
    (draw-image (str "player" num "hurt" health) x y)
    (foreach #(draw-image "bullet" (xcrd %) (ycrd %)) bullets)))

(defn draw-overlay [state]
  (draw-text (str "P1 score " (-> state :players first :kills)) 100 100)
  (draw-text (str "P2 score " (-> state :players second :kills)) 500 100))

(defn hit-player [player] (fassoc player :health (if (player-dead? player) identity ++)))

(defn render-scene [state]
  (draw-image "background" 0 0)
  (foreach draw-player (:players state))
  (draw-overlay state)
  (->> state
    #(fassoc % :players update-players)
    #(fassoc % :players (comp check-bullet-bullet-collisions check-player-bullet-collisions))))

(defn draw-cb [delta state] (-> state process-input render-scene))

;;; ENTRYPOINT ---------------------------------------------------------------------------------------------------------
(defn init-cb []
  (set-font "font" 30)
  (play-song "loop" true)
  {:players (mapv gen-player initial-positions)})

(defn -main [args] (init "DuelExample" [640 480] init-cb draw-cb))