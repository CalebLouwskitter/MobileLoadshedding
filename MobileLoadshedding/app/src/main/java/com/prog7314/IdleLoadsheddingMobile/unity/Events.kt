package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.api.EventsApi
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityEventPayload
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArraySet

object Events : UnityBridge.Listener {

    private const val TAG = "Events"
    private const val HISTORY_LIMIT = 20

    private val gson = Gson()
    private val api: EventsApi = BackendClient.create()

    interface Observer {
        fun onEventsUpdated(activeEvent: UnityEventPayload?, history: List<UnityEventPayload>)
    }

    private val observers = CopyOnWriteArraySet<Observer>()
    private val history = ArrayDeque<UnityEventPayload>()

    @Volatile
    var latestEvent: UnityEventPayload? = null
        private set

    @Volatile
    private var activeEvent: UnityEventPayload? = null

    init {
        UnityBridge.registerListener(this)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
        observer.onEventsUpdated(activeEvent, snapshotHistory())
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    override fun onGameEvent(event: String) {
        val payload = try {
            gson.fromJson(event, UnityEventPayload::class.java)
        } catch (ex: JsonSyntaxException) {
            Log.w(TAG, "Invalid event payload", ex)
            null
        }

        if (payload != null) {
            updateState(payload)
            notifyObservers()
            postEvent(payload)
        }
    }

    fun currentActiveEvent(): UnityEventPayload? = activeEvent

    fun history(): List<UnityEventPayload> = snapshotHistory()

    private fun updateState(event: UnityEventPayload) {
        latestEvent = event

        synchronized(history) {
            if (event.isStarting) {
                activeEvent = event
            } else if (activeEvent?.id == event.id) {
                activeEvent = null
            }

            history.addFirst(event)
            while (history.size > HISTORY_LIMIT) {
                history.removeLast()
            }
        }
    }

    private fun snapshotHistory(): List<UnityEventPayload> = synchronized(history) {
        history.toList()
    }

    private fun notifyObservers() {
        val snapshot = snapshotHistory()
        val current = activeEvent
        observers.forEach { it.onEventsUpdated(current, snapshot) }
    }

    private fun postEvent(payload: UnityEventPayload) {
        api.recordEvent(payload).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to record event: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "Network error recording event", t)
            }
        })
    }
}