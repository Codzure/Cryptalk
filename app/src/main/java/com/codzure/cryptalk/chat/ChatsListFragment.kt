package com.codzure.cryptalk.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.codzure.cryptalk.adapters.ConversationsAdapter
import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.databinding.FragmentChatsListBinding
import java.util.UUID

class ChatsListFragment : Fragment() {

    private var _binding: FragmentChatsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ConversationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val messages = getDummyMessages()
        val conversations = getConversations(messages)

        if (conversations.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.chatsRecyclerView.visibility = View.GONE
            return
        } else {
            binding.emptyView.visibility = View.GONE
            binding.chatsRecyclerView.visibility = View.VISIBLE
        }

        adapter = ConversationsAdapter(conversations) { userId ->
            val conversation = conversations.find { it.userId == userId }
            if (conversation != null) {
                val action =
                    ChatsListFragmentDirections.toChatFragment(userId, conversation.userName)
                findNavController().navigate(action)
            }
        }

        binding.chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatsListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun getConversations(messages: List<Message>): List<Conversation> {
        return messages.groupBy { it.senderNumber }
            .mapNotNull { (userId, userMessages) ->
                val lastMessage = userMessages.maxByOrNull { it.timestamp }
                lastMessage?.let {
                    Conversation(
                        userId = userId,
                        userName = it.sender,
                        lastMessage = if (it.isEncrypted) "🔒 Encrypted Message" else it.encodedText,
                        timestamp = it.timestamp,
                        isEncrypted = it.isEncrypted
                    )
                }
            }
            .sortedByDescending { it.timestamp }
    }

    private fun getDummyMessages(): List<Message> {
        return listOf(
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Luna",
                senderNumber = "101",
                encodedText = "yooo what's the vibe tonight? 🎉",
                pinHash = null,
                isEncrypted = false,

                ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Kai",
                senderNumber = "102",
                encodedText = "this msg is top secret 🤫",
                pinHash = "hashed_pin_456",
                isEncrypted = true
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Zara",
                senderNumber = "103",
                encodedText = "just dropped a 🔥 meme in the group chat lol",
                pinHash = null,
                isEncrypted = false
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Leo",
                senderNumber = "104",
                encodedText = "can't talk rn, vibin' to lo-fi 🎧",
                pinHash = null,
                isEncrypted = false
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Nina",
                senderNumber = "105",
                encodedText = "this msg self-destructs in 3...2...💥",
                pinHash = "hashed_pin_789",
                isEncrypted = true
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Kai",
                senderNumber = "102",
                encodedText = "bet. see u all at 8 🕗",
                pinHash = null,
                isEncrypted = false
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Luna",
                senderNumber = "101",
                encodedText = "don’t leave me on read 😤",
                pinHash = null,
                isEncrypted = false
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Leo",
                senderNumber = "104",
                encodedText = "new playlist just dropped 🚨",
                pinHash = null,
                isEncrypted = false
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Zara",
                senderNumber = "103",
                encodedText = "encrypted tea ☕️ incoming",
                pinHash = "hashed_pin_111",
                isEncrypted = true
            ),
            Message(
                id = UUID.randomUUID().toString(),
                sender = "Nina",
                senderNumber = "105",
                encodedText = "u ghosted? or nah?",
                pinHash = null,
                isEncrypted = false
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


//val conversations = viewModel.getConversations().observe(viewLifecycleOwner) { updateUI(it) }