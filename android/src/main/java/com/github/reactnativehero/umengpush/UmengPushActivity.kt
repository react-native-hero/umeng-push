package com.github.reactnativehero.umengpush

import android.content.Intent
import android.os.Bundle
import com.github.reactnativehero.umengpush.RNTUmengPushModule.Companion.handleMessage
import com.umeng.message.UmengNotifyClickActivity

open class UmengPushActivity : UmengNotifyClickActivity() {

    companion object {
        @JvmStatic var activityView: Int = 0
        @JvmStatic lateinit var mainActivityClass: Class<*>
    }

    override fun onCreate(p0: Bundle?) {
        super.onCreate(p0)
        if (activityView != 0) {
            setContentView(activityView)
        }
    }

    override fun onMessage(intent: Intent) {
        // 统计 【打开数】【收到数】【忽略数】
        super.onMessage(intent)
        handleMessage(this, mainActivityClass, intent)
    }

}
