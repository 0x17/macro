(ns org.andreschnabel.macro.utils
  (:import [com.badlogic.gdx.graphics.g2d PixmapPacker]
           [com.badlogic.gdx Gdx]
           [com.badlogic.gdx.files FileHandle]
           [com.badlogic.gdx.graphics Pixmap Pixmap$Format Texture Texture$TextureFilter]))

(defn filename-wout-ext [f]
  (let [parts (.split (.getName f) "\\.")]
    (aget parts (- (alength parts) 2))))

(defn list-image-files [dir]
  (filter #(.endsWith (.getName %) ".png") (.listFiles dir)))

(defn atlas-for-dir [dir]
  (let [packer (PixmapPacker. 1024 1024 Pixmap$Format/RGBA8888 2 true)]
    (doseq [f (list-image-files dir)]
      (.pack packer (filename-wout-ext f) (Pixmap. (FileHandle. f))))
    (.generateTextureAtlas packer Texture$TextureFilter/Linear Texture$TextureFilter/Linear true)))

(defn load-res [path ext] (.internal Gdx/files (str "data/" path "." ext)))

(defn put-or-keep [m key gen-func]
  (if (not (contains? @m key))
    (reset! m (assoc @m key (gen-func))))
  (@m key))

(defn safe-dispose [obj]
  (if (not (nil? obj))
    (.dispose obj)))

(defn assoc-in-place [m-atom key val]
  (reset! m-atom (assoc @m-atom key val)))

(defn fassoc-in-place [m-atom key f]
  (reset! m-atom (assoc @m-atom key (f (@m-atom key)))))

(defn repeat-vec [n x]
  (into [] (repeat n x)))

(defmacro defn-destr [name args body]
  `(def ~name
     (fn (~args ~body)
       ([param-to-val#] (let [{:keys ~args} param-to-val#] ~body)))))

(defmacro deflazy [name init-func]
  `(def ~name (memoize ~init-func)))

(defn group-pairs [elems]
  (if (empty? elems)
    '()
    (cons [(first elems) (second elems)]
      (group-pairs (-> elems rest rest)))))

(defmacro defbatch [& pairs]
  (cons 'do
    (->> pairs
         group-pairs
         (map #(list 'def (first %) (second %))))))