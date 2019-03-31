package me.aberrantfox.warmbot.commands

import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.internal.command.arguments.*
import me.aberrantfox.kjdautils.internal.di.PersistenceService
import me.aberrantfox.warmbot.extensions.idToTextChannel
import me.aberrantfox.warmbot.messages.Locale
import me.aberrantfox.warmbot.services.Configuration
import net.dv8tion.jda.core.entities.*

@CommandSet("configuration")
fun configurationCommands(configuration: Configuration, persistenceService: PersistenceService) = commands {
    command("SetReportCategory") {
        requiresGuild = true
        description = Locale.messages.SET_REPORT_CATEGORY_DESCRIPTION
        expect(ChannelCategoryArg)
        execute {
            val reportCategory = it.args.component1() as Category

            configuration.getGuildConfig(reportCategory.guild.id)!!.reportCategory = reportCategory.id
            persistenceService.save(configuration)
            it.respond(Locale.inject({ SET_REPORT_CATEGORY_SUCCESSFUL }, "categoryName" to reportCategory.name))
        }
    }

    command("SetArchiveChannel") {
        requiresGuild = true
        description = Locale.messages.SET_ARCHIVE_CHANNEL_DESCRIPTION
        expect(TextChannelArg)
        execute {
            val archiveChannel = it.args.component1() as TextChannel

            configuration.getGuildConfig(archiveChannel.guild.id)!!.archiveChannel = archiveChannel.id
            persistenceService.save(configuration)
            it.respond(Locale.inject({ SET_ARCHIVE_CHANNEL_SUCCESSFUL }, "archiveChannel" to archiveChannel.name))
        }
    }

    command("SetStaffRole") {
        requiresGuild = true
        description = Locale.messages.SET_STAFF_ROLE_DESCRIPTION
        expect(WordArg)
        execute {
            val staffRoleName = it.args.component1() as String
            val staffRole = it.jda.getRolesByName(staffRoleName, true).firstOrNull()

            staffRole ?: return@execute it.respond(Locale.inject({ FAIL_COULD_NOT_FIND_ROLE }, "staffRoleName" to staffRoleName))

            configuration.getGuildConfig(it.message.guild.id)!!.staffRoleName = staffRole.name
            persistenceService.save(configuration)
            it.respond(Locale.inject({ SET_STAFF_ROLE_SUCCESSFUL }, "staffRoleName" to staffRole.name))
        }
    }

    command("SetLoggingChannel") {
        requiresGuild = true
        description = Locale.messages.SET_LOGGING_CHANNEL_DESCRIPTION
        expect(TextChannelArg)
        execute {
            val loggingChannel = it.args.component1() as TextChannel

            configuration.getGuildConfig(loggingChannel.guild.id)!!.loggingConfiguration.loggingChannel = loggingChannel.id
            persistenceService.save(configuration)
            it.respond(Locale.inject({ SET_LOGGING_CHANNEL_SUCCESSFUL }, "loggingChannel" to loggingChannel.name))
        }
    }

    command("AddStaffChannel") {
        requiresGuild = true
        description = Locale.messages.ADD_STAFF_CHANNEL
        expect(TextChannelArg)
        execute {
            val staffChannel = it.args.component1() as TextChannel
            val channelId = staffChannel.id

            configuration.getGuildConfig(it.message.guild.id)!!.staffChannels.apply {
                if (channelId in this)
                    return@execute it.respond("Channel already whitelisted!")

                this.add(staffChannel.id)

                return@execute it.respond("Successfully whitelisted channel :: ${staffChannel.name}")
            }
        }
    }

    command("RemoveStaffChannel") {
        requiresGuild = true
        description = Locale.messages.REMOVE_STAFF_CHANNEL
        expect(TextChannelArg)
        execute {
            val staffChannel = it.args.component1() as TextChannel
            val channelId = staffChannel.id

            configuration.getGuildConfig(it.message.guild.id)!!.staffChannels.apply {
                if (channelId !in this)
                    return@execute it.respond("Channel not whitelisted!")

                this.remove(staffChannel.id)

                return@execute it.respond("Successfully unwhitelisted channel :: ${staffChannel.name}")
            }
        }
    }

    command("ListStaffChannels") {
        requiresGuild = true
        description = Locale.messages.LIST_STAFF_CHANNELS
        execute {
            val staffChannels = configuration.getGuildConfig(it.message.guild.id)!!.staffChannels

            it.respond(staffChannels.joinToString("\n") { "${it.idToTextChannel().asMention} - $it" })
        }
    }
}