package com.example.ragnarok.securitycammobile

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.example.ragnarok.securitycammobile.util.FirestoreUtil
import com.example.ragnarok.securitycammobile.model.User
import com.example.ragnarok.securitycammobile.recyclerview.item.DeviceItem
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.firebase.firestore.ListenerRegistration
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.OnItemLongClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity
import com.google.android.gms.nearby.connection.Payload
import android.text.TextUtils
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.ragnarok.securitycammobile.util.FirestoreUtil.addDeviceMember
import org.jetbrains.anko.toast


private const val appName:String = "SecureCam_2018"

class MainActivity : AppCompatActivity() {

    private lateinit var currentUser: User

    private lateinit var devicesListenerRegistration: ListenerRegistration
    private lateinit var devicesSection: Section
    private var shouldInitRecyclerView = true

    private lateinit var connectionsClient: ConnectionsClient
    var discover = false

    private var discoveryName:String?=""

    private val locationPermissionGranted
        get() = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val REQUEST_CODE_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBarDevices.visibility = View.GONE

        connectionsClient = Nearby.getConnectionsClient(this)

        addDeviceBtn.setOnClickListener {
            //TODO: addDevice()
            if (!locationPermissionGranted){
                requestContactsPermission()
            }
            else {
                startDiscovery()
            }
            //showCreateCategoryDialog()
        }


        FirestoreUtil.getCurrentUser {
            currentUser = it
        }


        devicesListenerRegistration =
                FirestoreUtil.addDevicesListener(this::onDevicesChanged)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun onDevicesChanged(devices: List<Item>){
        fun init() {
            recycler_view_devices.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = GroupAdapter<ViewHolder>().apply {
                    devicesSection = Section(devices)
                    this.add(devicesSection)
                    setOnItemClickListener(onItemClick)
                    setOnItemLongClickListener(onItemLongClick)

                }
            }
            shouldInitRecyclerView = false
        }

        fun updateItems() = devicesSection.update(devices)

        if (shouldInitRecyclerView)
            init()
        else
            updateItems()

        recycler_view_devices.scrollToPosition(recycler_view_devices.adapter!!.itemCount - 1)
    }

    private val onItemClick = OnItemClickListener { item, view ->
        if (item is DeviceItem) {
            startActivity<ChatActivity>(
                AppConstants.DEVICE_NAME to item.device.name,
                AppConstants.DEVICE_ID to item.deviceId
            )
        }
    }

    private val onItemLongClick = OnItemLongClickListener{ item, view ->
        if (item is DeviceItem) {
            startActivity<ConfigActivity>(
                AppConstants.DEVICE_ID to item.deviceId
            )
        }
        true
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        connectionsClient.startDiscovery(
            appName,//paquete name
            mEndpointDiscoveryCallback,
            options)
            .addOnSuccessListener{
                val toast = Toast.makeText(this, "Now discovering", Toast.LENGTH_SHORT)
                toast.show()
                progressBarDevices.visibility = View.VISIBLE

            }
            .addOnFailureListener{
                val error = it.message.toString()
                val toast = Toast.makeText(this, "Failed discovering:" + error, Toast.LENGTH_LONG)
                toast.show()
            }
    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId:String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
            Nearby.getConnectionsClient(this@MainActivity)
                .requestConnection("pepe", endpointId, mConnectionLifecycleCallback)
                .addOnSuccessListener {
                    // We successfully requested a connection. Now both sides
                    // must accept before the connection is establishe
                    Log.i("discorvered", endpointId)
                }
                .addOnFailureListener { e: Exception ->
                    // Nearby Connections failed to request the connection.
                    progressBarDevices.visibility = View.GONE
                }
        }


        override fun onEndpointLost(endpointId: String) {
        }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(
            endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            discoveryName = connectionInfo.endpointName
            connectionsClient.acceptConnection(endpointId, mPayloadCallback)
            //Stop the Discovery
            connectionsClient.stopDiscovery()

        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {

                    progressBarDevices.visibility = View.GONE
                    val conSsid = getCurrentSsid(this@MainActivity)
                    if (!conSsid.isNullOrEmpty()) {
                        showCreateCategoryDialog(conSsid!!, endpointId)

                    }
                    else{
                        toast("Yo have to be connected to a Wifi Network")
                        connectionsClient.disconnectFromEndpoint(endpointId)
                        progressBarDevices.visibility = View.GONE
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    toast("Connection Rejected")
                    progressBarDevices.visibility = View.GONE
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    toast("Connection Error. Please Try Again")
                    progressBarDevices.visibility = View.GONE
                }
            }// We're connected! Can now start sending and receiving data.
            // The connection was rejected by one or both sides.
            // The connection broke before it was able to be accepted.
        }

        override fun onDisconnected(endpointId: String) {
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            toast("Dispositivo desconectado")
        }
    }


    // Callbacks for receiving payloads
    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if(payload.toString() != "Incorrecto") {
                val deviceIDs = String(payload.asBytes()!!, Charsets.UTF_8)
                progressBarDevices.visibility = View.GONE
                toast("Dispositivo Agregado")
                //TODO: Hacer que se cree el Device
                addDeviceMember(deviceIDs)
            }

            else{
                progressBarDevices.visibility = View.GONE
                toast("Pin Incorrecto")
                connectionsClient.disconnectFromEndpoint(endpointId)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
        }
    }

    fun getCurrentSsid(context: Context): String? {
        var ssid: String? = null
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.getActiveNetworkInfo()
        if (networkInfo.isConnected) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
                ssid = connectionInfo.ssid
            }
        }
        return ssid
    }

    fun showCreateCategoryDialog(conSsid:String, endpointId: String) {
        val context = this
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Connect to " + discoveryName)

        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.alert_wifi_connect, null)

        val pinEditText = view.findViewById(R.id.editTextPin) as EditText
        val passwordEditText = view.findViewById(R.id.editTextpass) as EditText
        val netInf = view.findViewById(R.id.textViewNet) as TextView
        netInf.text = "Connect to Network: " + conSsid

        builder.setView(view);

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            val devicePin = pinEditText.text
            val netPass = passwordEditText.text
            var isValid = true
            if (devicePin.isBlank()) {
                pinEditText.error = getString(R.string.validation_empty)
                isValid = false
            }

            if (isValid) {
                // do something
                val msg = ":::" + devicePin + ":::" + conSsid + ":::" + netPass
                val pay = Payload.fromBytes(msg.toByteArray(Charsets.UTF_8))
                connectionsClient.sendPayload(
                    endpointId, pay)

                progressBarDevices.visibility = View.VISIBLE
            }

            if (isValid) {
                dialog.dismiss()
            }
        }

        builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
            connectionsClient.disconnectFromEndpoint(endpointId)
            dialog.cancel()
        }

        builder.show();
    }

    private fun requestContactsPermission() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            android.app.AlertDialog.Builder(this)
                .setMessage(getString(R.string.location_permissions_rationale))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_LOCATION_PERMISSION)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show();

        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_LOCATION_PERMISSION)
        }
    }
}
