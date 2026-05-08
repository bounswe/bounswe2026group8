package com.bounswe2026group8.emergencyhub.ui

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bounswe2026group8.emergencyhub.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class TutorialActivity : AppCompatActivity() {

    private enum class Screen { DASHBOARD, FORUM, POST_DETAIL, POST_CREATE, HELP_LIST, HELP_DETAIL, HELP_CREATE, INFO }

    private data class GuideStep(val target: String, val title: String, val text: String)
    private data class ForumPost(
        val id: String,
        val title: String,
        val body: String,
        val author: String,
        val type: String = "GLOBAL",
        var upvotes: Int = 0,
        var downvotes: Int = 0,
        val isMine: Boolean = false,
        val comments: MutableList<Comment> = mutableListOf()
    )
    private data class HelpRequest(
        val id: String,
        val title: String,
        val description: String,
        val category: String,
        val urgency: String,
        val location: String,
        val author: String,
        var status: String = "OPEN",
        val isMine: Boolean = false,
        val comments: MutableList<Comment> = mutableListOf()
    )
    private data class Comment(val author: String, val body: String)

    private var screen = Screen.DASHBOARD
    private var selectedPostId: String? = null
    private var selectedRequestId: String? = null
    private var guideIndex = 0
    private var guideSteps: List<GuideStep> = emptyList()
    private val targetViews = mutableMapOf<String, View>()
    private val originalTextBackgrounds = mutableMapOf<TextView, android.graphics.drawable.Drawable?>()

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var guideCard: MaterialCardView
    private lateinit var guideTitle: TextView
    private lateinit var guideText: TextView
    private lateinit var guideCount: TextView
    private lateinit var skipButton: MaterialButton
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton

    private val amber by lazy { ContextCompat.getColor(this, R.color.urgency_medium) }
    private val accent by lazy { ContextCompat.getColor(this, R.color.accent) }
    private val accentSecondary by lazy { ContextCompat.getColor(this, R.color.accent_secondary) }
    private val bgBase by lazy { ContextCompat.getColor(this, R.color.bg_base) }
    private val bgCard by lazy { ContextCompat.getColor(this, R.color.bg_surface) }
    private val bgInput by lazy { ContextCompat.getColor(this, R.color.bg_input) }
    private val textPrimary by lazy { ContextCompat.getColor(this, R.color.text_primary) }
    private val textSecondary by lazy { ContextCompat.getColor(this, R.color.text_secondary) }
    private val textMuted by lazy { ContextCompat.getColor(this, R.color.text_muted) }
    private val border by lazy { ContextCompat.getColor(this, R.color.border) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderDashboard()
    }

    private fun buildShell(title: String, steps: List<GuideStep>) {
        guideSteps = steps
        guideIndex = 0
        targetViews.clear()
        originalTextBackgrounds.clear()

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(bgBase)
            isFillViewport = true
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }
        scrollView.addView(root)
        setContentView(scrollView)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(text(title, 24, accent, bold = true), weightParams())
        val navButton = if (screen == Screen.DASHBOARD) {
            button("Exit", secondary = true) { finish() }
        } else {
            button("←", secondary = true) { renderDashboard() }.apply { minWidth = dp(48) }
        }
        header.addView(navButton)
        root.addView(header, margins(bottom = 12))

        guideCard = card(radiusDp = 14).apply {
            setCardBackgroundColor(bgCard)
            setStrokeColor(accent)
            strokeWidth = dp(1)
        }
        val guideLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        guideCount = text("", 12, accent, bold = true)
        guideTitle = text("", 18, textPrimary, bold = true)
        guideText = text("", 14, textSecondary)
        guideLayout.addView(guideCount)
        guideLayout.addView(guideTitle, margins(top = 4))
        guideLayout.addView(guideText, margins(top = 4))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(10), 0, 0)
        }
        skipButton = button("Skip guide", secondary = true) {
            guideCard.visibility = View.GONE
            clearHighlights()
        }
        previousButton = button("Previous", secondary = true) {
            if (guideIndex > 0) {
                guideIndex -= 1
                updateGuide()
            }
        }
        nextButton = button("Next") {
            if (guideIndex == guideSteps.lastIndex) {
                guideCard.visibility = View.GONE
                clearHighlights()
            } else {
                guideIndex += 1
                updateGuide()
            }
        }
        actions.addView(skipButton)
        actions.addView(previousButton)
        actions.addView(nextButton)
        guideLayout.addView(actions)
        guideCard.addView(guideLayout)
        root.addView(guideCard, margins(bottom = 16))

        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)
    }

    private fun renderDashboard() {
        screen = Screen.DASHBOARD
        buildShell(
            "Emergency Hub",
            listOf(
                GuideStep("welcome", "Start here", "Explore the main areas of the app and learn what to do during an emergency."),
                GuideStep("hub", "Choose your hub", "Your neighborhood hub keeps local posts and requests focused on the right area."),
                GuideStep("scenario", "Follow the story", "The guide follows one neighborhood emergency so each step feels connected."),
                GuideStep("forum", "Read community updates", "Use the forum when you want to share or follow neighborhood information."),
                GuideStep("help", "Ask for help", "Use help requests when someone needs supplies, shelter, transport, or medical help."),
                GuideStep("info", "Keep guidance close", "Emergency info gives you useful steps to review quickly.")
            )
        )

        val hub = pill("Besiktas Hub", active = true).apply { gravity = Gravity.CENTER }
        register("hub", hub)
        content.addView(hub, margins(bottom = 14))

        val welcome = dashboardWelcomeCard()
        register("welcome", welcome)
        content.addView(welcome, margins(bottom = 12))

        val scenario = cardBlock("Scenario", "After a neighborhood power outage, learn how to ask for water and share updates.")
        register("scenario", scenario)
        content.addView(scenario, margins(bottom = 12))

        content.addView(featureRow(
            featureCard("💬", "Forum", "Community updates", "forum") { renderForum() },
            featureCard("🆘", "Help Requests", "Ask or answer", "help") { renderHelpList() }
        ))
        content.addView(featureRow(
            featureCard("👤", "Profile", "Sign in to use Profile", null, enabled = false) {},
            featureCard("📶", "Emergency Info", "Offline guidance", "info") { renderInfo() }
        ), margins(top = 12))
        content.addView(featureRow(
            featureCard("📡", "Offline Messages", "Sign in to use Offline Messages", null, enabled = false) {},
            View(this)
        ), margins(top = 12))
        updateGuide()
    }

    private fun renderForum() {
        screen = Screen.FORUM
        buildShell(
            "Forum",
            listOf(
                GuideStep("tabs", "Switch forum areas", "Global, Standard, and Urgent tabs match the main forum structure."),
                GuideStep("sort", "Sort updates", "Switch between newest, most liked, and hot posts to scan the forum in different ways."),
                GuideStep("posts", "Open a post", "Tap a post to read the full update and join the conversation."),
                GuideStep("create", "Create a post", "Use the new post button to write a community update.")
            )
        )
        val newPost = button("New post") { renderPostCreate() }
        register("create", newPost)
        content.addView(labelRow("All hubs", "Besiktas Hub"), margins(bottom = 10))
        val tabs = forumTabs()
        register("tabs", tabs)
        content.addView(tabs, margins(bottom = 12))
        content.addView(newPost, margins(bottom = 10))

        val sortRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        sortRow.addView(pill("Newest", active = true))
        sortRow.addView(pill("Most liked"))
        sortRow.addView(pill("Hot"))
        register("sort", sortRow)
        content.addView(sortRow, margins(bottom = 10))

        SampleData.posts.forEach { post ->
            val postCard = forumPostCard(post)
            postCard.setOnClickListener {
                selectedPostId = post.id
                renderPostDetail()
            }
            register("posts", postCard)
            content.addView(postCard, margins(bottom = 10))
        }
        updateGuide()
    }

    private fun renderPostDetail() {
        screen = Screen.POST_DETAIL
        val post = SampleData.posts.firstOrNull { it.id == selectedPostId } ?: return renderForum()
        buildShell(
            "Forum Post",
            listOf(
                GuideStep("summary", "Read the update", "Start with the post type, title, author, and details."),
                GuideStep("votes", "React to the post", "Use upvote or downvote to signal whether the update is useful."),
                GuideStep("comments", "Join the conversation", "Comments help neighbors ask questions or add useful details.")
            )
        )
        val summary = forumDetailCard(post)
        register("summary", summary)
        content.addView(summary, margins(bottom = 12))

        val votes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        votes.addView(pill("▲ ${post.upvotes}").apply {
            setOnClickListener {
                post.upvotes += 1
                renderPostDetail()
            }
        })
        votes.addView(pill("▼ ${post.downvotes}").apply {
            setOnClickListener {
                post.downvotes += 1
                renderPostDetail()
            }
        })
        votes.addView(pill("Share"))
        register("votes", votes)
        content.addView(votes, margins(bottom = 12))

        val comments = addCommentSection(post.comments) { body ->
            post.comments.add(0, Comment("You", body))
            renderPostDetail()
        }
        register("comments", comments)
        content.addView(comments)
        updateGuide()
    }

    private fun renderPostCreate() {
        screen = Screen.POST_CREATE
        buildShell(
            "New Post",
            listOf(
                GuideStep("type", "Choose where it belongs", "Global posts are broad updates. Urgent posts should be used for time-sensitive warnings."),
                GuideStep("title", "Use a clear title", "A short title helps neighbors understand the update quickly."),
                GuideStep("content", "Write useful details", "Share what happened, where it happened, and what neighbors should do next."),
                GuideStep("save", "Save the post", "Save when the title and details are clear.")
            )
        )
        val titleInput = edit("Charging station open at the community center")
        val bodyInput = edit("Volunteers can help people charge phones until 18:00. Bring your own cable if possible.", minLines = 4)
        val typeCard = cardBlock("Forum type", "GLOBAL")
        register("type", typeCard)
        content.addView(typeCard, margins(bottom = 10))
        register("title", titleInput)
        content.addView(labeled("Title", titleInput))
        register("content", bodyInput)
        content.addView(labeled("Content", bodyInput))
        val save = button("Save post") {
            val title = titleInput.text.toString().trim()
            val body = bodyInput.text.toString().trim()
            if (title.isBlank() || body.isBlank()) {
                Toast.makeText(this, "Add a title and details.", Toast.LENGTH_SHORT).show()
            } else {
                SampleData.posts.add(0, ForumPost("local-post-${System.nanoTime()}", title, body, "You", upvotes = 1, isMine = true))
                renderForum()
            }
        }
        register("save", save)
        content.addView(save, margins(top = 4))
        updateGuide()
    }

    private fun renderHelpList() {
        screen = Screen.HELP_LIST
        buildShell(
            "Help Center",
            listOf(
                GuideStep("tabs", "Switch lists", "Requests and offers are separated so you can focus on what you need."),
                GuideStep("filters", "Filter by need", "Categories help neighbors find requests they can answer quickly."),
                GuideStep("requests", "Review requests", "Your requests appear here alongside other neighborhood needs."),
                GuideStep("create", "Create a request", "Create a request when someone needs supplies, transport, shelter, or medical help.")
            )
        )
        content.addView(labelRow("Hub", "Besiktas Hub"), margins(bottom = 10))
        val tabs = segmentedTabs("Requests", "Offers")
        register("tabs", tabs)
        content.addView(tabs, margins(bottom = 10))
        val create = button("New") { renderHelpCreate() }
        register("create", create)
        content.addView(create, margins(bottom = 10))
        val filters = chipScroller(listOf("All", "Medical", "Food", "Shelter", "Transport", "Other"))
        register("filters", filters)
        content.addView(filters, margins(bottom = 8))

        SampleData.requests.forEach { request ->
            val card = helpRequestCard(request)
            card.setOnClickListener {
                selectedRequestId = request.id
                renderHelpDetail()
            }
            register("requests", card)
            content.addView(card, margins(bottom = 10))
        }
        updateGuide()
    }

    private fun renderHelpDetail() {
        screen = Screen.HELP_DETAIL
        val request = SampleData.requests.firstOrNull { it.id == selectedRequestId } ?: return renderHelpList()
        buildShell(
            "Help Request Detail",
            listOf(
                GuideStep("summary", "Review the request", "Start with the category, urgency, description, and who posted it."),
                GuideStep("actions", "Manage your request", "You can only manage requests you created. When the need is handled, mark it as resolved or remove it if it is no longer needed."),
                GuideStep("location", "Check the location", "The location note helps helpers understand where support is needed."),
                GuideStep("comments", "Coordinate in comments", "Comments are useful for asking follow-up questions or offering help.")
            )
        )
        val summary = helpDetailCard(request)
        register("summary", summary)
        content.addView(summary, margins(bottom = 12))

        if (request.isMine) {
            val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            if (request.status != "RESOLVED") {
                actions.addView(button("Mark as resolved") {
                    request.status = "RESOLVED"
                    renderHelpDetail()
                })
            }
            actions.addView(button("Delete", secondary = true) {
                AlertDialog.Builder(this)
                    .setTitle("Delete request?")
                    .setMessage("This removes the request from the list.")
                    .setPositiveButton("Delete") { _, _ ->
                        SampleData.requests.removeAll { it.id == request.id }
                        renderHelpList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }, margins(top = 6))
            register("actions", actions)
            content.addView(actions, margins(bottom = 12))
        }

        val location = locationBlock(request.location)
        register("location", location)
        content.addView(location, margins(bottom = 12))

        val comments = addCommentSection(request.comments) { body ->
            request.comments.add(0, Comment("You", body))
            renderHelpDetail()
        }
        register("comments", comments)
        content.addView(comments)
        updateGuide()
    }

    private fun renderHelpCreate() {
        screen = Screen.HELP_CREATE
        buildShell(
            "New Help Request",
            listOf(
                GuideStep("title", "Write a clear title", "A short title helps neighbors understand the need quickly."),
                GuideStep("description", "Add useful details", "Describe who needs help, what is needed, and any safety details."),
                GuideStep("category", "Pick category and urgency", "These choices help neighbors find urgent needs and understand what they can do."),
                GuideStep("save", "Review before saving", "Review the details before adding the request to the list.")
            )
        )
        val titleInput = edit("Need drinking water for an elderly neighbor")
        val descInput = edit("Our building has no running water. One elderly neighbor cannot walk to the distribution point.", minLines = 4)
        val categoryCard = cardBlock("Category and urgency", "Food / water - MEDIUM")
        register("title", titleInput)
        content.addView(labeled("Title", titleInput))
        register("description", descInput)
        content.addView(labeled("Description", descInput))
        register("category", categoryCard)
        content.addView(categoryCard, margins(bottom = 10))
        val save = button("Save request") {
            val title = titleInput.text.toString().trim()
            val desc = descInput.text.toString().trim()
            if (title.isBlank() || desc.isBlank()) {
                Toast.makeText(this, "Add a title and description.", Toast.LENGTH_SHORT).show()
            } else {
                SampleData.requests.add(0, HelpRequest("local-help-${System.nanoTime()}", title, desc, "Food / water", "MEDIUM", "Besiktas community center, Block B entrance", "You", isMine = true))
                renderHelpList()
            }
        }
        register("save", save)
        content.addView(save, margins(top = 4))
        updateGuide()
    }

    private fun renderInfo() {
        screen = Screen.INFO
        buildShell(
            "Emergency Info",
            listOf(
                GuideStep("overview", "Use quick guidance", "This area keeps practical emergency guidance close."),
                GuideStep("checklist", "Open the checklist", "Use checklists to follow clear steps during a stressful moment.")
            )
        )
        val overview = cardBlock("📶 Offline guidance", "Fast emergency instructions for the same neighborhood outage.")
        register("overview", overview)
        content.addView(overview, margins(bottom = 10))
        val checklist = cardBlock("Emergency Checklist", "Review basic safety steps, contact priorities, and first-aid reminders.")
        checklist.setOnClickListener {
            Toast.makeText(this, "Checklist opened.", Toast.LENGTH_SHORT).show()
        }
        register("checklist", checklist)
        content.addView(checklist)
        updateGuide()
    }

    private fun dashboardWelcomeCard(): MaterialCardView {
        val card = card(radiusDp = 16)
        val layout = vertical(dp(20))
        layout.addView(text("Welcome, Neighbor", 20, textPrimary, bold = true), margins(bottom = 10))
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        badges.addView(badge("Standard Member", accent))
        badges.addView(badge("Safe", accentSecondary))
        badges.addView(badge("Besiktas", textSecondary))
        layout.addView(badges)
        card.addView(layout)
        return card
    }

    private fun forumTabs(): LinearLayout {
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(underlineTab("Global", active = true, color = ContextCompat.getColor(this, R.color.forum_global)), weightParams())
        tabs.addView(underlineTab("Standard", active = false, color = ContextCompat.getColor(this, R.color.forum_standard)), weightParams())
        tabs.addView(underlineTab("Urgent", active = false, color = ContextCompat.getColor(this, R.color.forum_urgent)), weightParams())
        return tabs
    }

    private fun segmentedTabs(first: String, second: String): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(segment(first, active = true), weightParams())
        row.addView(segment(second, active = false), weightParams())
        return row
    }

    private fun forumPostCard(post: ForumPost): MaterialCardView {
        val card = card(radiusDp = 14)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        val votes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(12), 0) }
        }
        votes.addView(text("▲ ${post.upvotes}", 12, textMuted))
        votes.addView(text("▼ ${post.downvotes}", 12, textMuted), margins(top = 4))
        row.addView(votes)

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(badge(post.type, forumTypeColor(post.type)), margins(bottom = 6))
        body.addView(text(post.title, 15, textPrimary, bold = true), margins(bottom = 6))
        body.addView(text("${post.author} · 8 min ago", 12, textSecondary), margins(bottom = 6))
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        stats.addView(text("💬 ${post.comments.size} comments", 12, textMuted), weightParams())
        stats.addView(pill("Share"))
        body.addView(stats)
        row.addView(body, weightParams())
        card.addView(row)
        return card
    }

    private fun forumDetailCard(post: ForumPost): MaterialCardView {
        val card = card(radiusDp = 16)
        val layout = vertical(dp(20))
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        badges.addView(badge(post.type, forumTypeColor(post.type)))
        badges.addView(badge("Besiktas Hub", textSecondary))
        layout.addView(badges, margins(bottom = 12))
        layout.addView(text(post.title, 20, textPrimary, bold = true), margins(bottom = 8))
        layout.addView(text("${post.author} · 8 min ago", 13, textSecondary), margins(bottom = 16))
        layout.addView(text(post.body, 15, textPrimary), margins(bottom = 4))
        card.addView(layout)
        return card
    }

    private fun helpRequestCard(request: HelpRequest): MaterialCardView {
        val card = card(radiusDp = 14)
        val layout = vertical(dp(16))
        layout.addView(text(request.title, 16, textPrimary, bold = true))
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        badges.addView(badge(request.category, accentSecondary))
        badges.addView(badge(request.urgency, urgencyColor(request.urgency)))
        badges.addView(badge(request.status, textSecondary))
        layout.addView(badges, margins(top = 8, bottom = 10))
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(text(request.author, 13, textSecondary), weightParams())
        bottom.addView(text("💬 ${request.comments.size}", 13, textMuted))
        bottom.addView(text(" · 12 min ago", 13, textMuted))
        layout.addView(bottom)
        card.addView(layout)
        return card
    }

    private fun helpDetailCard(request: HelpRequest): MaterialCardView {
        val card = card(radiusDp = 14)
        val layout = vertical(dp(16))
        layout.addView(text(request.title, 20, textPrimary, bold = true), margins(bottom = 10))
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        badges.addView(badge(request.category, accentSecondary))
        badges.addView(badge(request.urgency, urgencyColor(request.urgency)))
        badges.addView(badge(request.status, textSecondary))
        layout.addView(badges, margins(bottom = 12))
        layout.addView(text(request.description, 14, textSecondary), margins(bottom = 12))
        layout.addView(text("${request.author} · 12 min ago", 13, textMuted))
        card.addView(layout)
        return card
    }

    private fun locationBlock(location: String): TextView =
        text("📍 $location", 14, textSecondary).apply {
            background = rounded(bgCard, border, 1, 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

    private fun addCommentSection(comments: List<Comment>, onSubmit: (String) -> Unit): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        section.addView(text("Comments (${comments.size})", 18, textPrimary, bold = true), margins(bottom = 10))
        val inputCard = card(radiusDp = 12)
        val inputLayout = vertical(dp(14))
        val input = edit("", minLines = 2).apply { hint = "Write a comment" }
        inputLayout.addView(input, margins(bottom = 8))
        inputLayout.addView(button("Post comment") {
            val body = input.text.toString().trim()
            if (body.isNotBlank()) onSubmit(body)
        })
        inputCard.addView(inputLayout)
        section.addView(inputCard, margins(bottom = 10))
        comments.forEach { comment ->
            section.addView(cardBlock(comment.author, comment.body), margins(bottom = 8))
        }
        return section
    }

    private fun featureCard(icon: String, title: String, body: String, target: String?, enabled: Boolean = true, onClick: () -> Unit): MaterialCardView {
        val card = card(radiusDp = 14)
        val layout = vertical(dp(16))
        layout.addView(text(icon, 28, textPrimary), margins(bottom = 8))
        layout.addView(text(title, 15, textPrimary, bold = true), margins(bottom = 2))
        layout.addView(text(body, 13, textSecondary))
        card.addView(layout)
        card.isEnabled = enabled
        card.alpha = if (enabled) 1f else 0.65f
        if (target != null) register(target, card)
        if (enabled) card.setOnClickListener { onClick() }
        return card
    }

    private fun featureRow(left: View, right: View): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
            addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(6), 0, 0, 0) })
        }

    private fun labelRow(label: String, value: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text(label, 13, textSecondary), weightParams())
            addView(pill(value, active = true))
        }

    private fun chipScroller(labels: List<String>): HorizontalScrollView =
        HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(this@TutorialActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                labels.forEachIndexed { index, label -> addView(pill(label, active = index == 0)) }
            })
        }

    private fun cardBlock(title: String, body: String): MaterialCardView {
        val card = card()
        val layout = vertical(dp(16))
        layout.addView(text(title, 17, textPrimary, bold = true))
        layout.addView(text(body, 14, textSecondary), margins(top = 6))
        card.addView(layout)
        return card
    }

    private fun card(radiusDp: Int = 8): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(radiusDp).toFloat()
        setCardBackgroundColor(bgCard)
        setStrokeColor(border)
        strokeWidth = dp(1)
        cardElevation = 0f
        isClickable = true
        isFocusable = true
    }

    private fun button(label: String, secondary: Boolean = false, onClick: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = label
            isAllCaps = false
            cornerRadius = dp(8)
            minHeight = dp(40)
            if (secondary) {
                setTextColor(accent)
                strokeColor = ColorStateList.valueOf(accent)
                strokeWidth = dp(1)
                backgroundTintList = ColorStateList.valueOf(bgBase)
            } else {
                setTextColor(ContextCompat.getColor(this@TutorialActivity, R.color.white))
                backgroundTintList = ColorStateList.valueOf(accent)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

    private fun pill(label: String, active: Boolean = false): TextView =
        text(label, 13, if (active) accent else textMuted, bold = active).apply {
            gravity = Gravity.CENTER
            minHeight = dp(32)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(if (active) bgCard else bgBase, border, 1, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

    private fun badge(label: String, color: Int): TextView =
        text(label, 12, color, bold = true).apply {
            setPadding(dp(10), dp(3), dp(10), dp(3))
            background = rounded(bgBase, border, 1, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dp(6), 0)
            }
        }

    private fun underlineTab(label: String, active: Boolean, color: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(text(label, 15, if (active) color else textMuted, bold = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(10))
            })
            addView(View(this@TutorialActivity).apply {
                setBackgroundColor(if (active) color else android.graphics.Color.TRANSPARENT)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)))
        }

    private fun segment(label: String, active: Boolean): TextView =
        text(label, 15, if (active) accent else textMuted, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
            if (active) background = rounded(bgCard, border, 1, 8)
        }

    private fun edit(value: String, minLines: Int = 1): EditText =
        EditText(this).apply {
            setText(value)
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            background = rounded(bgInput, border, 1, 8)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setMinLines(minLines)
        }

    private fun labeled(label: String, view: View): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(label, 14, textSecondary, bold = true))
            addView(view, margins(top = 6, bottom = 10))
        }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun vertical(padding: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

    private fun register(target: String, view: View) {
        targetViews.putIfAbsent(target, view)
    }

    private fun updateGuide() {
        if (guideSteps.isEmpty()) {
            guideCard.visibility = View.GONE
            return
        }
        val step = guideSteps[guideIndex]
        guideCount.text = "Step ${guideIndex + 1} of ${guideSteps.size}"
        guideTitle.text = step.title
        guideText.text = step.text
        previousButton.isEnabled = guideIndex > 0
        nextButton.text = if (guideIndex == guideSteps.lastIndex) "Finish guide" else "Next"
        skipButton.visibility = if (guideIndex == guideSteps.lastIndex) View.INVISIBLE else View.VISIBLE
        clearHighlights()
        highlight(targetViews[step.target])
    }

    private fun clearHighlights() {
        targetViews.values.forEach { view ->
            when (view) {
                is MaterialCardView -> {
                    view.setStrokeColor(border)
                    view.strokeWidth = dp(1)
                }
                is MaterialButton -> view.strokeWidth = dp(0)
                is EditText -> view.background = rounded(bgInput, border, 1, 8)
                is TextView -> {
                    if (originalTextBackgrounds.containsKey(view)) {
                        view.background = originalTextBackgrounds[view]
                    }
                }
                is LinearLayout -> view.background = null
            }
        }
    }

    private fun highlight(view: View?) {
        when (view) {
            is MaterialCardView -> {
                view.setStrokeColor(amber)
                view.strokeWidth = dp(3)
            }
            is MaterialButton -> {
                view.strokeColor = ColorStateList.valueOf(amber)
                view.strokeWidth = dp(3)
            }
            is EditText -> view.background = rounded(bgInput, amber, 3, 8)
            is TextView -> {
                originalTextBackgrounds.putIfAbsent(view, view.background)
                view.background = rounded(bgCard, amber, 3, 10)
            }
            is LinearLayout -> view.background = rounded(bgBase, amber, 3, 8)
        }
    }

    private fun rounded(fillColor: Int, strokeColor: Int, strokeDp: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(strokeDp), strokeColor)
        }

    private fun margins(top: Int = 0, bottom: Int = 0, right: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(top), dp(right), dp(bottom))
        }

    private fun weightParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun forumTypeColor(type: String): Int = when (type) {
        "URGENT" -> ContextCompat.getColor(this, R.color.forum_urgent)
        "STANDARD" -> ContextCompat.getColor(this, R.color.forum_standard)
        else -> ContextCompat.getColor(this, R.color.forum_global)
    }

    private fun urgencyColor(urgency: String): Int = when (urgency) {
        "HIGH" -> ContextCompat.getColor(this, R.color.urgency_high)
        "MEDIUM" -> ContextCompat.getColor(this, R.color.urgency_medium)
        else -> ContextCompat.getColor(this, R.color.urgency_low)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private object SampleData {
        val posts = mutableListOf(
            ForumPost("post-1", "Power is out near Barbaros Boulevard", "Several buildings are affected. Elevators are not working, and residents are checking on older neighbors.", "Aylin Neighbor", "URGENT", upvotes = 12, comments = mutableListOf(Comment("Community Helper", "I checked Block A. Stairs are clear."))),
            ForumPost("post-2", "Volunteer list for charging phones", "A charging station is being organized at the community center. Bring your own cable if possible.", "Can Expert", "GLOBAL", upvotes = 19, comments = mutableListOf(Comment("Aylin Neighbor", "I can bring two extra charging cables."))),
            ForumPost("post-3", "Reminder: avoid downed cables", "If you see damaged electrical lines, keep distance and report the exact location to emergency services.", "Safety Moderator", "STANDARD", upvotes = 24)
        )
        val requests = mutableListOf(
            HelpRequest("help-1", "Need drinking water for an elderly neighbor", "A neighbor cannot walk to the distribution point after the outage.", "Food / water", "MEDIUM", "Besiktas community center, Block B entrance", "Aylin Neighbor", comments = mutableListOf(Comment("Community Helper", "I can bring two bottles in 20 minutes."))),
            HelpRequest("help-2", "Ride needed to pharmacy", "One resident needs transport to pick up medication before evening.", "Transport", "HIGH", "Near Barbaros Boulevard", "Mert Neighbor", comments = mutableListOf(Comment("Aylin Neighbor", "I am nearby with a car."))),
            HelpRequest("help-3", "Blankets for temporary shelter", "A small group at the community hall needs clean blankets tonight.", "Shelter", "LOW", "Community hall entrance desk", "Shelter Volunteer")
        )
    }
}
