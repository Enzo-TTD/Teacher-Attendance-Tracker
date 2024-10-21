package dev.kwasi.echoservercomplete

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.chatlist.ChatListAdapter
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.network.Client
import dev.kwasi.echoservercomplete.network.NetworkMessageInterface
import dev.kwasi.echoservercomplete.network.Server
import dev.kwasi.echoservercomplete.peerlist.PeerListAdapter
import dev.kwasi.echoservercomplete.peerlist.PeerListAdapterInterface
import dev.kwasi.echoservercomplete.peerlist.AttendeeListAdapter
import dev.kwasi.echoservercomplete.peerlist.AttendeeListAdapterInterface
import dev.kwasi.echoservercomplete.wifidirect.WifiDirectInterface
import dev.kwasi.echoservercomplete.wifidirect.WifiDirectManager
import android.util.Log
import android.widget.TextView
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.SecretKey
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

fun ByteArray.toHex() = joinToString(separator = "") { byte-> "%02x".format(byte) }
fun getFirstNChars(str: String, n:Int) = str.substring(0,n)

class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, PeerListAdapterInterface, AttendeeListAdapterInterface, NetworkMessageInterface {
    private var wfdManager: WifiDirectManager? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var peerListAdapter:PeerListAdapter? = null
    private var chatListAdapter:ChatListAdapter? = null
    private var attendeeListAdapter:AttendeeListAdapter? = null

    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var classStarted = false
    private var hasDevices = false
    private var server: Server? = null
    private var client: Client? = null
    private var deviceIp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)

        peerListAdapter = PeerListAdapter(this)
        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.adapter = peerListAdapter
        rvPeerList.layoutManager = LinearLayoutManager(this)

        attendeeListAdapter = AttendeeListAdapter(this)
        val rvAttendeeList: RecyclerView= findViewById(R.id.attendanceList)
        rvAttendeeList.adapter = attendeeListAdapter
        rvAttendeeList.layoutManager = LinearLayoutManager(this)

        chatListAdapter = ChatListAdapter()
        val rvChatList: RecyclerView = findViewById(R.id.studentChat)
        rvChatList.adapter = chatListAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }
    fun createGroup(view: View) {
        wfdManager?.createGroup()
        classStarted = true

        updateUI()
    }



    fun endClass(view: View) {
        server?.close()
        classStarted = false
        wfdHasConnection = false
        updateUI()
    }

    fun discoverNearbyPeers(view: View) {
        wfdManager?.discoverPeers()
    }

    private fun updateUI(){
        //The rules for updating the UI are as follows:
        // IF the WFD adapter is NOT enabled then
        //      Show UI that says turn on the wifi adapter
        // ELSE IF there is NO WFD connection then i need to show a view that allows the user to either
        // 1) create a group with them as the group owner OR
        // 2) discover nearby groups
        // ELSE IF there are nearby groups found, i need to show them in a list
        // ELSE IF i have a WFD connection i need to show a chat interface where i can send/receive messages



        val wfdAdapterErrorView:ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.visibility = if (wfdAdapterEnabled && !wfdHasConnection && hasDevices) View.VISIBLE else View.GONE

        val wfdConnectedView:ConstraintLayout = findViewById(R.id.clHasConnection)
        wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE

        val classStartedView:ConstraintLayout = findViewById(R.id.classStarted)

        if(classStarted)
        {
            classStartedView.visibility = View.VISIBLE
            wfdNoConnectionView.visibility = View.GONE
        }
        else
        {
            classStartedView.visibility = View.GONE
        }

        val networkName: TextView = findViewById(R.id.tvNetworkName)
        val networkNameString: String = wfdManager?.groupInfo?.networkName ?: ""
        networkName.text = "Class Network: $networkNameString"

        Log.e("CA", "Class Network: $networkNameString")

        val networkPassword: TextView = findViewById(R.id.tvNetworkPassword)
        val networkPasswordString: String = wfdManager?.groupInfo?.passphrase ?: ""
        networkPassword.text = "Network Password: $networkPasswordString"

        Log.e("CA", "Network Password: $networkPasswordString")

    }

    fun sendMessage(view: View) {
        val etMessage:EditText = findViewById(R.id.etMessage)
        val etString = etMessage.text.toString()
        val content = ContentModel(etString, deviceIp)
        etMessage.text.clear()
        server?.sendMessageToClient(content)
        chatListAdapter?.addItemToEnd(content)

    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        val toast = Toast.makeText(this, "Updated listing of nearby WiFi Direct devices", Toast.LENGTH_SHORT)
        toast.show()
        hasDevices = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        updateUI()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?) {
        val text = if (groupInfo == null){
            "Group is not formed"
        } else {
            "Group has been formed"
        }
        val toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        toast.show()
        wfdHasConnection = groupInfo != null

        if (groupInfo == null){
            server?.close()
            client?.close()
        } else if (groupInfo.isGroupOwner && server == null){
            server = Server(this)
            deviceIp = "192.168.49.1"


        } else if (!groupInfo.isGroupOwner && client == null) {
            client = Client(this)
            deviceIp = client!!.ip

            Log.e("CA", "A student joined the group")
        }

    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {

        val toast = Toast.makeText(this, "Device parameters have been updated" , Toast.LENGTH_SHORT)
        toast.show()
        server?.setOnStudentIdChangeListener { studentId ->
            runOnUiThread {
                attendeeListAdapter?.addAttendee(studentId)
                Log.e("CA", "New student joined with ID: $studentId")
            }
        }
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        wfdManager?.connectToPeer(peer)
    }


    override fun onAttendeeClicked(attendee: String)
    {

    }

    override fun onGetStudentIdClicked(studentId: String) {

        val ipAddress = server?.getIpByStudentId(studentId)

        if (ipAddress != null) {
            deviceIp = ipAddress // Set the device IP from the server's response
            chatListAdapter?.clearChatList() // Clear the chat list

//            val student: TextView = findViewById(R.id.studentChat)
//            student.text = "Student Chat - $studentId" // Update the text with the student ID
        } else {
            // Handle the case where the IP address is null
            Log.e("Chat", "No IP address found for student ID: $studentId")
            }

    }

    private fun onAttendeeClicked()
    {
        Log.e("CA","Attendee clicked")
    }



    override fun onContent(content: ContentModel) {
        runOnUiThread{
            chatListAdapter?.addItemToEnd(content)
        }
    }

}