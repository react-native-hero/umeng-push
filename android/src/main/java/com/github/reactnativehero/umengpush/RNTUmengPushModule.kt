package com.github.reactnativehero.umengpush

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.taobao.accs.utl.ALog
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.statistics.common.MLog
import com.umeng.message.*
import com.umeng.message.entity.UMessage
import com.umeng.message.tag.TagManager
import org.android.agoo.common.AgooConstants
import org.android.agoo.huawei.HuaWeiRegister
import org.android.agoo.mezu.MeizuRegister
import org.android.agoo.oppo.OppoRegister
import org.android.agoo.vivo.VivoRegister
import org.android.agoo.xiaomi.MiPushRegistar
import org.json.JSONObject

class RNTUmengPushModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {

        private const val ALIAS_TYPE_SINA = "sina"
        private const val ALIAS_TYPE_TENCENT = "tencent"
        private const val ALIAS_TYPE_QQ = "qq"
        private const val ALIAS_TYPE_WEIXIN = "weixin"
        private const val ALIAS_TYPE_BAIDU = "baidu"
        private const val ALIAS_TYPE_RENREN = "renren"
        private const val ALIAS_TYPE_KAIXIN = "kaixin"
        private const val ALIAS_TYPE_DOUBAN = "douban"
        private const val ALIAS_TYPE_FACEBOOK = "facebook"
        private const val ALIAS_TYPE_TWITTER = "twitter"

        private var appKey = ""
        private var pushSecret = ""
        private var channel = ""

        private var deviceToken = ""
        private var launchMessage: UMessage? = null
        private var pushModule: RNTUmengPushModule? = null

        private var isStarted = false
        private var isStartPending = false

        // 初始化友盟基础库
        @JvmStatic fun init(app: Application, metaData: Bundle, debug: Boolean) {

            appKey = metaData.getString("UMENG_APP_KEY", "").trim()
            pushSecret = metaData.getString("UMENG_PUSH_SECRET", "").trim()
            channel = metaData.getString("UMENG_CHANNEL", "").trim()

            ALog.isUseTlog = debug
            UMConfigure.setLogEnabled(debug)
            UMConfigure.preInit(app, appKey, channel)

        }

        // 初始化友盟推送
        @JvmStatic fun push(app: Application, resourcePackageName: String, notificationOnForeground: Boolean) {

            UMConfigure.init(app, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, pushSecret)

            val pushAgent = PushAgent.getInstance(app)

            // UMConfigure.setLogEnabled(debug) 会设值到 MLog.DEBUG
            if (MLog.DEBUG) {
                pushAgent.isPushCheck = true
            }

            // 自定义资源包名
            pushAgent.resourcePackageName = resourcePackageName

            // app 在前台时是否显示推送
            // 在 pushAgent.register 方法之前调用
            pushAgent.setNotificaitonOnForeground(notificationOnForeground)

            // 通知栏可以设置最多显示通知的条数
            // 当通知栏显示数目大于设置值，此时再有新通知到达时，会把旧的一条通知隐藏
            // 参数可以设置为0~10之间任意整数，当参数为 0 时，表示不合并通知
            pushAgent.displayNotificationNumber = 0

            // 默认情况下，同一台设备在1分钟内收到同一个应用的多条通知时，不会重复提醒，同时在通知栏里新的通知会替换掉旧的通知
            // 可以通过如下方法来设置冷却时间
            pushAgent.muteDurationSeconds = 1

            pushAgent.messageHandler = object : UmengMessageHandler() {
                override fun dealWithNotificationMessage(context: Context?, msg: UMessage?) {
                    super.dealWithNotificationMessage(context, msg)
                    pushModule?.let {
                        if (msg != null) {
                            it.onNotificationPresented(msg)
                        }
                    }
                }

                override fun dealWithCustomMessage(context: Context?, msg: UMessage?) {
                    pushModule?.let {
                        if (msg != null) {
                            it.onMessage(msg)
                        }
                    }
                }
            }

            // 自定义通知栏打开动作，让 js 去处理
            pushAgent.notificationClickHandler = object : UmengNotificationClickHandler() {
                override fun launchApp(context: Context?, msg: UMessage?) {
                    super.launchApp(context, msg)
                    msg?.let {
                        if (isStarted) {
                            pushModule?.onNotificationClicked(it)
                        }
                        else {
                            launchMessage = it
                        }
                    }
                }

                override fun dealWithCustomAction(context: Context?, msg: UMessage?) {
                    this.launchApp(context, msg)
                }
            }

            pushAgent.register(object : IUmengRegisterCallback {
                override fun onSuccess(token: String) {
                    deviceToken = token
                    if (isStartPending) {
                        pushModule?.start()
                    }
                }
                override fun onFailure(code: String, msg: String) {

                }
            })

        }

