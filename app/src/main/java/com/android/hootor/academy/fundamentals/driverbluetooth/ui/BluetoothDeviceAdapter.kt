package com.android.hootor.academy.fundamentals.driverbluetooth.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.hootor.academy.fundamentals.driverbluetooth.R
import com.android.hootor.academy.fundamentals.models.BlDevice

class BluetoothDeviceAdapter constructor(private val listener: (BlDevice) -> Unit) :
    RecyclerView.Adapter<BluetoothDeviceAdapter.BluetoothDeviceHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<BlDevice>() {
        override fun areItemsTheSame(oldItem: BlDevice, newItem: BlDevice): Boolean =
            oldItem.mac == newItem.mac

        override fun areContentsTheSame(oldItem: BlDevice, newItem: BlDevice): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(newList: List<BlDevice>) {
        differ.submitList(newList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothDeviceHolder {
        return BluetoothDeviceHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_bluetooth,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BluetoothDeviceHolder, position: Int) {
        val blDevice: BlDevice = differ.currentList[position]
        holder.bind(blDevice, listener)
    }

    override fun getItemCount() = differ.currentList.size

    class BluetoothDeviceHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        private val title = itemView.findViewById<TextView>(R.id.tv_title)
        private val mac = itemView.findViewById<TextView>(R.id.tv_mac)

        fun bind(blDevice: BlDevice, listener: (BlDevice) -> Unit) {

            title.text = blDevice.title
            mac.text = blDevice.mac

            itemView.setOnClickListener {
                listener(blDevice)
            }
        }
    }


}