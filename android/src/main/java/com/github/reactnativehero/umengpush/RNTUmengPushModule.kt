package com.github.reactnativehero.umengpush

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.statistics.common.MLog
import com.umeng.commonsdk.utils.UMUtils
import com.umeng.message.MsgConstant
import com.umeng.message.PushAgent
import com.umeng.message.UmengMessageHandler
import com.umeng.message.UmengNotificationClickHandler
import com.umeng.message.api.UPushRegisterCallback
import com.umeng.message.api.UPushSettingCallback
import com.umeng.message.entity.UMessage
import com.umeng.message.tag.TagManager
import org.android.agoo.common.AgooConstants
import org.android.agoo.huawei.HuaWeiRegister
import org.android.agoo.honor.HonorRegister
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

        // 友盟初始化参数
        private var appKey = ""
        private var pushSecret = ""
        private var channel = ""

        // 小米初始化参数
        private var xiaomiAppId = ""
        private var xiaomiAppKey = ""

        // oppo 初始化参数
        private var oppoAppKey = ""
        private var oppoAppSecret = ""

        // 魅族初始化参数
        private var meizuAppId = ""
        private var meizuAppKey = ""

        private var launchMessage: UMessage? = null
        private var pushModule: RNTUmengPushModule? = null

        private var isStarted = false
        private var isStartPending = false

        private var jsInitOptions: ReadableMap? = null

        // 初始化友盟基础库
        @JvmStatic fun init(app: Application, metaData: Bundle, debug: Boolean) {

            appKey = metaData.getString("UMENG_APP_KEY", "").trim()
            pushSecret = metaData.getString("UMENG_PUSH_SECRET", "").trim()
            channel = metaData.getString("UMENG_CHANNEL", "").trim()

            xiaomiAppId = metaData.getString("XIAOMI_PUSH_APP_ID", "").trim()
            xiaomiAppKey = metaData.getString("XIAOMI_PUSH_APP_KEY", "").trim()

            oppoAppKey = metaData.getString("OPPO_PUSH_APP_KEY", "").trim()
            oppoAppSecret = metaData.getString("OPPO_PUSH_APP_SECRET", "").trim()

            meizuAppId = metaData.getString("MEIZU_PUSH_APP_ID", "").trim()
            meizuAppKey = metaData.getString("MEIZU_PUSH_APP_KEY", "").trim()

            UMConfigure.setLogEnabled(debug)

            // 解决推送消息显示乱码的问题
            PushAgent.setup(app, appKey, pushSecret)
            UMConfigure.preInit(app, appKey, channel)

            jsInitOptions?.let {
                pushModule?.initSDKByProcess(app, it)
            }

        }

        @JvmStatic fun handleMessage(currentActivity: Activity, nextActivityClass: Class<*>, intent: Intent?) {

            // 离线状态收到多条推送
            // 点击第一条会经此 activity 启动 app
            // 点击其他的依然会经此 activity，这里需要做区别

            // 跳转到 main activity
            val body = intent?.getStringExtra(AgooConstants.MESSAGE_BODY)

            body?.let {
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
                    catch (e: Exception) {
                    }

                }
            }

            val newIntent = Intent(currentActivity, nextActivityClass)
            newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
            currentActivity.startActivity(newIntent)
            currentActivity.overridePendingTransition(0, 0)
            currentActivity.finish()

        }

    }

    private lateinit var pushAgent: PushAgent
    private var deviceToken = ""

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
        jsInitOptions = null
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        pushModule = null
        isStarted = false
        isStartPending = false
        jsInitOptions = null
    }

    @ReactMethod
    fun init(options: ReadableMap) {
        val context = reactContext.applicationContext as Application
        initSDKByProcess(context, options)
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
        setPushSetting(pushAgent, options)
    }

    @ReactMethod
    fun enable(promise: Promise) {
        pushAgent.enable(object : UPushSettingCallback {
            override fun onSuccess() {
                val map = Arguments.createMap()
                promise.resolve(map)
            }
            override fun onFailure(var1: String?, var2: String?) {
                promise.reject("-1", var1)
            }
        })
    }

    @ReactMethod
    fun disable(promise: Promise) {
        pushAgent.disable(object : UPushSettingCallback {
            override fun onSuccess() {
                val map = Arguments.createMap()
                promise.resolve(map)
            }
            override fun onFailure(var1: String?, var2: String?) {
                promise.reject("-1", var1)
            }
        })
    }

    private fun initSDKByProcess(context: Application, options: ReadableMap) {
        if (UMUtils.isMainProgress(reactContext)) {
            Thread {
                initSDK(context, options)
            }.start()
        }
        else {
            initSDK(context, options)
        }
    }

    private fun initSDK(context: Application, options: ReadableMap) {

        UMConfigure.submitPolicyGrantResult(reactContext, true)
        UMConfigure.init(context, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, pushSecret)

        pushAgent = PushAgent.getInstance(context)

        // UMConfigure.setLogEnabled(debug) 会设值到 MLog.DEBUG
        if (MLog.DEBUG) {
            pushAgent.isPushCheck = true
        }

        pushAgent.displayNotificationNumber = 0

        // 自定义资源包名
        if (options.hasKey("resourcePackageName")) {
            pushAgent.resourcePackageName = options.getString("resourcePackageName")
        }

        // app 在前台时是否显示推送
        // 在 pushAgent.register 方法之前调用
        var notificationOnForeground = true
        if (options.hasKey("notificationOnForeground")) {
            notificationOnForeground = options.getBoolean("notificationOnForeground")
        }
        pushAgent.notificationOnForeground = notificationOnForeground

        setPushSetting(pushAgent, options)

        pushAgent.messageHandler = object : UmengMessageHandler() {
            override fun dealWithNotificationMessage(context: Context?, msg: UMessage?) {
                super.dealWithNotificationMessage(context, msg)
                if (msg != null) {
                    onNotificationPresented(msg)
                }
            }

            override fun dealWithCustomMessage(context: Context?, msg: UMessage?) {
                if (msg != null) {
                    onMessage(msg)
                }
            }
        }

        // 自定义通知栏打开动作，让 js 去处理
        pushAgent.notificationClickHandler = object : UmengNotificationClickHandler() {
            override fun launchApp(context: Context?, msg: UMessage?) {
                super.launchApp(context, msg)
                if (msg != null) {
                    if (isStarted) {
                        onNotificationClicked(msg)
                    }
                    else {
                        launchMessage = msg
                    }
                }
            }

            override fun dealWithCustomAction(context: Context?, msg: UMessage?) {
                this.launchApp(context, msg)
            }
        }

        pushAgent.register(object : UPushRegisterCallback {
            override fun onSuccess(token: String) {
                deviceToken = token
                if (isStartPending) {
                    start()
                }
            }

            override fun onFailure(errCode: String, errDesc: String) {

            }
        })

        // 日活统计及多维度推送的必调用方法
        pushAgent.onAppStart()

        if (options.hasKey("huaweiEnabled") && options.getBoolean("huaweiEnabled")) {
            HuaWeiRegister.register(context)
        }
        if (options.hasKey("honorEnabled") && options.getBoolean("honorEnabled")) {
            HonorRegister.register(context)
        }
        if (options.hasKey("xiaomiEnabled") && options.getBoolean("xiaomiEnabled")) {
            MiPushRegistar.register(context, xiaomiAppId, xiaomiAppKey)
        }
        if (options.hasKey("oppoEnabled") && options.getBoolean("oppoEnabled")) {
            OppoRegister.register(context, oppoAppKey, oppoAppSecret)
        }
        if (options.hasKey("vivoEnabled") && options.getBoolean("vivoEnabled")) {
            VivoRegister.register(context)
        }
        if (options.hasKey("meizuEnabled") && options.getBoolean("meizuEnabled")) {
            MeizuRegister.register(context, meizuAppId, meizuAppKey)
        }

        jsInitOptions = options

    }

    private fun setPushSetting(pushAgent: PushAgent, options: ReadableMap) {

        // 设置显示通知的数量
        // 可以设置最多显示通知的条数，当显示数目大于设置值时，若再有新通知到达，会移除一条最早的通知
        // 值为 0~10，为 0 时，表示不限制显示个数
        if (options.hasKey("displayNotificationNumber")) {
            val value = options.getInt("displayNotificationNumber")
            if (value in 0..10) {
                pushAgent.displayNotificationNumber = value
            }
        }

        // 默认情况下，同一台设备在1分钟内收到同一个应用的多条通知时，不会重复提醒，同时在通知栏里新的通知会替换掉旧的通知
        // 可以通过如下方法来设置冷却时间
        if (options.hasKey("muteDurationSeconds")) {
            pushAgent.muteDurationSeconds = options.getInt("muteDurationSeconds")
        }

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
