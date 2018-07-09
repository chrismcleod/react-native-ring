# Installation
yarn add react-native-ring
react-native link

## Required Manual Android Steps
ensure app package name has at least three parts (i.e. com.company.appname)
add manifest placeholder for permission to android in app/build.gradle
```
defaultConfig {
  ...
  manifestPlaceholders = [WBRingPermission: "com.company.apps.RING_PERMISSION"]
  ...
}
in javascript initialize with first part of domain:

```
import Ring from 'react-native-ring';

const ring = await Ring.create("com.company")
```

## Required Manual iOS Steps
ensure app has App Group capability enabled
ensure the app belongs to app group that corresponds to domain sent from create (omit the group.prefix)
