package com.example.paneltest

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class MainActivity : Activity() {
    private val token: IBinder = Binder()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HiddenApiBypass.addHiddenApiExemptions("L")
        status = TextView(this).apply { textSize = 18f; text = "Ready" }
        val block = Button(this).apply {
            text = "Block panel"; setOnClickListener { setFlags(true) }
        }
        val unblock = Button(this).apply {
            text = "Unblock panel"; setOnClickListener { setFlags(false) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 120, 48, 48)
            addView(status); addView(block); addView(unblock)
        })
    }

    private fun setFlags(block: Boolean) {
        try {
            if (!Shizuku.pingBinder()) { status.text = "Shizuku not running"; return }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1)
                status.text = "Allow permission, then tap again"; return
            }
            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("statusbar"))
            val stub = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            val svc = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
            val int = Int::class.javaPrimitiveType
            svc.javaClass.getMethod("disable", int, IBinder::class.java, String::class.java)
                .invoke(svc, if (block) 0x10000 else 0, token, "com.android.shell")
            svc.javaClass.getMethod("disable2", int, IBinder::class.java, String::class.java)
                .invoke(svc, if (block) 5 else 0, token, "com.android.shell")
            status.text = if (block) "Blocked. Keep this app open." else "Unblocked"
        } catch (e: Throwable) {
            status.text = "Error: " + (e.cause ?: e)
        }
    }
}
