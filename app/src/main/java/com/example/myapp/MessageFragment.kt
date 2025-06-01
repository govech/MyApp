package com.example.myapp

import android.widget.Toast
import com.example.base.BaseLazyFragment
import com.example.myapp.databinding.FragmentMessageBinding


class MessageFragment : BaseLazyFragment<FragmentMessageBinding>() {
    override fun onLazyLoad() {
        Toast .makeText(context, "MessageFragment", Toast.LENGTH_SHORT).show()
    }


    override fun onBindingCreated() {

    }
}