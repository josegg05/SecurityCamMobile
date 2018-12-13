package com.example.ragnarok.securitycammobile.recyclerview.item

import android.content.Context
import android.view.View
import com.example.ragnarok.securitycammobile.util.StorageUtil
import com.example.ragnarok.securitycammobile.R
import com.example.ragnarok.securitycammobile.glide.GlideApp
import com.example.ragnarok.securitycammobile.model.SecureMessage
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_image_message.*

class SecureMessageItem(val message: SecureMessage,
                       val context: Context)
    : MessageItem(message) {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        super.bind(viewHolder, position)
        viewHolder.textView_message_image_sender.visibility = View.GONE
        viewHolder.textView_tag.text = "TAG: " + message.tag

        GlideApp.with(context)
            .load(StorageUtil.pathToReference(message.imagePath))
            .placeholder(R.drawable.ic_image_black_24dp)
            .into(viewHolder.imageView_message_image)
    }

    override fun getLayout() = R.layout.item_image_message

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if (other !is SecureMessageItem)
            return false
        if (this.message != other.message)
            return false
        return true
    }

    override fun equals(other: Any?): Boolean {
        return isSameAs(other as? SecureMessageItem)
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }
}