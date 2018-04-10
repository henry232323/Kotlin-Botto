@file:JvmName("kotlinbot")
package com.github.henry232323.kotlinbot

import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess
import java.util.*
import java.util.function.Consumer


val prefix = "!"

fun main(args: Array<String>) {
    var config: Map<Any, String>? = null
    connect("token")
}

fun connect(token: String) {
    try {
        JDABuilder(AccountType.BOT)
                .addListener(MessageListener())
                .setBulkDeleteSplittingEnabled(false)
                .setGame(Game.of("Big Doinks")) // pun xd
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
    init {
        val commands: Commands = Commands()
    }
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
        val content = e.message.rawContent
        val selfId = e.jda.selfUser.id

        if (content[0].toString() != prefix) {
            return
        }
        val parts: List<String> = content.split("")
        val cmd = parts[0].substring(1)

        if (this.commands.cmds.containsKey(cmd)) {
            try {
                this.commands.cmds[cmd](e, parts.subList(1, parts.size))
            } catch (ex: IndexOutOfBoundsException) {
                reply(e, "You are missing an argument! Look at the help for this command")
            } catch (ex: Exception) {
                reply(e, ex.message)
            }
        }
    }

}

class Commands() {
    val cmds = mutableMapOf<String, Pair<(MessageReceivedEvent, List<String>) -> Unit, String>>()
    val polls = mutableMapOf<Pair<Guild, String>, Poll>()
    init {
        cmds.put("poll", Pair(::poll<MessageReceivedEvent, List<String>>, "Start a poll, `!poll <poll name> <time in seconds> [*option names]`"))
        cmds.put("vote", Pair(::vote<MessageReceivedEvent, List<String>>, "Vote for an option in a poll"))
    }

    fun poll(e: MessageReceivedEvent, args: List<String>) {
        val name = args[0]
        val options = args.subList(2, args.size)
        this.polls[Pair(e.guild, name)] = Poll(e.guild, name, options)
        val time = args[1].toInt()
        if (time > 3600) {
            reply(e, "Polls cannot last longer than 1 hour!")
            return
        }
        reply(e, "Started poll #{name}, `!vote #{name} {option}` to vote!")
        Thread.sleep(time.toLong() * 1000)
    }

    fun vote(e: MessageReceivedEvent, args: List<String>) {
        val poll = polls[Pair(e, args[0])]
        poll.votes.get(args.get(1)!!)!!.add(e.message.getAuthor()!!.getID()!!)!!
        reply(e, "You voted for #{args[1]} in vote #{args[0]}! This option now has #{vote.size} votes!")
    }

}

fun reply(e: MessageReceivedEvent, msg: Message, success: Consumer<Message>? = null) {
    if (!e.isFromType(ChannelType.TEXT) || e.textChannel.canTalk()) {
        e.channel.sendMessage(stripEveryoneHere(msg)).queue(success)
    }
}

fun reply(e: MessageReceivedEvent, embed: MessageEmbed, success: Consumer<Message>? = null) {
    reply(build(embed), success)
}

fun reply(e: MessageReceivedEvent, text: String, success: Consumer<Message>? = null) {
    reply(build(text), success)
}

fun stripEveryoneHere(text: String): String
        = text.replace("@here", "@\u180Ehere")
        .replace("@everyone", "@\u180Eeveryone")

fun build(o: Any): Message
        = MessageBuilder().append(o).build()

class Poll(guild: Guild, name: String, options: List<String>) {
    val votes = options.map { it to mutableSetOf<String>() }.toMap().toMutableMap()
}