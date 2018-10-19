package me.aberrantfox.warmbot.services

import com.google.gson.GsonBuilder
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import me.aberrantfox.kjdautils.internal.logging.DefaultLogger
import me.aberrantfox.warmbot.extensions.fullContent
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class Report(val user: String, val channelId: String, val guildId: String, val messages: MutableMap<String, String>, var queuedMessageId: String?)

data class QueuedReport(val messages: Vector<String> = Vector(), val user: String)

class ReportService(val jda: JDA, private val config: Configuration) {

    private val reportDir = File("reports/")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    val reports = Vector<Report>()
    private val queuedReports = Vector<QueuedReport>()

    fun isReportChannel(channelId: String) = reports.any { it.channelId == channelId }
    fun hasReportChannel(userId: String) = reports.any { it.user == userId } || queuedReports.any { it.user == userId }
    fun getReportByChannel(channelId: String): Report = reports.first { it.channelId == channelId }
    fun getReportByUserId(userId: String): Report = reports.first { it.user == userId }

    fun addReport(user: User, guild: Guild, firstMessage: Message?) {

        val guildConfiguration = config.guildConfigurations.first { g -> g.guildId == guild.id }
        val reportCategory = jda.getCategoryById(guildConfiguration.reportCategory)

        if (reports.none { it.user == user.id }) {
            if (reports.filter { it.guildId == guild.id }.size == config.maxOpenReports)
                return
        }

        if (guild.textChannels.size >= 250) {
            return
        }

        reportCategory.createTextChannel(user.name).queue { channel ->
            channel as TextChannel

            val openingMessage = embed {
                addField("New Report Opened!", "${user.descriptor()} :: ${user.asMention}", false)
                setColor(Color.green)
            }

            channel.sendMessage(openingMessage).queue()
            queuedReports.first { it.user == user.id }.messages.forEach {
                channel.sendMessage(it).queue()
            }

            val newReport = Report(user.id, channel.id, guild.id, ConcurrentHashMap(), firstMessage?.id)
            reports.add(newReport)
            writeReportToFile(newReport)

            queuedReports.removeAll { it.user == user.id }
        }
    }

    fun removeReport(channel: String) {
        reports.removeAll { it.channelId == channel }

        if (config.recoverReports)
            reportDir.listFiles().first { file -> file.name.startsWith(channel) }.delete()
    }

    fun receiveFromUser(userObject: User, message: Message) {
        val user = userObject.id
        val safeMessage = message.fullContent().trim().sanitiseMentions()

        if (reports.any { it.user == user }) {
            val report = reports.first { it.user == user }
            jda.getTextChannelById(report.channelId).sendMessage(safeMessage).queue()
            report.queuedMessageId = message.id

            return
        }

        val queued = queuedReports.firstOrNull { it.user == user }

        if (queued == null) {
            val vector = Vector<String>()
            vector.add(safeMessage)
            queuedReports.add(QueuedReport(vector, user))
        } else {
            queued.messages.addElement(safeMessage)
        }
    }

    fun sendToUser(channelId: String, message: Message) {
        val report = reports.firstOrNull { it.channelId == channelId }

        if (report != null) {
            jda.getUserById(report.user).sendPrivateMessage(message.fullContent(), DefaultLogger())
            report.queuedMessageId = message.id
        }
    }

    fun buildGuildChoiceEmbed(commonGuilds: List<Guild>): MessageEmbed {
        return embed {
            setColor(Color.CYAN)
            setAuthor("Please choose which server's staff you'd like to contact.")
            setThumbnail(jda.selfUser.avatarUrl)
            description("Respond with the number that correlates with the desired server to get started.")
            addBlankField(true)

            commonGuilds.forEachIndexed { index, guild ->
                field {
                    name = "$index) ${guild.name}"
                    inline = false
                }
            }
        }
    }

    fun buildReportOpenedEmbed(guildObject: Guild): MessageEmbed {
        return embed {
            setColor(Color.PINK)
            setAuthor("You've successfully opened a report with the staff of ${guildObject.name}")
            description("Someone will respond shortly, please be patient.")
            setThumbnail(guildObject.iconUrl)
        }
    }

    fun sendReportClosedEmbed(report: Report) {
        jda.getUserById(report.user).sendPrivateMessage(embed {
            setColor(Color.LIGHT_GRAY)
            setAuthor("The staff of ${jda.getGuildById(report.guildId).name} have closed this report.")
            setDescription("If you continue to reply, a new report will be created.")
        }, DefaultLogger())
    }

    fun getCommonGuilds(userObject: User): List<Guild> {
        return userObject.mutualGuilds.filter { g -> g.id in config.guildConfigurations.associateBy { it.guildId } }
    }

    fun loadReports() {
        if (!config.recoverReports && reportDir.exists()) {
            reportDir.deleteRecursively()
            return
        }

        if (!reportDir.exists()) {
            reportDir.mkdirs()
            return
        }

        reportDir.listFiles().forEach {
            val report = gson.fromJson(it.readText(), Report::class.java)

            //If text channel was deleted while bot was offline, delete report file
            if (jda.getTextChannelById(report.channelId) != null)
                reports.addElement(report)
            else
                it.delete()
        }
    }

    fun writeReportToFile(report: Report) {
        if (config.recoverReports)
            File("$reportDir/${report.channelId}.json").writeText(gson.toJson(report))
    }
}
