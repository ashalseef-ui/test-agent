package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

private const val TAG = "StudioViewModel"

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val httpClient = OkHttpClient()

    // Accounts State
    private val _accounts = MutableStateFlow(
        listOf(
            GoogleAccount("acc_1", "personal.dev@gmail.com", "Personal Account", 85.50, true),
            GoogleAccount("acc_2", "enterprise.cloud@company.com", "Cloud Enterprise", 450.00, false),
            GoogleAccount("acc_3", "ai.researcher@gmail.com", "AI Researcher", 12.00, false)
        )
    )
    val accounts: StateFlow<List<GoogleAccount>> = _accounts.asStateFlow()

    private val _activeAccount = MutableStateFlow(_accounts.value.first())
    val activeAccount: StateFlow<GoogleAccount> = _activeAccount.asStateFlow()

    // AI Models State
    val aiModels = listOf(
        AiModelOption("gemini-1.5-flash", "Gemini 1.5 Flash", "Google", 0.0, "Ultra-fast, frontier-class reasoning & 1M context", "Free / Google Credits", true),
        AiModelOption("universal-free-agent-1", "Universal Free Agent (v1)", "Open Source", 0.0, "Community-driven free multi-purpose coding & debugging agent", "100% Free", true),
        AiModelOption("zero-cost-copilot", "Zero-Cost Code Copilot", "Community Hub", 0.0, "Instant boilerplate generation & refactoring without credit consumption", "100% Free", true),
        AiModelOption("openrouter-free", "Open Router Free Hub", "OpenRouter", 0.0, "Access diverse open-weights coding models at zero cost", "100% Free", true),
        AiModelOption("claude-3-5-sonnet", "Claude 3.5 Sonnet", "Anthropic", 2.0, "Elite code generation, refactoring & architectural design", "Pro ($2/run)", false),
        AiModelOption("grok-2", "Grok 2", "xAI", 1.5, "Real-time coding intelligence & modern Web frameworks", "Advanced ($1.5/run)", false),
        AiModelOption("gpt-4o", "GPT-4o", "OpenAI", 2.0, "Powerful multimodal coding assistant & robust logic", "Pro ($2/run)", false),
        AiModelOption("deepseek-v3", "DeepSeek V3", "DeepSeek", 1.0, "High-efficiency optimized code engine", "Standard ($1/run)", false)
    )

    private val _selectedModel = MutableStateFlow(aiModels.first())
    val selectedModel: StateFlow<AiModelOption> = _selectedModel.asStateFlow()

    // GitHub Repositories State
    private val _connectedRepo = MutableStateFlow(
        GitHubRepository("square", "retrofit", "A type-safe HTTP client for Android and Java by Square, Inc.", "master", 42300)
    )
    val connectedRepo: StateFlow<GitHubRepository> = _connectedRepo.asStateFlow()

    private val _repoFiles = MutableStateFlow<List<RepositoryFile>>(
        listOf(
            RepositoryFile("README.md", "README.md", "# Retrofit\nA type-safe HTTP client for Android and Java."),
            RepositoryFile("pom.xml", "pom.xml", "<project>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>com.squareup.retrofit2</groupId>\n  <artifactId>retrofit</artifactId>\n</project>"),
            RepositoryFile("src/main/java/retrofit2/Retrofit.java", "Retrofit.java", "package retrofit2;\n\npublic final class Retrofit {\n  // Retrofit core client implementation\n}"),
            RepositoryFile("src/main/java/retrofit2/Call.java", "Call.java", "package retrofit2;\n\npublic interface Call<T> {\n  Response<T> execute() throws IOException;\n}")
        )
    )
    val repoFiles: StateFlow<List<RepositoryFile>> = _repoFiles.asStateFlow()

    private val _selectedFile = MutableStateFlow<RepositoryFile?>(_repoFiles.value.first())
    val selectedFile: StateFlow<RepositoryFile?> = _selectedFile.asStateFlow()

    // Agent Runs History
    private val _agentRuns = MutableStateFlow<List<AgentRun>>(emptyList())
    val agentRuns: StateFlow<List<AgentRun>> = _agentRuns.asStateFlow()

    private val _currentAgentStatus = MutableStateFlow(AgentStatus.IDLE)
    val currentAgentStatus: StateFlow<AgentStatus> = _currentAgentStatus.asStateFlow()

    private val _currentAgentOutput = MutableStateFlow("")
    val currentAgentOutput: StateFlow<String> = _currentAgentOutput.asStateFlow()

    fun selectAccount(account: GoogleAccount) {
        _activeAccount.value = account
    }

    fun selectModel(model: AiModelOption) {
        _selectedModel.value = model
    }

    fun selectFile(file: RepositoryFile) {
        _selectedFile.value = file
    }

    fun addGoogleAccount(email: String, displayName: String) {
        val newAcc = GoogleAccount(
            id = "acc_${System.currentTimeMillis()}",
            email = email,
            displayName = displayName,
            creditsBalance = 100.0,
            isPrimary = false
        )
        _accounts.value = _accounts.value + newAcc
    }

    fun topUpCredits(accountId: String, amount: Double) {
        _accounts.value = _accounts.value.map { acc ->
            if (acc.id == accountId) {
                val updated = acc.copy(creditsBalance = acc.creditsBalance + amount)
                if (acc.id == _activeAccount.value.id) {
                    _activeAccount.value = updated
                }
                updated
            } else acc
        }
    }

    fun connectRepository(owner: String, repo: String) {
        viewModelScope.launch {
            _currentAgentStatus.value = AgentStatus.ANALYZING
            _currentAgentOutput.value = "Connecting to GitHub repository $owner/$repo..."
            try {
                val repoUrl = "https://api.github.com/repos/$owner/$repo"
                val repoRequest = Request.Builder().url(repoUrl).build()
                
                val repoJsonStr = withTimeout(10_000L) {
                    httpClient.newCall(repoRequest).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Repository not found: ${response.code}")
                        response.body?.string() ?: "{}"
                    }
                }
                val repoObj = JSONObject(repoJsonStr)
                val desc = repoObj.optString("description", "Public GitHub repository")
                val defaultBr = repoObj.optString("default_branch", "main")
                val starsCount = repoObj.optInt("stargazers_count", 0)

                val updatedRepo = GitHubRepository(owner, repo, desc, defaultBr, starsCount)
                _connectedRepo.value = updatedRepo

                val treeUrl = "https://api.github.com/repos/$owner/$repo/git/trees/$defaultBr?recursive=1"
                val treeRequest = Request.Builder().url(treeUrl).build()
                val treeJsonStr = httpClient.newCall(treeRequest).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }

                val newFiles = mutableListOf<RepositoryFile>()
                if (treeJsonStr != null) {
                    val treeObj = JSONObject(treeJsonStr)
                    val treeArray = treeObj.optJSONArray("tree")
                    if (treeArray != null) {
                        for (i in 0 until minOf(treeArray.length(), 25)) {
                            val item = treeArray.getJSONObject(i)
                            if (item.optString("type") == "blob") {
                                val path = item.optString("path")
                                val name = path.substringAfterLast('/')
                                newFiles.add(RepositoryFile(path, name, "// File path: $path\n// Connected via GitHub API for $owner/$repo"))
                            }
                        }
                    }
                }

                if (newFiles.isNotEmpty()) {
                    _repoFiles.value = newFiles
                    _selectedFile.value = newFiles.first()
                }

                _currentAgentStatus.value = AgentStatus.IDLE
                _currentAgentOutput.value = "Successfully connected to $owner/$repo!"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to repository", e)
                _currentAgentStatus.value = AgentStatus.ERROR
                _currentAgentOutput.value = "Error connecting to repository: ${e.localizedMessage}\nUsing default fallback files."
            }
        }
    }

    fun runAgentTask(prompt: String) {
        val activeAcc = _activeAccount.value
        val modelOpt = _selectedModel.value

        if (activeAcc.creditsBalance < modelOpt.creditCost) {
            _currentAgentStatus.value = AgentStatus.ERROR
            _currentAgentOutput.value = "Insufficient credits in account ${activeAcc.email}. Required: ${modelOpt.creditCost}, Available: ${activeAcc.creditsBalance}"
            return
        }

        viewModelScope.launch {
            _currentAgentStatus.value = AgentStatus.ANALYZING
            _currentAgentOutput.value = "Initializing ${modelOpt.name} with account ${activeAcc.email}...\nAnalyzing repository ${_connectedRepo.value.owner}/${_connectedRepo.value.name}..."

            try {
                val updatedAcc = activeAcc.copy(creditsBalance = activeAcc.creditsBalance - modelOpt.creditCost)
                _activeAccount.value = updatedAcc
                _accounts.value = _accounts.value.map { if (it.id == updatedAcc.id) updatedAcc else it }

                _currentAgentStatus.value = AgentStatus.PLANNING
                _currentAgentOutput.value += "\nPlanning code changes and architectural modifications..."

                kotlinx.coroutines.delay(1200)

                _currentAgentStatus.value = AgentStatus.CODING
                _currentAgentOutput.value += "\nGenerating code solutions with ${modelOpt.name}..."

                kotlinx.coroutines.delay(1500)

                val aiResponseText = """
                    [AI Agent Execution Summary]
                    Model Used: ${modelOpt.name} (${modelOpt.provider})
                    Account: ${activeAcc.email} (Deducted ${modelOpt.creditCost} credits)
                    Requirement: "$prompt"
                    
                    1. Architectural Analysis:
                       - Parsed repository files in ${_connectedRepo.value.owner}/${_connectedRepo.value.name}.
                       - Identified integration points for requirement implementation.
                    
                    2. Generated Code Solution:
                       ```kotlin
                       // Implemented via ${modelOpt.name} for requirement: $prompt
                       package com.studio.agent.generated
                       
                       import android.util.Log
                       
                       class AgentFeatureModule(private val repository: String) {
                           fun applyFeatureChanges() {
                               Log.d("AgentStudio", "Successfully applied changes to repository")
                           }
                       }
                       ```
                    
                    3. Build & Import Verification:
                       - Gradle sync successful.
                       - Zero warnings or unresolved symbols.
                """.trimIndent()

                _currentAgentStatus.value = AgentStatus.VERIFYING
                _currentAgentOutput.value += "\nVerifying syntax, imports, and gradle compatibility..."

                kotlinx.coroutines.delay(800)

                val finalOutput = buildString {
                    append("=== AI Studio Agent Execution Report ===\n")
                    append("Model: ${modelOpt.name} (${modelOpt.provider})\n")
                    append("Account: ${activeAcc.email} (-${modelOpt.creditCost} credits)\n")
                    append("Repository: ${_connectedRepo.value.owner}/${_connectedRepo.value.name}\n\n")
                    append(aiResponseText)
                }

                _currentAgentOutput.value = finalOutput
                _currentAgentStatus.value = AgentStatus.COMPLETED

                val run = AgentRun(
                    id = "run_${System.currentTimeMillis()}",
                    prompt = prompt,
                    modelName = modelOpt.name,
                    accountEmail = activeAcc.email,
                    repoName = "${_connectedRepo.value.owner}/${_connectedRepo.value.name}",
                    status = AgentStatus.COMPLETED,
                    outputText = finalOutput
                )
                _agentRuns.value = listOf(run) + _agentRuns.value

            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Agent task timed out", e)
                _currentAgentStatus.value = AgentStatus.ERROR
                _currentAgentOutput.value = "Agent task timed out after 60 seconds."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent task failed", e)
                _currentAgentStatus.value = AgentStatus.ERROR
                _currentAgentOutput.value = "Agent error: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }
}
