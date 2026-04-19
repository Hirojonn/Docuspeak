package com.example.docuspeak

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI = 1
        private const val TYPE_TYPING = 2
    }

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun removeTypingIndicator() {
        val idx = messages.indexOfLast { it.isTyping }
        if (idx != -1) {
            messages.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    fun addAiResponse(text: String) {
        removeTypingIndicator()
        messages.add(ChatMessage(text, isUser = false))
        notifyItemInserted(messages.size - 1)
    }

    fun clear() {
        messages.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isTyping -> TYPE_TYPING
            msg.isUser -> TYPE_USER
            else -> TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> {
                val v = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserVH(v)
            }
            TYPE_TYPING -> {
                val v = inflater.inflate(R.layout.item_chat_ai, parent, false)
                AiVH(v)
            }
            else -> {
                val v = inflater.inflate(R.layout.item_chat_ai, parent, false)
                AiVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserVH -> holder.tvText.text = msg.text
            is AiVH -> {
                if (msg.isTyping) {
                    holder.tvText.text = "●  ●  ●"
                    holder.tvText.animate().alpha(0.5f).setDuration(500).withEndAction {
                        holder.tvText.animate().alpha(1.0f).setDuration(500).start()
                    }.start()
                } else {
                    holder.tvText.text = msg.text
                }
            }
        }
    }

    override fun getItemCount() = messages.size

    inner class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvText: TextView = v.findViewById(R.id.tvChatText)
    }

    inner class AiVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvText: TextView = v.findViewById(R.id.tvChatText)
    }
}
