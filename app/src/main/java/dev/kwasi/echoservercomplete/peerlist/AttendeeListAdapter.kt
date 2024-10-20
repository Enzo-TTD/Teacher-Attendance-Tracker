package dev.kwasi.echoservercomplete.peerlist

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.R
import dev.kwasi.echoservercomplete.models.ContentModel

class AttendeeListAdapter(private val iFaceImpl:AttendeeListAdapterInterface): RecyclerView.Adapter<AttendeeListAdapter.ViewHolder>() {
    private val attendeeList:MutableList<WifiP2pDevice> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentId: TextView = itemView.findViewById(R.id.studentId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attendee_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendee = attendeeList[position]

        holder.studentId.text = attendee.deviceName

        holder.itemView.setOnClickListener {
            iFaceImpl.onAttendeeClicked(attendee)
        }
    }

    override fun getItemCount(): Int {
        return attendeeList.size
    }

    fun addItemToEnd(attendee: WifiP2pDevice){
        attendeeList.add(attendee)
        notifyItemInserted(attendeeList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newAttendeeList:Collection<WifiP2pDevice>){
        attendeeList.clear()
        attendeeList.addAll(newAttendeeList)
        notifyDataSetChanged()
    }
}