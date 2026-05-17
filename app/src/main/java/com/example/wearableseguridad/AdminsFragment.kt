package com.example.wearableseguridad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminsFragment : Fragment() {

    private lateinit var rvAdmins: RecyclerView
    private lateinit var adapter: AdminAdapter
    private val adminList = mutableListOf<AdminUser>()
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admins, container, false)

        rvAdmins = view.findViewById(R.id.rvAdmins)
        rvAdmins.layoutManager = LinearLayoutManager(context)
        
        adapter = AdminAdapter(adminList) { admin ->
            val fragment = AdminDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("adminUid", admin.uid)
                    putString("adminNombre", admin.nombre)
                    putString("adminCorreo", admin.correo)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        rvAdmins.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("usuarios_admin")
        cargarAdministradores()

        return view
    }

    private fun cargarAdministradores() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adminList.clear()
                for (adminSnapshot in snapshot.children) {
                    val admin = adminSnapshot.getValue(AdminUser::class.java)?.copy(uid = adminSnapshot.key ?: "")
                    if (admin != null && admin.correo != "admin@segtium.com") {
                        adminList.add(admin)
                    }
                }
                adapter.updateList(adminList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}