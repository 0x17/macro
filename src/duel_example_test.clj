(ns duel-example.test
  (:use duel-example clojure.test))

(deftest test-update-state-with-action
  (let [state {:players '({:num 1} {:num 2})}
        action [(fn [player] (assoc player :num 3)) 0]
        expected-state {:players '({:num 3} {:num 2})}]
    (is (= expected-state (update-state-with-action state action)))))

(run-tests)