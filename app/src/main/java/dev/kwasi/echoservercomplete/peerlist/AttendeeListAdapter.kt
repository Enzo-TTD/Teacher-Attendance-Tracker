package dev.kwasi.echoservercomplete.peerlist

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.R
import dev.kwasi.echoservercomplete.models.ContentModel

class AttendeeListAdapter(private val iFaceImpl:AttendeeListAdapterInterface): RecyclerView.Adapter<AttendeeListAdapter.ViewHolder>() {
    private val attendeeList:MutableList<String> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentId: TextView = itemView.findViewById(R.id.studentId)
        val getStudentIdButton: Button = itemView.findViewById(R.id.question)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attendee_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendee = attendeeList[position]

        holder.studentId.text = attendee

        holder.itemView.setOnClickListener {
            iFaceImpl.onAttendeeClicked(attendee)
        }

        holder.getStudentIdButton.setOnClickListener {
            // Call a method in the interface to handle button click
            iFaceImpl.onGetStudentIdClicked(attendee) // Pass the attendee (studentId)
        }
    }

    override fun getItemCount(): Int {
        return attendeeList.size
    }

    fun addItemToEnd(attendee: String){
        attendeeList.add(attendee)
        notifyItemInserted(attendeeList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newAttendeeList:Collection<String>){
        attendeeList.clear()
        attendeeList.addAll(newAttendeeList)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addAttendee(newAttendee: String){
        attendeeList.add(newAttendee)
        notifyDataSetChanged()
    }
}