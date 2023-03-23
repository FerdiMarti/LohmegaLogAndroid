package com.example.lohmegalog.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lohmegalog.R

/**
 * RecyclerView Adapter for scan results
 *
 * @property scanResults - takes a list of ScanResultData to Display
 * @property onClickListener - takes a click listener for list elements
 */
class ScanResultAdapter(
    private val scanResults: List<ScanResultData>,
    private val onClickListener: OnClickListener
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {
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
        holder.textView.text = data.address

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

    /**
     * On click listener for result list entry. Takes the ScanResultData of the row that was clicked.
     */
    class OnClickListener(val clickListener: (data: ScanResultData) -> Unit) {
        fun onClick(data: ScanResultData) = clickListener(data)
    }
}