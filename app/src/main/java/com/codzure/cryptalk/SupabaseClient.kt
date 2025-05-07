import android.util.Log
import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.data.User
import com.codzure.cryptalk.utils.DataUtils
import com.codzure.cryptalk.utils.SupabaseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannelBuilder
import io.github.jan.supabase.realtime.postgresChangeFlow

/**
 * Singleton class to manage Supabase connection and operations
 */
object SupabaseClient {
    private const val MESSAGES_TABLE = "messages"
    private const val CONVERSATIONS_TABLE = "conversations"
    private const val USERS_TABLE = "users"
    private const val TAG = "SupabaseClient"

    private lateinit var messagesChannel: RealtimeChannel
    private lateinit var conversationsChannel: RealtimeChannel

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO)

    // StateFlows to hold data
    private val _messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    val messagesFlow: StateFlow<List<Message>> = _messagesFlow

    private val _conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())
    val conversationsFlow: StateFlow<List<Conversation>> = _conversationsFlow

    private val _usersFlow = MutableStateFlow<List<User>>(emptyList())
    val usersFlow: StateFlow<List<User>> = _usersFlow

    // Initialize Supabase client
    private val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.SUPABASE_URL,
        supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
    }

    init {
        fetchInitialData()
        setupRealtime()
    }

    private fun setupRealtime() {
        scope.launch {
            try {
                // Setup messages channel
                messagesChannel = client.realtime.channel("public:$MESSAGES_TABLE")
                messagesChannel.subscribe()

                // Setup conversations channel
                conversationsChannel = client.realtime.channel("public:$CONVERSATIONS_TABLE")
                conversationsChannel.subscribe()

                // Collect changes for messages
                scope.launch {
                    client.realtime.postgresChangeFlow<PostgresAction> {
                        schema = "public"
                        table = MESSAGES_TABLE
                    }.collect { action ->
                        Log.d(TAG, "Message table update: $action")
                        fetchMessages()
                    }
                }

                // Collect changes for conversations
                scope.launch {
                    client.realtime.postgresChangeFlow<PostgresAction> {
                        schema = "public"
                        table = CONVERSATIONS_TABLE
                    }.collect { action ->
                        Log.d(TAG, "Conversation table update: $action")
                        fetchConversations()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up realtime channels: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun fetchInitialData() {
        fetchUsers()
        fetchConversations()
        fetchMessages()
    }

    fun fetchMessages() {
        scope.launch {
            try {
                val messages = client.from(MESSAGES_TABLE)
                    .select {
                        order("timestamp", Order.ASCENDING)
                    }
                    .decodeList<Message>()
                _messagesFlow.value = messages
                Log.d(TAG, "Fetched ${messages.size} messages")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching messages: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun fetchConversations() {
        scope.launch {
            try {
                val conversations = client.from(CONVERSATIONS_TABLE)
                    .select {
                        order("last_message_time", Order.DESCENDING)
                    }
                    .decodeList<Conversation>()
                _conversationsFlow.value = conversations
                Log.d(TAG, "Fetched ${conversations.size} conversations")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching conversations: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun fetchUsers() {
        scope.launch {
            try {
                val users = client.from(USERS_TABLE)
                    .select()
                    .decodeList<User>()
                _usersFlow.value = users
                Log.d(TAG, "Fetched ${users.size} users")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching users: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun addMessage(message: Message) {
        try {
            val response = client.from(MESSAGES_TABLE)
                .insert(message)
            Log.d(TAG, "Message added successfully: $response")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding message: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updateMessageReadStatus(messageId: String, isRead: Boolean) {
        try {
            val response = client.from(MESSAGES_TABLE)
                .update(mapOf("is_read" to isRead))
                //.eq("id", messageId)

            Log.d(TAG, "Message read status updated: $response")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message read status: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getOrCreateConversation(
        participantOneId: String,
        participantTwoId: String
    ): String {
        // Try to find existing conversation
        val existingConversation = conversationsFlow.value.find { conv ->
            (conv.participantOneId == participantOneId && conv.participantTwoId == participantTwoId) ||
                    (conv.participantOneId == participantTwoId && conv.participantTwoId == participantOneId)
        }

        if (existingConversation != null) {
            return existingConversation.id
        }

        // Create new conversation
        val newConversation = Conversation(
            id = UUID.randomUUID().toString(),
            participantOneId = participantOneId,
            participantTwoId = participantTwoId,
            lastMessageTime = System.currentTimeMillis()
        )

        try {
            val response = client.from(CONVERSATIONS_TABLE)
                .insert(newConversation)
            Log.d(TAG, "New conversation created: $response")
            fetchConversations() // Refresh conversations
            return newConversation.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating conversation: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updateConversationLastMessage(
        conversationId: String,
        messageId: String,
        timestamp: Long
    ) {
        try {
            // Find the conversation
            val conversation = conversationsFlow.value.find { it.id == conversationId }
                ?: throw Exception("Conversation not found")

            // Update unread count based on message sender
            val currentUserId = DataUtils.currentUser.id
            val updateData = if (conversation.participantOneId == currentUserId) {
                mapOf(
                    "last_message_id" to messageId,
                    "last_message_time" to timestamp,
                    "unread_two" to conversation.unreadTwo + 1
                )
            } else {
                mapOf(
                    "last_message_id" to messageId,
                    "last_message_time" to timestamp,
                    "unread_one" to conversation.unreadOne + 1
                )
            }

            val response = client.from(CONVERSATIONS_TABLE)
                .update(updateData) {
                    //eq("id", conversationId)
                }
            Log.d(TAG, "Conversation last message updated: $response")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating conversation: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun markConversationAsRead(conversationId: String) {
        try {
            // Find the conversation
            val conversation = conversationsFlow.value.find { it.id == conversationId }
                ?: throw Exception("Conversation not found")

            // Update unread count based on current user
            val updateData = if (conversation.participantOneId == DataUtils.currentUser.id) {
                mapOf("unread_one" to 0)
            } else {
                mapOf("unread_two" to 0)
            }

            val response = client.from(CONVERSATIONS_TABLE)
                .update(updateData) {
                    //eq("id", conversationId)
                }
            Log.d(TAG, "Conversation marked as read: $response")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking conversation as read: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun unsubscribeAll() {
        try {
            messagesChannel.unsubscribe()
            conversationsChannel.unsubscribe()
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from channels: ${e.message}")
            e.printStackTrace()
        }
    }
}