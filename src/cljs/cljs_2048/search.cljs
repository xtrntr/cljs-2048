(ns cljs-2048.search
  (:require [clojure.core.reducers :as reducers]
            [cljs.core.async :refer [chan close!]]
            [cljs-2048.utils :as utils]
            [cljs-2048.logic :as logic]
            [om.core :as om :include-macros true])
  (:require-macros
   [cljs.core.async.macros :as m :refer [go]]))

(def geom-seq
  (for [idx (range 16)]
    (/ 1 (.pow js/Math 2 idx))))

(defn sumlist [list]
  (reduce + list))

(defn unmemoized-monotonicity [grid]
  "state is vector of 16 values
   zip multiply the vector with a geometric sequence"
  (let [configs (list grid
                      (reverse grid)
                      (logic/rotate-grid grid)
                      (reverse (logic/rotate-grid grid)))]
    (apply max 
           (map 
            (fn [grid] (sumlist (map * geom-seq grid)))
            configs))))

(def monotonicity (memoize unmemoized-monotonicity))

(defn weight-zero-tiles [grid]
  "bonus for more empty tiles"
  (* (/ 1 16) (count (inc (logic/find-zero-indexes grid)))))

(defn score-grid [grid]
  "2 heuristics used : number of empty spaces, monotonicity of the board."
  (monotonicity grid))

(defn score-direction [grid]
  "takes a grid and score the possibility of 2/4 tiles spawning in each empty index"
  (let [indexes (logic/find-zero-indexes grid)]
    (apply max (for [idx indexes]
                 (max (* 0.9 (score-grid (assoc grid idx 2))) 
                      (* 0.1 (score-grid (assoc grid idx 4))))))))

(defn unmemoized-weighted-average [grid]
  (let [indexes (logic/find-zero-indexes grid)
        res (sumlist (for [idx indexes]
                       (+ (* (/ 1 (count indexes)) 0.9 (score-grid (assoc grid idx 2)))
                          (* (/ 1 (count indexes)) 0.1 (score-grid (assoc grid idx 4))))))]
    res))

(def weighted-average (memoize unmemoized-weighted-average))

(defn unmemoized-generate-states [grid]
  (let [indexes (logic/find-zero-indexes grid)
        res (for [idx indexes]
              (into (assoc grid idx 2) (assoc grid idx 4)))]
    res))

(def generate-states (memoize unmemoized-generate-states))

(defn score-branch [grid curr-depth search-depth]
  (let [grid-left (logic/move-left grid)
        grid-right (logic/move-right grid)
        grid-up (logic/move-up grid) 
        grid-down (logic/move-down grid)
        valid-moves (filter (fn [x] (not (not x))) (list grid-left grid-right
                                                         grid-up grid-down))
        res (map (fn [mv] (-> mv
                              weighted-average
                                        ;time
                              ))
                 valid-moves)]
    (cond (= search-depth curr-depth) res 
          (= search-depth 1) (weighted-average grid)
          :else (if (empty? valid-moves) 
                  '()
                  (reduce into (map (fn [grid] (score-branch grid (inc curr-depth) search-depth)) valid-moves))))))

;; alternate between CHANCE and MAX nodes
;; option 1 - takes a grid and builds a list of all possible grids then score all of them
;; option 2 - using iterative depth first search and traverse the tree (alpha beta pruning wont help)
(defn tree-search [init-grid search-depth]
  "init grid will be a vector of 16 values, search depth is a number"
  (let [left (logic/move-left init-grid)
        right (logic/move-right init-grid)
        up (logic/move-up init-grid)
        down (logic/move-down init-grid) 
        grid-left (if left (apply max (flatten (map (fn [grid] (score-branch grid 1 search-depth)) 
                                                    (generate-states left)))) -1)
        grid-right (if right (apply max (flatten (map (fn [grid] (score-branch grid 1 search-depth)) 
                                                      (generate-states right)))) -1)
        grid-up (if up (apply max (flatten (map (fn [grid] (score-branch grid 1 search-depth)) 
                                                (generate-states up)))) -1)
        grid-down (if down (apply max (flatten (map (fn [grid] (score-branch grid 1 search-depth)) 
                                                    (generate-states down)))) -1)
        res {:left grid-left
             :right grid-right
             :up grid-up
             :down grid-down}]
    (println res)
    (key (apply max-key val res)))) 

(defn timeout [ms] 
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn run-ai [app]
  ;; (let [res (tree-search (get @app :grid-values) 2)]
  ;;   (println res) 
  ;;   (logic/move app res))
  (om/update! app [:game-running] true)
  (go (while (and (get @app :game-running)
                  (not (get @app :game-over))) 
        (let [res (-> (get @app :grid-values)
                      (tree-search 4)
                      (time))]
          (println res)
          (<! (timeout 5))
          (logic/move app res)))))
 
(defn stop-ai [app]
  (om/update! app [:game-running] false))
 
