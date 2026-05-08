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
            Screen.DASHBOARD -> button(getString(R.string.tutorial_exit), secondary = true) { finish() }
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
        skipButton = button(getString(R.string.tutorial_skip_guide), secondary = true) {
            hideGuide()
        }
        previousButton = button(getString(R.string.tutorial_previous), secondary = true) {
            if (guideIndex > 0) {
                guideIndex -= 1
                updateGuide()
            }
        }
        nextButton = button(getString(R.string.tutorial_next)) {
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

        showTutorialButton = button(getString(R.string.tutorial_show_tutorial), secondary = true) {
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
            getString(R.string.emergency_hub),
            listOf(
                GuideStep("welcome", getString(R.string.tutorial_dashboard_start_title), getString(R.string.tutorial_dashboard_start_text)),
                GuideStep("hub", getString(R.string.tutorial_dashboard_hub_title), getString(R.string.tutorial_dashboard_hub_text)),
                GuideStep("scenario", getString(R.string.tutorial_dashboard_situation_title), getString(R.string.tutorial_dashboard_situation_text)),
                GuideStep("forum", getString(R.string.tutorial_dashboard_forum_title), getString(R.string.tutorial_dashboard_forum_text)),
                GuideStep("help", getString(R.string.tutorial_dashboard_help_title), getString(R.string.tutorial_dashboard_help_text)),
                GuideStep("info", getString(R.string.tutorial_dashboard_info_title), getString(R.string.tutorial_dashboard_info_text))
            )
        )

        val hub = pill(getString(R.string.tutorial_hub_name), active = true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showMainAppOnly(getString(R.string.hub_label)) }
        }
        register("hub", hub)
        content.addView(hub, margins(bottom = 14))

        val welcome = dashboardWelcomeCard()
        register("welcome", welcome)
        content.addView(welcome, margins(bottom = 12))

        val scenario = cardBlock(getString(R.string.tutorial_dashboard_situation_title), getString(R.string.tutorial_scenario_body))
        register("scenario", scenario)
        content.addView(scenario, margins(bottom = 12))

        content.addView(featureRow(
            featureCard("💬", getString(R.string.feature_forum), getString(R.string.tutorial_forum_desc), "forum") { renderForum() },
            featureCard("🆘", getString(R.string.feature_help), getString(R.string.tutorial_help_desc), "help") { renderHelpList() }
        ))
        content.addView(featureRow(
            featureCard("👤", getString(R.string.feature_profile), getString(R.string.tutorial_sign_in_profile), null, enabled = false) { showMainAppOnly(getString(R.string.feature_profile)) },
            featureCard("📶", getString(R.string.feature_offline), getString(R.string.tutorial_offline_guidance), "info") { renderInfo() }
        ), margins(top = 12))
        content.addView(featureRow(
            featureCard("📡", getString(R.string.feature_offline_messages), getString(R.string.tutorial_sign_in_offline_messages), null, enabled = false) { showMainAppOnly(getString(R.string.feature_offline_messages)) },
            View(this)
        ), margins(top = 12))
        updateGuide()
    }

    private fun renderForum() {
        screen = Screen.FORUM
        buildShell(
            getString(R.string.forum_title),
            listOf(
                GuideStep("tabs", getString(R.string.tutorial_forum_tabs_title), getString(R.string.tutorial_forum_tabs_text)),
                GuideStep("sort", getString(R.string.tutorial_forum_sort_title), getString(R.string.tutorial_forum_sort_text)),
                GuideStep("posts", getString(R.string.tutorial_forum_posts_title), getString(R.string.tutorial_forum_posts_text)),
                GuideStep("create", getString(R.string.tutorial_forum_create_title), getString(R.string.tutorial_forum_create_text))
            )
        )
        val newPost = button(getString(R.string.tutorial_new_post)) { renderPostCreate() }
        register("create", newPost)
        content.addView(labelRow(getString(R.string.tutorial_all_hubs), getString(R.string.tutorial_hub_name)) { showMainAppOnly(getString(R.string.hub_label)) }, margins(bottom = 10))
        val tabs = forumTabs()
        register("tabs", tabs)
        content.addView(tabs, margins(bottom = 12))
        content.addView(newPost, margins(bottom = 10))

        val sortRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        sortRow.addView(pill(getString(R.string.sort_newest), active = selectedForumSort == "newest").apply {
            setOnClickListener {
                selectedForumSort = "newest"
                renderForum()
            }
        })
        sortRow.addView(pill(getString(R.string.tutorial_sort_most_liked), active = selectedForumSort == "most_liked").apply {
            setOnClickListener {
                selectedForumSort = "most_liked"
                renderForum()
            }
        })
        sortRow.addView(pill(getString(R.string.tutorial_sort_hot), active = selectedForumSort == "hot").apply {
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
            content.addView(cardBlock(getString(R.string.tutorial_no_posts_title), getString(R.string.tutorial_no_posts_body, selectedForumType.lowercase())), margins(bottom = 10))
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
            getString(R.string.tutorial_forum_post_title),
            listOf(
                GuideStep("summary", getString(R.string.tutorial_read_update_title), getString(R.string.tutorial_read_update_text)),
                GuideStep("votes", getString(R.string.tutorial_react_post_title), getString(R.string.tutorial_react_post_text)),
                GuideStep("comments", getString(R.string.tutorial_join_conversation_title), getString(R.string.tutorial_join_conversation_text))
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
        votes.addView(pill(getString(R.string.share)).apply {
            setOnClickListener { showMainAppOnly(getString(R.string.share)) }
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
            getString(R.string.tutorial_create_post_title),
            listOf(
                GuideStep("type", getString(R.string.tutorial_post_type_title), getString(R.string.tutorial_post_type_text)),
                GuideStep("title", getString(R.string.tutorial_clear_title_title), getString(R.string.tutorial_clear_title_text)),
                GuideStep("content", getString(R.string.tutorial_post_details_title), getString(R.string.tutorial_post_details_text)),
                GuideStep("images", getString(R.string.tutorial_add_images_title), getString(R.string.tutorial_add_images_text)),
                GuideStep("save", getString(R.string.tutorial_save_post_title), getString(R.string.tutorial_save_post_text))
            )
        )
        val titleInput = edit("Charging station open at the community center")
        val bodyInput = edit("Volunteers can help people charge phones until 18:00. Bring your own cable if possible.", minLines = 6)

        content.addView(labelRow(getString(R.string.tutorial_posting_to), getString(R.string.tutorial_hub_name)) { showMainAppOnly(getString(R.string.hub_label)) }, margins(bottom = 8))
        content.addView(text(getString(R.string.tutorial_share_update), 14, textSecondary), margins(bottom = 20))

        val typeSelector = toggleRow(
            listOf(getString(R.string.tab_global), getString(R.string.tab_standard), getString(R.string.tab_urgent)),
            activeIndex = when (selectedForumType) {
                "STANDARD" -> 1
                "URGENT" -> 2
                else -> 0
            }
        )
        register("type", typeSelector)
        content.addView(sectionLabel(getString(R.string.tutorial_post_type)))
        content.addView(typeSelector, margins(bottom = 18))

        register("title", titleInput)
        content.addView(labeled(getString(R.string.title_label), titleInput))
        register("content", bodyInput)
        content.addView(labeled(getString(R.string.tutorial_content), bodyInput))

        val images = formBlock(getString(R.string.images_label), listOf(getString(R.string.upload_from_device), getString(R.string.tutorial_image_urls)), getString(R.string.tutorial_optional))
        register("images", images)
        content.addView(images, margins(bottom = 16))

        val save = fullWidthButton(getString(R.string.tutorial_create_post_button)) {
            val title = titleInput.text.toString().trim()
            val body = bodyInput.text.toString().trim()
            if (title.isBlank() || body.isBlank()) {
                Toast.makeText(this, getString(R.string.tutorial_post_validation), Toast.LENGTH_SHORT).show()
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
        if (selectedHelpCategory == "All") {
            selectedHelpCategory = getString(R.string.tutorial_filter_all)
        }
        buildShell(
            getString(R.string.help_center_title),
            listOf(
                GuideStep("tabs", getString(R.string.tutorial_help_switch_title), getString(R.string.tutorial_help_switch_text)),
                GuideStep("filters", getString(R.string.tutorial_help_filter_title), getString(R.string.tutorial_help_filter_text)),
                GuideStep("requests", getString(R.string.tutorial_help_review_title), getString(R.string.tutorial_help_review_text)),
                GuideStep("create", getString(R.string.tutorial_help_create_title), getString(R.string.tutorial_help_create_text))
            )
        )
        content.addView(labelRow(getString(R.string.hub_label), getString(R.string.tutorial_hub_name)) { showMainAppOnly(getString(R.string.hub_label)) }, margins(bottom = 10))
        val tabs = segmentedTabs(
            first = getString(R.string.tab_requests),
            second = getString(R.string.tab_offers),
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
        val create = button(getString(R.string.tutorial_new)) { renderHelpCreate() }
        register("create", create)
        content.addView(create, margins(bottom = 10))
        val filters = chipScroller(listOf(getString(R.string.tutorial_filter_all), getString(R.string.tutorial_filter_medical), getString(R.string.tutorial_filter_food), getString(R.string.tutorial_filter_shelter), getString(R.string.tutorial_filter_transport), getString(R.string.tutorial_filter_other)))
        register("filters", filters)
        content.addView(filters, margins(bottom = 8))

        if (selectedHelpMode == "REQUESTS") {
            val visibleRequests = SampleData.requests.filter {
                helpCategoryMatches(it.category)
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
                helpCategoryMatches(it.category)
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
            getString(R.string.tutorial_help_detail_title),
            listOf(
                GuideStep("summary", getString(R.string.tutorial_review_request_title), getString(R.string.tutorial_review_request_text)),
                GuideStep("actions", getString(R.string.tutorial_manage_request_title), getString(R.string.tutorial_manage_request_text)),
                GuideStep("location", getString(R.string.tutorial_check_location_title), getString(R.string.tutorial_check_location_text)),
                GuideStep("comments", getString(R.string.tutorial_coordinate_comments_title), getString(R.string.tutorial_coordinate_comments_text))
            )
        )
        val summary = helpDetailCard(request)
        register("summary", summary)
        content.addView(summary, margins(bottom = 12))

        if (request.isMine) {
            val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            if (request.status != "RESOLVED") {
                actions.addView(button(getString(R.string.mark_resolved)) {
                    request.status = "RESOLVED"
                    renderHelpDetail()
                })
            }
            actions.addView(button(getString(R.string.delete), secondary = true) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.tutorial_delete_request_title))
                    .setMessage(getString(R.string.tutorial_delete_request_message))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        SampleData.requests.removeAll { it.id == request.id }
                        renderHelpList()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
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
            getString(R.string.tutorial_create_help_title),
            listOf(
                GuideStep("title", getString(R.string.tutorial_clear_title_title), getString(R.string.tutorial_clear_title_text)),
                GuideStep("description", getString(R.string.tutorial_help_desc_title), getString(R.string.tutorial_help_desc_text)),
                GuideStep("category", getString(R.string.tutorial_category_urgency_title), getString(R.string.tutorial_category_urgency_text)),
                GuideStep("location", getString(R.string.tutorial_location_details_title), getString(R.string.tutorial_location_details_text)),
                GuideStep("images", getString(R.string.tutorial_add_images_title), getString(R.string.tutorial_add_images_text)),
                GuideStep("save", getString(R.string.tutorial_review_save_title), getString(R.string.tutorial_review_save_text))
            )
        )
        val titleInput = edit("Need drinking water for an elderly neighbor")
        val descInput = edit("Our building has no running water. One elderly neighbor cannot walk to the distribution point.", minLines = 4)
        val locationInput = edit("Besiktas community center, Block B entrance")
        content.addView(text(getString(R.string.tutorial_help_intro), 14, textSecondary), margins(bottom = 20))
        register("title", titleInput)
        content.addView(labeled(getString(R.string.title_label), titleInput))
        register("description", descInput)
        content.addView(labeled(getString(R.string.help_description), descInput))

        val categoryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(dropdownLike(getString(R.string.tutorial_food_water)))
            addView(dropdownLike(getString(R.string.tutorial_medium_urgency)), margins(top = 10))
        }
        register("category", categoryCard)
        content.addView(sectionLabel(getString(R.string.tutorial_category_urgency)))
        content.addView(categoryCard, margins(bottom = 16))

        register("location", locationInput)
        content.addView(labeled(getString(R.string.location_label), locationInput))
        content.addView(button(getString(R.string.use_my_location), secondary = true) {
            showMainAppOnly(getString(R.string.use_my_location))
        }, margins(bottom = 16))

        val images = formBlock(getString(R.string.images_label), listOf(getString(R.string.tutorial_upload_gallery), getString(R.string.tutorial_take_photo)), getString(R.string.tutorial_optional))
        register("images", images)
        content.addView(images, margins(bottom = 16))

        val save = fullWidthButton(getString(R.string.submit)) {
            val title = titleInput.text.toString().trim()
            val desc = descInput.text.toString().trim()
            if (title.isBlank() || desc.isBlank()) {
                Toast.makeText(this, getString(R.string.tutorial_help_validation), Toast.LENGTH_SHORT).show()
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
                GuideStep("overview", getString(R.string.tutorial_offline_features_title), getString(R.string.tutorial_offline_features_text)),
                GuideStep("map", getString(R.string.tutorial_open_offline_map_title), getString(R.string.tutorial_open_offline_map_text)),
                GuideStep("checklist", getString(R.string.tutorial_open_checklist_title), getString(R.string.tutorial_open_checklist_text)),
                GuideStep("contacts", getString(R.string.tutorial_open_contacts_title), getString(R.string.tutorial_open_contacts_text))
            )
        )
        content.gravity = Gravity.CENTER_HORIZONTAL

        val overview = text(getString(R.string.offline_features_title), 24, textPrimary, bold = true).apply {
            gravity = Gravity.CENTER
        }
        register("overview", overview)
        content.addView(overview, margins(bottom = 24))

        val map = offlineFeatureButton(getString(R.string.offline_map)) { renderInfoMap() }
        register("map", map)
        content.addView(map, margins(bottom = 12))

        val checklist = offlineFeatureButton(getString(R.string.emergency_checklist)) { renderInfoChecklist() }
        register("checklist", checklist)
        content.addView(checklist, margins(bottom = 12))

        val contacts = offlineFeatureButton(getString(R.string.offline_contacts)) { renderInfoContacts() }
        register("contacts", contacts)
        content.addView(contacts)
        updateGuide()
    }

    private fun renderInfoChecklist() {
        screen = Screen.INFO_CHECKLIST
        buildShell(
            "",
            listOf(
                GuideStep("title", getString(R.string.tutorial_basic_skills_title), getString(R.string.tutorial_basic_skills_text)),
                GuideStep("displacement", getString(R.string.first_aid_displacement_title), getString(R.string.tutorial_displacement_text)),
                GuideStep("checking", getString(R.string.first_aid_checking_title), getString(R.string.tutorial_checking_text)),
                GuideStep("cpr", getString(R.string.tutorial_cpr_title), getString(R.string.tutorial_cpr_text))
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
                GuideStep("title", getString(R.string.tutorial_emergency_numbers_title), getString(R.string.tutorial_emergency_numbers_text)),
                GuideStep("general", getString(R.string.tutorial_call_services_title), getString(R.string.tutorial_call_services_text)),
                GuideStep("custom", getString(R.string.tutorial_custom_number_title), getString(R.string.tutorial_custom_number_text))
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
                GuideStep("map", getString(R.string.offline_map), getString(R.string.tutorial_open_offline_map_text)),
                GuideStep("openMap", getString(R.string.tutorial_open_map_title), getString(R.string.tutorial_open_map_text))
            )
        )
        val map = cardBlock(
            getString(R.string.offline_map),
            getString(R.string.tutorial_map_card_body)
        )
        register("map", map)
        content.addView(map, margins(bottom = 12))

        val openMap = fullWidthButton(getString(R.string.tutorial_open_offline_map_button)) {
            startActivity(Intent(this, MapActivity::class.java))
        }
        register("openMap", openMap)
        content.addView(openMap)
        updateGuide()
    }

    private fun dashboardWelcomeCard(): MaterialCardView {
        val card = card(radiusDp = 16)
        val layout = vertical(dp(20))
        layout.addView(text(getString(R.string.dashboard_welcome_format, getString(R.string.tutorial_neighbor)), 20, textPrimary, bold = true), margins(bottom = 10))
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        badges.addView(badge(getString(R.string.role_standard), accent))
        badges.addView(badge(getString(R.string.status_safe), accentSecondary))
        badges.addView(badge(getString(R.string.tutorial_hub_name), textSecondary))
        layout.addView(badges)
        card.addView(layout)
        return card
    }

    private fun forumTabs(): LinearLayout {
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(underlineTab(getString(R.string.tab_global), active = selectedForumType == "GLOBAL", color = ContextCompat.getColor(this, R.color.forum_global)).apply {
            setOnClickListener {
                selectedForumType = "GLOBAL"
                renderForum()
            }
        }, weightParams())
        tabs.addView(underlineTab(getString(R.string.tab_standard), active = selectedForumType == "STANDARD", color = ContextCompat.getColor(this, R.color.forum_standard)).apply {
            setOnClickListener {
                selectedForumType = "STANDARD"
                renderForum()
            }
        }, weightParams())
        tabs.addView(underlineTab(getString(R.string.tab_urgent), active = selectedForumType == "URGENT", color = ContextCompat.getColor(this, R.color.forum_urgent)).apply {
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
        stats.addView(pill(getString(R.string.share)).apply {
            setOnClickListener { showMainAppOnly(getString(R.string.share)) }
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
        badges.addView(badge(getString(R.string.tutorial_hub_name), textSecondary))
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
        section.addView(text(getString(R.string.tutorial_comments_count, comments.size), 18, textPrimary, bold = true), margins(bottom = 10))
        val inputCard = card(radiusDp = 12)
        val inputLayout = vertical(dp(14))
        val input = edit("", minLines = 2).apply { hint = getString(R.string.tutorial_write_comment) }
        inputLayout.addView(input, margins(bottom = 8))
        inputLayout.addView(button(getString(R.string.tutorial_post_comment)) {
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
            setOnClickListener { showMainAppOnly(getString(R.string.tutorial_changing_field)) }
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

    private fun helpCategoryMatches(category: String): Boolean {
        val selected = selectedHelpCategory
        if (selected == "All" || selected == getString(R.string.tutorial_filter_all)) return true
        return category.equals(selected, ignoreCase = true) ||
                category.contains(selected, ignoreCase = true) ||
                selected.equals(getString(R.string.tutorial_filter_medical), ignoreCase = true) && category.contains("Medical", ignoreCase = true) ||
                selected.equals(getString(R.string.tutorial_filter_food), ignoreCase = true) && category.contains("Food", ignoreCase = true) ||
                selected.equals(getString(R.string.tutorial_filter_shelter), ignoreCase = true) && category.contains("Shelter", ignoreCase = true) ||
                selected.equals(getString(R.string.tutorial_filter_transport), ignoreCase = true) && category.contains("Transport", ignoreCase = true) ||
                selected.equals(getString(R.string.tutorial_filter_other), ignoreCase = true) && category.contains("Other", ignoreCase = true)
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
            text = getString(R.string.tutorial_call)
            isAllCaps = false
            textSize = 12f
            cornerRadius = dp(8)
            minHeight = dp(40)
            setTextColor(ContextCompat.getColor(this@TutorialActivity, R.color.white))
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E63946"))
            setOnClickListener { showMainAppOnly(getString(R.string.tutorial_calling, title)) }
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
        Toast.makeText(this, getString(R.string.tutorial_main_app_only, feature), Toast.LENGTH_SHORT).show()
    }

    private fun updateGuide() {
        if (guideSteps.isEmpty()) {
            guideCard.visibility = View.GONE
            return
        }
        showTutorialButton.visibility = View.GONE
        val step = guideSteps[guideIndex]
        guideCount.text = getString(R.string.tutorial_step_count, guideIndex + 1, guideSteps.size)
        guideTitle.text = step.title
        guideText.text = step.text
        previousButton.isEnabled = guideIndex > 0
        nextButton.text = if (guideIndex == guideSteps.lastIndex) getString(R.string.tutorial_finish) else getString(R.string.tutorial_next)
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
