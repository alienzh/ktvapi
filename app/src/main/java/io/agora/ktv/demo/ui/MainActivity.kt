package io.agora.ktv.demo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.agora.ktv.demo.databinding.MainActivityBinding
import io.agora.ktv.demo.utils.PermissionHelp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PermissionHelp(this).checkMicPerm({

        }, {}, true)
    }
}