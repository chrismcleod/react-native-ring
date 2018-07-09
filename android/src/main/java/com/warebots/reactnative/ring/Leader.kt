package com.warebots.reactnative.ring

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import io.reactivex.Observable

class Leader(context: Context, appGroup: Group) : Node(context, appGroup) {

  override fun initialize(): Observable<Bundle> {
    return read()
  }

  override fun read(): Observable<Bundle> {

    val data: String
    val version: Int

    if (!hasData()) {
      resetLeaderHistory()
      data = "{}"
      version = -1
    } else {
      data = currentData()
      version = currentVersion()
    }

    val bundle = bundleOf(
      "success" to true,
      "version" to version,
      "data" to data
    )

    return if (!hasBeenLeader()) {
      client.read(appGroup.followerAppNames)
        .map { results ->
          val mutableResults = results.toMutableList()
          mutableResults.add(bundle)
          mutableResults.filter { it.containsKey("version") }.maxBy { it.getInt("version") }!!
        }.take(1).doOnNext {
          val newVersion = it.getInt("version")
          val newData = it.getString("data")
          saveData(newVersion, newData)
          client.commit(appGroup.followerAppNames, newVersion, newData).subscribe()
        }
    } else {
      Observable.just(bundle)
    }
  }

  override fun write(bundle: Bundle): Observable<Bundle> {
    val newVersion = bundle.getInt("version")
    val newData = bundle.getString("data")
    saveData(newVersion + 1, newData)
    client.commit(appGroup.followerAppNames, newVersion + 1, newData).subscribe()
    return Observable.just(bundleOf("success" to true, "version" to newVersion + 1, "data" to newData))
  }

}
