package com.team7.socialblind.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.team7.socialblind.util.Loading

const val SUBJECT_STRING = "Her is a subject to talk about it have fun : "
class MainActivity : AppCompatActivity() {
    private lateinit var dialog:AlertDialog

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
        binding.toolbar.userName.text = "Anonymous"
        dialog = AlertDialog.Builder(this).setView(R.layout.search_match).create()
        viewModel.initialize(repository)
        viewModel.observe(this ){
            it.discusion.handleDiscussion()
            it.onNewMassageSent?.getContentIfNotHandled()?.apply {
                handleEvent()
            }
            it.timeFinishedEvent?.getContentIfNotHandled().apply {
             Toast.makeText(this@MainActivity ,"the time is finished ", Toast.LENGTH_LONG ).show()
            }
            it.timeLeft?.getContentIfNotHandled()?.apply {
                if(this ==0L){
                    Timber.e("entered")
                    binding.toolbar.timerView.text = "00:00"
                }else{
                    binding.toolbar.timerView.startTimer(this ){
                        viewModel.setTimeFinished()
                    }
                }

            }
        }
        binding.toolbar.nextButton.setOnClickListener {
            dialog.show()
        }
        binding.sendButton.setOnClickListener {
            // implement the on dots pic clicked
        }
        binding.changeSubject.setOnClickListener {
            viewModel.changeSubject()
        }
        binding.messageEdittext.setOnEditorActionListener { textView, i, keyEvent ->
            val text = binding.messageEdittext.text.toString()
            if (i == EditorInfo.IME_ACTION_SEND && !text.isEmpty()) {
                viewModel.sendMessage(text)
                true
            }
             false
        }

    }
    fun Async<Discussion>.handleDiscussion(){
        when(this){
            is Success -> {
                val disc = invoke()
                controller.setData(disc)
                showLastMessage()
                binding.subject.text = SUBJECT_STRING +disc.currentSubject
                binding.subjectCard.visibility = View.VISIBLE
                binding.progress.visibility = View.GONE
            }
            is Loading -> {
                binding.progress.visibility = View.VISIBLE
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
