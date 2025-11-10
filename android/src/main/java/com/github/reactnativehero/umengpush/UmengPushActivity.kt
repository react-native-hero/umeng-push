package com.github.reactnativehero.umengpush

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.github.reactnativehero.umengpush.RNTUmengPushModule.Companion.handleMessage
import com.umeng.message.UmengNotifyClick
import com.umeng.message.entity.UMessage
open class UmengPushActivity : Activity() {

    companion object {
        @JvmStatic var activityView: Int = 0
        @JvmStatic lateinit var mainActivityClass: Class<*>
    }

    private val mNotificationClick: UmengNotifyClick = object : UmengNotifyClick() {
        override fun onMessage(msg: UMessage) {
            val body = msg.raw.toString()
            if (!TextUtils.isEmpty(body)) {
                handleMessage(this@UmengPushActivity, mainActivityClass, msg)
            }
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        if (activityView != 0) {
            setContentView(activityView)
        }
        mNotificationClick.onCreate(this, intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        mNotificationClick.onNewIntent(intent)
    }

}
