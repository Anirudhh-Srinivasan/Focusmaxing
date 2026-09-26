package com.topdawg.focusmaxxing.viewmodels

import androidx.lifecycle.ViewModel
import kotlin.random.Random

class LobbyViewModel : ViewModel() {

    // Generate a random 6-character room code (uppercase letters + digits)
    fun generateRoomCode(): String {
        val chars: List<Char> = ('A'..'Z').toList() + ('0'..'9').toList()
        return (1..6).map { chars.random() }.joinToString("")
    }

    // Simple validation: exactly 6 non-empty characters
    fun validateRoomCode(code: String): Boolean {
        return code.length == 6 && code.isNotBlank()
    }
}
