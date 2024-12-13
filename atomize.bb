#!/usr/bin/env bb

(ns atomize
  (:require [shell-smith.core :as smith]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(def usage "
Atomize

Usage:
  atomize [-i[ -b]] [-n=<entry-limit>] <atom-file>
  atomize -h

Options:
  -i --inplace                    Update the atom file in place
  -b --backup                     Keep backup of original atom file
  -n --entry-limit=<entry-limit>  Number of entries to keep [default: 20]
  -h --help                       Show help
")

(def config (smith/config usage))

(defn parse-atom [file]
  (with-open [rdr (io/reader file)]
    (xml/parse rdr)))

#_(def atom-xml (parse-atom "feed.atom"))

(defn author []
  (or (System/getenv "USERNAME")
      (System/getenv "USER")
      "(author missing)"))

(defn entry->map [{:keys [title url id updated summary content author email]
                   :or {title "Untitled"
                        id (str "uri:uuid:" (random-uuid))
                        updated (str (java.time.Instant/now))
                        summary "(summary missing)"
                        content "(content missing)"
                        author (author)
                        email "(email missing)"}}]
  {:tag :entry
   :attrs {}
   :content [{:tag :title :content [title]}
             {:tag :link :attrs {:href url}}
             {:tag :id :content [id]}
             {:tag :updated :content [updated]}
             {:tag :summary :content [summary]}
             {:tag :content :attrs {:type "html"} :content [(xml/cdata content)]}
             {:tag :author :content
              [{:tag :name :content [author]}
               {:tag :email :content [email]}]}]})

(defn entry? [{:keys [tag]}]
  (case tag :entry :entries :others))

(defn add-entry [atom-xml new-entry]
  (let [{:keys [entry-limit]} config
        entry-limit (if (string? entry-limit) (Integer/parseInt entry-limit) entry-limit)
        {:keys [entries others]} (group-by entry? (:content atom-xml))
        new-entry-xml (entry->map new-entry)
        all-entries (concat [new-entry-xml] entries)
        final-entries (if entry-limit
                        (take entry-limit all-entries)
                        all-entries)]
    (assoc atom-xml
           :content
           (concat others final-entries))
    ))

#_(add-entry atom-xml {:title "a"})

(defn backup-file [file]
  (let [backup-name (str file ".bak")]
    (io/copy (io/file file) (io/file backup-name))))

(def example-atom "
<?xml version=\"1.0\" encoding=\"utf-8\"?>
<feed xmlns=\"http://www.w3.org/2005/Atom\">
  <title>My Feed</title>
  <subtitle>Feed Description</subtitle>
  <link href=\"https://example.com/feed.atom\" rel=\"self\"/>
  <link href=\"https://example.com\"/>
  <updated>2024-12-12T00:00:00Z</updated>
  <id>urn:uuid:60a76c80-d399-11d9-b93C-0003939e0af6</id>
  <author>
    <name>John Doe</name>
    <email>john@example.com</email>
  </author>
</feed>
")

(defn -main [& args]
  (when (:help config)
    (println usage)
    (System/exit 0))

  (let [{:keys [atom-file inplace backup]} config]
    (when-not (fs/exists? atom-file)
      (spit atom-file (str/trim example-atom)))
    (let [atom-xml (parse-atom atom-file)
          input (when-not (System/console) (slurp *in*))
          entry (json/parse-string input true)
          updated-xml (add-entry atom-xml entry)]

      (let [result (xml/emit-str updated-xml)]
        (if inplace
          (do
            (when backup
              (backup-file atom-file))
            (spit atom-file result))
          (println result))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
