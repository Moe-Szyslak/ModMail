package me.aberrantfox.warmbot

import me.aberrantfox.kjdautils.api.startBot
import me.aberrantfox.kjdautils.internal.command.ConversationService
import me.aberrantfox.warmbot.conversations.guildSetupConversation
import me.aberrantfox.warmbot.listeners.*
import me.aberrantfox.warmbot.services.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Game


fun main(args: Array<String>) {
    val config = loadConfiguration()

    if (config == null) {
        println("Please fill in the configuration file (config/config.json)")
        return
    }

    start(config)
}

private fun start(config: Configuration) = startBot(config.token) {
    val reportService = ReportService(jda, config).apply {  loadReports() }
    val conversationService = ConversationService(jda, this.config).apply {
        registerConversations("me.aberrant.warmbot.conversations")
    }

    registerInjectionObject(reportService, config, config.guildConfigurations, conversationService)

    configure {
        prefix = "!!"
        commandPath = "me.aberrantfox.warmbot.commands"
        listenerPath = "me.aberrantfox.warmbot.listeners"
    }

    registerCommandPreconditions(produceIsStaffMemberPrecondition(config.guildConfigurations), produceIsGuildOwnerPrecondition())

    config.guildConfigurations.forEach {
        addOverrides(jda, it)
    }

    jda.presence.setPresence(Game.of(Game.GameType.DEFAULT, "DM to contact Staff"), true)
}

private fun addOverrides(jda: JDA, config: GuildConfiguration) {
    val staffRole = jda.getRolesByName(config.staffRoleName, true).first()
    val reportCategory = jda.getCategoryById(config.reportCategory)
    val archiveChannel = jda.getTextChannelById(config.archiveChannel)

    reportCategory.putPermissionOverride(staffRole).setAllow(Permission.MESSAGE_READ).queue()
    reportCategory.putPermissionOverride(reportCategory.guild.publicRole).setDeny(Permission.MESSAGE_READ).queue()

    archiveChannel.putPermissionOverride(staffRole).setAllow(Permission.MESSAGE_READ).queue()
    archiveChannel.putPermissionOverride(reportCategory.guild.publicRole).setDeny(Permission.MESSAGE_READ).queue()
}