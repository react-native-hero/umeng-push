# @react-native-hero/umeng-push

## Getting started

Install the library using either Yarn:

```
yarn add @react-native-hero/umeng-push
```

or npm:

```
npm install --save @react-native-hero/umeng-push
```

## Link

- React Native v0.60+

For iOS, use `cocoapods` to link the package.

run the following command:

```
$ cd ios && pod install
```

For android, the package will be linked automatically on build.

- React Native <= 0.59

run the following command to link the package:

```
$ react-native link @react-native-hero/umeng-push
```

## Setup

![image](https://user-images.githubusercontent.com/2732303/77606227-ded8b680-6f51-11ea-9aa4-0378e79deaa7.png)

打开应用信息页面，安卓推送有 `Appkey` 和 `Umeng Message Secret` 两个字段，iOS 只有 `Appkey` 字段，后面将用这些字段初始化友盟。

### iOS

确保推送证书配置正确。举个例子，如果你的 App 有两个版本：测试版和正式版，`Bundle ID` 分别是 `com.abc.test` 和 `com.abc.prod`，那么证书必须和 `Bundle ID` 对应。

打开 Xcode，开启推送。

![image](https://user-images.githubusercontent.com/2732303/77887093-8fb9bb00-729c-11ea-8d71-8a97c1b6a3a2.png)

修改 `AppDelegate.m`，如下

```oc
#import <RNTUmengPush.h>

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  ...
  // 初始化友盟基础库
  // channel 一般填 App Store，如果有测试环境，可按需填写
  // debug 表示是否打印调试信息
  [RNTUmengPush init:@"appKey" channel:@"App Store" debug:false launchOptions:launchOptions];

  return YES;
}

- (void)application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
  [RNTUmengPush didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
  [RNTUmengPush didReceiveRemoteNotification:userInfo fetchCompletionHandler:completionHandler];
}
```

### Android

修改 `android/build.gradle`，如下：

```
allprojects {
    repositories {
        // 确保添加了华为和友盟仓库
        maven { url 'https://developer.huawei.com/repo/'}
        maven { url 'https://repo1.maven.org/maven2/' }
    }
}
```

`android/app/build.gradle` 根据不同的包填写不同的配置，如下：

```
android {
    buildTypes {
        debug {
            manifestPlaceholders = [
                UMENG_APP_KEY: '',
                UMENG_PUSH_SECRET: '',
                UMENG_CHANNEL: '',
                HUAWEI_PUSH_APP_ID: '',
                XIAOMI_PUSH_APP_ID: '',
                XIAOMI_PUSH_APP_KEY: '',
                OPPO_PUSH_APP_KEY: '',
                OPPO_PUSH_APP_SECRET: '',
                VIVO_PUSH_APP_ID: '',
                VIVO_PUSH_APP_KEY: '',
                MEIZU_PUSH_APP_ID: '',
                MEIZU_PUSH_APP_KEY: '',
            ]
        }
        release {
            manifestPlaceholders = [
                UMENG_APP_KEY: '',
                UMENG_PUSH_SECRET: '',
                UMENG_CHANNEL: '',
                HUAWEI_PUSH_APP_ID: '',
                XIAOMI_PUSH_APP_ID: '',
                XIAOMI_PUSH_APP_KEY: '',
                OPPO_PUSH_APP_KEY: '',
                OPPO_PUSH_APP_SECRET: '',
                VIVO_PUSH_APP_ID: '',
                VIVO_PUSH_APP_KEY: '',
                MEIZU_PUSH_APP_ID: '',
                MEIZU_PUSH_APP_KEY: '',
            ]
        }
    }
}
```

配置厂商通道请先阅读[官方文档](https://developer.umeng.com/docs/66632/detail/98589)，主要是获取各个通道的 `appId`、`appKey`、`appSecret` 等数据，并保存到友盟后台的应用信息里。

在 `MainApplication` 的 `onCreate` 方法进行初始化，如下：

```kotlin
override fun onCreate() {

    // 配置厂商通道 Activity 展示的页面
    UmengPushActivity.activityView = R.layout.splash_screen_default
    // 配置厂商通道 Activity 跳转的 mainActivity
    UmengPushActivity.mainActivityClass = MainActivity::class.java

    val metaData = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData

    // 初始化友盟基础库
    // 第三个参数表示是否显示调试信息
    RNTUmengPushModule.init(this, metaData, false)
}
```

### 配置混淆规则

在 `android/app/proguard-rules.pro` 添加以下混淆规则，注意替换自己的包名，并且删掉 `[` 和 `]`。

```
-keep public class [您的应用包名].R$*{
public static final int *;
}
```

### 配置华为、小米、魅族厂商通道使用的 Activity

`打开指定页面` 填入 `${包名}.UmengPushActivity`，比如 `com.abc.UmengPushActivity`。

![](https://user-images.githubusercontent.com/2732303/77288805-9764e700-6d13-11ea-91e1-3c2218f14bcb.png)

注意，如果在打包阶段用了别的包名，需改为对应的包名。

### 解决魅族的兼容问题

在 `drawable` 目录下添加一个图标，命名为 `stat_sys_third_app_notify.png`，建议尺寸 `64px * 64px`，图标四周留有透明。若不添加此图标，可能在部分魅族手机上无法弹出通知。

## Usage

```js
import {
  ALIAS_TYPE,
  NOTIFICATION_PLAY,
  init,
  start,
  getTags,
  addTags,
  removeTags,
  setAlias,
  addAlias,
  removeAlias,
  setAdvanced,
  addListener,
  supportDisable,
  enable,
  disable,
} from '@react-native-hero/push'

// 注册获取 device token
addListener(
  'register',
  function (data) {
    data.deviceToken
    // 如果 app 未启动状态下，点击推送打开 app，会有两个新字段
    // 点击的推送
    data.notification
    // 推送的自定义参数
    // 字符 d、p 为友盟保留字段，不能作为自定义参数的 key，value 只能是字符串类型，
    // 字符总和不能超过 1000 个字符
    data.custom
  }
)

// 远程推送
addListener(
  'remoteNotification',
  function (data) {
    // 如果点击了推送，data.clicked 是 true
    data.clicked
    // 如果推送送达并展示了，data.presented 是 true
    data.presented

    // 推送详情，如标题、内容
    data.notification
    // 推送的自定义参数
    // 字符 d、p 为友盟保留字段，不能作为自定义参数的 key，value 只能是字符串类型，
    // 字符总和不能超过 1000 个字符
    data.custom
  }
)

// 透传消息
// ios 通过静默推送实现，例子：payload: { aps: { 'content-available': 1 }, key1: 'value1' }
// android 通过自定义消息实现，例子：payload: { display_type: 'message', body: { custom: '' }, extra: { key1: 'value1' } }
//
// 注意：ios aps 下面不能包含 alert、badge、sound 等字段，如果包含了其中任何一个，就会变成通知，即会显示在通知栏。
// 你也可以加上 alert，比如 aps: { alert: '你有一条新消息', 'content-available': 1 }，这样还是会走进 message 事件回调里，只是展现为通知形式
addListener(
  'message',
  function (data) {

    // ios 用于静默推送实现消息透传
    // 静默推送不会在通知栏展现，这一点很符合透传消息的要求
    // 但是苹果要求静默推送的 aps 对象下不能包含 alert、badge、sound 等字段
    // 如果我们加了 alert，它就会变成通知，会展现在通知栏里
    // 有时候，你会希望加上 alert，类似 aps: { alert: '你有一条新消息', 'content-available': 1 }
    // 这时依然会触发 message 事件回调，只是会带上 remoteNotification 特有的 clicked 或 presented 属性
    // 如果你希望和安卓表现一样，只需要过滤掉 clicked 即可，如下
    if (data.clicked) {
      return
    }

    // 这样只有当 app 在前台时，才会响应透传消息

    // 自定义参数
    // 字符 d、p 为友盟保留字段，不能作为自定义参数的 key，value 只能是字符串类型，
    // 字符总和不能超过 1000 个字符
    // 即例子中的 { key1: 'value1' }
    data.custom

    // alert 或 custom 字段中的字符串值
    data.message

  }
)

// 对于安卓来说，需要等用户同意隐私政策后，再调用 init，js 的 init 才是真正的初始化
// https://developer.umeng.com/docs/67966/detail/207155
// 对于 ios 来说，如果无需使用『高级设置』，则可以不调用 init 方法
init({
  // 『安卓』app 在前台时是否显示推送
  notificationOnForeground: true,
  // 『安卓』填 AndroidManifest.xml 中 package 的值
  resourcePackageName: 'com.abc',
  // 『安卓』开启厂商推送通道，按需开启即可
  huaweiEnabled: true,
  xiaomiEnabled: true,
  oppoEnabled: true,
  vivoEnabled: true,
  meizuEnabled: true,
  // 文档最后 『高级设置』中的所有配置项都可以适用于 init，方便在初始化时一次性做好配置
})

// 注意！！！
// 下面的所有方法需在 init 调用之后才可调用

// 启动
// 调用 start 之后才会触发 register 事件
// 因此注册 register 事件应早于 start
start()

// 下面这些方法的具体用法和注意事项，请参考文档
// https://developer.umeng.com/docs/67966/detail/98583#h1--tag-alias-4
getTags().then(data => {
  // success
  data.tags
})
.catch(err => {
  // failure
})

addTags(['tag1', 'tag2']).then(data => {
  // success
  data.remain
})
.catch(err => {
  // failure
})

removeTags(['tag1', 'tag2']).then(data => {
  // success
  data.remain
})
.catch(err => {
  // failure
})


// type 如果是第三方帐号，使用导入的常量 ALIAS_TYPE.XXX
// 如果是自建帐号体系，可传入自己产品的英文名或拼音
setAlias('alias', 'type').then(data => {
  // success
})
.catch(err => {
  // failure
})

addAlias('alias', 'type').then(data => {
  // success
})
.catch(err => {
  // failure
})

removeAlias('alias', 'type').then(data => {
  // success
})
.catch(err => {
  // failure
})

// 高级设置
setAdvanced({
  // ios: 当应用在前台时收到推送是否弹出 Alert，默认弹出
  autoAlert: true,
  // ios: 是否允许 SDK 自动清空角标，默认自动角标清零
  badgeClear: true,

  // android: 最多显示通知的条数，当显示数目大于设置值时，若再有新通知到达，会移除一条最早的通知，只为 0-10，0 表示不限制个数
  displayNotificationNumber: 0,
  // android: 默认情况下，同一台设备在1分钟内收到同一个应用的多条通知时，不会重复提醒，同时在通知栏里新的通知会替换掉旧的通知，可以通过此配置来设置冷却时间
  muteDurationSeconds: 60,
  // android: 是否响铃，使用 NOTIFICATION_PLAY 枚举值
  notificationPlaySound: NOTIFICATION_PLAY.SERVER,
  // android: 是否点亮呼吸灯，使用 NOTIFICATION_PLAY 枚举值
  notificationPlayLights: NOTIFICATION_PLAY.SERVER,
  // android: 是否振动，使用 NOTIFICATION_PLAY 枚举值
  notificationPlayVibrate: NOTIFICATION_PLAY.SERVER,
  // android: 设置免打扰时间段，期间收到通知消息时不响铃，不闪灯，不振动
  noDisturbStartHour: 23,
  noDisturbStartMinute: 0,
  noDisturbEndHour: 7,
  noDisturbEndMinute: 0,
})


// 是否支持关闭推送
// 初始化 SDK 后，默认是开启推送状态，如果需要开关，可借鉴以下代码
if (supportDisable) {
  // 关闭推送
  disable()
  // 开启推送
  enable()
}

```