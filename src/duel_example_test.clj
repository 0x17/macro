(ns duel-example.test
  (:use duel-example clojure.test))

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
        (move-player {:num 1 :pos [10 10] :health 0} 13 (- 2)))))

(deftest test-hit-player
  (is (= {:health 1} (hit-player {:health 0})))
  (is (= {:health 3} (hit-player {:health 3}))))

(deftest test-update-state-with-action
  (let [state {:players [{:num 1} {:num 2}]}
        action [(fn [player] (assoc player :num 3)) 0]
        expected-state {:players '({:num 3} {:num 2})}]
    (is (= expected-state (update-state-with-action state action)))))

(run-tests)