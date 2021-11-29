package com.ddwan.heremap.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ddwan.heremap.R
import com.here.android.mpa.search.DiscoveryResult

class RecyclerViewAdapter(var list: ArrayList<DiscoveryResult>) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    lateinit var itemClick: (position: Int) -> Unit
    fun setCallback(click: (position: Int) -> Unit) {
        itemClick = click
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.result_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.name)
        private val vicinity: TextView = itemView.findViewById(R.id.vicinity)
        private val layout: LinearLayout = itemView.findViewById(R.id.layout)
        fun setData() {
            val info = list[adapterPosition]
            name.text = info.title
            vicinity.text = "Vicinity: ${info.vicinity}"
            layout.setOnClickListener { itemClick.invoke(adapterPosition) }
        }
    }
}