package com.libetario.peerprep.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.libetario.peerprep.R
import com.libetario.peerprep.model.StudyGroup

class StudyGroupAdapter(
    private val groups: List<StudyGroup>,
    private val isMyGroups: Boolean,
    private val onViewDetails: (Long) -> Unit,
    private val onJoin: (Long) -> Unit,
    private val onLeave: (Long) -> Unit,
    private val onDelete: (Long) -> Unit
) : RecyclerView.Adapter<StudyGroupAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_group_title)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_group_desc)
        private val tvDetails: TextView = itemView.findViewById(R.id.tv_group_details)
        private val tvMembers: TextView = itemView.findViewById(R.id.tv_group_members)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_group_location)
        private val btnViewDetails: Button = itemView.findViewById(R.id.btn_view_details)
        private val btnAction: Button = itemView.findViewById(R.id.btn_join_group)

        fun bind(group: StudyGroup) {
            tvTitle.text = group.subject
            tvDesc.text = group.description
            tvDetails.text = "${group.day} • ${group.meetingTime}"
            tvMembers.text = "${group.currentMembers}/${group.maxMembers} members"
            tvLocation.text = group.location

            btnViewDetails.setOnClickListener {
                onViewDetails(group.id)
            }

            if (isMyGroups) {
                btnAction.text = if (group.ownedByCurrentUser) "Delete" else "Leave"
                btnAction.setOnClickListener {
                    if (group.ownedByCurrentUser) {
                        onDelete(group.id)
                    } else {
                        onLeave(group.id)
                    }
                }
            } else {
                btnAction.text = if (group.joinable) "Join Group" else "Full"
                btnAction.isEnabled = group.joinable
                btnAction.setOnClickListener {
                    if (group.joinable) {
                        onJoin(group.id)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size
}
