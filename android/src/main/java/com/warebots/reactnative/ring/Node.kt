package com.warebots.reactnative.ring

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.os.bundleOf
import io.reactivex.Observable
import java.io.File

enum class NodeMethod { INITIALIZE, WRITE, READ, ELECT }

abstract class Node(protected val context: Context, protected val appGroup: Group) {

  init {
    if (!appGroup.appNames.contains(context.packageName)) throw Exception("Ring node initialized from node outside of group.  Ensure the Android package name for this application begins with domain value passed in during setup.")
  }

  protected val client = RingClient(context, appGroup)

  companion object {
    fun getInstance(context: Context, appGroup: Group): Node {
      if (appGroup.leaderAppName == context.packageName) return Leader(context, appGroup)
      return Follower(context, appGroup)
    }
  }

  fun invokeMethod(payload: Bundle): Observable<Bundle> {
    val method = payload.getString("method")
    return when (method) {
      "initialize" -> initialize()
      "read" -> read()
      "commit" -> commit(payload)
      else -> write(payload)
    }
  }

  protected fun resetLeaderHistory() {
    with(mPreferences.edit()) {
      putBoolean("hasBeenLeader", false)
      apply()
    }
  }

  protected fun saveData(version: Int, data: String) {
    with(mPreferences.edit()) {
      putInt("version", version)
      apply()
    }
    if (!hasData()) {
      mDataFile.parentFile.mkdirs()
      mDataFile.createNewFile()
    }
    mDataFile.writeText(data)
  }

  protected fun hasData(): Boolean {
    return mDataFile.exists()
  }

  protected fun currentData(): String {
    return mDataFile.readText()
  }

  protected fun currentVersion(): Int {
    return mPreferences.getInt("version", -1)
  }

  protected fun hasBeenLeader(): Boolean {
    return mPreferences.getBoolean("hasBeenLeader", false)
  }

  protected fun leaderInitialized() {
    with(mPreferences.edit()) {
      putBoolean("hasBeenLeader", true)
      apply()
    }
  }

  protected fun unlock() {
    with(mPreferences.edit()) {
      putLong("locked", 0)
      apply()
    }
  }

  protected fun lock() {
    with(mPreferences.edit()) {
      putLong("locked", System.currentTimeMillis())
      apply()
    }
  }

  protected val locked: Boolean
    get() {
      return mPreferences.getLong("locked", 0) > 0
    }

  protected val lockTimedOut: Boolean
  get() {
    return System.currentTimeMillis() - mPreferences.getLong("locked", 0) > 5000
  }

  private fun commit(bundle: Bundle): Observable<Bundle> {
    resetLeaderHistory()
    val newVersion = bundle.getInt("version")
    val newData = bundle.getString("data")
    saveData(newVersion, newData)
    return Observable.just(bundleOf(
      "success" to true,
      "version" to newVersion,
      "data" to newData
    ))
  }

  private val mDataFile: File
    get() {
      return File(context.filesDir, "com.warebots.reactnative.ring/data.json")
    }

  private val mPreferences: SharedPreferences
    get() {
      return context.getSharedPreferences("com.warebots.reactnative.ring.preferences", Context.MODE_PRIVATE)
    }

  abstract fun initialize(): Observable<Bundle>
  abstract fun read(): Observable<Bundle>
  abstract fun write(bundle: Bundle): Observable<Bundle>
}