        @JvmStatic fun huawei(app: Application, metaData: Bundle) {
            HuaWeiRegister.register(app)
        }

        @JvmStatic fun xiaomi(app: Application, metaData: Bundle) {
            val appId = metaData.getString("XIAOMI_PUSH_APP_ID", "").trim()
            val appKey = metaData.getString("XIAOMI_PUSH_APP_KEY", "").trim()
            MiPushRegistar.register(app, appId, appKey)
        }

        @JvmStatic fun oppo(app: Application, metaData: Bundle) {
            val appKey = metaData.getString("OPPO_PUSH_APP_KEY", "").trim()
            val appSecret = metaData.getString("OPPO_PUSH_APP_SECRET", "").trim()
            OppoRegister.register(app, appKey, appSecret)
        }

        @JvmStatic fun vivo(app: Application, metaData: Bundle) {
            VivoRegister.register(app)
        }

        @JvmStatic fun meizu(app: Application, metaData: Bundle) {
            val appId = metaData.getString("MEIZU_PUSH_APP_ID", "").trim()
            val appKey = metaData.getString("MEIZU_PUSH_APP_KEY", "").trim()
            MeizuRegister.register(app, appId, appKey)
        }

        @JvmStatic fun handleMessage(currentActivity: Activity, nextActivityClass: Class<*>, intent: Intent?) {

            // 离线状态收到多条推送
            // 点击第一条会经此 activity 启动 app
            // 点击其他的依然会经此 activity，这里需要做区别

            // 跳转到 main activity
            intent?.getStringExtra(AgooConstants.MESSAGE_BODY)?.let {
                if (it.isNotEmpty()) {

                    try {
                        val msg = UMessage(JSONObject(it))

                        if (isStarted) {
                            pushModule?.onNotificationClicked(msg)
                        }
                        else {
                            launchMessage = msg
                        }
                    }
                    catch (e: Exception) {}

                }
            }

            val newIntent = Intent(currentActivity, nextActivityClass)
            newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
            currentActivity.startActivity(newIntent)
            currentActivity.overridePendingTransition(0, 0)
            currentActivity.finish()

        }

    }

    private var pushAgent = PushAgent.getInstance(reactContext)

    init {
        // 日活统计及多维度推送的必调用方法
        pushAgent.onAppStart()
    }

    override fun getName(): String {
        return "RNTUmengPush"
    }

    override fun getConstants(): Map<String, Any>? {

        val constants: MutableMap<String, Any> = HashMap()

        constants["NOTIFICATION_PLAY_SERVER"] = MsgConstant.NOTIFICATION_PLAY_SERVER
        constants["NOTIFICATION_PLAY_SDK_ENABLE"] = MsgConstant.NOTIFICATION_PLAY_SDK_ENABLE
        constants["NOTIFICATION_PLAY_SDK_DISABLE"] = MsgConstant.NOTIFICATION_PLAY_SDK_DISABLE

        constants["ALIAS_TYPE_SINA"] = ALIAS_TYPE_SINA
        constants["ALIAS_TYPE_TENCENT"] = ALIAS_TYPE_TENCENT
        constants["ALIAS_TYPE_QQ"] = ALIAS_TYPE_QQ
        constants["ALIAS_TYPE_WEIXIN"] = ALIAS_TYPE_WEIXIN
        constants["ALIAS_TYPE_BAIDU"] = ALIAS_TYPE_BAIDU
        constants["ALIAS_TYPE_RENREN"] = ALIAS_TYPE_RENREN
        constants["ALIAS_TYPE_KAIXIN"] = ALIAS_TYPE_KAIXIN
        constants["ALIAS_TYPE_DOUBAN"] = ALIAS_TYPE_DOUBAN
        constants["ALIAS_TYPE_FACEBOOK"] = ALIAS_TYPE_FACEBOOK
        constants["ALIAS_TYPE_TWITTER"] = ALIAS_TYPE_TWITTER

        return constants

    }

    override fun initialize() {
        super.initialize()
        pushModule = this
        isStarted = false
        isStartPending = false
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        pushModule = null
        isStarted = false
        isStartPending = false
    }

    @ReactMethod
    fun start() {

        if (deviceToken.isEmpty()) {
            isStartPending = true
            return
        }

        // 接收启动 app 的推送
        val map = Arguments.createMap()
        map.putString("deviceToken", deviceToken)

        // 启动参数
        launchMessage?.let {
            map.putMap("notification", formatNotification(it))
            map.putMap("custom", formatCustom(it))
            launchMessage = null
        }

        sendEvent("register", map)

        isStarted = true
        isStartPending = false

    }

    @ReactMethod
    fun getTags(promise: Promise) {

        pushAgent.tagManager.getTags { isSuccess, result ->
            if (isSuccess) {
                val map = Arguments.createMap()
                val list = Arguments.createArray()
                for (tag in result) {
                    list.pushString(tag)
                }
                map.putArray("tags", list)
                promise.resolve(map)
            }
            else {
                promise.reject("-1", "error")
            }
        }

    }

    @ReactMethod
    fun addTags(tags: ReadableArray, promise: Promise) {

        val list = ArrayList<String>()

        for (i in 0 until tags.size()) {
            tags.getString(i)?.let {
                list.add(it)
            }
        }

        pushAgent.tagManager.addTags(
            TagManager.TCallBack { isSuccess, result ->
                if (isSuccess) {
                    val map = Arguments.createMap()
                    result?.let {
                        map.putInt("remain", it.remain)
                    }
                    promise.resolve(map)
                } else {
                    promise.reject("-1", "error")
                }
            },
            *list.toTypedArray()
        )

    }

    @ReactMethod
    fun removeTags(tags: ReadableArray, promise: Promise) {

        val list = ArrayList<String>()

        for (i in 0 until tags.size()) {
            tags.getString(i)?.let {
                list.add(it)
            }
        }

        pushAgent.tagManager.deleteTags(
            TagManager.TCallBack { isSuccess, result ->
                if (isSuccess) {
                    val map = Arguments.createMap()
                    result?.let {
                        map.putInt("remain", it.remain)
                    }
                    promise.resolve(map)
                } else {
                    promise.reject("-1", "error")
                }
            },
            *list.toTypedArray()
        )

    }

    @ReactMethod
    fun setAlias(alias: String, type: String, promise: Promise) {

        pushAgent.setAlias(alias, getAliasType(type)) { isSuccess, _ ->
            if (isSuccess) {
                val map = Arguments.createMap()
                promise.resolve(map)
            }
            else {
                promise.reject("-1", "error")
            }
        }

    }

    @ReactMethod
    fun addAlias(alias: String, type: String, promise: Promise) {

        pushAgent.addAlias(alias, getAliasType(type)) { isSuccess, _ ->
            if (isSuccess) {
                val map = Arguments.createMap()
                promise.resolve(map)
            }
            else {
                promise.reject("-1", "error")
            }
        }

    }

    @ReactMethod
    fun removeAlias(alias: String, type: String, promise: Promise) {

        pushAgent.deleteAlias(alias, getAliasType(type)) { isSuccess, _ ->
            if (isSuccess) {
                val map = Arguments.createMap()
                promise.resolve(map)
            }
            else {
                promise.reject("-1", "error")
            }
        }

    }

    @ReactMethod
    fun setAdvanced(options: ReadableMap) {

        // 为免过度打扰用户，SDK默认在“23:00”到“7:00”之间收到通知消息时不响铃，不振动，不闪灯
        // 如果需要改变默认的静音时间，可以使用以下接口：
        if (options.hasKey("noDisturbStartHour")
            && options.hasKey("noDisturbStartMinute")
            && options.hasKey("noDisturbEndHour")
            && options.hasKey("noDisturbEndMinute")
        ) {
            pushAgent.setNoDisturbMode(
                options.getInt("noDisturbStartHour"),
                options.getInt("noDisturbStartMinute"),
                options.getInt("noDisturbEndHour"),
                options.getInt("noDisturbEndMinute")
            )
        }

        // 0: MsgConstant.NOTIFICATION_PLAY_SERVER
        // 1: MsgConstant.NOTIFICATION_PLAY_SDK_ENABLE
        // 2: MsgConstant.NOTIFICATION_PLAY_SDK_DISABLE

        // 是否响铃
        if (options.hasKey("notificationPlaySound")) {
            pushAgent.notificationPlaySound = options.getInt("notificationPlaySound")
        }

        // 是否点亮呼吸灯
        if (options.hasKey("notificationPlayLights")) {
            pushAgent.notificationPlayLights = options.getInt("notificationPlayLights")
        }

        // 是否振动
        if (options.hasKey("notificationPlayVibrate")) {
            pushAgent.notificationPlayVibrate = options.getInt("notificationPlayVibrate")
        }

    }

    private fun getAliasType(type: String): String {
        return when (type) {
            ALIAS_TYPE_SINA -> {
               "sina"
            }
            ALIAS_TYPE_TENCENT -> {
                "tencent"
            }
            ALIAS_TYPE_QQ -> {
                "qq"
            }
            ALIAS_TYPE_WEIXIN -> {
                "weixin"
            }
            ALIAS_TYPE_BAIDU -> {
                "baidu"
            }
            ALIAS_TYPE_RENREN -> {
                "renren"
            }
            ALIAS_TYPE_KAIXIN -> {
                "kaixin"
            }
            ALIAS_TYPE_DOUBAN -> {
                "douban"
            }
            ALIAS_TYPE_FACEBOOK -> {
                "facebook"
            }
            ALIAS_TYPE_TWITTER -> {
                "twitter"
            }
            else -> {
                type
            }
        }
    }

    private fun onNotificationPresented(message: UMessage) {

        val map = Arguments.createMap()
        map.putMap("notification", formatNotification(message))
        map.putMap("custom", formatCustom(message))
        map.putBoolean("presented", true)

        sendEvent("remoteNotification", map)

    }

    private fun onNotificationClicked(message: UMessage) {

        val map = Arguments.createMap()
        map.putMap("notification", formatNotification(message))
        map.putMap("custom", formatCustom(message))
        map.putBoolean("clicked", true)

        sendEvent("remoteNotification", map)

    }

    private fun onMessage(message: UMessage) {

        val map = Arguments.createMap()
        map.putString("message", message.custom)
        map.putMap("custom", formatCustom(message))

        sendEvent("message", map)

    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
    }

    private fun formatNotification(msg: UMessage): WritableMap {
        val body = Arguments.createMap()
        body.putString("title", msg.title)
        body.putString("content", msg.text)
        return body
    }

    private fun formatCustom(msg: UMessage): WritableMap {
        val custom = Arguments.createMap()
        msg.extra?.let {
            for ((key,value) in it) {
                custom.putString(key, value)
            }
        }
        return custom
    }

}
