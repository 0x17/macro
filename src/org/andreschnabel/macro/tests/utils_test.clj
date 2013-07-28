(ns org.andreschnabel.macro.tests.utils-test
  (:use clojure.test org.andreschnabel.macro.utils))

(deftest test-assoc-if
  (is (= {:x 2} (assoc-if true {:x 1} :x 2)))
  (is (= {:x 1} (assoc-if false {:x 1} :x 2)))
  (is (= {:x 1 :y 3} (assoc-if true {:x 1} :y 3)))
  (is (= {:x 1} (assoc-if false {:x 1} :y 3))))

(run-tests)