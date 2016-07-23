(ns cljs-2048.utils)

(defn subset [grid indexes]
  "given a grid and a list of idx, return values of grid corresponding to each idx in a list"
  (for [idx indexes]
    (get grid idx)))
