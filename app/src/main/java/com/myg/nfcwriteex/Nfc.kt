package com.myg.nfcwriteex

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.os.Build
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
        private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(
            activity
        ) ?: let {
            Toast.makeText(activity, "NFC not supported on this device", Toast.LENGTH_SHORT).show()
            null
        }

        override val isEnabled: Boolean
            get() = nfcAdapter?.isEnabled ?: false

        override fun use(action: (adapter: NfcAdapter) -> Unit) {
            TODO("Not yet implemented")
        }
    }

    private class NfcDefault(
        val activity: ComponentActivity
    ) : Nfc {
        init {
            activity.lifecycle.addObserver(this)
        }

        private var nfcAdapter: NfcAdapter? = null
            get() {
                //Defence code for nullable
                if(field != null) {
                    return field
                }

                //Defence code for nullable application context
                if(activity.applicationContext == null){
                    return null
                }

                //When nfcAdapter null,
                field = NfcAdapter.getDefaultAdapter(
                    activity
                ) ?: let {
                    Toast.makeText(activity, "NFC not supported on this device", Toast.LENGTH_SHORT).show()
                    null
                }
                return field
            }
            @Suppress("UNUSED_PARAMETER")
            private set(newValue) {}

        private lateinit var pendingIntent: PendingIntent

        override val isEnabled: Boolean
            get() = nfcAdapter?.isEnabled ?: false

        private fun getActivity(owner: LifecycleOwner): ComponentActivity? {
            if(owner !is ComponentActivity) {
                return null
            }
            return owner
        }

        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)

            val activity = getActivity(owner) ?: return

            @Suppress("UnspecifiedImmutableFlag")
            pendingIntent = PendingIntent.getActivity(
                activity,
                0,
                Intent(activity, javaClass).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT.let {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        it.or(PendingIntent.FLAG_MUTABLE)
                    } else {
                        it
                    }
                }
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