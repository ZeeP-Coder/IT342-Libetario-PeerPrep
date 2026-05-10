package com.libetario.peerprep.features.studygroups.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.libetario.peerprep.R
import com.libetario.peerprep.features.studygroups.model.StudyPartner

class StudyPartnerAdapter(
    private val partners: List<StudyPartner>
) : RecyclerView.Adapter<StudyPartnerAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_partner_name)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_partner_info)
        private val tvGroups: TextView = itemView.findViewById(R.id.tv_partner_groups)

        fun bind(partner: StudyPartner) {
            tvName.text = partner.fullName
            tvInfo.text = "${partner.university} • ${partner.major}"
            tvGroups.text = "${partner.sharedGroups} shared group${if (partner.sharedGroups > 1) "s" else ""}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_partner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(partners[position])
    }

    override fun getItemCount() = partners.size
}
