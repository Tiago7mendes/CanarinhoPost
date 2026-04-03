package br.com.canarinho.redesocial.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.canarinho.redesocial.model.Post
import com.example.canarinho.redesocial.R

class PostAdapter(private val posts: Array<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPost: ImageView = itemView.findViewById(R.id.imgPost)
        val txtDescricao: TextView = itemView.findViewById(R.id.txtDescricao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.txtDescricao.text = post.descricao
        if (post.imagem != null) {
            holder.imgPost.setImageBitmap(post.imagem)
        } else {
            holder.imgPost.setImageResource(R.drawable.empty_profile)
        }
    }

    override fun getItemCount(): Int = posts.size
}