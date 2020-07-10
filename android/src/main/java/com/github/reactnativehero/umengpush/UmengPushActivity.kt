package com.github.reactnativehero.umengpush

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import com.umeng.message.UmengNotifyClickActivity

// com.github.reactnativehero.umengpush.UmengPushActivity 要填写到友盟后台系统通道
class UmengPushActivity : UmengNotifyClickActivity() {

    companion object {
        lateinit var mainActivityClass: Class<*>
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.umeng_push_activity)
    }

    override fun onMessage(intent: Intent?) {

        // 统计 【打开数】【收到数】【忽略数】
        super.onMessage(intent)

        RNTUmengPushModule.handleMessage(this, mainActivityClass, intent)

    }

}