(ns cvf.web
  (:use [compojure.core])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [cvf.core :as cvf]
            [hiccup.def :refer (defhtml)]))

(defhtml graph-script [stats]
  (let [max-forks (->> stats (map :forks) (apply max 0))
        max-contributors (->> stats (map :contributors) (apply max 0))]
    [:script {:type "text/javascript"}
      "google.load('visualization', '1', {packages:['corechart']});
       google.setOnLoadCallback(drawChart);
       function drawChart() {
         var data = new google.visualization.DataTable();
         data.addColumn('number', 'Forks');
         data.addColumn('number', 'Contributors');
         data.addColumn({type:'string', role:'tooltip'});
         data.addRows(["
          (for [{:keys [forks contributors repo]} stats
                :let [percent (* (/ (float contributors) (inc forks)) 100)]]
            (str "[ " forks ", " contributors ", "
                 "'" (format "%2.0f%%" percent) " " repo "'],"))
         "]);
         var options = {
           title: 'Forks vs. Contributors comparison',
           hAxis: {title: 'Forks', minValue: 0, "
                   "maxValue: " (str max-forks) "},
           vAxis: {title: 'Contributors', minValue: 0, "
                   "maxValue: " (str max-contributors) "},
           legend: 'none'
         };
         var chart =
           new google.visualization.ScatterChart(
             document.getElementById('chart_div'));
         chart.draw(data, options);
       }"]))

(defhtml graph [stats]
  [:div#chart_div {:style "width: 900px; height: 600px;"}])

(defhtml repo-form [error]
  [:form {:method "POST" :action "/add"}
   [:label {:for "repo"} "Add repository:"]
   [:input#repo {:type "text" :name "repo"}]
   [:input {:type "submit"}]
   (when error
     [:b {:style "color: red;"} "Error: "error])
   [:br]
   [:small "Format:&nbsp;&nbsp; username/repository"]])

(defhtml stats-table [stats repo-id]
  [:table
   [:tr
    [:th "Repository"]
    [:th "Forks"]
    [:th "Watchers"]
    [:th "Contributors"]]
   (for [{:keys [id url repo forks watchers contributors]} stats]
     [:tr (when (= id repo-id) {:style "background: #fff8e7;"})
      [:td [:a {:href url} repo]]
      [:td forks]
      [:td watchers]
      [:td contributors]])])

(defhtml home [repo error]
  (let [stats (cvf/stats)]
    [:html
     [:head
      [:script {:type "text/javascript" :src "https://www.google.com/jsapi"}]
      (graph-script stats)
      [:title "Contributors vs Forks"]]
     [:body
      (graph stats)
      [:br]
      (repo-form error)
      [:br]
      (stats-table stats repo)]]))

(defn data []
  {:status 200
   :headers {"Content-Type" "text/plain"
             "Content-Disposition" "attachment; filename=\"data.clj\""}
   :body (with-out-str (clojure.pprint/pprint @cvf/db))})

(defn add-repo [repo]
  (when-let [repo (.trim (or repo ""))]
    (let [result (cvf/add-repo! repo)]
      (response/redirect
        (if (keyword? result)
          (str "/?error=" (name result))
          (str "/?repo=" result))))))

(defroutes app-routes
  (GET "/" [repo error] (home repo error))
  (GET "/data" [] (data))
  (POST "/add" [repo] (add-repo repo))
  (route/not-found "<h1>Page not found</h1>"))

(def handler
  (handler/site app-routes))
