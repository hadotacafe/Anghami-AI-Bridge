package com.hadota.anghamiaibridge

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View

class MainActivity : Activity() {
    private val bg = Color.rgb(22,18,15)
    private val panel = Color.rgb(39,31,25)
    private val gold = Color.rgb(222,180,92)
    private val cream = Color.rgb(246,238,222)
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var query: EditText

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    override fun onResume() { super.onResume(); if (::status.isInitialized) updateStatus() }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22,28,22,28) }
        scroll.addView(root); setContentView(scroll)
        root.addView(text("ANGHAMI  ×  CHATGPT",13,gold,true))
        root.addView(text("Anghami AI Bridge",28,cream,true))
        root.addView(text("مساعدك الموسيقي — تحكم فعلي عبر جلسة تشغيل Android",15,cream,false))
        status=text("",14,gold,true).apply { setPadding(14,14,14,14); setBackgroundColor(panel) }
        root.addView(status, margin(0,20,0,12)); updateStatus()
        root.addView(text("اطلب أغنية أو فنان",19,cream,true))
        query=EditText(this).apply { hint="مثال: عمرو دياب، موسيقى هادئة..."; setHintTextColor(Color.LTGRAY); setTextColor(cream); textSize=16f; setSingleLine(true); setPadding(14,12,14,12); setBackgroundColor(panel) }
        root.addView(query,margin(0,10,0,8))
        root.addView(button("ابحث وافتح نتائج Anghami") {
            val q=query.text.toString().trim()
            if(q.isEmpty()) toast("اكتب اسم أغنية أو فنان أولاً") else {
                result.text="تم فتح بحث Anghami عن: $q"
                openUrl("https://www.anghami.com/search/"+Uri.encode(q))
            }
        })
        root.addView(button("افتح تطبيق Anghami") { openAnghami() })
        root.addView(text("التحكم في التشغيل",19,cream,true).apply { setPadding(0,22,0,8) })
        root.addView(text("امنح صلاحية الوصول للإشعارات مرة واحدة، ثم شغّل Anghami. تعمل الأزرار إذا أتاح Anghami جلسة MediaSession للنظام.",13,Color.LTGRAY,false))
        root.addView(button("تفعيل صلاحية التحكم الإعلامي") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        val controls=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        listOf("السابق","تشغيل / إيقاف","التالي").forEachIndexed { index, label ->
            controls.addView(button(label) {
                when(index) { 0 -> sendMediaCommand { it.transportControls.skipToPrevious() }; 1 -> togglePlayback(); 2 -> sendMediaCommand { it.transportControls.skipToNext() } }
            },LinearLayout.LayoutParams(0,dp(52),1f).apply{setMargins(dp(3),dp(8),dp(3),0)})
        }
        root.addView(controls)
        root.addView(button("تحديث حالة المشغل") { updateStatus() })
        root.addView(text("نتيجة العملية",19,cream,true).apply{setPadding(0,22,0,8)})
        result=text("جاهز.",14,cream,false).apply{setPadding(14,14,14,14);setBackgroundColor(panel)}
        root.addView(result)
        root.addView(text("الحدود الحالية",19,cream,true).apply{setPadding(0,22,0,8)})
        root.addView(text("يعمل البحث وفتح Anghami. أزرار السابق/تشغيل/التالي ترسل أوامر Android MediaSession عند توفر جلسة متوافقة. لا يوجد اتصال فعلي بـ ChatGPT API حتى الآن.",12,Color.LTGRAY,false).apply{setPadding(0,8,0,4)})
    }

    private fun hasListenerPermission(): Boolean {
        val enabled = Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(ComponentName(this, BridgeNotificationListener::class.java).flattenToString())
    }

    private fun activeController(): MediaController? {
        if (!hasListenerPermission()) return null
        return try {
            val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = manager.getActiveSessions(ComponentName(this, BridgeNotificationListener::class.java))
            sessions.firstOrNull { it.packageName.contains("anghami", ignoreCase=true) }
                ?: sessions.firstOrNull()
        } catch (_: SecurityException) { null } catch (_: Exception) { null }
    }

    private fun sendMediaCommand(command: (MediaController) -> Unit) {
        val controller = activeController()
        if (controller == null) {
            result.text = if (!hasListenerPermission()) "فعّل صلاحية التحكم الإعلامي أولاً، ثم ارجع للتطبيق." else "لم يتم العثور على جلسة تشغيل نشطة. افتح Anghami وشغّل أغنية أولاً."
            return
        }
        try { command(controller); result.text = "تم إرسال الأمر إلى المشغل: " + controller.packageName }
        catch (_: Exception) { result.text = "تعذر إرسال الأمر إلى جلسة التشغيل الحالية." }
    }

    private fun togglePlayback() = sendMediaCommand { controller ->
        val state = controller.playbackState?.state
        if (state == android.media.session.PlaybackState.STATE_PLAYING || state == android.media.session.PlaybackState.STATE_BUFFERING)
            controller.transportControls.pause()
        else controller.transportControls.play()
    }

    private fun updateStatus() {
        val installed=try { packageManager.getPackageInfo("com.anghami",0); true } catch(_:Exception){false}
        val session = activeController()
        status.text = "Anghami: " + if(installed) "موجود على الجهاز ✓" else "غير مكتشف كتطبيق مثبت"
        status.append("\nصلاحية التحكم: " + if(hasListenerPermission()) "مفعّلة ✓" else "غير مفعّلة")
        status.append("\nجلسة التشغيل: " + (session?.packageName ?: "لا توجد جلسة نشطة"))
    }
    private fun openAnghami() {
        val launch = packageManager.getLaunchIntentForPackage("com.anghami")
        if (launch != null) startActivity(launch) else openUrl("https://www.anghami.com/")
    }
    private fun openUrl(url:String){ try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:ActivityNotFoundException){toast("تعذر فتح الرابط")}}
    private fun text(v:String,s:Int,c:Int,b:Boolean)=TextView(this).apply{text=v;textSize=s.toFloat();setTextColor(c);if(b)setTypeface(typeface,Typeface.BOLD);textDirection=View.TEXT_DIRECTION_FIRST_STRONG}
    private fun button(v:String,a:()->Unit)=Button(this).apply{text=v;textSize=13f;setTextColor(bg);setBackgroundColor(gold);isAllCaps=false;setOnClickListener{a()}}
    private fun margin(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun toast(v:String)=Toast.makeText(this,v,Toast.LENGTH_LONG).show()
}
