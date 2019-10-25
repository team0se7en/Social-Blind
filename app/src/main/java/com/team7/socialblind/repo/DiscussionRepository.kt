package com.team7.socialblind.repo

import android.content.SharedPreferences
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.models.Message
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class DiscussionRepository(val sharedPreferences: SharedPreferences) {


    private val databaseReference = FirebaseDatabase.getInstance().reference
    fun observeMessages():Observable<Discussion> {
        val behaviorSubject  = BehaviorSubject.create<Discussion>()
        val currentDiscussion = getCurrentDiscussionId()
        val userId = getUserId()
        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussion).addValueEventListener(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Timber.e(p0.details)
                behaviorSubject.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                var list= emptyList<Message>()
                for (message in p0.children){
                    list += Message(message.key!! ,message.child(MESSAGE_TEXT).getValue(String::class.java)!!
                        , message.child(from).getValue(String::class.java)!! == userId )
                }
                behaviorSubject.onNext(Discussion(list , p0.child(REMOTE_CURRENT_SUBJECT).value as String ))
            }
        })
        return behaviorSubject.toFlowable(BackpressureStrategy.DROP).toObservable()
    }
    private fun getUserId():String{
        return sharedPreferences.getString(UID ,"")!!
    }
    private fun getCurrentDiscussionId():String{
        return sharedPreferences.getString(CURRENT_DISSC , "")!!

    }
}