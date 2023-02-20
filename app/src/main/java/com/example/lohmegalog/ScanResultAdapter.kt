package com.example.lohmegalog
import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView

class ScanResultAdapter(private val scanResults: List<ScanResultData>, private val onClickListener: OnClickListener) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.scan_result_design, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val data = scanResults[position]
        holder.textView.text = data.name

        holder.itemView.setOnClickListener {
            onClickListener.onClick(data)
        }

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return scanResults.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    class OnClickListener(val clickListener: (data: ScanResultData) -> Unit) {
        fun onClick(data: ScanResultData) = clickListener(data)
    }
}