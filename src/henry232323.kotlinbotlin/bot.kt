package henry232323.kotlinbotlin

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.hooks.ListenerAdapter
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess


val prefix = "!"
fun main(args: Array<String>) {
    var config: Map<Any, String>? = null
    connect("Token goes here")
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
    commands = Commands()
    override fun onReady(e: ReadyEvent) {
            val selfUser = e.jda.selfUser
            println("""
            ||-========================================================
            || Account Info: ${selfUser.name}#${selfUser.discriminator} (ID: ${selfUser.id})
            || Connected to ${e.jda.guilds.size} guilds, ${e.jda.textChannels.size} text channels
            || Prefix: ${ConfigManager.getDefaultPrefix()}
            ||-========================================================
            """.trimMargin("|"))
    }

    override fun onMessageReceived(e: MessageReceivedEvent) {
        val content = e.message.rawContent
        val selfId = e.jda.selfUser.id

        if content[0] != prefix {
            return
        }
        val parts: List<String> = content.split("")
        val cmd = parts[0].substring(1)

        if commands.cmds.containsKey(cmd) {
            try {
                commands.cmds[cmd](e, parts.subList(1))
            } catch (ex: Exception) {
                commands.reply(e, ex.message)
            }
        }
    }

}

class Commands() {
    val cmds = mutableMapOf<String, Pair<(MessageReceivedEvent, List<String>) -> Unit, String>>()
    val polls = mutableMapOf<Pair<Guiold, String>, mutableMapOf<String, Int>>
    fun poll(e: MessageReceivedEvent, args: List<String>) {

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

}
