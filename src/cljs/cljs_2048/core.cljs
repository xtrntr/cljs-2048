(ns cljs-2048.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs-2048.global :as global]
            [cljs-2048.search :as search]
            [cljs-2048.logic :as logic]
            [cljs.core.async :refer [put! chan <! alts!]]
            [clojure.string :as string]))

(enable-console-print!)

(defn tx-listener [tx-data root-cursor]
  (global/handle-transaction tx-data root-cursor))

(defonce app-state (atom {:grid-values [0 0 0 0
                                        0 0 0 0
                                        0 0 0 0
                                        0 0 0 0]
                          :game-over false}))

(defn pos-to-index [cell pos-str]
  "given a position (i.e. 1-1, 3-4), return the corresponding index. 1-1 is 0 and 4-4 is 16"
  (let [vec (.map (.split pos-str "-") js/Number)
        col (first vec)
        row (second vec)]
    (- (+ col (* (- row 1) 4)) 1)))

(defn cell [cell owner] 
  (reify
    om/IRenderState
    (render-state [_ {:keys [grid-values]}]
      (let [cell-value (get grid-values (pos-to-index grid-values (:id cell)))]
        (when-not (= cell-value 0)
          (dom/div #js {:className (str "tile " 
                                        "tile-position-" (:id cell) 
                                        " tile-" cell-value)} 
                   (dom/div #js {:className "tile-inner"} cell-value)))))))

(defn row [row owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [grid-values]}]
      (apply dom/div nil
             (om/build-all cell [{:id (str (:id row) "-1")}
                                 {:id (str (:id row) "-2")}
                                 {:id (str (:id row) "-3")}
                                 {:id (str (:id row) "-4")}] {:key :id
                                                              :state {:grid-values grid-values}})))))

(defn grid-cell [row owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "grid-cell"}))))

(defn grid-row [row owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className (str "grid-row " (:id row))}
             (om/build-all grid-cell (int-array (range 4)))))))

(defn game-over-message [game-over? app]
  (dom/div #js {:className (when game-over? "game-message game-over")}
           (dom/p nil (if game-over? "Game over!"))
           (dom/div #js {:className (when game-over? "lower")}
                    (dom/a #js {:className (when game-over? "retry-button")
                                :onClick #(logic/restart-game app)}
                           (if game-over? "Try again" "")))))

(defn board [app owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [game-over? (get @app :game-over)]
        (dom/div #js {:className "game-container"}
                 (if game-over? 
                   (game-over-message game-over? app)
                   (dom/div nil))
                 (dom/div #js {:className "grid-container"}
                          (apply dom/div nil
                                 (om/build-all grid-row (int-array (range 4)))))
                 (let [grid-values (get @app :grid-values)]
                   (dom/div #js {:className "tile-container"}
                            (apply dom/div nil
                                   (om/build-all row [{:id "1"}
                                                      {:id "2"}
                                                      {:id "3"}
                                                      {:id "4"}] {:key :id
                                                                  :state {:grid-values grid-values}})))))))))

(defn heading [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "heading"}
               (dom/h1 #js {:className "title"} 2048)))))

(defn above-game [app owner] 
  (reify
    om/IRender 
    (render [this]
      (dom/div #js {:className "above-game"}
               (dom/p #js {:className "game-intro"} 
                      "Join the numbers and get to the "
                      (dom/strong nil "2048 tile!")) 
               (dom/a #js {:className "restart-button"
                           :onClick #(search/run-ai app)} "Run AI") 
               (dom/a #js {:className "restart-button"
                           :onClick #(logic/restart-game app)} "New Game")))))

(defn screen [app owner]
  (om/component 
   (dom/div nil
            (om/build global/key-listener app)
            (om/build heading app)
            (om/build above-game app)
            (om/build board app))))

(om/root 
 screen
 app-state 
 {:target (js/document.getElementById "abc")
  :shared {:command-chan global/command-chan}
  :tx-listen #(tx-listener % %)})
