package com.warebots.reactnative.ring

import androidx.core.os.bundleOf
import com.facebook.react.bridge.*

class Module(private val mContext: ReactApplicationContext) : ReactContextBaseJavaModule(mContext) {

  private var mDomain: String = ""

  override fun getName(): String {
    return "Ring"
  }

  @ReactMethod
  fun setup(domain: String, promise: Promise) {
    class Task() : GuardedAsyncTask<Void, Void>(reactApplicationContext) {
      override fun doInBackgroundGuarded(vararg params: Void?) {
        try {
          mDomain = domain
          val appGroup = Group.getForDomain(domain, mContext)
          val node = Node.getInstance(mContext, appGroup)
          node.initialize().subscribe { response ->
            val map = Arguments.fromBundle(response)
            promise.resolve(map)
          }
        } catch (e: Exception) {
            promise.reject(e)
        }
      }
    }
    Task().execute()
  }

  @ReactMethod
  fun write(version: Int, data: String, promise: Promise) {
    class Task() : GuardedAsyncTask<Void, Void>(reactApplicationContext) {
      override fun doInBackgroundGuarded(vararg params: Void?) {
        try {
          val appGroup = Group.getForDomain(mDomain, mContext)
          val node = Node.getInstance(mContext, appGroup)
          node.write(bundleOf("version" to version, "data" to data)).subscribe { response ->
            val map = Arguments.fromBundle(response)
            promise.resolve(map)
          }
        } catch (e: Exception) {
          promise.reject(e)
        }
      }
    }
    Task().execute()
  }

  @ReactMethod
  fun read(promise: Promise) {
    class Task() : GuardedAsyncTask<Void, Void>(reactApplicationContext) {
      override fun doInBackgroundGuarded(vararg params: Void?) {
        try {
          val appGroup = Group.getForDomain(mDomain, mContext)
          val node = Node.getInstance(mContext, appGroup)
          node.read().subscribe { response ->
            val map = Arguments.fromBundle(response)
            promise.resolve(map)
          }
        } catch (e: Exception) {
          promise.reject(e)
        }
      }
    }
    Task().execute()
  }

}
