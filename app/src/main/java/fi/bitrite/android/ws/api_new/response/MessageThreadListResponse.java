package fi.bitrite.android.ws.api_new.response;

import android.support.annotation.NonNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.MessageThread;

@JsonAdapter(MessageThreadListResponse.Deserializer.class)
public class MessageThreadListResponse {
    public class Thread {

        public class Participant {
            @SerializedName("uid") public int userId;
            public String name;
            public String fullname;
        }

        @SerializedName("thread_id") public int id;
        public String subject;
        @SerializedName("thread_started") public Date started;
        @SerializedName("is_new") public int numUnread;
        public List<Participant> participants;
        public Date lastUpdated;
        public int count;
        public boolean hasTokens;

        public MessageThread toMessageThread(@NonNull List<Message> messages) {
            // Prepares the participants.
            List<Integer> participantIds = new ArrayList<>(participants.size());
            for (Participant participant : participants) {
                participantIds.add(participant.userId);
            }

            return new MessageThread(
                    id, subject, started, isUnread(), participantIds, messages, lastUpdated);
        }

        public boolean isUnread() {
            return numUnread > 0;
        }

    }

    public List<Thread> messageThreads;


    /**
     * This deserializer moves the messages in the raw json array into the {@link #messageThreads}
     * member.
     *
     * [ {message_thread1}, {message_thread2} ]
     * becomes
     * { messageThreads: [{message_thread1}, {message_thread2}] }
     */
    public static class Deserializer implements JsonDeserializer<MessageThreadListResponse> {

        @Override
        public MessageThreadListResponse deserialize(
                JsonElement jsonElement, Type type,
                JsonDeserializationContext context) throws JsonParseException {

            MessageThreadListResponse result = new MessageThreadListResponse();

            Type collectionType = new TypeToken<List<Thread>>(){}.getType();
            result.messageThreads = context.deserialize(jsonElement, collectionType);

            return result;
        }
    }
}
