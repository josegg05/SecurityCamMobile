package com.example.ragnarok.securitycammobile

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.ragnarok.securitycammobile.util.FirestoreUtil
import com.example.ragnarok.securitycammobile.util.StorageUtil
import com.example.ragnarok.securitycammobile.model.User
import com.example.ragnarok.securitycammobile.recyclerview.item.SecureMessageItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.StorageReference
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*

import java.io.File
import java.io.IOException
import java.util.*


private const val REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 10

class ChatActivity : AppCompatActivity() {
    private lateinit var deviceId: String
    private lateinit var messagesListenerRegistration: ListenerRegistration
    private lateinit var messagesSection: Section
    private var shouldInitRecyclerView = true

    private var permissionToWriteAccepted = false
        get() = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(AppConstants.DEVICE_NAME)


        deviceId = intent.getStringExtra(AppConstants.DEVICE_ID)

        messagesListenerRegistration =
                FirestoreUtil.addChatMessagesListener(deviceId, this, this::onMessageChanged)

    }

    private fun onMessageChanged(messages: List<Item>){
        fun init() {
            recyclerViewChat.apply {
                layoutManager = LinearLayoutManager(this@ChatActivity)
                adapter = GroupAdapter<ViewHolder>().apply {
                    messagesSection = Section(messages)
                    this.add(messagesSection)
                    setOnItemClickListener(onItemClick)
                }
            }
            shouldInitRecyclerView = false
        }

        fun updateItems() = messagesSection.update(messages)

        if (shouldInitRecyclerView)
            init()
        else
            updateItems()

        recyclerViewChat.scrollToPosition(recyclerViewChat.adapter!!.itemCount - 1)
    }

    private val onItemClick = OnItemClickListener { item, view ->
        val context = this
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Descargar archivo")

        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.dialog_download, null)
        val categoryEditText = view.findViewById<TextView>(R.id.show_message_View)
        categoryEditText.text = "Desea descargar el archivo?"
        builder.setView(view)

        builder.setPositiveButton("Aceptar") { dialog, p1 ->
            if(item is SecureMessageItem) {
                downloadToLocalImage(
                    StorageUtil.storageInstance.getReference(item.message.imagePath),
                    item.message.size
                )
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, p1 ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun downloadToLocalImage(fileReference: StorageReference, size: String){
        if (!permissionToWriteAccepted) {
            requestWritePermission()
        }
        else{
            if (fileReference != null){
                try {
                    val dir = File(Environment.getExternalStorageDirectory().toString() + File.separator + "SecureCam")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = File(Environment.getExternalStorageDirectory().toString() + File.separator + "SecureCam" + "/" + UUID.randomUUID() + ".jpeg")

                    fileReference.getFile(file)
                        .addOnSuccessListener {
                            Toast.makeText(this,"Descarga Finalizada", Toast.LENGTH_SHORT).show()
                            //downloadProgressBar.visibility = View.GONE
                        }
                        .addOnProgressListener {
                            //Toast.makeText(this,"Descargando", Toast.LENGTH_SHORT).show()
                            //downloadProgressBar.visibility = View.VISIBLE
                            //downloadProgressBar.setProgress(((it.bytesTransferred/1000)/size.toInt()).toInt())

                        }
                }catch (e: IOException){
                    e.printStackTrace()
                    Toast.makeText(this,"Descarga Fallida", Toast.LENGTH_SHORT).show()
                    //downloadProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun requestWritePermission() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.record_permissions_rationale))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show();

        } else {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
        }
    }
}