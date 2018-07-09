package com.warebots.reactnative.ring

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import io.reactivex.Observable

class Follower(context: Context, appGroup: Group) : Node(context, appGroup) {

  override fun initialize(): Observable<Bundle> {
    return client.initialize(listOf(appGroup.leaderAppName)).map { it[0] }.doOnNext { leaderState ->
      val version = leaderState.getInt("version")
      val data = leaderState.getString("data")
      saveData(version, data)
    }
  }

  override fun read(): Observable<Bundle> {
    return if (!hasData()) {
      Observable.just(bundleOf("success" to false))
    } else {
      Observable.just(bundleOf(
        "success" to true,
        "version" to currentVersion(),
        "data" to currentData()
      ))
    }
  }

  override fun write(bundle: Bundle): Observable<Bundle> {
    val version = bundle.getInt("version")
    val data = bundle.getString("data")
    return client.write(listOf(appGroup.leaderAppName), version, data).map{ it[0] }
  }

}