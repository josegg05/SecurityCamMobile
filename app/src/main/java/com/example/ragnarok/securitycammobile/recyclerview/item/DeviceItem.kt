package com.example.ragnarok.securitycammobile.recyclerview.item

import com.example.ragnarok.securitycammobile.R
import com.example.ragnarok.securitycammobile.model.Device
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_device.*
import org.jetbrains.anko.image

class DeviceItem(val device: Device,
                 val deviceId: String)
    : Item() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView_name.text = device.name
        viewHolder.textView_bio.text = device.place
        //viewHolder.imageView_profile_picture.setImageResource(R.drawable.ic_developer_board_black_24dp)
    }

    override fun getLayout() = R.layout.item_device
}