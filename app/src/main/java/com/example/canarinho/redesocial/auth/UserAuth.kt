package br.com.canarinho.redesocial.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class UserAuth {
    private val auth = FirebaseAuth.getInstance()

    fun login(email: String, pass: String, callback: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task -> callback(task.isSuccessful) }
    }

    fun cadastro(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful, task.exception?.message)
            }
    }

    fun getEmailUsuarioLogado(): String? = auth.currentUser?.email

    fun logout() = auth.signOut()

    // Para alterar a senha o Firebase exige que o usuário se reautentique primeiro
    fun alterarSenha(
        senhaAtual: String,
        novaSenha: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onFailure("Usuário não autenticado")
            return
        }
        val email = user.email ?: run {
            onFailure("Email não encontrado")
            return
        }

        // Reautentica com a senha atual antes de trocar
        val credential = EmailAuthProvider.getCredential(email, senhaAtual)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(novaSenha)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.message ?: "Erro ao atualizar senha") }
            }
            .addOnFailureListener {
                onFailure("Senha atual incorreta")
            }
    }
}