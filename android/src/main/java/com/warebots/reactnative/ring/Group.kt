package com.warebots.reactnative.ring

import android.content.Context
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable

enum class GroupName { LEADER, FOLLOWERS }

data class Group(
  val installedAppNames: List<String>,
  val appNames: List<String>,
  val leaderAppName: String,
  val followerAppNames: List<String>,
  val remoteAppNames: List<String>,
  val remoteFollowerAppNames: List<String>
) : Parcelable {

  constructor(parcel: Parcel) : this(
    parcel.createStringArrayList(),
    parcel.createStringArrayList(),
    parcel.readString(),
    parcel.createStringArrayList(),
    parcel.createStringArrayList(),
    parcel.createStringArrayList())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeStringList(installedAppNames)
    parcel.writeStringList(appNames)
    parcel.writeString(leaderAppName)
    parcel.writeStringList(followerAppNames)
    parcel.writeStringList(remoteAppNames)
    parcel.writeStringList(remoteFollowerAppNames)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<Group> {
    override fun createFromParcel(parcel: Parcel): Group {
      return Group(parcel)
    }

    override fun newArray(size: Int): Array<Group?> {
      return arrayOfNulls(size)
    }

    fun getForDomain(domain: String, context: Context): Group {
      val installedPackages = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map { it.packageName }
      val appNames = when {
        domain.isEmpty() -> listOf()
        else -> installedPackages.filter { name -> name.startsWith(domain) }
      }.sortedBy {
        context.packageManager.getPackageInfo(it, 0).firstInstallTime
      }
      val masterAppName = appNames.first()
      val followerAppNames = appNames.filterNot { name -> name == masterAppName }
      val remoteAppNames = appNames.filterNot { name -> name == context.packageName }
      val remoteFollowerAppNames = followerAppNames.filterNot { name -> name == context.packageName }
      return Group(
        installedPackages,
        appNames,
        masterAppName,
        followerAppNames,
        remoteAppNames,
        remoteFollowerAppNames
      )
    }
  }

}