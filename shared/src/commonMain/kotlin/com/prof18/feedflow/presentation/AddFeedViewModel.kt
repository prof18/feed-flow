package com.prof18.feedflow.presentation

import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.model.AddFeedResponse
import com.prof18.feedflow.domain.model.FeedAddedState
import com.prof18.feedflow.utils.sanitizeUrl
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val feedManagerRepository: FeedManagerRepository,
) : BaseViewModel() {

    private var feedUrl: String = ""
    private val feedAddedMutableState: MutableSharedFlow<FeedAddedState> = MutableSharedFlow()

    private val categoriesMutableState: MutableStateFlow<CategoriesState> = MutableStateFlow(CategoriesState())

    @NativeCoroutines
    val feedAddedState = feedAddedMutableState.asSharedFlow()

    @NativeCoroutines
    val categoriesState = categoriesMutableState.asStateFlow()

    init {
        initCategories()
    }

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrl = feedUrlTextFieldValue
        scope.launch {
            feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
        }
    }

    fun addFeed() {
        scope.launch {
            if (feedUrl.isNotEmpty()) {
                val url = sanitizeUrl(feedUrl)
                val categoryName = getSelectedCategory()

                when (val feedResponse = feedRetrieverRepository.fetchSingleFeed(url, categoryName)) {
                    is AddFeedResponse.FeedFound -> {
                        feedRetrieverRepository.addFeedSource(feedResponse)
                        feedAddedMutableState.emit(
                            FeedAddedState.FeedAdded(
                                message = StringDesc.ResourceFormatted(
                                    stringRes = MR.strings.feed_added_message,
                                    feedResponse.parsedFeedSource.title,
                                ),
                            ),
                        )
                        initCategories()
                    }

                    AddFeedResponse.EmptyFeed -> {
                        feedAddedMutableState.emit(
                            FeedAddedState.Error(
                                errorMessage = StringDesc.ResourceFormatted(
                                    stringRes = MR.strings.missing_title_and_link,
                                ),
                            ),
                        )
                    }

                    AddFeedResponse.NotRssFeed -> {
                        feedAddedMutableState.emit(
                            FeedAddedState.Error(
                                errorMessage = StringDesc.ResourceFormatted(
                                    stringRes = MR.strings.invalid_rss_url,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun clearAddDoneState() {
        scope.launch {
            feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
        }
    }

    fun onExpandCategoryClick() {
        categoriesMutableState.update { state ->
            state.copy(isExpanded = state.isExpanded.not())
        }
    }

    fun addNewCategory(categoryName: CategoryName) {
        scope.launch {
            feedManagerRepository.createCategory(categoryName)
            val newCategory = feedManagerRepository.getCategories()
                .firstOrNull {
                    it.title == categoryName.name
                }

            if (newCategory != null) {
                categoriesMutableState.update { state ->
                    val newCategoryItem = newCategory.toCategoryItem()
                    val newCategories = state.categories
                        .toMutableList()
                        .plus(newCategoryItem)
                        .distinctBy { categoryItem ->
                            categoryItem.id
                        }
                        .map { categoryItem ->
                            if (categoryItem.id == newCategoryItem.id) {
                                categoryItem.copy(
                                    isSelected = true,
                                )
                            } else {
                                categoryItem.copy(
                                    isSelected = false,
                                )
                            }
                        }

                    state.copy(
                        categories = newCategories,
                        isExpanded = false,
                        header = newCategoryItem.name,
                    )
                }
            }
        }
    }

    private fun getSelectedCategory(): FeedSourceCategory? {
        val category = categoriesState.value.categories.firstOrNull { it.isSelected } ?: return null
        return FeedSourceCategory(
            id = category.id,
            title = category.name,
        )
    }

    private fun onCategorySelected(categoryId: CategoryId) {
        categoriesMutableState.update { state ->
            var selectedCategoryName: String? = null
            val updatedCategories = state.categories.map { categoryItem ->
                if (categoryId.value == categoryItem.id) {
                    selectedCategoryName = categoryItem.name
                    categoryItem.copy(
                        isSelected = true,
                    )
                } else {
                    categoryItem.copy(
                        isSelected = false,
                    )
                }
            }
            state.copy(
                header = selectedCategoryName,
                isExpanded = false,
                categories = updatedCategories,
            )
        }
    }

    private fun FeedSourceCategory.toCategoryItem() =
        CategoriesState.CategoryItem(
            id = id,
            name = title,
            isSelected = false,
            onClick = { categoryId ->
                onCategorySelected(categoryId)
            },
        )

    private fun initCategories() {
        scope.launch {
            val categories = feedManagerRepository.getCategories()
            val categoriesState = CategoriesState(
                isExpanded = false,
                header = null,
                categories = categories.map { feedSourceCategory ->
                    feedSourceCategory.toCategoryItem()
                },
            )
            categoriesMutableState.update { categoriesState }
        }
    }
}
