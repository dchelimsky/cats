(ns cats.types
  "Monadic types definition."
  (:require [cats.protocols :as proto]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Either
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype Either [v type]
  Object
  (equals [self other]
    (if (instance? Either other)
      (and (= v (.v other))
           (= type (.type other)))
      false))

  (toString [self]
    (with-out-str (print [v type])))

  proto/Monad
  (bind [s f]
    (case type
      :right (f v)
      s))

  proto/Functor
  (fmap [s f]
    (case type
      :right (Either. (f v) :right)
      s))

  proto/Applicative
  (pure [_ v]
    (Either. v type))

  (fapply [s av]
    (case type
      :right (proto/fmap av v)
      s)))

(defn left
  "Left constructor for Either type."
  [^Object v]
  (Either. v :left))

(defn right
  "Right constructor for Either type."
  [^Object v]
  (Either. v :right))

(defn left?
  [mv]
  (= (.type mv) :left))

(defn right?
  [mv]
  (= (.type mv) :right))

(defn from-either
  "Return inner value of either monad."
  [mv]
  (.v mv))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maybe
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype Nothing []
  Object
  (equals [_ other]
    (instance? Nothing other))

  (toString [_]
    (with-out-str (print "")))

  proto/Monad
  (bind [s f] s)

  proto/MonadPlus
  (mzero [_] (Nothing.))
  (mplus [_ mv] mv)

  proto/Functor
  (fmap [s f] s)

  proto/Applicative
  (pure [s v] s)
  (fapply [s av] s))

(deftype Just [v]
  Object
  (equals [self other]
    (if (instance? Just other)
      (= v (.v other))
      false))

  (toString [self]
    (with-out-str (print [v])))

  proto/Monad
  (bind [self f]
    (f v))

  proto/MonadPlus
  (mzero [_]
    (Nothing.))
  (mplus [mv _] mv)

  proto/Functor
  (fmap [s f]
    (Just. (f v)))

  proto/Applicative
  (pure [_ v]
    (Just. v))
  (fapply [_ av]
    (proto/fmap av v)))

(defn just
  [v]
  (Just. v))

(defn nothing
  []
  (Nothing.))

(defn maybe?
  [v]
  (or (instance? Just v)
     (instance? Nothing v)))

(defn just?
  [v]
  (instance? Just v))

(defn nothing?
  [v]
  (instance? Nothing v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#+clj
(extend-type clojure.lang.PersistentVector
  proto/Monad
  (bind [self f]
    (vec (flatten (map f self))))

  proto/MonadPlus
  (mzero [_] [])
  (mplus [mv mv'] (into mv mv'))

  proto/Functor
  (fmap [self f] (vec (map f self)))

  proto/Applicative
  (pure [_ v] [v])
  (fapply [self av]
    (vec (for [f self
               v av]
           (f v)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pair (State monad related)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype Pair [fst snd]
  clojure.lang.Seqable
  (seq [_] (list fst snd))

  clojure.lang.Indexed
  (nth [_ i]
    (case i
      0 fst
      1 snd
      (throw (IndexOutOfBoundsException.))))

  (nth [_ i notfound]
    (case i
      0 fst
      1 snd
      notfound))

  clojure.lang.Counted
  (count [_] 2)

  Object
  (equals [this other]
    (if (instance? Pair other)
      (and (= (.fst this) (.fst other))
           (= (.snd this) (.snd other)))
      false))

  (toString [this]
    (with-out-str (print [fst snd]))))

(defn pair
  [fst snd]
  (Pair. fst snd))

(defn pair?
  [v]
  (instance? Pair v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State Monad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare state-t)

(deftype State [mfn]
  proto/Monad
  (bind [self f]
    (-> (fn [s]
          (let [p        (mfn s)
                value    (.fst p)
                newstate (.snd p)]
            ((f value) newstate)))
        (state-t)))

  clojure.lang.IFn
  (invoke [self seed]
    (mfn seed))

  proto/Applicative
  (pure [_ v]
    (State. (fn [s] (pair v s))))
  (fapply [_ av]
    (throw (RuntimeException. "Not implemented"))))

(defn state-t
  "Transform a simple state-monad function
  to State class instance.
  State class instance work as simple wrapper
  for standard clojure function, just for avoid
  extend plain function type of clojure."
  [f]
  (State. f))
