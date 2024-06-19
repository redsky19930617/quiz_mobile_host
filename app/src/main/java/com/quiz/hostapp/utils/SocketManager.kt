package com.quiz.hostapp.utils

import android.util.Log
import com.quiz.hostapp.MyApplication
import com.quiz.hostapp.network.BASE_URL
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.Transport
import org.json.JSONObject
import java.util.*


class SocketManager {
    companion object {
        private const val SERVER_URL = BASE_URL
        private const val TAG = "SocketManager"
    }

    private var socket: Socket? = null
    private var connectionListener: ConnectionListener? = null
    private var messageSendListener: MessageSendListener? = null


    init {
        try {
            socket = IO.socket(SERVER_URL)
            Log.e(TAG, "socket connecting")
        } catch (e: Exception) {
            Log.e(TAG, "failed to connect to socket: ${e.message}")
        }
    }

    fun connect() {
        val token = MyApplication.sessionManager?.getPrefString(SessionManager.ACCESS_TOKEN)
        socket?.connect()

        socket?.io()?.on(Manager.EVENT_TRANSPORT, Emitter.Listener { args ->
            val transport: Transport = args[0] as Transport
            // Adding headers when EVENT_REQUEST_HEADERS is called
            transport.on(Transport.EVENT_REQUEST_HEADERS, Emitter.Listener { args ->
                Log.v(TAG, "Caught EVENT_REQUEST_HEADERS after EVENT_TRANSPORT, adding headers")
                val mHeaders = args[0] as MutableMap<String, List<String>>
                mHeaders["Authorization"] = listOf("Bearer $token")
            })
            transport.on(Transport.EVENT_ERROR) { args ->
                Log.e(TAG, "transport error:${args.first()}")
            }
        })
        socket?.on(Socket.EVENT_CONNECT) {
            connectionListener?.onConnected()
        }
        socket?.on(Socket.EVENT_DISCONNECT) {
            connectionListener?.onDisconnected()
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.e(TAG, "connection error: $it")
            connectionListener?.onConnectError(it)
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun sendMessage(message: JSONObject, event: String) {
        Log.e(TAG, "emitting message for: $event")
        socket?.emit(event, message, Emitter.Listener { args ->
            Log.e(TAG, "message sent: ${args.isNotEmpty()}")
            messageSendListener?.onMessageSent(args.isNotEmpty())
        })
    }

    fun setEventListener(name: String, eventListener: EventListener) {
        Log.e(TAG,"listening to: $name")
        socket?.on(name)
        { args ->
            // Handle event data and notify the listener
            Log.e(TAG, "message from socket: $args")
            eventListener.onEventReceived(args)
        }
    }

    fun setConnectionListener(listener: ConnectionListener) {
        connectionListener = listener
    }

    interface EventListener {
        fun onEventReceived(args: Array<Any>)
    }

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onConnectError(args: Array<Any>)
    }

    interface MessageSendListener {
        fun onMessageSent(isSuccessful: Boolean)
    }
}
