package me.aberrantfox.warmbot.services

import com.google.gson.GsonBuilder
import java.io.File

data class LoggingConfiguration(val loggingChannel: String = "insert-id",
                                val logStartup: Boolean = true,
                                val logMemberOpen: Boolean = true,
                                val logStaffOpen: Boolean = true,
                                val logArchive: Boolean = true,
                                val logClose: Boolean = true)

data class GuildConfiguration(var guildId: String = "insert-id",
                              var reportCategory: String = "insert-id",
                              var archiveChannel: String = "insert-id",
                              var prefix: String = "!",
                              var staffRoleName: String = "Staff",
                              var loggingConfiguration: LoggingConfiguration? = LoggingConfiguration())

data class Configuration(val token: String = "insert-token-here",
                         val maxOpenReports: Int = 50,
                         val recoverReports: Boolean = true,
                         var guildConfigurations: MutableList<GuildConfiguration> = mutableListOf(GuildConfiguration()))

private val gson = GsonBuilder().setPrettyPrinting().create()
private val configDir = File("config/")
private val configFile = File("${configDir.name}/config.json")

fun loadConfiguration(): Configuration? =
    if (!configFile.exists()) {
        configDir.mkdirs()
        configFile.writeText(gson.toJson(Configuration()))
        println("Please fill in the configuration file:\n${configFile.absolutePath}")
        null
    }
    else
        gson.fromJson(configFile.readText(), Configuration::class.java)

fun saveConfiguration(config: Configuration) = configFile.writeText(gson.toJson(config))

fun hasGuildConfiguration(guildConfigurations: List<GuildConfiguration>,
                          guildId: String) = guildConfigurations.any { g -> g.guildId == guildId }