package com.example.service

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    
    private val TAG = "AuthManager"
    
    suspend fun authenticateAnonymously(): FirebaseUser? {
        return try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInAnonymously().await()
            Log.d(TAG, "Authenticated with Firebase Auth: ${result.user?.uid}")
            result.user
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth failed or missing google-services config: ${e.message}")
            null
        }
    }
    
    fun getCurrentUserUid(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }
}
