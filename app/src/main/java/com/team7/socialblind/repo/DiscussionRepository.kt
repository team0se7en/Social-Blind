package com.team7.socialblind.repo

import android.content.SharedPreferences
import com.google.firebase.database.*
import com.roacult.kero.team7.jstarter_domain.functional.Either
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.models.Message
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DiscussionRepository(private val sharedPreferences: SharedPreferences) {


    private val databaseReference = FirebaseDatabase.getInstance().reference
    fun observeMessages():Observable<Discussion> {
        val behaviorSubject  = BehaviorSubject.create<Discussion>()
        val currentDiscussion = getCurrentDiscussionId()
        val userId = getUserId()
        Timber.e("$currentDiscussion")

        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussion)
            .addValueEventListener(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Timber.e(p0.details)
                behaviorSubject.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                val messages = p0.child(MESSAGES)
                var list= emptyList<Message>()
                for (message in messages.children){
                    list += Message(message.key!!
                        ,message.child(MESSAGE_TEXT).getValue(String::class.java)!!
                        , message.child(from).getValue(String::class.java)!! == userId )
                }
                behaviorSubject.onNext(Discussion(list.asReversed() , p0.child(REMOTE_CURRENT_SUBJECT).value as String ))
            }
        })
        return behaviorSubject.toFlowable(BackpressureStrategy.DROP).toObservable()
    }
    suspend fun sendMessage(text:String):Either<SendMessageFailure , None> {
        val currentDiscussion = getCurrentDiscussionId()
        val userId = getUserId()
        return databaseReference.child(DISCUSSION_ROOT).child(currentDiscussion).child(MESSAGES).push().submitMessage(text, userId)
    }
    private fun getUserId():String{
        return sharedPreferences.getString(UID ,"anonymous_00")!!
    }
    private fun getCurrentDiscussionId():String{
        return sharedPreferences.getString(CURRENT_DISSC , "0")!!

    }
    private suspend fun DatabaseReference.submitMessage(text:String , uid:String):Either<SendMessageFailure , None> = suspendCoroutine{
        continuation ->
        this.setValue(RemoteMessage(uid , text)).addOnCompleteListener {
            if(it.isSuccessful){
                continuation.resume(Either.Right(None()))
            }else{
                Timber.e(it.exception)
                continuation.resume(Either.Left(SendMessageFailure))
            }
        }

    }

    suspend fun getLeftSeconds():Either<GetCreatedAtFailure , Long> = suspendCoroutine{
        val currentDiscussion = getCurrentDiscussionId()
        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussion).addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Timber.e(p0.details)
                it.resume(Either.Left(GetCreatedAtFailure))
            }

            override fun onDataChange(p0: DataSnapshot) {
                val long = p0.child(CREATED_AT).getValue(Long::class.java)!!
                val currentSeconde  = System.currentTimeMillis() /1000
                Timber.e("${currentSeconde-long}")
                it.resume(Either.Right(if(currentSeconde - long >1800) -1 else currentSeconde - long ))
            }
        })

    }
    fun deleteTime(){
        val currentDiscussion = getCurrentDiscussionId()
        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussion).child(CREATED_AT).setValue(0L).addOnCompleteListener {
            if(!it.isSuccessful){
                Timber.e(it.exception)
                Timber.e("Failure")
            }
        }
    }
}
data class RemoteMessage(val from:String ,val text :String )

class None