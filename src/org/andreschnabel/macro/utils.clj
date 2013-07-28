(ns org.andreschnabel.macro.utils
  (:import [com.badlogic.gdx.graphics.g2d PixmapPacker]
           [com.badlogic.gdx Gdx]
           [com.badlogic.gdx.files FileHandle]
           [com.badlogic.gdx.graphics Pixmap Pixmap$Format Texture Texture$TextureFilter]))

(defn ticks [] (System/currentTimeMillis))

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

(defn fassoc [m key f]
  (assoc m key (f (m key))))

(defn assoc-if [cond m key val]
  (if cond (assoc m key val) m))

(defn merge-if-elsel [cond m1 m2]
  (if cond (merge m1 m2) m1))

(defmacro to-vec [coll] `(into [] ~coll))

(defn repeat-vec [n x]
  (to-vec (repeat n x)))

(defn vec-addf [u v]
  [(+ (u 0) (v 0)) (+ (u 1) (v 1))])

(defmacro vec-add [u v]
  `(mapv + ~u ~v))

(defmacro vec-scal-mul [v c]
  `(mapv * ~v (repeat 2 ~c)))

(defmacro xcrd [v] `(first ~v))
(defmacro ycrd [v] `(second ~v))
(defmacro width [v] `(first ~v))
(defmacro height [v] `(second ~v))

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

(defmacro foreach [f coll]
  `(doseq [elem# ~coll] (~f elem#)))

(def last-calls (atom {}))
(defmacro limit-rate [sym min-delay & body]
  `(if (> (- (ticks) (put-or-keep last-calls ~sym (fn [] 0))) ~min-delay)
     ~(cons 'do (cons `(assoc-in-place last-calls ~sym (ticks)) body))))

(defmacro limit-rate-else [elseval sym min-delay & body]
  `(if (> (- (ticks) (put-or-keep last-calls ~sym (fn [] 0))) ~min-delay)
     ~(cons 'do (cons `(assoc-in-place last-calls ~sym (ticks)) body))
     ~elseval))

(defn ++ [x] (+ 1 x))

(defn set-diff [a b]
  (remove (fn [e] (some #(= e %) b)) a))