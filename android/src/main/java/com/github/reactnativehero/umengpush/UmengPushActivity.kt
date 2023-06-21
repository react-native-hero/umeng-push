package com.github.reactnativehero.umengpush

import android.content.Intent
import android.os.Bundle
import com.github.reactnativehero.umengpush.RNTUmengPushModule.Companion.handleMessage
import com.umeng.message.UmengNotifyClickActivity
import com.umeng.message.entity.UMessage
import org.android.agoo.common.AgooConstants
import org.json.JSONObject

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

    override fun onMessage(p0: UMessage?) {
        super.onMessage(p0)
        handleMessage(this, mainActivityClass, p0)
    }

    override fun onMessage(intent: Intent?) {
        // 统计 【打开数】【收到数】【忽略数】
        super.onMessage(intent)

        var msg: UMessage? = null
        intent?.getStringExtra(AgooConstants.MESSAGE_BODY)?.let {
            if (it.isNotEmpty()) {
                try {
                    msg = UMessage(JSONObject(it))
                }
                catch (e: Exception) {
                }
            }
        }
        handleMessage(this, mainActivityClass, msg)
    }

}
