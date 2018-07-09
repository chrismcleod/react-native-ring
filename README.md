# Installation
```bash
yarn add react-native-ring
react-native link
```

## Required Manual Android Steps
- Ensure app package name has at least three parts (i.e. com.company.appname)
- Add manifest placeholder for unique ring permission to android in app/build.gradle.
  This permission ensures no other compnents can send broadcasts to your app and receive
  the data back.

  ```gradle
  android {
    ...
    defaultConfig {
      ...
      manifestPlaceholders = [WBRingPermission: "com.company.apps.RING_PERMISSION"]
      ...
    }
    ...
  }
  ```

## Required Manual iOS Steps
- Ensure app has App Group capability enabled
- Ensure the app belongs to App Group that corresponds to domain sent from create (omit the **group.** prefix)

# Usage
## Javascript
- In javascript initialize with first part of domain (this corresponds to Android package name and iOS App Group name):

  ```javascript
  import Ring from 'react-native-ring';

  const ring = await Ring.create("com.company")
  await ring.setItem("mykey", "myvalue");
  console.log(await ring.getItem("mykey"));

  /** or **/
  await ring.multiSet([
    ["mykey", "myvalue"],
    ["myOtherKey", {hello: "world"}]
  ]);
  console.log(await ring.multiGet(["mykey", "myOtherKey"]));

  ```

# Notes
- The interface is almost exactly the same as AsyncStorage.  The main difference is this lets items be any value, not just strings. Typescript definitions are included, so explore those to get a feel for the api.
- You can add listeners for changes in state from other apps.  These listeners will fire if the data has changed when your goes from not being active to active.  (i.e. when your app is in the background and becomes the active app)
- Due to the isolated nature of Android data files, this module uses several broadcasts for every call to setup and write to keep data in sync across apps.  Because of this, it is recommeded to avoid heavy (moderate?) writing and only store shared data when sharing is the ONLY way for a feature to work. (i.e. SSO)
- Broadcasting could be avoided if all your apps have the same `sharedUserId` declared in the manifest.  In that case, a module could be created to directly modify files in those apps rather than sending broadcasts.  This module does not do that, but could be a foundation for an implementation that would.  The only problem with that is if you have released apps to Play Store that did not have a `sharedUserId`, users would have to uninstall their current app and install your update.  Simply upgrading seems to break the app.  Also, you lose all your app history in the Play Store when you do this (i.e. ratings etc).
- Why Not Use a Provider?  Using a Provider is somewhat of an implementation detail. However, most of the Android documentation seemed to suggest a single app be the provider that all other apps use.  This module does not care which app in your group of related apps exists.  The first one installed becomes the leader.  If this leader is uninstalled, the next earliest installed becomse the leader.  This is what is meant by "decentralized". The Android algorithm **HOPEFULLY** ensures that all apps in the group have the state of the leader.  Basically this is a failover cluster.
- Why Not Use Shared Preferences / User Defaults / Shared Keychain?  Android shared prefs cannot be shared across apps as far as I could tell.  iOS App Groups can share user defaults, but if we want an arbitraily large data object, it seemed safer to use files.  Shared keychain might work, but again worried about data size.
- Right now this module is experimental and not hardened in a production environment. There are 0 tests for native and javascript code.  **These will be added later™**. In order to mitigate potentially irrecoverable errors, you should monitor this module for errors in your app, and if the number of them crosses a threshold, use AsyncStorage instead.  Granted your data wont be shared, but it's better than not working at all!
  ```javascript
  const errorCount = 0;
  const sharedStorage = await Ring.create(...);
  try {
    await sharedStorage.setItem("mykey", "value");
  } catch(error) {
    errorCount += 1;
    if(errorCount > 30) sharedStorage = AsyncStorage;
  }
  ```

# Roadmap
- Add option to enable storing data as encrypted string
