package net.veldor.flibustaloader.utils

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FireStorageHandler {
    fun saveCustomBridges() {
        Log.d("surprise", "FireStorageHandler.kt 5 saveCustomBridges saving current bridges!!")
        // delete current value
        val db = Firebase.firestore
        db.collection("bridges")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("surprise", "${document.id} => ${document.data}")
                    // save document data
                    db.collection("bridges").document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("surprise", "DocumentSnapshot successfully deleted!")
                            // save new document
                            val bridge = hashMapOf(
                                "bridge" to PreferencesHandler.instance.getCustomBridges(),
                            )

                            db.collection("bridges").document("bridges")
                                .set(bridge)
                                .addOnSuccessListener {
                                    Log.d(
                                        "surprise",
                                        "DocumentSnapshot successfully written!"
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Log.w(
                                        "surprise",
                                        "Error writing document",
                                        e
                                    )
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w(
                                "surprise",
                                "Error deleting document",
                                e
                            )
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.d("surprise", "Error getting documents.", exception)
            }
    }
}