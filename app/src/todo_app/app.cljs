; This is a client-only application for TODO list. It has a single text entry 
; with button for entering new todos, and a list of all the entered values.
;
; I based this on two examples from pedestal: helloworld-app and chat. I think
; it's more relevant than helloworld, and much easier to grasp than chat. And it
; has actual comments to explain what is where!
;
; Here's the flow: When the button is clicked, event handler pushes a message to
; :todo topic. There is a transform listening on that topic which updates the 
; data model. There also is an emitter that will push the current list of todos
; up to application model. Finally, in the application level there is a renderer
; that will rebuild part of the page with the current model.
;
; Watch out - this one namespace has things that are in data model and 
; application model, as well as all transforms/dataflows/renders between those 
; models and DOM. Be careful and make sure you understand which model is 
; operated where. 

(ns todo-app.app
  (:require [io.pedestal.app :as app]
            [io.pedestal.app.protocols :as p]
            [io.pedestal.app.render :as render]
            [io.pedestal.app.render.push :as push]
            [io.pedestal.app.messages :as msg]
            [io.pedestal.app.render.events :as events]
            [domina.events :as dom-event]
            [domina :as dom]
            ))

;;;;;;;;;;;;;;;
;; TRANSFORM ;;
;;;;;;;;;;;;;;;



; This function updates the data model in response to a message. It will be 
; triggered whenever a message appears in the :todo topic. The only accepted 
; message in this implementation has type :add and :value corresponding to the 
; new todo text.

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn todo-transform [state message]
  (case (msg/type message)
    msg/init (:value message)
    :add (update-in state [:model-todos] merge (:value message))
    ;:add (.log js/console (number? (first (keys (:value message)))))
    ;:cancel (.log js/console (str (number? (:value message)) " " (number? (first (keys (get-in state [:model-todos])))) ))
    :cancel (dissoc-in state [:model-todos (:value message)])
  )
)

;;;;;;;;;;;;;;
;; DATAFLOW ;;
;;;;;;;;;;;;;;

; This function generates a list of deltas for the application model - in this 
; case it replaces [:app :todos] with the current list of all todos.
(defn todo-deltas [new-value]
  [[:value [:app :todos] (:model-todos new-value)]])

; This function generates change messages (deltas) for the application model.
(defn todo-emit
  ([inputs] 
  ;(.log js/console (str "primo caso, inputs: " inputs)) 
   initial-app-model)
  ([inputs changed-inputs]
  (.log js/console (str "inputs: " inputs))
  (.log js/console (str "changed inputs: " changed-inputs))
    (reduce (fn [a input-name]
              (let [new-value (:new (get inputs input-name))]
                (concat a (case input-name
                            :todo (todo-deltas new-value)
                            []))))
            []
            changed-inputs)))

; This defines operations on the data model. The :transform is triggered when
; something appears in the :todo queue. After the transform does its thing, 
; :emit is triggered in the same step (because it's also bound on :todo).
(def count-app {:transform {:todo {:init {} :fn todo-transform}}
                :emit {:emit {:fn todo-emit :input #{:todo}}}})

;;;;;;;;;;;;;;;;;;;;;;;
;; APPLICATION MODEL ;;
;;;;;;;;;;;;;;;;;;;;;;;

; Initial state of the application model. It's always a single tree.
(def ^:private initial-app-model
  [{:app
    {:todos []}}]
)

;;;;;;;;;;;;;;;
;; RENDERING ;;
;;;;;;;;;;;;;;;

(defn bind-cancel-button [input-queue button-id]
  (.log js/console (str "bind-cancel-button !! Button id " button-id " " (number? button-id)))
  (let [cancel-button (dom/by-id (str button-id))]
    (events/send-on :click
                    cancel-button
                    input-queue
                    (fn []
                        ;(.log js/console "button pressed !")
                        [{msg/topic :todo msg/type :cancel :value button-id}]))
  
  )
)

; Renderer for the todo list. In new-value it receives only the part of model 
; that it is listening to (the vector at [:app :todos]). Here it's doing some
; DOM manipulation with Domina. I imagine it could instead be a bridge to 
; AngularJS.
(defn render-todos [r [_ _ old-value new-value] input-queue]
  ;(.log js/console new-value)
  (let [container (dom/by-id "todo-list")]
    (dom/destroy-children! container)
    (doseq [new-todo new-value]
      (dom/append! container
                   (str "<li>" (new-todo 1) "<form> <button id = \""(new-todo 0)"\"> Cancel </button> </form></li>"
                   )
      )
      (bind-cancel-button input-queue (new-todo 0))
    )
  ))

; When the button is clicked, send a message to :todo topic to kick off the
; process.
(defn bind-todo-form [input-queue]
  (let [form (dom/by-id "todo-form")
        btn (dom/by-id "todo-add-button")
       ]
    (.focus (dom/by-id "todo-entry"))
    (events/send-on :click
                    btn
                    input-queue
                    (fn []
                      (let [text-node (dom/by-id "todo-entry")
                            text (.-value text-node)]
                        (set! (.-value text-node) "")
                        [{msg/topic :todo msg/type :add :value {(.getUTCMilliseconds (js/Date.)), text}}])))

))

(defn ^:export main []
  (let [app (app/build count-app)
        ; Plug in render-todos to changes in value at [:app :todos]
        render-fn (push/renderer "content" [[:value [:app :todos] render-todos]])]
    (render/consume-app-model app render-fn)
    (bind-todo-form (:input app))
    (app/begin app)))
