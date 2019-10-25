package com.team7.socialblind.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.team7.socialblind.databinding.ActivityMainBinding
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.models.Message
import com.team7.socialblind.repo.DiscussionRepository
import com.team7.socialblind.repo.None
import com.team7.socialblind.repo.SHARED_PREFERENCES_STRING
import com.team7.socialblind.util.Async
import com.team7.socialblind.util.Event
import com.team7.socialblind.util.Success
import timber.log.Timber
import androidx.recyclerview.widget.LinearLayoutManager
import com.team7.socialblind.R


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
            it.onNewMassageSent?.getContentIfNotHandled()?.apply {
                handleEvent()
            }
        }
        binding.sendButton.setOnClickListener {
            viewModel.sendMessage(binding.messageEdittext.text.toString())
        }

    }
    fun Async<Discussion>.handleDiscussion(){
        when(this){
            is Success -> {
                Timber.e("${invoke()}")
                controller.setData(invoke())
                showLastMessage()
            }
        }

    }
    fun handleEvent(){
        showLastMessage()
        binding.messageEdittext.setText("")

    }
    fun showLastMessage(){
        val layoutManager = binding.recyclerView
            .layoutManager as LinearLayoutManager
        layoutManager.scrollToPositionWithOffset(0, 0)
    }
}
