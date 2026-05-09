package com.example.energisaver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Device(
    var id: String = "",
    val name: String = "",
    val consumption: Float = 0f,
    val status: String = "Ativo",
    val ipAddress: String = ""
)

class DeviceAdapter(
    private var deviceList: List<Device>,
    private val onItemClick: (Device) -> Unit,      //Simple click show details
    private val onItemLongClick: (Device) -> Unit   //Long click delete device
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvDeviceName)
        val consumption: TextView = view.findViewById(R.id.tvDeviceConsumption)
        val status: TextView = view.findViewById(R.id.tvDeviceStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.name.text = device.name
        holder.consumption.text = "Hoje: ${device.consumption} kWh"
        holder.status.text = device.status

        //Simple click listener
        holder.itemView.setOnClickListener { onItemClick(device) }
        //Long click listener
        holder.itemView.setOnLongClickListener {
            onItemLongClick(device)
            true
        }
    }

    override fun getItemCount() = deviceList.size

    fun updateList(newList: List<Device>) {
        deviceList = newList
        notifyDataSetChanged()
    }
}