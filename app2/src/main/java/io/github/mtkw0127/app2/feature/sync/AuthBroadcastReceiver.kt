package io.github.mtkw0127.app2.feature.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private val _message = MutableStateFlow<String?>(null)
        val message: StateFlow<String?> = _message
    }

    override fun onReceive(context: Context, intent: Intent) {
        _message.value = intent.getStringExtra("message")
    }
}
