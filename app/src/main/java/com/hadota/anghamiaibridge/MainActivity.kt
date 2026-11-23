package com.hadota.anghamiaibridge

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup

class MainActivity : Activity() {
    private val bg = Color.rgb(22,18,15)
    private val panel = Color.rgb(39,31,25)
    private val gold = Color.rgb(222,180,92)
    private val cream = Color.rgb(246,238,222)
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var query: EditText

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22,28,22,28) }
        scroll.addView(root); setContentView(scroll)
        root.addView(text("ANGHAMI  ×  CHATGPT",13,gold,true))
        root.addView(text("Anghami AI Bridge",28,cream,true))
        root.addView(text("مساعدك الموسيقي — نسخة تجريبية Android",15,cream,false))
        status=text("",14,gold,true).apply { setPadding(14,14,14,14); setBackgroundColor(panel) }
        root.addView(status, margin(0,20,0,12)); updateStatus()
        root.addView(text("اطلب أغنية أو فنان",19,cream,true))
        query=EditText(this).apply { hint="مثال: عمرو دياب، موسيقى هادئة..."; setHintTextColor(Color.LTGRAY); setTextColor(cream); textSize=16f; setSingleLine(true); setPadding(14,12,14,12); setBackgroundColor(panel) }
        root.addView(query,margin(0,10,0,8))
        root.addView(button("ابحث وافتح نتائج Anghami") {
            val q=query.text.toString().trim()
            if(q.isEmpty()) toast("اكتب اسم أغنية أو فنان أولاً") else {
                result.text="سيتم فتح بحث Anghami عن: $q\nالبحث يعتمد على دعم الموقع للرابط."
                openUrl("https://www.anghami.com/search/"+Uri.encode(q))
            }
        })
        root.addView(button("افتح Anghami") { openUrl("https://www.anghami.com/") })
        root.addView(text("التحكم في التشغيل",19,cream,true).apply { setPadding(0,22,0,8) })
        root.addView(text("أزرار تجريبية؛ لا يوجد تحكم فعلي قبل اكتشاف Media Session متوافقة.",13,Color.LTGRAY,false))
        val controls=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        listOf("⏮","▶ / ⏸","⏭").forEach { label -> controls.addView(button(label){toast("التحكم غير متاح بعد: لم يتم اكتشاف جلسة تشغيل متوافقة.")},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(4),dp(8),dp(4),0)}) }
        root.addView(controls)
        root.addView(text("نتيجة العملية",19,cream,true).apply{setPadding(0,22,0,8)})
        result=text("لا توجد عمليات بعد.",14,cream,false).apply{setPadding(14,14,14,14);setBackgroundColor(panel)}
        root.addView(result)
        root.addView(text("الفحص والتشخيص",19,cream,true).apply{setPadding(0,22,0,8)})
        root.addView(button("تحديث حالة تثبيت Anghami"){updateStatus()})
        root.addView(button("إعدادات إشعارات الجهاز"){startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))})
        root.addView(button("معلومات النسخة والحدود"){result.text="Beta 0.1.0\nيعمل: فتح Anghami وفتح رابط البحث.\nغير متاح بعد: OpenAI API والتحكم المباشر وإدارة قوائم الحساب.\nلا تدخل كلمة مرور Anghami هنا."})
        root.addView(text("الخصوصية: لا يطلب التطبيق كلمة مرور Anghami ولا يرسل بياناتك إلى خادم. لا يوجد اتصال فعلي بـ OpenAI في هذه النسخة.",12,Color.LTGRAY,false).apply{setPadding(0,18,0,4)})
    }

    private fun updateStatus() {
        val installed=try { packageManager.getPackageInfo("com.anghami",0); true } catch(_:Exception){false}
        status.text="Anghami: "+if(installed) "موجود على الجهاز ✓" else "غير مكتشف — سيتم فتح نسخة الويب عند الطلب"
    }
    private fun openUrl(url:String){ try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:ActivityNotFoundException){toast("تعذر فتح الرابط")}}
    private fun text(v:String,s:Int,c:Int,b:Boolean)=TextView(this).apply{text=v;textSize=s.toFloat();setTextColor(c);if(b)setTypeface(typeface,Typeface.BOLD);textDirection=View.TEXT_DIRECTION_FIRST_STRONG}
    private fun button(v:String,a:()->Unit)=Button(this).apply{text=v;textSize=14f;setTextColor(bg);setBackgroundColor(gold);isAllCaps=false;setOnClickListener{a()}}
    private fun margin(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun toast(v:String)=Toast.makeText(this,v,Toast.LENGTH_LONG).show()
}
