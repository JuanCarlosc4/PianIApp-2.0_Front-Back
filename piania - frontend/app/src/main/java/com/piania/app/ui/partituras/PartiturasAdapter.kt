package com.piania.app.ui.partituras

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.piania.app.R
import com.piania.app.data.model.response.PartituraResponseDTO // Usamos el DTO correcto

// 1. Callback para calcular diferencias
class PartituraDiffCallback : DiffUtil.ItemCallback<PartituraResponseDTO>() {
    override fun areItemsTheSame(oldItem: PartituraResponseDTO, newItem: PartituraResponseDTO): Boolean {
        // Usamos idPartitura del DTO
        return oldItem.idPartitura == newItem.idPartitura
    }

    override fun areContentsTheSame(oldItem: PartituraResponseDTO, newItem: PartituraResponseDTO): Boolean {
        return oldItem == newItem
    }
}

// 2. El Adaptador
class PartiturasAdapter(private val onItemClicked: (PartituraResponseDTO) -> Unit) :
    ListAdapter<PartituraResponseDTO, PartiturasAdapter.PartituraViewHolder>(PartituraDiffCallback()) {

    // 3. El ViewHolder
    class PartituraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Asegúrate de que estos IDs existan en tu 'item_partitura.xml'
        private val titulo: TextView = itemView.findViewById(R.id.item_titulo)
        // private val autor: TextView = itemView.findViewById(R.id.item_autor) // Descomenta si lo añades al XML

        fun bind(partitura: PartituraResponseDTO, onItemClicked: (PartituraResponseDTO) -> Unit) {
            titulo.text = partitura.titulo
            // autor.text = partitura.nombreUsuario ?: "Anónimo"

            itemView.setOnClickListener { onItemClicked(partitura) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartituraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_partitura, parent, false)
        return PartituraViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartituraViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked)
    }
}