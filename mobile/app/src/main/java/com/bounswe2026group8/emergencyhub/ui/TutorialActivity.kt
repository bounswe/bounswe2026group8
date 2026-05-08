package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.map.ui.MapActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class TutorialActivity : AppCompatActivity() {

    private enum class Screen {
        DASHBOARD,
        FORUM,
        POST_DETAIL,
        POST_CREATE,
        HELP_LIST,
        HELP_DETAIL,
        HELP_CREATE,
        INFO,
        INFO_MAP,
        INFO_CHECKLIST,
        INFO_AID_DETAIL,
        INFO_CONTACTS
    }

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
    private data class HelpOffer(
        val id: String,
        val title: String,
        val description: String,
        val category: String,
        val availability: String,
        val author: String
    )
    private data class OfflineContact(val name: String, val phone: String)
    private data class Comment(val author: String, val body: String)

    private var screen = Screen.DASHBOARD
    private var selectedPostId: String? = null
    private var selectedRequestId: String? = null
    private var selectedForumType = "GLOBAL"
    private var selectedForumSort = "newest"
    private var selectedHelpMode = "REQUESTS"
    private var selectedHelpCategory = "All"
    private var selectedAidTitle = ""
    private var selectedAidDetail = ""
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
    private lateinit var showTutorialButton: MaterialButton

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
        val navButton = when (screen) {
            Screen.DASHBOARD -> button("Exit", secondary = true) { finish() }
            Screen.INFO_AID_DETAIL -> button("←", secondary = true) { renderInfoChecklist() }.apply { minWidth = dp(48) }
            Screen.INFO_MAP, Screen.INFO_CHECKLIST, Screen.INFO_CONTACTS -> button("←", secondary = true) { renderInfo() }.apply { minWidth = dp(48) }
            else -> button("←", secondary = true) { renderDashboard() }.apply { minWidth = dp(48) }
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
            hideGuide()
        }
        previousButton = button("Previous", secondary = true) {
            if (guideIndex > 0) {
                guideIndex -= 1
                updateGuide()
            }
        }
        nextButton = button("Next") {
            if (guideIndex == guideSteps.lastIndex) {
                hideGuide()
            } else {
                guideIndex += 1
                updateGuide()
            }
        }
        actions.addView(skipButton)
        actions.addView(previousButton)
        actions.addView(nextButton)
        listOf(skipButton, previousButton, nextButton).forEach { actionButton ->
            actionButton.isSingleLine = true
            actionButton.maxLines = 1
            actionButton.ellipsize = TextUtils.TruncateAt.END
            actionButton.textSize = 12f
            actionButton.setPadding(dp(8), 0, dp(8), 0)
            actionButton.minWidth = 0
            actionButton.minimumWidth = 0
            actionButton.layoutParams = LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            ).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
        }
        guideLayout.addView(actions)
        guideCard.addView(guideLayout)
        root.addView(guideCard, margins(bottom = 16))

        showTutorialButton = button("Show tutorial", secondary = true) {
            guideIndex = 0
            guideCard.visibility = View.VISIBLE
            showTutorialButton.visibility = View.GONE
            updateGuide()
        }
        showTutorialButton.visibility = View.GONE
        root.addView(showTutorialButton, margins(bottom = 16))

        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)
    }

    private fun renderDashboard() {
        screen = Screen.DASHBOARD
        buildShell(
            "Emergency Hub",
            listOf(
                GuideStep("welcome", "Start here", "Your main areas are grouped here so you can move quickly during an emergency."),
                GuideStep("hub", "This is your hub", "Your hub keeps nearby posts and requests focused on the right neighborhood."),
                GuideStep("scenario", "Current situation", "A neighborhood power outage is affecting buildings nearby. Use the app to share updates and ask for help."),
                GuideStep("forum", "Read community updates", "Open the forum to follow what neighbors are reporting and add useful information."),
                GuideStep("help", "Ask for help", "Open help requests when someone needs supplies, shelter, transport, or medical support."),
                GuideStep("info", "Keep guidance close", "Offline info gives you quick steps to check when things feel urgent.")
            )
        )

        val hub = pill("Besiktas Hub", active = true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showMainAppOnly("Hub selection") }
        }
        register("hub", hub)
        content.addView(hub, margins(bottom = 14))

        val welcome = dashboardWelcomeCard()
        register("welcome", welcome)
        content.addView(welcome, margins(bottom = 12))

        val scenario = cardBlock("Current situation", "A neighborhood power outage is affecting nearby buildings. You can ask for water, offer help, and share updates.")
        register("scenario", scenario)
        content.addView(scenario, margins(bottom = 12))

        content.addView(featureRow(
            featureCard("💬", "Forum", "Community updates", "forum") { renderForum() },
            featureCard("🆘", "Help Requests", "Ask or answer", "help") { renderHelpList() }
        ))
        content.addView(featureRow(
            featureCard("👤", "Profile", "Sign in to use Profile", null, enabled = false) { showMainAppOnly("Profile") },
            featureCard("📶", "Offline Info", "Offline guidance", "info") { renderInfo() }
        ), margins(top = 12))
        content.addView(featureRow(
            featureCard("📡", "Offline Messages", "Sign in to use Offline Messages", null, enabled = false) { showMainAppOnly("Offline messages") },
            View(this)
        ), margins(top = 12))
        updateGuide()
    }

    private fun renderForum() {
        screen = Screen.FORUM
        buildShell(
            "Forum",
            listOf(
                GuideStep("tabs", "Switch forum areas", "Use Global, Standard, and Urgent to focus on the kind of update you need."),
                GuideStep("sort", "Sort updates", "Newest, Most liked, and Hot help you scan active conversations quickly."),
                GuideStep("posts", "Open a post", "Tap a post to read the full update and add a comment."),
                GuideStep("create", "Create a post", "Write a post when you have information that can help others nearby.")
            )
        )
        val newPost = button("New post") { renderPostCreate() }
        register("create", newPost)
        content.addView(labelRow("All hubs", "Besiktas Hub") { showMainAppOnly("Hub selection") }, margins(bottom = 10))
        val tabs = forumTabs()
        register("tabs", tabs)
        content.addView(tabs, margins(bottom = 12))
        content.addView(newPost, margins(bottom = 10))

        val sortRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        sortRow.addView(pill("Newest", active = selectedForumSort == "newest").apply {
            setOnClickListener {
                selectedForumSort = "newest"
                renderForum()
            }
        })
        sortRow.addView(pill("Most liked", active = selectedForumSort == "most_liked").apply {
            setOnClickListener {
                selectedForumSort = "most_liked"
                renderForum()
            }
        })
        sortRow.addView(pill("Hot", active = selectedForumSort == "hot").apply {
            setOnClickListener {
                selectedForumSort = "hot"
                renderForum()
            }
        })
        register("sort", sortRow)
        content.addView(sortRow, margins(bottom = 10))

        val visiblePosts = SampleData.posts
            .filter { it.type == selectedForumType }
            .let { posts ->
                when (selectedForumSort) {
                    "most_liked" -> posts.sortedByDescending { it.upvotes }
                    "hot" -> posts.sortedByDescending { it.upvotes + it.downvotes + it.comments.size }
                    else -> posts
                }
            }
        if (visiblePosts.isEmpty()) {
            content.addView(cardBlock("No posts here yet", "Create a post to add the first ${selectedForumType.lowercase()} update."), margins(bottom = 10))
        }
        visiblePosts.forEach { post ->
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
                GuideStep("summary", "Read the update", "Check the type, title, author, and details before responding."),
                GuideStep("votes", "React to the post", "Upvote helpful updates or downvote information that is not useful."),
                GuideStep("comments", "Join the conversation", "Add a comment when you can answer a question or share more detail.")
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
        votes.addView(pill("Share").apply {
            setOnClickListener { showMainAppOnly("Sharing") }
        })
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
            "Create Post",
            listOf(
                GuideStep("type", "Choose where it belongs", "Pick the forum type that best matches your update."),
                GuideStep("title", "Use a clear title", "A short title helps neighbors understand the update quickly."),
                GuideStep("content", "Write useful details", "Share what happened, where it happened, and what people should know next."),
                GuideStep("images", "Add images if useful", "Photos can help others understand the situation more quickly."),
                GuideStep("save", "Save the post", "Post it once the information is clear and ready for others to read.")
            )
        )
        val titleInput = edit("Charging station open at the community center")
        val bodyInput = edit("Volunteers can help people charge phones until 18:00. Bring your own cable if possible.", minLines = 6)

        content.addView(labelRow("Posting to", "Besiktas Hub") { showMainAppOnly("Hub selection") }, margins(bottom = 8))
        content.addView(text("Share an update with neighbors nearby.", 14, textSecondary), margins(bottom = 20))

        val typeSelector = toggleRow(
            listOf("Global", "Standard", "Urgent"),
            activeIndex = when (selectedForumType) {
                "STANDARD" -> 1
                "URGENT" -> 2
                else -> 0
            }
        )
        register("type", typeSelector)
        content.addView(sectionLabel("Post type"))
        content.addView(typeSelector, margins(bottom = 18))

        register("title", titleInput)
        content.addView(labeled("Title", titleInput))
        register("content", bodyInput)
        content.addView(labeled("Content", bodyInput))

        val images = formBlock("Images", listOf("Upload from device", "Image URLs"), "Optional")
        register("images", images)
        content.addView(images, margins(bottom = 16))

        val save = fullWidthButton("Create post") {
            val title = titleInput.text.toString().trim()
            val body = bodyInput.text.toString().trim()
            if (title.isBlank() || body.isBlank()) {
                Toast.makeText(this, "Add a title and details.", Toast.LENGTH_SHORT).show()
            } else {
                SampleData.posts.add(0, ForumPost("local-post-${System.nanoTime()}", title, body, "You", selectedForumType, upvotes = 1, isMine = true))
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
                GuideStep("tabs", "Switch lists", "Requests show people who need help. Offers show people who can help."),
                GuideStep("filters", "Filter by need", "Categories make it easier to find medical, food, shelter, or transport needs."),
                GuideStep("requests", "Review requests", "Open a request to see what is needed and how you might respond."),
                GuideStep("create", "Create a request", "Create a request when someone needs supplies, transport, shelter, or medical help.")
            )
        )
        content.addView(labelRow("Hub", "Besiktas Hub") { showMainAppOnly("Hub selection") }, margins(bottom = 10))
        val tabs = segmentedTabs(
            first = "Requests",
            second = "Offers",
            firstActive = selectedHelpMode == "REQUESTS",
            onFirstClick = {
                selectedHelpMode = "REQUESTS"
                renderHelpList()
            },
            onSecondClick = {
                selectedHelpMode = "OFFERS"
                renderHelpList()
            }
        )
        register("tabs", tabs)
        content.addView(tabs, margins(bottom = 10))
        val create = button("New") { renderHelpCreate() }
        register("create", create)
        content.addView(create, margins(bottom = 10))
        val filters = chipScroller(listOf("All", "Medical", "Food", "Shelter", "Transport", "Other"))
        register("filters", filters)
        content.addView(filters, margins(bottom = 8))

        if (selectedHelpMode == "REQUESTS") {
            val visibleRequests = SampleData.requests.filter {
                selectedHelpCategory == "All" || it.category.equals(selectedHelpCategory, ignoreCase = true) || it.category.contains(selectedHelpCategory, ignoreCase = true)
            }
            visibleRequests.forEach { request ->
                val card = helpRequestCard(request)
                card.setOnClickListener {
                    selectedRequestId = request.id
                    renderHelpDetail()
                }
                register("requests", card)
                content.addView(card, margins(bottom = 10))
            }
        } else {
            val visibleOffers = SampleData.offers.filter {
                selectedHelpCategory == "All" || it.category.equals(selectedHelpCategory, ignoreCase = true) || it.category.contains(selectedHelpCategory, ignoreCase = true)
            }
            visibleOffers.forEach { offer ->
                val card = helpOfferCard(offer)
                register("requests", card)
                content.addView(card, margins(bottom = 10))
            }
        }
        updateGuide()
    }

    private fun renderHelpDetail() {
        screen = Screen.HELP_DETAIL
        val request = SampleData.requests.firstOrNull { it.id == selectedRequestId } ?: return renderHelpList()
        buildShell(
            "Help Request Detail",
            listOf(
                GuideStep("summary", "Review the request", "Check the category, urgency, description, and who posted it."),
                GuideStep("actions", "Manage your request", "You can only manage requests you created. Mark it as resolved when the need is handled, or delete it if it is no longer needed."),
                GuideStep("location", "Check the location", "Use the location note to understand where support is needed."),
                GuideStep("comments", "Coordinate in comments", "Comment to ask a follow-up question, offer help, or share an update.")
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
            "Create Help Request",
            listOf(
                GuideStep("title", "Write a clear title", "A short title helps neighbors understand the need quickly."),
                GuideStep("description", "Add useful details", "Describe who needs help, what is needed, and any safety details."),
                GuideStep("category", "Pick category and urgency", "These choices help the right people notice the request faster."),
                GuideStep("location", "Add location details", "A clear location helps nearby people understand where support is needed."),
                GuideStep("images", "Attach images if helpful", "Images are optional, but they can make the request clearer."),
                GuideStep("save", "Review before saving", "Save the request once the details are accurate.")
            )
        )
        val titleInput = edit("Need drinking water for an elderly neighbor")
        val descInput = edit("Our building has no running water. One elderly neighbor cannot walk to the distribution point.", minLines = 4)
        val locationInput = edit("Besiktas community center, Block B entrance")
        content.addView(text("Tell neighbors what is needed and where they can help.", 14, textSecondary), margins(bottom = 20))
        register("title", titleInput)
        content.addView(labeled("Title", titleInput))
        register("description", descInput)
        content.addView(labeled("Description", descInput))

        val categoryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(dropdownLike("Food / water"))
            addView(dropdownLike("Medium urgency"), margins(top = 10))
        }
        register("category", categoryCard)
        content.addView(sectionLabel("Category and urgency"))
        content.addView(categoryCard, margins(bottom = 16))

        register("location", locationInput)
        content.addView(labeled("Location", locationInput))
        content.addView(button("Use my location", secondary = true) {
            showMainAppOnly("Using your current location")
        }, margins(bottom = 16))

        val images = formBlock("Images", listOf("Upload from gallery", "Take photo"), "Optional")
        register("images", images)
        content.addView(images, margins(bottom = 16))

        val save = fullWidthButton("Submit") {
            val title = titleInput.text.toString().trim()
            val desc = descInput.text.toString().trim()
            if (title.isBlank() || desc.isBlank()) {
                Toast.makeText(this, "Add a title and description.", Toast.LENGTH_SHORT).show()
            } else {
                val location = locationInput.text.toString().trim().ifBlank { "Besiktas community center, Block B entrance" }
                SampleData.requests.add(0, HelpRequest("local-help-${System.nanoTime()}", title, desc, "Food / water", "MEDIUM", location, "You", isMine = true))
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
            "",
            listOf(
                GuideStep("overview", "Offline features", "Use these tools when internet access is limited or you need quick emergency guidance."),
                GuideStep("map", "Open offline map", "This opens the same offline map used in the main app."),
                GuideStep("checklist", "Open the checklist", "Use checklists to follow clear steps during a stressful moment."),
                GuideStep("contacts", "Open offline contacts", "Emergency contacts keep important numbers easy to reach.")
            )
        )
        content.gravity = Gravity.CENTER_HORIZONTAL

        val overview = text("Offline Features", 24, textPrimary, bold = true).apply {
            gravity = Gravity.CENTER
        }
        register("overview", overview)
        content.addView(overview, margins(bottom = 24))

        val map = offlineFeatureButton("Offline Map") { renderInfoMap() }
        register("map", map)
        content.addView(map, margins(bottom = 12))

        val checklist = offlineFeatureButton("Emergency Checklist") { renderInfoChecklist() }
        register("checklist", checklist)
        content.addView(checklist, margins(bottom = 12))

        val contacts = offlineFeatureButton("Offline Contacts") { renderInfoContacts() }
        register("contacts", contacts)
        content.addView(contacts)
        updateGuide()
    }

    private fun renderInfoChecklist() {
        screen = Screen.INFO_CHECKLIST
        buildShell(
            "",
            listOf(
                GuideStep("title", "Basic skills guide", "Use these first aid topics when you need quick guidance."),
                GuideStep("displacement", "Displacement skills", "Open this topic to review how to move someone away from danger."),
                GuideStep("checking", "Checking skills", "Open this topic to review the first checks before help arrives."),
                GuideStep("cpr", "CPR guidance", "Open this topic to review cardiopulmonary resuscitation guidance.")
            )
        )
        val title = text(getString(R.string.first_aid_guide_title), 22, accent, bold = true)
        register("title", title)
        content.addView(title, margins(bottom = 24))

        val displacement = firstAidTopicCard(
            getString(R.string.first_aid_displacement_title),
            getString(R.string.first_aid_displacement_summary),
            getString(R.string.first_aid_displacement_detail),
            R.drawable.rautek_maneuver,
            R.drawable.blanket_pull
        )
        register("displacement", displacement)
        content.addView(displacement, margins(bottom = 16))

        val checking = firstAidTopicCard(
            getString(R.string.first_aid_checking_title),
            getString(R.string.first_aid_checking_summary),
            getString(R.string.first_aid_checking_detail),
            R.drawable.carotidian_pulse,
            R.drawable.checking_respiration
        )
        register("checking", checking)
        content.addView(checking, margins(bottom = 16))

        val cpr = firstAidTopicCard(
            getString(R.string.first_aid_cpr_title),
            getString(R.string.first_aid_cpr_summary),
            getString(R.string.first_aid_cpr_detail)
        )
        register("cpr", cpr)
        content.addView(cpr, margins(bottom = 16))

        content.addView(
            firstAidTopicCard(
                getString(R.string.first_aid_abc_title),
                getString(R.string.first_aid_abc_summary),
                getString(R.string.first_aid_abc_detail)
            )
        )
        updateGuide()
    }

    private fun renderAidDetail(title: String, detail: String, image1: Int = 0, image2: Int = 0) {
        screen = Screen.INFO_AID_DETAIL
        selectedAidTitle = title
        selectedAidDetail = detail
        buildShell("", emptyList())
        if (image1 != 0) {
            content.addView(firstAidImageCard(image1), margins(bottom = 16))
        }
        if (image2 != 0) {
            content.addView(firstAidImageCard(image2), margins(bottom = 24))
        }
        val titleView = text(selectedAidTitle, 26, accent, bold = true)
        content.addView(titleView, margins(bottom = 16))

        val detailView = text(selectedAidDetail, 16, ContextCompat.getColor(this, R.color.text_primary)).apply {
            setLineSpacing(dp(6).toFloat(), 1f)
        }
        content.addView(detailView)
        updateGuide()
    }

    private fun renderInfoContacts() {
        screen = Screen.INFO_CONTACTS
        buildShell(
            "",
            listOf(
                GuideStep("title", "Emergency numbers", "Keep the most important public numbers easy to reach."),
                GuideStep("general", "Call emergency services", "Use the call button when there is an urgent situation."),
                GuideStep("custom", "Add custom number", "Save personal emergency contacts from the main app.")
            )
        )
        val title = text(getString(R.string.emergency_numbers), 22, accent, bold = true)
        register("title", title)
        content.addView(title, margins(bottom = 24))

        val general = emergencyNumberCard(getString(R.string.general_emergency), "112")
        register("general", general)
        content.addView(general, margins(bottom = 16))

        SampleData.contacts.forEach { contact ->
            content.addView(emergencyNumberCard(contact.name, contact.phone), margins(bottom = 12))
        }

        val custom = button(getString(R.string.add_custom_number), secondary = true) {
            SampleData.contacts.add(OfflineContact("Neighborhood volunteer", "+90 555 010 7788"))
            renderInfoContacts()
        }.apply {
            minHeight = dp(56)
            cornerRadius = dp(12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        }
        register("custom", custom)
        content.addView(custom, margins(top = 16))
        updateGuide()
    }

    private fun renderInfoMap() {
        screen = Screen.INFO_MAP
        buildShell(
            "",
            listOf(
                GuideStep("map", "Offline map", "Open the same offline map used in the main app."),
                GuideStep("openMap", "Open map", "The map will ask for location permission when needed and show nearby gathering points.")
            )
        )
        val map = cardBlock(
            "Offline Map",
            "Use the app map to find nearby gathering points. It can use your current location if you allow permission."
        )
        register("map", map)
        content.addView(map, margins(bottom = 12))

        val openMap = fullWidthButton("Open offline map") {
            startActivity(Intent(this, MapActivity::class.java))
        }
        register("openMap", openMap)
        content.addView(openMap)
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
        tabs.addView(underlineTab("Global", active = selectedForumType == "GLOBAL", color = ContextCompat.getColor(this, R.color.forum_global)).apply {
            setOnClickListener {
                selectedForumType = "GLOBAL"
                renderForum()
            }
        }, weightParams())
        tabs.addView(underlineTab("Standard", active = selectedForumType == "STANDARD", color = ContextCompat.getColor(this, R.color.forum_standard)).apply {
            setOnClickListener {
                selectedForumType = "STANDARD"
                renderForum()
            }
        }, weightParams())
        tabs.addView(underlineTab("Urgent", active = selectedForumType == "URGENT", color = ContextCompat.getColor(this, R.color.forum_urgent)).apply {
            setOnClickListener {
                selectedForumType = "URGENT"
                renderForum()
            }
        }, weightParams())
        return tabs
    }

    private fun segmentedTabs(
        first: String,
        second: String,
        firstActive: Boolean = true,
        onFirstClick: (() -> Unit)? = null,
        onSecondClick: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(segment(first, active = firstActive).apply {
            if (onFirstClick != null) setOnClickListener { onFirstClick() }
        }, weightParams())
        row.addView(segment(second, active = !firstActive).apply {
            if (onSecondClick != null) setOnClickListener { onSecondClick() }
        }, weightParams())
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
        stats.addView(pill("Share").apply {
            setOnClickListener { showMainAppOnly("Sharing") }
        })
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

    private fun helpOfferCard(offer: HelpOffer): MaterialCardView {
        val card = card(radiusDp = 14)
        val layout = vertical(dp(16))
        layout.addView(text(offer.title, 16, textPrimary, bold = true))
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        badges.addView(badge(offer.category, accentSecondary))
        badges.addView(badge(offer.availability, textSecondary))
        layout.addView(badges, margins(top = 8, bottom = 10))
        layout.addView(text(offer.description, 14, textSecondary), margins(bottom = 10))
        layout.addView(text(offer.author, 13, textMuted))
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
        card.alpha = if (enabled) 1f else 0.65f
        if (target != null) register(target, card)
        card.setOnClickListener { onClick() }
        return card
    }

    private fun featureRow(left: View, right: View): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
            addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(6), 0, 0, 0) })
        }

    private fun toggleRow(labels: List<String>, activeIndex: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            labels.forEachIndexed { index, label ->
                addView(segment(label, active = index == activeIndex), LinearLayout.LayoutParams(0, dp(44), 1f))
            }
        }

    private fun sectionLabel(label: String): TextView =
        text(label, 14, textSecondary).apply {
            setPadding(0, 0, 0, dp(8))
        }

    private fun dropdownLike(value: String): TextView =
        text(value, 15, textPrimary).apply {
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(52)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(bgInput, border, 1, 8)
            setOnClickListener { showMainAppOnly("Changing this field") }
        }

    private fun formBlock(title: String, actions: List<String>, caption: String): MaterialCardView {
        val card = card(radiusDp = 12)
        val layout = vertical(dp(14))
        layout.addView(text(title, 14, textSecondary, bold = true), margins(bottom = 8))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.forEach { action ->
            row.addView(button(action, secondary = true) {
                showMainAppOnly(action)
            })
        }
        layout.addView(row)
        layout.addView(text(caption, 12, textMuted), margins(top = 8))
        card.addView(layout)
        return card
    }

    private fun labelRow(label: String, value: String, onValueClick: (() -> Unit)? = null): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text(label, 13, textSecondary), weightParams())
            addView(pill(value, active = true).apply {
                if (onValueClick != null) setOnClickListener { onValueClick() }
            })
        }

    private fun chipScroller(labels: List<String>): HorizontalScrollView =
        HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(this@TutorialActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                labels.forEach { label ->
                    addView(pill(label, active = selectedHelpCategory == label).apply {
                        setOnClickListener {
                            selectedHelpCategory = label
                            renderHelpList()
                        }
                    })
                }
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

    private fun checklistBlock(title: String, items: List<String>): MaterialCardView {
        val card = card(radiusDp = 12)
        val layout = vertical(dp(16))
        layout.addView(text(title, 17, textPrimary, bold = true), margins(bottom = 8))
        items.forEach { item ->
            layout.addView(text("• $item", 14, textSecondary), margins(top = 4))
        }
        card.addView(layout)
        return card
    }

    private fun firstAidTopicCard(
        title: String,
        summary: String,
        detail: String,
        image1: Int = 0,
        image2: Int = 0
    ): MaterialCardView {
        val card = card(radiusDp = 16)
        val layout = vertical(dp(16))
        layout.addView(text(title, 18, textPrimary, bold = true), margins(bottom = 8))
        layout.addView(text(summary, 14, textMuted))
        card.addView(layout)
        card.setOnClickListener { renderAidDetail(title, detail, image1, image2) }
        return card
    }

    private fun firstAidImageCard(imageRes: Int): MaterialCardView {
        val card = card(radiusDp = 16)
        card.addView(ImageView(this).apply {
            setImageResource(imageRes)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(bgCard)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })
        return card
    }

    private fun emergencyNumberCard(title: String, number: String): MaterialCardView {
        val card = card(radiusDp = 16)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(text(title, 16, textPrimary, bold = true), margins(bottom = 4))
        labels.addView(text(number, 14, textMuted))
        row.addView(labels, weightParams())
        row.addView(MaterialButton(this).apply {
            text = "Call"
            isAllCaps = false
            textSize = 12f
            cornerRadius = dp(8)
            minHeight = dp(40)
            setTextColor(ContextCompat.getColor(this@TutorialActivity, R.color.white))
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E63946"))
            setOnClickListener { showMainAppOnly("Calling $title") }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40))
        })
        card.addView(row)
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

    private fun fullWidthButton(label: String, onClick: () -> Unit): MaterialButton =
        button(label, secondary = false, onClick = onClick).apply {
            minHeight = dp(52)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        }

    private fun fullWidthSecondaryButton(label: String, onClick: () -> Unit): MaterialButton =
        button(label, secondary = true, onClick = onClick).apply {
            minHeight = dp(48)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        }

    private fun offlineFeatureButton(label: String, onClick: () -> Unit): MaterialButton =
        button(label, secondary = false, onClick = onClick).apply {
            isAllCaps = false
            textSize = 14f
            minHeight = dp(48)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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

    private fun showMainAppOnly(feature: String) {
        Toast.makeText(this, "$feature is available in the main app.", Toast.LENGTH_SHORT).show()
    }

    private fun updateGuide() {
        if (guideSteps.isEmpty()) {
            guideCard.visibility = View.GONE
            return
        }
        showTutorialButton.visibility = View.GONE
        val step = guideSteps[guideIndex]
        guideCount.text = "Step ${guideIndex + 1} of ${guideSteps.size}"
        guideTitle.text = step.title
        guideText.text = step.text
        previousButton.isEnabled = guideIndex > 0
        nextButton.text = if (guideIndex == guideSteps.lastIndex) "Finish" else "Next"
        skipButton.visibility = if (guideIndex == guideSteps.lastIndex) View.INVISIBLE else View.VISIBLE
        clearHighlights()
        highlight(targetViews[step.target])
    }

    private fun hideGuide() {
        guideCard.visibility = View.GONE
        showTutorialButton.visibility = View.VISIBLE
        clearHighlights()
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
                is HorizontalScrollView -> view.background = null
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
            is HorizontalScrollView -> view.background = rounded(bgBase, amber, 3, 8)
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
            ForumPost("post-1", "Power is out near Barbaros Boulevard", "Several buildings are affected. Elevators are not working, and residents are checking on older neighbors.", "Aylin Neighbor", "URGENT", upvotes = 12, downvotes = 1, comments = mutableListOf(Comment("Community Helper", "I checked Block A. Stairs are clear."))),
            ForumPost("post-2", "Volunteer list for charging phones", "A charging station is being organized at the community center. Bring your own cable if possible.", "Can Expert", "GLOBAL", upvotes = 19, comments = mutableListOf(Comment("Aylin Neighbor", "I can bring two extra charging cables."))),
            ForumPost("post-3", "Reminder: avoid downed cables", "If you see damaged electrical lines, keep distance and report the exact location to emergency services.", "Safety Moderator", "STANDARD", upvotes = 24)
        )
        val requests = mutableListOf(
            HelpRequest("help-1", "Need drinking water for an elderly neighbor", "A neighbor cannot walk to the distribution point after the outage.", "Food / water", "MEDIUM", "Besiktas community center, Block B entrance", "Aylin Neighbor", comments = mutableListOf(Comment("Community Helper", "I can bring two bottles in 20 minutes."))),
            HelpRequest("help-2", "Ride needed to pharmacy", "One resident needs transport to pick up medication before evening.", "Transport", "HIGH", "Near Barbaros Boulevard", "Mert Neighbor", comments = mutableListOf(Comment("Aylin Neighbor", "I am nearby with a car."))),
            HelpRequest("help-3", "Blankets for temporary shelter", "A small group at the community hall needs clean blankets tonight.", "Shelter", "LOW", "Community hall entrance desk", "Shelter Volunteer"),
            HelpRequest("help-4", "First aid kit needed near the bus stop", "Someone has a minor cut and needs bandages and antiseptic.", "Medical", "MEDIUM", "Barbaros bus stop", "Health Volunteer"),
            HelpRequest("help-5", "Power bank needed for one phone", "A resident needs to keep their phone on for emergency calls.", "Other", "LOW", "Apartment Block C lobby", "Mina Neighbor")
        )
        val offers = mutableListOf(
            HelpOffer("offer-1", "Can bring bottled water", "I have extra sealed water bottles and can walk them nearby.", "Food", "Available now", "Aylin Neighbor"),
            HelpOffer("offer-2", "Car available for pharmacy trips", "I can drive one person at a time to nearby pharmacies.", "Transport", "Available this afternoon", "Mert Neighbor"),
            HelpOffer("offer-3", "Basic first aid support", "I can help with minor cuts and checking supplies.", "Medical", "Available nearby", "Can Expert"),
            HelpOffer("offer-4", "Spare blankets", "Clean blankets are available at the community hall desk.", "Shelter", "Available tonight", "Shelter Volunteer"),
            HelpOffer("offer-5", "Phone charging cable", "I have a spare USB-C cable at the charging station.", "Other", "Available now", "Mina Neighbor")
        )
        val contacts = mutableListOf(
            OfflineContact("Family contact", "+90 555 010 1122"),
            OfflineContact("Building manager", "+90 555 010 3344")
        )
    }
}
