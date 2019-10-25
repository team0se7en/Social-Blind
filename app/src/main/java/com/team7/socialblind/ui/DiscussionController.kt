package com.team7.socialblind.ui

import com.airbnb.epoxy.TypedEpoxyController
import com.team7.socialblind.message
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.receivedMessage
import timber.log.Timber

class DiscussionController: TypedEpoxyController<Discussion>() {
    override fun buildModels(data: Discussion) {
       data.messages.forEach {
           Timber.e("Current message $it")
           if(it.isFromMe){
               message {
                   id(it.id)
                   text(it.text)
               }
           }else{
               receivedMessage {
                   id(it.id)
                   text(it.text)
               }
           }
       }
    }
}