@file:JvmName("kotlinbot")

package com.github.henry232323.kotlinbot

import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder

import javax.security.auth.login.LoginException
import java.util.function.Consumer

import kotlin.concurrent.thread
import kotlin.system.exitProcess


val prefix = "pr!"

fun main(args: Array<String>) {
    connect("MzE5MjkwOTQzOTI2Njk3OTk0.Da2UbA.vsU9tOdsigbEitXfdPIoyQglaSI") // fuck it, ill just regen the token every time i update i guess
}

fun connect(token: String) {
    try {
        JDABuilder(AccountType.BOT)
                .addEventListener(MessageListener())
                .setBulkDeleteSplittingEnabled(false)
                .setToken(token)
                .buildBlocking()
    } catch (ex: LoginException) {
        System.err.println(ex.message)
        exitProcess(ExitStatus.INVALID_TOKEN.code)
    }
}

enum class ExitStatus(val code: Int) {
    // Non error
    UPDATE(10),
    SHUTDOWN(11),
    RESTART(12),
    NEW_CONFIG(13),

    // Error
    INVALID_TOKEN(20),
    CONFIG_MISSING(21),
    INSUFFICIENT_ARGS(22),

    // SQL
    SQL_ACCESS_DENIED(30),
    SQL_INVALID_PASSWORD(31),
    SQL_UNKNOWN_HOST(32),
    SQL_UNKNOWN_DATABASE(33)
}

class MessageListener : ListenerAdapter() {
    val commands: Commands = Commands()

    override fun onReady(e: ReadyEvent) {
        val selfUser = e.jda.selfUser
        println("""
            ||-========================================================
            || Account Info: ${selfUser.name}#${selfUser.discriminator} (ID: ${selfUser.id})
            || Connected to ${e.jda.guilds.size} guilds, ${e.jda.textChannels.size} text channels
            || Prefix: ${prefix}
            ||-========================================================
            """.trimMargin("|"))
    }

    override fun onMessageReceived(e: MessageReceivedEvent) {
        if (e.message.getTextChannel().getId() != "324789027280650260") {
            return
        }
        val content = e.message.getContentRaw()
        if (content.length > prefix.length) {
            if (content.substring(0, prefix.length) != prefix) {
                return
            } else {
                thread(block = { processCommand(e, content) })
            }
        }
    }

    fun processCommand(e: MessageReceivedEvent, content: String) {
        val parts: List<String> = content.split(" ")
        if (parts[0] == "") {
            return
        }
        val cmd = parts[0].substring(prefix.length, parts[0].length)
        if (commands.cmds.containsKey(cmd)) {
            try {
                val (func, _) = commands.cmds[cmd]!!
                func(e, parts.subList(1, parts.size))
            } catch (ex: IndexOutOfBoundsException) {
                reply(e, "You are missing an argument! Look at the help for this command")
            } catch (ex: Exception) {
                reply(e, ex.message!!)
            }
        }
    }

}

class Commands {
    val cmds = mutableMapOf<String, Pair<(MessageReceivedEvent, List<String>) -> Unit, String>>()
    val polls = mutableMapOf<Pair<Guild, String>, Poll>()

    init {
        cmds["poll"] = Pair(::poll, "Start a poll, `!poll <poll name> <time in seconds> [*option names]`")
        cmds["vote"] = Pair(::vote, "Vote for an option in a poll")
        cmds["polls"] = Pair(::pollsCmd, "See all available polls")
    }

    fun poll(e: MessageReceivedEvent, args: List<String>) {
        val name = args[0]
        val options = args.subList(2, args.size)
        val cpoll = Poll(e.guild, name, options)
        polls[Pair(e.guild, name)] = cpoll
        val time = args[1].toInt()
        if (time > 3600) {
            reply(e, "Polls cannot last longer than 1 hour!")
            return
        }
        reply(e, "Started poll $name, `${prefix}vote $name {option}` to vote!")
        Thread.sleep(time.toLong() * 1000)
        var top = -1
        var win = 0
        var ctr = 0
        var s: Int
        for ((_, votes) in cpoll.votes.entries) {
            s = votes.size
            if (s > top) {
                top = votes.size
                win = ctr
            }
            ctr += 1
        }
        val winner = options[win]
        reply(e, "Poll ended! $winner won!")
    }

    fun vote(e: MessageReceivedEvent, args: List<String>) {
        val poll = polls[Pair<Guild, String>(e.guild, args[0])]
        if (poll == null) {
            reply(e, "Poll ${args[0]} doesn't exist!")
        } else {
            val votes = poll.votes[args[1]]
            if (votes == null) {
                reply(e, "${args[1]} is not a valid option!")
            } else {
                val authorId: String = e.message.getAuthor().getId()
                votes.add(authorId)
                reply(e, "You voted for ${args[1]} in vote ${args[0]}! This option now has ${votes.size} votes!")
            }
        }
    }

    @Suppress("unused")
    fun pollsCmd(e: MessageReceivedEvent, _args: List<String>) {
        var str = "Current Polls: "
        for ((k, v) in polls.entries) {
            val (_, key) = k
            str += "\n\t$key:"
            for ((opt: String, _: MutableSet<String>) in v.votes.entries) {
                str += "\n\t\t$opt"
            }
        }
        reply(e, str)
    }

}

fun reply(e: MessageReceivedEvent, msg: Message, success: Consumer<Message>? = null) {
    if (!e.isFromType(ChannelType.TEXT) || e.textChannel.canTalk()) {
        e.channel.sendMessage(msg).queue(success)
    }
}

fun reply(e: MessageReceivedEvent, embed: MessageEmbed, success: Consumer<Message>? = null) {
    reply(e, build(embed), success)
}

fun reply(e: MessageReceivedEvent, text: String, success: Consumer<Message>? = null) {
    reply(e, build(stripEveryoneHere(text)), success)
}

fun stripEveryoneHere(text: String): String = text.replace("@here", "@\u180Ehere")
        .replace("@everyone", "@\u180Eeveryone")

fun build(o: Any): Message = MessageBuilder().append(o).build()

class Poll(guild: Guild, name: String, options: List<String>) {
    val votes = options.map { it to mutableSetOf<String>() }.toMap().toMutableMap()
}
