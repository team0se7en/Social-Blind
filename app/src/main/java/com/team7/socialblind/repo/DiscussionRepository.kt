package com.team7.socialblind.repo

import android.content.SharedPreferences
import android.os.SystemClock
import com.google.firebase.database.*
import com.roacult.kero.team7.jstarter_domain.functional.Either
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.models.Message
import com.team7.socialblind.ui.Info
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import timber.log.Timber
import java.lang.ClassCastException
import java.lang.NullPointerException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DiscussionRepository(private val sharedPreferences: SharedPreferences) {

    var currentString = ""

    private val databaseReference = FirebaseDatabase.getInstance().reference
    fun observeMessages():Observable<Discussion> {
        val behaviorSubject  = BehaviorSubject.create<Discussion>()
        val currentDiscussion = getCurrentDiscussionId()
        val userId = getUserId()
        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussion)
            .addValueEventListener(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Timber.e(p0.details)
                behaviorSubject.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.value==null){

                }
                val messages = p0.child(MESSAGES)
                var list= emptyList<Message>()
                try {
                    for (message in messages.children) {
                        list += Message(
                            message.key!!
                            , message.child(MESSAGE_TEXT).getValue(String::class.java)!!
                            , message.child(from).getValue(String::class.java)!! == userId
                        )

                    }
                }catch (np:NullPointerException){
                    list = emptyList()
                }
                try {
                    val currentSubjectid = p0.child(REMOTE_CURRENT_SUBJECT).value as String
                    databaseReference.child(subject).child(currentSubjectid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {

                            override fun onCancelled(p0: DatabaseError) {
                                behaviorSubject.onError(p0.toException())
                            }

                            override fun onDataChange(p0: DataSnapshot) {

                                behaviorSubject.onNext(Discussion(list.asReversed(), p0.value as String))
                            }
                        })
                }catch (cp:ClassCastException){
                    behaviorSubject.onNext(Discussion(list , SUBJECTS.random()))
                }
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
                it.resume(Either.Right(if(currentSeconde - long >1800) -1 else 1800 -(currentSeconde - long) ))
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

    fun getSubjectObservable():Observable<String>{
        val currentDiscussionId = getCurrentDiscussionId()
        val behaviorSubject = BehaviorSubject.create<String>()
        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussionId).child(REMOTE_CURRENT_SUBJECT).addValueEventListener(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                behaviorSubject.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                val subjectId = p0.value as? String
                val subject = if(subjectId==null) SUBJECTS.random()
                else  SUBJECTS[subjectId.toInt()]
                currentString = subject
                behaviorSubject.onNext(subject)
            }
        })
        return behaviorSubject.toFlowable(BackpressureStrategy.DROP).toObservable()
    }
    suspend fun changeSubject():Either<SendMessageFailure , None> = suspendCoroutine{
        continuation ->
        val element = SUBJECTS.toList().random()
        val currentDiscussionId = getCurrentDiscussionId()
        Timber.e("${SUBJECTS.indexOf(element)}")
        databaseReference.child(DISCUSSION_ROOT).child(currentDiscussionId).child(REMOTE_CURRENT_SUBJECT)
            .setValue(SUBJECTS.indexOf(element).toString()).addOnCompleteListener {
                if(it.isSuccessful){
                    continuation.resume(Either.Right(None()))
                }else{
                    continuation.resume(Either.Left(SendMessageFailure))
                }
            }
    }
    fun onNextClicked():Observable<None>{
        val behaviorSubject = BehaviorSubject.create<None>()
        databaseReference.child(USERS).addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                behaviorSubject.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                val usersFree = p0.children.filter {
                    !it.child("currentDiss").exists()
                }
                if(!usersFree.isEmpty()){
                    val user  = usersFree.random()
                    val userId = getUserId()
                    val otherUserId = getOtherUserId()
                    val currentId = getCurrentDiscussionId()
                    val discussion = databaseReference.child(DISCUSSION_ROOT).push()
                    databaseReference.child(DISCUSSION_ROOT).child(currentId).removeValue().addOnCompleteListener {
                        if(it.isSuccessful){
                            databaseReference.child(USERS).child(userId).child("currentDiss").removeValue().addOnCompleteListener {
                                if(it.isSuccessful){
                                    databaseReference.child(USERS).child(otherUserId).child("currentDiss").removeValue().addOnCompleteListener {
                                        if(it.isSuccessful){
                                            Timber.e("Called")
                                            sharedPreferences.edit().remove(IS_USER).commit()
                                            sharedPreferences.edit().putString(CURRENT_DISSC , discussion.key).commit()
                                            sharedPreferences.edit().putString(OTHER_USER_ID  , user.key).commit()
                                            discussion.child(CREATED_AT).setValue(System.currentTimeMillis() / 1000)
                                            databaseReference.child(USERS).child(userId).child("currentDiss").setValue(discussion.key).addOnCompleteListener {
                                                if(it.isSuccessful){
                                                    databaseReference.child(USERS).child(user.key!!).child("currentDiss").setValue(discussion.key).addOnCompleteListener {
                                                        if(it.isSuccessful){
                                                            val random  = SUBJECTS.random()
                                                            databaseReference.child(DISCUSSION_ROOT).child(discussion.key!!)
                                                                .child(REMOTE_CURRENT_SUBJECT).setValue(SUBJECTS.indexOf(random).toString()).addOnCompleteListener {
                                                                    if(it.isSuccessful){
                                                                        behaviorSubject.onNext(None())
                                                                    }
                                                                }

                                                        }
                                                    }
                                                }
                                            }


                                        }
                                    }
                                }
                            }
                        }
                    }



                }
            }
        })
        return behaviorSubject.toFlowable(BackpressureStrategy.DROP).toObservable()
    }
    suspend fun getProfileInfo(isTimeFinished:Boolean ):Either<SendMessageFailure , Info> = suspendCoroutine {
        continuation ->
        if(isTimeFinished){
            sharedPreferences.edit().putBoolean(IS_USER , true)
            databaseReference.child(USERS).child(sharedPreferences.getString(OTHER_USER_ID, "")!!)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        continuation.resume(Either.Right(Info(p0.child("name").value as String , p0.child("imageUrl").value as String )))
                    }
                })
        }else if(sharedPreferences.getBoolean(IS_USER , false)){
            databaseReference.child(USERS).child(getOtherUserId())
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        continuation.resume(Either.Right(Info(p0.child("name").value as String , p0.child("imageUrl").value as String )))
                    }
                })
        }else{
            continuation.resume(Either.Right(Info("Anonymous", null)))
        }
    }
    fun getOtherUserId():String{
        return sharedPreferences.getString(OTHER_USER_ID, "anonyous_01")!!
    }

}
data class RemoteMessage(val from:String ,val text :String )

class None