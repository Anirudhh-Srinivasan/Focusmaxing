package com.topdawg.focusmaxxing.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    val auth: AuthRepository
    val users: UserRepository
    val rooms: RoomRepository
    val friends: FriendRepository
    init {
        if (context.applicationContext.assets.list("") != null && context.applicationContext.resources.getIdentifier("google_app_id", "string", context.packageName) != 0) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val authRepo = FirebaseAuthRepository(firebaseAuth)
            val userRepo = FirebaseUserRepository(firestore)
            auth = authRepo; users = userRepo
            rooms = FirebaseRoomRepository(firestore, authRepo, userRepo)
            friends = FirebaseFriendRepository(firestore, authRepo, userRepo)
        } else {
            val fake = FakeRepositories()
            auth = fake; users = fake; rooms = fake; friends = fake
        }
    }
}
