(ns cljs-2048.global
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <! alts!]]
            [cljs-2048.logic :as logic]))

(def command-chan (chan))

(defn up []
  (put! command-chan :up))

(defn down []
  (put! command-chan :down))

(defn left []
  (put! command-chan :left))

(defn right []
  (put! command-chan :right))

(defn handle-transaction [tx-data root-cursor]
  (let [transaction-path (:path tx-data)]
    (when (= (last transaction-path) :editing-frame)
      (put! command-chan :frame-switched))))

(def LEFT-KEY 37)
(def UP-KEY 38)
(def RIGHT-KEY 39)
(def DOWN-KEY 40)

(defn handle-key-event [app event] 
  (let [keyCode (.-keyCode event)
        handler (cond
                 (= keyCode LEFT-KEY)  #(logic/move app :left)
                 (= keyCode UP-KEY)    #(logic/move app :up)
                 (= keyCode RIGHT-KEY) #(logic/move app :right)
                 (= keyCode DOWN-KEY)  #(logic/move app :down))]
    (when-not (= handler nil) (handler app))))

(defn key-listener [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:key-chan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [key-chan (om/get-state owner :key-chan)] 
        (go
          (loop []
            (let [[v ch] (alts! [key-chan])]
              (when (= ch key-chan) (handle-key-event app v))
              (recur))))))

    om/IDidMount
    (did-mount [_]
      (let [key-chan (om/get-state owner :key-chan)]
        (events/listen js/document "keydown" #(put! key-chan %))))

    om/IRender
    (render [this]
      (dom/div nil ""))))
