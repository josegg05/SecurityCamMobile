package com.example.ragnarok.securitycammobile.util

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.support.v4.content.ContextCompat


import android.util.Log
import com.example.ragnarok.securitycammobile.model.*

import com.example.ragnarok.securitycammobile.recyclerview.item.*
//import com.example.ragnarok.securitycammobile.task.ContactSetupTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.xwray.groupie.kotlinandroidextensions.Item
import java.lang.NullPointerException
import java.lang.ref.WeakReference


object FirestoreUtil {
    private val firestoreInstance: FirebaseFirestore by lazy {FirebaseFirestore.getInstance()}

    private val currentUserDocRef: DocumentReference
        get() = firestoreInstance.document("users/${FirebaseAuth.getInstance().currentUser?.uid
                ?: throw NullPointerException("UID is null.")}")

    private val currentGroupDocRef: DocumentReference
        get() = firestoreInstance.document("groups/${FirebaseAuth.getInstance().currentUser?.uid
                ?: throw NullPointerException("UID is null.")}")


    private val devicesChannelsCollectionRef = firestoreInstance.collection("devicesChannels")


    fun initCurrentUserIfFirstTime(onComplete: () -> Unit){
        currentUserDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()){
                val newUser = User(FirebaseAuth.getInstance().currentUser?.displayName ?: "",
                        FirebaseAuth.getInstance().currentUser?.email ?: "", mutableListOf())
                currentUserDocRef.set(newUser).addOnSuccessListener {
                    onComplete()
                }
            }
            else {
                onComplete()
            }
        }
    }


    fun updateCurrentDevice(deviceID:String, name:String="",place:String=""){
        devicesChannelsCollectionRef.document(deviceID)
            .update(mapOf("name" to name))

        devicesChannelsCollectionRef.document(deviceID)
            .update(mapOf("place" to place))
    }
    /*
    fun updateCurrentUser(name: String = "", bio: String = "", profilePicturePath: String? = null){
        val userFieldMap = mutableMapOf<String,Any>()
        if (name.isNotBlank()) userFieldMap["name"] = name
        if (bio.isNotBlank()) userFieldMap["bio"] = bio
        if (profilePicturePath != null)
            userFieldMap["profilePicturePath"] = profilePicturePath
        currentUserDocRef.update(userFieldMap)
    }*/

    fun getCurrentUser(onComplete: (User) -> Unit){
        currentUserDocRef.get()
                .addOnSuccessListener {
                    onComplete(it.toObject(User::class.java)!!)
                }
    }


    //TODO: Ver si necesito esto
    /*
    fun createGroup(name: String = "", bio: String = "", profilePicturePath: String? = null){

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val groupToCreate = Group(name, bio, currentUserId, profilePicturePath,mutableListOf())

        val newChannel = groupChannelsCollectionRef.document()
        newChannel.set(groupToCreate)

        //revisar
        currentUserDocRef.get().addOnSuccessListener {
            newChannel.collection("members").document(currentUserId)
                .set(mapOf("email" to it["email"]))
        }


        currentUserDocRef
            .collection("engagedGroupChannels")
            .document(newChannel.id)
            .set(mapOf("channelId" to newChannel.id))
    }*/


    fun addDevicesListener(onListen: (List<Item>) -> Unit): ListenerRegistration {
        return firestoreInstance.collection("devicesChannels")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Devices listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }

                val itemsDevices = mutableListOf<Item>()
                querySnapshot!!.documents.forEach { doc1 ->
                    val device = currentUserDocRef.collection("engagedDevices")
                        .document(doc1.id).get()
                    while(!device.isComplete) {}
                    if (device.result.exists())
                        itemsDevices.add(DeviceItem(doc1.toObject(Device::class.java)!!, doc1.id))
                }
                onListen(itemsDevices)
            }
    }


    //fun removeListener(registration: ListenerRegistration) = registration.remove()


    /*
    fun getOrCreateChatChannel(otherUserId: String,
                               onComplete: (channelId: String) -> Unit) {
        currentUserDocRef.collection("engagedChatChannels")
            .document(otherUserId).get().addOnSuccessListener {
                if (it.exists()) {
                    onComplete(it["channelId"] as String)
                    return@addOnSuccessListener
                }

                val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

                val newChannel = devicesChannelsCollectionRef.document()
                newChannel.set(ChatChannel(mutableListOf(currentUserId, otherUserId)))

                currentUserDocRef
                    .collection("engagedChatChannels")
                    .document(otherUserId)
                    .set(mapOf("channelId" to newChannel.id))

                firestoreInstance.collection("users").document(otherUserId)
                    .collection("engagedChatChannels")
                    .document(currentUserId)
                    .set(mapOf("channelId" to newChannel.id))

                onComplete(newChannel.id)
            }
    }*/


    fun addChatMessagesListener(channelId: String, context: Context,
                                onListen: (List<Item>) -> Unit): ListenerRegistration {
        return devicesChannelsCollectionRef.document(channelId).collection("messages")
                .orderBy("time")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (firebaseFirestoreException != null) {
                        Log.e("FIRESTORE", "ChatMessagesListener error.", firebaseFirestoreException)
                        return@addSnapshotListener
                    }

                    val items = mutableListOf<Item>()
                    querySnapshot!!.documents.forEach {
                        if (it["type"] == MessageType.SECURE)
                            items.add(SecureMessageItem(it.toObject(SecureMessage::class.java)!!, context))
                        else if (it["type"] == MessageType.TEXT){}

                        else if (it["type"] == MessageType.IMAGE){}

                        else if (it["type"] == MessageType.FILE){}

                        else if (it["type"] == MessageType.AUDIO){}

                        return@forEach
                    }
                    onListen(items)
                }
    }

    /*
    fun sendMessage(message: Message, channelId: String) {
        devicesChannelsCollectionRef.document(channelId)
                .collection("messages")
                .add(message)
    }*/


    //TODO: ESTA FUNCION
    fun addDeviceMember(newDeviceid: String){
        val newDevice = Device(newDeviceid,"","","",mutableListOf())

        currentUserDocRef.collection("engagedDevices")
            .document(newDeviceid).set(newDevice)

    }

    fun acceptDeclineInvitation(delete: Boolean, channel: String): Boolean {

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        if (!delete){
            currentUserDocRef
                    .collection("engagedGroupChannels")
                    .document(channel)
                    .set(mapOf("channelId" to channel))

            currentUserDocRef.get().addOnSuccessListener {
                firestoreInstance.collection("groupChannels").document(channel)
                        .collection("members")
                        .document(currentUserId)
                        .set(mapOf("email" to it["email"]))
            }
        }

        currentUserDocRef
                .collection("invitationGroupChannels")
                .document(channel)
                .delete()

        return true
    }


    //region FCM
    fun getFCMRegistrationTokens(onComplete: (tokens: MutableList<String>) -> Unit) {
        currentUserDocRef.get().addOnSuccessListener {
            val user = it.toObject(User::class.java)!!
            onComplete(user.registrationTokens)
        }
    }

    fun setFCMRegistrationTokens(registrationTokens: MutableList<String>) {
        currentUserDocRef.update(mapOf("registrationTokens" to registrationTokens))
    }
    //endregion FCM
}