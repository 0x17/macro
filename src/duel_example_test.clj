(ns duel-example.test
  (:use duel-example
        clojure.test
        (org.andreschnabel.macro core utils)))

(deftest test-player-in-bounds
  (is (player-in-bounds 1 [10 10]))
  (is (not (player-in-bounds 1 [400 10])))
  (is (player-in-bounds 2 [400 10]))
  (is (not (player-in-bounds 2 [10 10]))))

(deftest test-player-dead?
  (is (player-dead? {:health 3}))
  (is (player-dead? {:health 4}))
  (is (not (player-dead? {:health 2}))))

(deftest test-move-player
  (is (= {:num 1 :pos [23 8] :health 0}
        (move-player 13 (- 2) {:num 1 :pos [10 10] :health 0}))))

(ns org.andreschnabel.macro.core)
(defn play-sound [str])
(ns duel-example.test (:use duel-example clojure.test))
(deftest test-shoot
  (is (= {:num 1 :pos [10 10] :health 0 :bullets '([22.5 22.5])}
        (shoot {:num 1 :pos [10 10] :health 0 :bullets '()}))))

(deftest test-bullet-move-vec
  (is (= (neg? (xcrd (bullet-move-vec 2)))))
  (is (= (pos? (xcrd (bullet-move-vec 1)))))
  (is (= 0.0 (ycrd (bullet-move-vec 1)) (ycrd (bullet-move-vec 2)))))

(deftest test-update-bullets
  (is (= {:num 1 :bullets '([20.0 10.0])} (update-bullets {:num 1 :bullets '([10.0 10.0])}))))

;(deftest test-update-player)

(deftest test-hit-player
  (is (= {:health 1} (hit-player {:health 0})))
  (is (= {:health 3} (hit-player {:health 3}))))

(deftest test-update-state-with-action
  (let [state {:players [{:num 1} {:num 2}]}
        action [(fn [player] (assoc player :num 3)) 0]
        expected-state {:players '({:num 3} {:num 2})}]
    (is (= expected-state (update-state-with-action state action)))))

(run-tests)