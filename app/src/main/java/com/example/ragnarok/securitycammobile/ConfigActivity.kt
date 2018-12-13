package com.example.ragnarok.securitycammobile

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.ragnarok.securitycammobile.util.FirestoreUtil
import kotlinx.android.synthetic.main.activity_config.*

class ConfigActivity : AppCompatActivity() {

    private lateinit var deviceId: String
    private var name: String = ""
    private var place:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        deviceId = intent.getStringExtra(AppConstants.DEVICE_ID)

        buttonConfigSave.setOnClickListener {
            name = editTextName.text.toString()
            place = editTextPlace.text.toString()

            FirestoreUtil.updateCurrentDevice(deviceId, name, place)
        }
    }
}
