package br.com.canarinho.redesocial.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.canarinho.redesocial.model.Comment
import com.example.canarinho.redesocial.R

class CommentAdapter(private val comments: MutableList<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFotoComentario)
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsernameComentario)
        val txtTexto: TextView = itemView.findViewById(R.id.txtTextoComentario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.txtUsername.text = "@${comment.usernameAutor}"
        holder.txtTexto.text = comment.texto
        if (comment.fotoAutor != null) {
            holder.imgFoto.setImageBitmap(comment.fotoAutor)
        } else {
            holder.imgFoto.setImageResource(R.drawable.empty_profile)
        }
    }

    override fun getItemCount(): Int = comments.size

    fun adicionarComentario(comment: Comment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }
}