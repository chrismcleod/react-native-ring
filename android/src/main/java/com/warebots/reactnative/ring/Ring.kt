package com.warebots.reactnative.ring

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import android.os.AsyncTask



private typealias Callback = (bundle: Bundle, origin: String?) -> Any

class Ring : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    if(context != null && intent != null) {
      val pendingResult = goAsync()
      class Task() : AsyncTask<String, Int, String>() {
        override fun doInBackground(vararg params: String): String {
          val replyTo = intent.getStringExtra("replyTo")
          if(replyTo != null) {
            val payload = intent.getBundleExtra("payload")
            val node = Node.getInstance(context, payload.getParcelable<Group>("appGroup"))
            node.invokeMethod(payload).subscribe { response ->
              sendReply(intent, context, response)
            }
          } else if(intent.getStringExtra("repliedTo") != null) {
            val response = intent.getBundleExtra("response")
            val id = intent.getStringExtra("id")
            val callback = Adapter.callbacks[id]
            callback?.invoke(response, intent.getStringExtra("origin"))
          }
          pendingResult.finish()
          return ""
        }
      }
      Task().execute()
    }
  }
}

fun send(target: String, context: Context, payload: Bundle, callback: Callback) {
  val id = UUID.randomUUID().toString()
  val intent = Intent()
  intent.component = ComponentName(target, "com.warebots.reactnative.ring.Ring")
  intent.putExtra("id", id)
  intent.putExtra("origin", context.packageName)
  intent.putExtra("replyTo", context.packageName)
  intent.putExtra("payload", payload)
  Adapter.callbacks[id] = callback
  context.sendBroadcast(intent)
}

fun sendReply(origin: Intent, context: Context, response: Bundle? = bundleOf("success" to true)) {
  val reply = Intent()
  reply.putExtra("id", origin.getStringExtra("id"))
  reply.putExtra("origin", context.packageName)
  reply.putExtra("repliedTo", origin.getStringExtra("replyTo"))
  reply.putExtra("response", response)
  reply.component = ComponentName(origin.getStringExtra("replyTo"), "com.warebots.reactnative.ring.Ring")
  context.sendBroadcast(reply)
}

object Adapter {
  val callbacks: MutableMap<String, Callback> = mutableMapOf()
}

class RingClient(private val context: Context, private val mAppGroup: Group) {

  fun initialize(targets: List<String>): Observable<List<Bundle>> {
    val subject: Subject<Bundle> = PublishSubject.create()
    if(targets.isEmpty()) return Observable.just(listOf(bundleOf("success" to true)))
    val payload = bundleOf(
      "method" to "initialize",
      "appGroup" to mAppGroup
    )
    return subject.buffer(targets.size).doOnSubscribe {
      targets.forEach { target -> send(target, context, payload) { response, _ -> subject.onNext(response) } }
    }
  }

  fun read(targets: List<String>): Observable<List<Bundle>> {
    val subject: Subject<Bundle> = PublishSubject.create()
    /* This can happen if there is only one app in the group and we invoke  on follower apps */
    if(targets.isEmpty()) return Observable.just(listOf(bundleOf("success" to true)))
    val payload = bundleOf(
      "method" to "read",
      "appGroup" to mAppGroup
    )
    return subject.buffer(targets.size).doOnSubscribe {
      targets.forEach { target -> send(target, context, payload) { response, _ -> subject.onNext(response) } }
    }
  }

  fun write(targets: List<String>, version: Int, data: String): Observable<List<Bundle>> {
    val subject: Subject<Bundle> = PublishSubject.create()
    if(targets.isEmpty()) return Observable.just(listOf(bundleOf("success" to true)))
    val payload = bundleOf(
      "method" to "write",
      "appGroup" to mAppGroup,
      "version" to version,
      "data" to data
    )
    return subject.buffer(targets.size).doOnSubscribe {
      targets.forEach { target -> send(target, context, payload) { response, _ -> subject.onNext(response) } }
    }
  }

  fun commit(targets: List<String>, version: Int, data: String): Observable<List<Bundle>> {
    val subject: Subject<Bundle> = PublishSubject.create()
    if(targets.isEmpty()) return Observable.just(listOf(bundleOf("success" to true)))
    val payload = bundleOf(
      "method" to "commit",
      "appGroup" to mAppGroup,
      "version" to version,
      "data" to data
    )
    return subject.buffer(targets.size).doOnSubscribe {
      targets.forEach { target -> send(target, context, payload) { response, _ -> subject.onNext(response) } }
    }
  }

}