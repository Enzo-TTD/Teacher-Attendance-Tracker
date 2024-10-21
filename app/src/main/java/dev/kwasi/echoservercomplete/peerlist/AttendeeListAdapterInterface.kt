package dev.kwasi.echoservercomplete.peerlist

import android.net.wifi.p2p.WifiP2pDevice

interface AttendeeListAdapterInterface {
    fun onAttendeeClicked(attendee:String)
    fun onGetStudentIdClicked(studentId: String)
}