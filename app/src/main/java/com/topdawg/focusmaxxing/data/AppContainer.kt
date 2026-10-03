package com.topdawg.focusmaxxing.data

import android.content.Context
import com.topdawg.focusmaxxing.BuildConfig
import com.topdawg.focusmaxxing.solo.SoloLocalStore
import com.topdawg.focusmaxxing.solo.SoloMaterialFiles
import com.topdawg.focusmaxxing.solo.ai.BackendRecallAiService
import com.topdawg.focusmaxxing.solo.ai.FakeRecallAiService
import com.topdawg.focusmaxxing.solo.ai.RecallAiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext
    val auth: AuthRepository
    val users: UserRepository
    val rooms: RoomRepository
    val friends: FriendRepository
    val solo: SoloRepository
    val soloStore: SoloLocalStore
    val soloMaterialFiles: SoloMaterialFiles
    val recallAi: RecallAiService
    init {
        soloStore = SoloLocalStore(context)
        soloMaterialFiles = SoloMaterialFiles(context, soloStore)
        var configuredFirebaseAuth: FirebaseAuth? = null
        if (context.applicationContext.resources.getIdentifier("google_app_id", "string", context.packageName) != 0) {
            val firebaseAuth = FirebaseAuth.getInstance()
            configuredFirebaseAuth = firebaseAuth
            val firestore = FirebaseFirestore.getInstance()
            val authRepo = FirebaseAuthRepository(firebaseAuth)
            val userRepo = FirebaseUserRepository(firestore)
            auth = authRepo; users = userRepo
            rooms = FirebaseRoomRepository(firestore, authRepo, userRepo)
            friends = FirebaseFriendRepository(firestore, authRepo, userRepo)
            solo = FirebaseSoloRepository(firestore)
        } else {
            val fake = FakeRepositories()
            auth = fake; users = fake; rooms = fake; friends = fake
            solo = fake
        }
        recallAi = if (BuildConfig.BACKEND_URL.isBlank()) FakeRecallAiService() else BackendRecallAiService(
            BuildConfig.BACKEND_URL.trimEnd('/'),
            userIdProvider = { auth.currentUserId },
            tokenProvider = { configuredFirebaseAuth?.currentUser?.getIdToken(false)?.await()?.token }
        )
    }
}
