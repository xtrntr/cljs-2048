(ns cljs-2048.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [cljs-2048.core-test]
   [cljs-2048.common-test]))

(enable-console-print!)

(doo-tests 'cljs-2048.core-test
           'cljs-2048.common-test)
