package com.warebots.reactnative.ring

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.core.os.bundleOf
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import android.os.HandlerThread


data class Invocation(val result: Result, val intent: Intent = Intent())

class Result : BroadcastReceiver() {
  val subject = PublishSubject.create<Bundle>()
  override fun onReceive(context: Context?, intent: Intent?) {
    if (intent != null) subject.onNext(intent.extras)
    else subject.onNext(Bundle())
    subject.onComplete()
  }
}

class Client(val appGroup: Group, val context: Context) {

  fun invoke(groupName: GroupName, method: NodeMethod, payload: Bundle = Bundle()): Observable<Bundle> {

    val packageNames = when (groupName) {
      GroupName.LEADER -> listOf(appGroup.leaderAppName)
      else -> appGroup.followerAppNames
    }

    val invocations = packageNames.map {
      val intent = Intent()
      val result = Result()
      intent.component = ComponentName(it, "com.warebots.reactnative.ring.Receiver")
      intent.putExtra("group", appGroup)
      intent.putExtra("method", method)
      intent.putExtra("payload", payload)
      Invocation(result, intent)
    }

    val observables = invocations.map {
      it.result.subject.subscribeOn(Schedulers.newThread())
    }.toTypedArray()

    return Observable.merge(observables.toObservable())
      .subscribeOn(Schedulers.newThread())
      .take(packageNames.size.toLong())
      .doOnNext { result ->
        System.out.println(Thread.currentThread().name)
        System.out.println(result.toString())
      }
      .doOnSubscribe {
        invocations.forEach { invocation ->
          context.sendOrderedBroadcast(
            invocation.intent,
            null,
            invocation.result,
            null,
            Activity.RESULT_OK,
            null,
            bundleOf("success" to false)
          )
        }
      }
  }

}