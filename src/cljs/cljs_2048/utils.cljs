(ns cljs-2048.utils)

(defn subset [grid indexes]
  "given a grid and a list of idx, return values of grid corresponding to each idx in a list"
  (for [idx indexes]
    (get grid idx)))

(defmacro timed [expr]
  (let [sym (= (type expr) clojure.lang.Symbol)]
    `(let [start# (. System (nanoTime))
           return# ~expr
           res# (if ~sym  
                    (resolve '~expr)  
                    (resolve (first '~expr)))]
       (prn (str "Timed "
           (:name (meta res#))
           ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
       return#)))
