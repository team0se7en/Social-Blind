package com.team7.socialblind.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.team7.socialblind.R
import com.team7.socialblind.databinding.ActivityMainBinding
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.models.Message
import com.team7.socialblind.repo.DiscussionRepository
import com.team7.socialblind.repo.SHARED_PREFERENCES_STRING
import com.team7.socialblind.util.Async
import com.team7.socialblind.util.Success

class MainActivity : AppCompatActivity() {

    private lateinit var binding :ActivityMainBinding
    val viewModel :DiscussionViewModel by lazy {
         ViewModelProviders.of(this)[DiscussionViewModel::class.java]
    }
    val controller  = DiscussionController()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = DiscussionRepository(getSharedPreferences(SHARED_PREFERENCES_STRING, Context.MODE_PRIVATE))
        binding = DataBindingUtil.setContentView(this , R.layout.activity_main)
        binding.controller = controller
        setSupportActionBar(binding.toolbar)
        viewModel.initialize(repository)
        viewModel.observe(this ){
            it.discusion.handleDiscussion()
        }

    }
    fun Async<Discussion>.handleDiscussion(){
        when(this){
            is Success -> {
                controller.setData(invoke())
            }
        }

    }
}
