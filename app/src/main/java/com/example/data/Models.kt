package com.example.data

data class GoogleAccount(
    val id: String,
    val email: String,
    val displayName: String,
    val creditsBalance: Double,
    val isPrimary: Boolean = false
)

data class GitHubRepository(
    val owner: String,
    val name: String,
    val description: String,
    val defaultBranch: String = "main",
    val stars: Int = 0
)

data class RepositoryFile(
    val path: String,
    val name: String,
    val content: String
)

data class AiModelOption(
    val id: String,
    val name: String,
    val provider: String,
    val creditCost: Double,
    val description: String,
    val badge: String,
    val isFree: Boolean = false
)

data class AgentRun(
    val id: String,
    val prompt: String,
    val modelName: String,
    val accountEmail: String,
    val repoName: String,
    val status: AgentStatus,
    val outputText: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AgentStatus {
    IDLE, ANALYZING, PLANNING, CODING, VERIFYING, COMPLETED, ERROR
}
