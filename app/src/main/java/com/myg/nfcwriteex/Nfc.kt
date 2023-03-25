package com.myg.nfcwriteex

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

//interface Decorator {
//    fun decorate(view: View)
//}
//
//class DecoratorA : Decorator {
//    override fun decorate(view: View) {
//        TODO("Not yet implemented")
//    }
//}
//
//class MyView(
//    val decorator: Decorator,
//): View() {
//    override fun onDraw(canvas: Canvas?) {
//        super.onDraw(canvas)
//
//        decorator.decorate(this)
//    }
//}

interface Nfc : DefaultLifecycleObserver {
    val isEnabled: Boolean

    fun use(action: (adapter: NfcAdapter)->Unit)

    companion object {
        fun get(activity: ComponentActivity, isTest: Boolean = false): Nfc {
            val nfcDefault = if(isTest){
                NfcTest(activity)
            } else {
                NfcDefault(activity)
            }

            return nfcDefault
        }
    }

    private class NfcTest(
        activity: ComponentActivity
    ) : Nfc {
        override val isEnabled: Boolean
            get() = TODO("Not yet implemented")

        override fun use(action: (adapter: NfcAdapter) -> Unit) {
            TODO("Not yet implemented")
        }
    }

    private class NfcDefault(
        activity: ComponentActivity
    ) : Nfc {
        init {
            activity.lifecycle.addObserver(this)
        }

        private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(
            activity
        ) ?: let {
            Toast.makeText(activity, "NFC not supported on this device", Toast.LENGTH_SHORT).show()
            null
        }

        private lateinit var pendingIntent: PendingIntent

        override val isEnabled: Boolean
            get() = nfcAdapter?.isEnabled ?: false

        fun getActivity(owner: LifecycleOwner): ComponentActivity? {
            if(owner !is ComponentActivity) {
                return null
            }
            return owner
        }

        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)


            val activity = getActivity(owner) ?: return

            pendingIntent = PendingIntent.getActivity(
                activity,
                0,
                Intent(activity, javaClass).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                0
            )
        }

        override fun onResume(owner: LifecycleOwner) {
            val activity = getActivity(owner) ?: return

            val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
            val techList = arrayOf(arrayOf(Ndef::class.java.name))

            use {
                it.enableForegroundDispatch(activity, pendingIntent, filters, techList)
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            val activity = getActivity(owner) ?: return

            use {
                it.disableForegroundDispatch(activity)
            }
        }

        override fun use(action: (adapter: NfcAdapter)->Unit){
            if(nfcAdapter == null) {
                return
            }

            val nfcAdapter: NfcAdapter = this.nfcAdapter!!
            action(nfcAdapter)
        }
    }
}