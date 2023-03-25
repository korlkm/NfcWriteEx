package com.myg.nfcwriteex

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException


class MainActivity : AppCompatActivity() {
    val nfc = Nfc.get(this)

    private lateinit var ndef: Ndef
    private lateinit var messageToWrite: NdefMessage
    private lateinit var messageWrittenTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageWrittenTextView = findViewById(R.id.message_written_text_view)
        messageToWrite = NdefMessage(NdefRecord.createTextRecord(null, "Hello, NFC World!"))

        findViewById<Button>(R.id.write_button).setOnClickListener {
            writeMessageToTag()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        nfc.use {
            if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
                @Suppress("NewApi")
                val tag = if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                } else {
                    intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                }
                ndef = Ndef.get(tag)
                writeMessageToTag()
            }
        }
    }

    private fun writeMessageToTag() {
        nfc.use {
            try {
                ndef.connect()
                ndef.writeNdefMessage(messageToWrite)
                messageWrittenTextView.text =
                    "Message written to tag: ${String(messageToWrite.toByteArray())}"
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error writing to tag", Toast.LENGTH_SHORT).show()
            } finally {
                try {
                    ndef.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}