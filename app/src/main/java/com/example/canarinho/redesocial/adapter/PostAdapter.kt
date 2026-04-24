package br.com.canarinho.redesocial.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.canarinho.redesocial.model.Post
import com.example.canarinho.redesocial.R

class PostAdapter(
    private val posts: MutableList<Post>,
    private val emailUsuarioLogado: String,
    private val onEdit: (Post, Int) -> Unit,
    private val onDelete: (Post, Int) -> Unit,
    private val onComment: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFotoAutor: ImageView = itemView.findViewById(R.id.imgFotoAutor)
        val txtUsernameAutor: TextView = itemView.findViewById(R.id.txtUsernameAutor)
        val txtCidadePost: TextView = itemView.findViewById(R.id.txtCidadePost)
        val imgPost: ImageView = itemView.findViewById(R.id.imgPost)
        val txtDescricao: TextView = itemView.findViewById(R.id.txtDescricao)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnDeletar: ImageButton = itemView.findViewById(R.id.btnDeletar)
        val btnComentar: TextView = itemView.findViewById(R.id.btnComentar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.txtUsernameAutor.text = "@${post.usernameAutor}"

        // Exibe a cidade se disponível
        if (post.cidade.isNotEmpty()) {
            holder.txtCidadePost.text = "📍 ${post.cidade}"
            holder.txtCidadePost.visibility = View.VISIBLE
        } else {
            holder.txtCidadePost.visibility = View.GONE
        }

        if (post.fotoAutor != null) {
            holder.imgFotoAutor.setImageBitmap(post.fotoAutor)
        } else {
            holder.imgFotoAutor.setImageResource(R.drawable.empty_profile)
        }

        if (post.imagem != null) {
            holder.imgPost.setImageBitmap(post.imagem)
        } else {
            holder.imgPost.setImageResource(R.drawable.empty_profile)
        }

        holder.txtDescricao.text = post.descricao
        holder.btnComentar.setOnClickListener { onComment(post) }

        if (post.emailAutor == emailUsuarioLogado) {
            holder.btnEditar.visibility = View.VISIBLE
            holder.btnDeletar.visibility = View.VISIBLE
            holder.btnEditar.setOnClickListener { onEdit(post, position) }
            holder.btnDeletar.setOnClickListener { onDelete(post, position) }
        } else {
            holder.btnEditar.visibility = View.GONE
            holder.btnDeletar.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = posts.size

    fun adicionarPosts(novosPosts: List<Post>) {
        val inicio = posts.size
        posts.addAll(novosPosts)
        notifyItemRangeInserted(inicio, novosPosts.size)
    }

    fun removeItem(position: Int) {
        posts.removeAt(position)
        notifyItemRemoved(position)
    }
}